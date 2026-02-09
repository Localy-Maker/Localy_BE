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

        // 2. 현재 위치 반경(3km) 내 DB 장소 필터링
        List<Place> allPlaces = placeRepository.findAll();
        List<Place> nearbyPlaces = allPlaces.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .filter(p -> DistanceCalculator.calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude()) <= 3.0)
                .collect(Collectors.toList());

        log.info("현재 위치 주변 DB 내 장소 개수: {}", nearbyPlaces.size());

        // 3. 주변 장소 부족 시 API 강제 호출
        if (nearbyPlaces.size() < 5) {
            log.info("데이터 부족으로 API를 새로 호출합니다.");
            List<TourApiDto.Data> apiList = tourApiService.getContentsList();

            if (apiList != null && !apiList.isEmpty()) {
                List<Place> newPlaces = apiList.stream()
                        .map(this::saveNewPlaceFromApi)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                nearbyPlaces.addAll(newPlaces.stream()
                        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                        .filter(p -> DistanceCalculator.calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude()) <= 3.0)
                        .collect(Collectors.toList()));
            }
        }

        // 4. 여전히 장소가 부족하면 빈 응답 반환
        if (nearbyPlaces.isEmpty()) {
            log.warn("주변에 추천 가능한 장소가 없습니다.");
            return createEmptyRecommendResponse(latestEmotion);
        }

        // 5. GPT 추천 로직 가동
        GPTService.PlaceRecommendationResult aiResult = gptService.getRecommendedPlacesByEmotion(
                nearbyPlaces,
                latestEmotion.getEmotion(),
                user.getInterests()
        );

        // 6. 추천 결과 가공 및 응답
        return convertToRecommendResponse(aiResult, latestEmotion);
    }

    /**
     * 기본 감정 데이터 생성 (감정 데이터가 없을 때 사용)
     */
    private EmotionWindowResult createDefaultEmotion(Users user) {
        EmotionWindowResult defaultEmotion = new EmotionWindowResult();
        defaultEmotion.setUserId(user.getId());
        defaultEmotion.setEmotion("중립");
        defaultEmotion.setAvgScore(50.0);
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
                .emotion(latestEmotion.getEmotion()) // emotionKeyword -> emotion
                .score(latestEmotion.getAvgScore()) // score 필드 추가
                .recommendations(recommendations) // recommendedPlaces -> recommendations
                .missions(new ArrayList<>()) // missions 필드 초기화
                .build();
    }

    private Place saveNewPlaceFromApi(TourApiDto.Data data) {
        // TourApiDto.Data -> Place 수동 매핑 (toEntity 메서드 대체)
        return placeRepository.findByContentId(data.getCid())
                .orElseGet(() -> {
                    try {
                        return placeRepository.save(Place.builder()
                                .contentId(data.getCid())
                                .title(data.getPost_sj())
                                .category(data.getCate_depth())
                                .thumbnailImage(data.getMain_img())
                                .shortDescription(data.getSumry())
                                .overview(data.getPost_desc())
                                .latitude(data.getTraffic() != null && data.getTraffic().getMap_position_y() != null ?
                                        Double.parseDouble(data.getTraffic().getMap_position_y()) : null)
                                .longitude(data.getTraffic() != null && data.getTraffic().getMap_position_x() != null ?
                                        Double.parseDouble(data.getTraffic().getMap_position_x()) : null)
                                .build());
                    } catch (Exception e) {
                        log.error("장소 저장 실패: cid={}", data.getCid(), e);
                        return null;
                    }
                });
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

        // data가 List이므로 첫 번째 항목 가져오기
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