package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Place;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.DistanceCalculator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendService {
    private final TourApiService tourApiService;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final GPTService gptService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmotionWindowResultRepository emotionWindowResultRepository;

    @Transactional
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        // 1. 오늘 기준 최신 감정 분석 결과 가져오기 (없으면 기본값 사용)
        EmotionWindowResult latestEmotion = emotionWindowResultRepository
                .findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        user.getId(),
                        java.time.LocalDate.now().atStartOfDay(),
                        java.time.LocalDateTime.now()
                ).orElseGet(() -> {
                    log.warn("사용자 {}의 감정 데이터가 없습니다. 기본 감정 데이터를 사용합니다.", user.getId());
                    return createDefaultEmotion(user);
                });

        // 2. DB에서 모든 장소 가져오기
        List<Place> allPlaces = placeRepository.findAll();

        // 3. 주변 장소 필터링 (동적 반경)
        List<Place> nearbyPlaces = findNearbyPlacesWithDynamicRadius(allPlaces, latitude, longitude);

        log.info("현재 위치 주변 DB 내 장소 개수: {}", nearbyPlaces.size());

        // 4. 주변 장소 부족 시 API 호출하여 추가
        if (nearbyPlaces.size() < 5) {
            log.info("데이터 부족으로 API를 새로 호출합니다. (현재: {}개)", nearbyPlaces.size());
            List<TourApiDto.Data> apiList = tourApiService.getContentsList();

            if (apiList != null && !apiList.isEmpty()) {
                log.info("API에서 {}개의 데이터를 받았습니다.", apiList.size());

                List<Place> newPlaces = new ArrayList<>();
                for (TourApiDto.Data data : apiList) {
                    Place saved = saveNewPlaceFromApi(data);
                    if (saved != null) {
                        newPlaces.add(saved);
                    }
                }

                log.info("{}개의 새 장소를 저장했습니다.", newPlaces.size());

                // 좌표 있는 장소만 거리순 정렬
                List<Place> placesWithCoords = newPlaces.stream()
                        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                        .sorted((p1, p2) -> {
                            double dist1 = DistanceCalculator.calculateDistance(latitude, longitude, p1.getLatitude(), p1.getLongitude());
                            double dist2 = DistanceCalculator.calculateDistance(latitude, longitude, p2.getLatitude(), p2.getLongitude());
                            return Double.compare(dist1, dist2);
                        })
                        .collect(Collectors.toList());

                nearbyPlaces.addAll(placesWithCoords);
                log.info("좌표 있는 장소 {}개 추가. API 호출 후 총 장소 개수: {}", placesWithCoords.size(), nearbyPlaces.size());

                // 여전히 부족하면 좌표 없는 장소도 추가
                if (nearbyPlaces.size() < 5) {
                    List<Place> placesWithoutCoords = newPlaces.stream()
                            .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
                            .limit(5 - nearbyPlaces.size())
                            .collect(Collectors.toList());

                    nearbyPlaces.addAll(placesWithoutCoords);
                    log.warn("좌표 없는 장소 {}개를 추가했습니다. 총 장소 개수: {}", placesWithoutCoords.size(), nearbyPlaces.size());
                }
            }
        }

        // 5. 여전히 장소가 부족하면 좌표 없는 장소라도 추가
        if (nearbyPlaces.size() < 5) {
            log.warn("좌표 있는 장소가 부족합니다. 좌표 없는 장소도 포함합니다.");
            List<Place> placesWithoutCoords = allPlaces.stream()
                    .filter(p -> !nearbyPlaces.contains(p))
                    .limit(5 - nearbyPlaces.size())
                    .collect(Collectors.toList());

            nearbyPlaces.addAll(placesWithoutCoords);
        }

        // 6. 최종적으로도 장소가 없으면 빈 응답
        if (nearbyPlaces.isEmpty()) {
            log.warn("추천 가능한 장소가 전혀 없습니다.");
            return createEmptyRecommendResponse(latestEmotion);
        }

        // 7. 최소 5개 확보 (부족하면 중복 허용)
        if (nearbyPlaces.size() < 5) {
            log.warn("장소가 {}개뿐입니다. 최대한 활용합니다.", nearbyPlaces.size());
        }

        // 8. GPT 추천 로직 가동
        GPTService.PlaceRecommendationResult aiResult = gptService.getRecommendedPlacesByEmotion(
                nearbyPlaces,
                latestEmotion.getEmotion(),
                user.getInterests()
        );

        // 9. 추천 결과 가공 및 응답
        return convertToRecommendResponse(aiResult, latestEmotion);
    }

    /**
     * 동적 반경으로 주변 장소 찾기 (3km → 5km → 10km → 50km 순으로 확장)
     */
    private List<Place> findNearbyPlacesWithDynamicRadius(List<Place> allPlaces, Double latitude, Double longitude) {
        double[] radii = {3.0, 5.0, 10.0, 50.0}; // km 단위

        for (double radius : radii) {
            List<Place> nearbyPlaces = allPlaces.stream()
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .filter(p -> DistanceCalculator.calculateDistance(
                            latitude, longitude, p.getLatitude(), p.getLongitude()) <= radius)
                    .collect(Collectors.toList());

            if (nearbyPlaces.size() >= 5) {
                log.info("{}km 반경 내 {}개 장소 발견", radius, nearbyPlaces.size());
                return nearbyPlaces;
            }
        }

        // 50km 내에도 없으면 전체에서 가장 가까운 순으로
        log.warn("50km 내 장소 부족. 전체에서 가장 가까운 장소 반환");
        return allPlaces.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .sorted((p1, p2) -> {
                    double dist1 = DistanceCalculator.calculateDistance(latitude, longitude, p1.getLatitude(), p1.getLongitude());
                    double dist2 = DistanceCalculator.calculateDistance(latitude, longitude, p2.getLatitude(), p2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());
    }

    /**
     * 기본 감정 데이터 생성 (감정 데이터가 없을 때 사용)
     */
    private EmotionWindowResult createDefaultEmotion(Users user) {
        EmotionWindowResult defaultEmotion = new EmotionWindowResult();
        defaultEmotion.setUserId(user.getId());
        defaultEmotion.setEmotion("중립");
        defaultEmotion.setAvgScore(50.0); // int -> Double로 수정
        defaultEmotion.setWindow("default");
        defaultEmotion.setSection(3); // 중립 구간
        defaultEmotion.setCreatedAt(java.time.LocalDateTime.now());
        return defaultEmotion;
    }

    /**
     * 빈 추천 응답 생성 (추천 장소가 없을 때)
     */
    private RecommendDto.RecommendResponse createEmptyRecommendResponse(EmotionWindowResult emotion) {
        return RecommendDto.RecommendResponse.builder()
                .emotion(emotion.getEmotion())
                .score(emotion.getAvgScore())
                .recommendations(new ArrayList<>())
                .missions(new ArrayList<>())
                .build();
    }

    private RecommendDto.RecommendResponse convertToRecommendResponse(GPTService.PlaceRecommendationResult aiResult, EmotionWindowResult latestEmotion) {
        // RecommendDto.PlaceRecommendation 빌더에 맞춰 필드 매핑
        List<RecommendDto.PlaceRecommendation> recommendations = aiResult.getRecommendedPlaces().stream()
                .map(rec -> {
                    Place p = placeRepository.findById(rec.getPlaceId()).orElse(null);
                    if (p == null) return null;
                    return RecommendDto.PlaceRecommendation.builder()
                            .placeId(p.getId())
                            .contentId(p.getContentId())
                            .title(p.getTitle())
                            .category(p.getCategory())
                            .description(p.getShortDescription()) // address 대신 description 사용
                            .reason(rec.getReason())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // RecommendDto.RecommendResponse 빌더 구조에 맞춰 생성
        return RecommendDto.RecommendResponse.builder()
                .emotion(latestEmotion.getEmotion())
                .score(latestEmotion.getAvgScore())
                .recommendations(recommendations)
                .missions(new ArrayList<>())
                .build();
    }

    private Place saveNewPlaceFromApi(TourApiDto.Data data) {
        try {
            if (data == null || data.getCid() == null) {
                log.warn("API 데이터가 null이거나 cid가 없습니다.");
                return null;
            }

            // 이미 존재하는 장소면 반환
            Optional<Place> existing = placeRepository.findByContentId(data.getCid());
            if (existing.isPresent()) {
                log.debug("이미 존재하는 장소입니다. cid: {}", data.getCid());
                return existing.get();
            }

            // 좌표 파싱 시도
            Double latitude = null;
            Double longitude = null;

            if (data.getTraffic() != null) {
                String mapY = data.getTraffic().getMap_position_y();
                String mapX = data.getTraffic().getMap_position_x();

                log.debug("장소 좌표 정보 - cid: {}, Y: {}, X: {}", data.getCid(), mapY, mapX);

                if (mapY != null && mapX != null && !mapY.isEmpty() && !mapX.isEmpty()) {
                    try {
                        latitude = Double.parseDouble(mapY);
                        longitude = Double.parseDouble(mapX);
                        log.debug("좌표 파싱 성공 - cid: {}, lat: {}, lng: {}", data.getCid(), latitude, longitude);
                    } catch (NumberFormatException e) {
                        log.warn("좌표 파싱 실패 - cid: {}, Y: {}, X: {}", data.getCid(), mapY, mapX);
                    }
                }
            } else {
                log.warn("Traffic 정보가 없습니다. cid: {}", data.getCid());
            }

            Place place = Place.builder()
                    .contentId(data.getCid())
                    .title(data.getPost_sj())
                    .longitude(Double.parseDouble(data.getTraffic().getMap_position_x())) // 경도
                    .latitude(Double.parseDouble(data.getTraffic().getMap_position_y()))  // 위도
                    .address(data.getTraffic().getAdres())
                    .build();

            Place saved = placeRepository.save(place);
            log.info("새 장소 저장 완료 - cid: {}, 제목: {}, 좌표: ({}, {})",
                    saved.getContentId(), saved.getTitle(), saved.getLatitude(), saved.getLongitude());

            return saved;

        } catch (Exception e) {
            log.error("장소 저장 실패 - cid: {}, error: {}", data.getCid(), e.getMessage(), e);
            return null;
        }
    }

    public Place saveOrUpdatePlace(String cid) {
        String redisKey = "place_detail:" + cid;
        Place cachedPlace = (Place) redisTemplate.opsForValue().get(redisKey);
        if (cachedPlace != null) return cachedPlace;

        Optional<Place> existingPlace = placeRepository.findByContentId(cid);

        // 상세정보가 이미 있으면 DB에서 반환
        if (existingPlace.isPresent() && StringUtils.hasText(existingPlace.get().getLongDescription())) {
            redisTemplate.opsForValue().set(redisKey, existingPlace.get(), 1, TimeUnit.DAYS);
            return existingPlace.get();
        }

        // 상세정보 없을 때만 VisitSeoul API 호출
        TourApiDto response = tourApiService.getPlaceDetailByCid(cid);
        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            log.warn("장소 상세 정보를 가져올 수 없습니다. cid: {}", cid);
            return existingPlace.orElse(null);
        }

        // ⭐ data가 List이므로 첫 번째 항목 가져오기
        TourApiDto.Data d = response.getData().get(0);
        String cleanDesc = d.getPost_desc() != null ? d.getPost_desc().replaceAll("<[^>]*>", " ").trim() : "";

        Place place = existingPlace.orElseGet(() -> Place.builder().build());
        place.setContentId(d.getCid());
        place.setTitle(d.getPost_sj() != null ? d.getPost_sj() : "제목 없음");
        place.setCategory(d.getCate_depth() != null ? d.getCate_depth() : "기타");

        // null 처리 추가
        if (d.getTraffic() != null) {
            place.setAddress(d.getTraffic().getNew_adres());
            if (d.getTraffic().getMap_position_y() != null && d.getTraffic().getMap_position_x() != null) {
                try {
                    place.setLatitude(Double.parseDouble(d.getTraffic().getMap_position_y()));
                    place.setLongitude(Double.parseDouble(d.getTraffic().getMap_position_x()));
                } catch (NumberFormatException e) {
                    log.warn("좌표 파싱 실패. cid: {}", cid);
                }
            }
        }

        if (d.getExtra() != null) {
            place.setOpeningHours(d.getExtra().getCmmn_use_time());
            place.setPhoneNumber(d.getExtra().getCmmn_telno());
        }

        place.setLongDescription(cleanDesc);
        place.setShortDescription(d.getSumry() != null ? d.getSumry() : "");
        place.setThumbnailImage(d.getMain_img() != null ? d.getMain_img() : "");

        Place savedPlace = placeRepository.save(place);
        redisTemplate.opsForValue().set(redisKey, savedPlace, 1, TimeUnit.DAYS);
        return savedPlace;
    }
}