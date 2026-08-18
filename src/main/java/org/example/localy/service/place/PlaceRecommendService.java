package org.example.localy.service.place;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.dto.place.PlaceCandidateDto;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.dto.place.TourApiDetailDto;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Place;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.DistanceCalculator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
    private final EmotionDataService emotionDataService;
    private final ObjectMapper objectMapper;
    private final ExecutorService externalApiExecutor;

    // 목록 API가 좌표를 안 줄 때, 상세 API로 좌표를 보강하는 최대 호출 수 (응답 지연 방지)
    private static final int MAX_COORDINATE_ENRICH_CALLS = 15;

    // 추천 다양성을 위한 후보 풀 최소 크기 (이보다 적으면 이미 5개 이상이어도 API로 더 채움)
    private static final int MIN_CANDIDATE_POOL_SIZE = 15;

    // 목록 API로 새로 받아온 장소를 후보에 포함할 최대 거리 (km) — 위치 필터가 없는 API라 직접 걸러냄
    private static final double MAX_RECOMMEND_DISTANCE_KM = 10.0;

    // 카탈로그 전체 동기화 시 한 번에 순회할 최대 페이지 수 (외부 API 과호출 방지 안전장치)
    private static final int MAX_CATALOG_SYNC_PAGES = 20;
    private static final int CATALOG_SYNC_PAGE_SIZE = 50;

    // 카탈로그 전체 동기화 시 한 번에 좌표를 보강할 최대 개수 (요청 경로가 아니라 배치라 넉넉하게, 그래도 상한은 둠)
    private static final int MAX_CATALOG_SYNC_ENRICH_CALLS = 100;

    @Transactional
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        // 1. 실시간 감정 데이터 가져오기 (채팅 등으로 방금 바뀐 감정이 바로 반영되도록 Redis 기반 실시간 값 사용)
        RecommendDto.EmotionData latestEmotion = emotionDataService.getCurrentEmotion(user);

        // 2. DB에서 모든 장소 가져오기
        List<Place> allPlaces = placeRepository.findAll();

        // 3. 주변 장소 필터링 (동적 반경)
        List<Place> nearbyPlaces = findNearbyPlacesWithDynamicRadius(allPlaces, latitude, longitude);

        log.info("현재 위치 주변 DB 내 장소 개수: {}", nearbyPlaces.size());

        // 4. 추천 다양성을 위해 후보 풀이 작으면 API 호출하여 추가 (이미 5개 이상이어도 보강)
        if (nearbyPlaces.size() < MIN_CANDIDATE_POOL_SIZE) {
            log.info("추천 후보가 부족하여 API를 새로 호출합니다. (현재: {}개)", nearbyPlaces.size());
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

                // 목록 API는 좌표를 주지 않으므로, 좌표 있는/없는 장소를 분리
                List<Place> placesWithCoords = newPlaces.stream()
                        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                        .collect(Collectors.toList());
                List<Place> placesWithoutCoords = newPlaces.stream()
                        .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
                        .collect(Collectors.toList());

                // 부족분만큼 상세 API로 좌표를 보강 (최대 호출 수 제한, 서로 독립적인 호출이라 병렬 처리)
                int needed = MIN_CANDIDATE_POOL_SIZE - nearbyPlaces.size();
                List<Place> enrichCandidates = placesWithoutCoords.stream()
                        .limit(MAX_COORDINATE_ENRICH_CALLS)
                        .collect(Collectors.toList());

                List<CompletableFuture<Place>> enrichFutures = enrichCandidates.stream()
                        .map(place -> CompletableFuture.supplyAsync(() -> enrichPlaceCoordinates(place), externalApiExecutor))
                        .collect(Collectors.toList());

                // 외부 API 호출 결과(join)를 모은 뒤, DB 저장은 JPA 세션이 있는 이 스레드에서 순차 수행
                List<Place> enrichedPlaces = enrichFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .map(placeRepository::save)
                        .collect(Collectors.toList());

                placesWithCoords.addAll(enrichedPlaces);

                if (needed > 0) {
                    log.info("좌표 없는 장소 {}개 중 {}건을 상세 API로 좌표 보강 시도하여 {}건 성공했습니다.",
                            placesWithoutCoords.size(), enrichCandidates.size(), enrichedPlaces.size());
                }

                // 목록 API는 위치 필터가 없어 서울 전역이 섞여 나올 수 있으므로,
                // 실제 거리를 계산해 반경 밖 장소는 후보에서 제외한 뒤 거리순으로 추가
                List<Place> nearbyFromApi = placesWithCoords.stream()
                        .filter(p -> DistanceCalculator.calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude()) <= MAX_RECOMMEND_DISTANCE_KM)
                        .sorted((p1, p2) -> {
                            double dist1 = DistanceCalculator.calculateDistance(latitude, longitude, p1.getLatitude(), p1.getLongitude());
                            double dist2 = DistanceCalculator.calculateDistance(latitude, longitude, p2.getLatitude(), p2.getLongitude());
                            return Double.compare(dist1, dist2);
                        })
                        .collect(Collectors.toList());

                nearbyPlaces.addAll(nearbyFromApi);
                log.info("좌표 있는 장소 {}개 중 {}km 이내 {}개 추가. API 호출 후 총 장소 개수: {}",
                        placesWithCoords.size(), MAX_RECOMMEND_DISTANCE_KM, nearbyFromApi.size(), nearbyPlaces.size());
            }
        }

        // 5. 여전히 장소가 부족하면 좌표 없는 장소라도 추가 (거리를 알 수 없는 장소만, 먼 장소는 제외)
        if (nearbyPlaces.size() < 5) {
            log.warn("좌표 있는 장소가 부족합니다. 좌표 없는 장소도 포함합니다.");
            List<Place> placesWithoutCoords = allPlaces.stream()
                    .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
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
                latestEmotion.getDominantEmotion(),
                user.getInterests()
        );

        // 9. 추천 결과 가공 및 응답
        return convertToRecommendResponse(aiResult, latestEmotion);
    }

    /**
     * 동적 반경으로 주변 장소 찾기 (3km → 5km → MAX_RECOMMEND_DISTANCE_KM 순으로 확장, 그 이상은 확장하지 않음)
     */
    private List<Place> findNearbyPlacesWithDynamicRadius(List<Place> allPlaces, Double latitude, Double longitude) {
        double[] radii = {3.0, 5.0, MAX_RECOMMEND_DISTANCE_KM}; // km 단위

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

        // MAX_RECOMMEND_DISTANCE_KM 내에도 5개가 안 되면, 더 멀리 확장하지 않고
        // 그 범위 안에 있는 장소만(5개 미만이라도) 가까운 순으로 반환
        List<Place> withinMaxDistance = allPlaces.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .filter(p -> DistanceCalculator.calculateDistance(latitude, longitude, p.getLatitude(), p.getLongitude()) <= MAX_RECOMMEND_DISTANCE_KM)
                .sorted((p1, p2) -> {
                    double dist1 = DistanceCalculator.calculateDistance(latitude, longitude, p1.getLatitude(), p1.getLongitude());
                    double dist2 = DistanceCalculator.calculateDistance(latitude, longitude, p2.getLatitude(), p2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());

        log.warn("{}km 내 장소가 {}개뿐입니다. 더 멀리 확장하지 않습니다.", MAX_RECOMMEND_DISTANCE_KM, withinMaxDistance.size());
        return withinMaxDistance;
    }

    /**
     * 빈 추천 응답 생성 (추천 장소가 없을 때)
     */
    private RecommendDto.RecommendResponse createEmptyRecommendResponse(RecommendDto.EmotionData emotion) {
        return RecommendDto.RecommendResponse.builder()
                .emotion(emotion.getDominantEmotion())
                .score((double) emotion.getEmotionScore())
                .recommendations(new ArrayList<>())
                .missions(new ArrayList<>())
                .build();
    }

    private RecommendDto.RecommendResponse convertToRecommendResponse(GPTService.PlaceRecommendationResult aiResult, RecommendDto.EmotionData latestEmotion) {
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
                .emotion(latestEmotion.getDominantEmotion())
                .score((double) latestEmotion.getEmotionScore())
                .recommendations(recommendations)
                .missions(new ArrayList<>())
                .build();
    }

    private Place saveNewPlaceFromApi(TourApiDto.Data data) {
        try {
            if (data == null || data.getCid() == null) return null;

            Optional<Place> existing = placeRepository.findByContentId(data.getCid());
            if (existing.isPresent()) return existing.get();

            Double lat = null, lon = null;
            String address = null;

            if (data.getTraffic() != null) {
                address = data.getTraffic().getAdres();
                try {
                    if (org.springframework.util.StringUtils.hasText(data.getTraffic().getMap_position_y())) {
                        lat = Double.parseDouble(data.getTraffic().getMap_position_y());
                    }
                    if (org.springframework.util.StringUtils.hasText(data.getTraffic().getMap_position_x())) {
                        lon = Double.parseDouble(data.getTraffic().getMap_position_x());
                    }
                } catch (NumberFormatException e) {
                    log.warn("좌표 형식 오류: cid={}", data.getCid());
                }
            }

            Place place = Place.builder()
                    .contentId(data.getCid())
                    .title(data.getPost_sj())
                    .latitude(lat)
                    .longitude(lon)
                    .address(address)
                    .category(data.getCate_depth())
                    .shortDescription(data.getSumry())
                    .thumbnailImage(data.getMain_img())
                    .build();

            return placeRepository.save(place);
        } catch (Exception e) {
            log.error("저장 실패: cid={}, error={}", data.getCid(), e.getMessage());
            return null;
        }
    }

    /**
     * 목록 API에는 없는 좌표를 상세 API(getPlaceDetailByCid)로 조회해 Place 필드에 채워 넣는다.
     * 상세 API에도 좌표가 없으면 null을 반환한다.
     * DB 저장은 하지 않는다 — 외부 API 호출(느린 부분)만 병렬 워커 스레드에서 수행하고,
     * 저장은 JPA 세션이 바인딩된 원래 트랜잭션 스레드에서 별도로 수행해야 하기 때문.
     */
    private Place enrichPlaceCoordinates(Place place) {
        try {
            TourApiDetailDto response = tourApiService.getPlaceDetailByCid(place.getContentId());
            if (response == null || response.getData() == null) {
                return null;
            }

            TourApiDto.Data detail = response.getData();
            if (detail.getTraffic() == null
                    || !StringUtils.hasText(detail.getTraffic().getMap_position_y())
                    || !StringUtils.hasText(detail.getTraffic().getMap_position_x())) {
                return null;
            }

            place.setLatitude(Double.parseDouble(detail.getTraffic().getMap_position_y()));
            place.setLongitude(Double.parseDouble(detail.getTraffic().getMap_position_x()));
            place.setAddress(detail.getTraffic().getNew_adres());

            return place;
        } catch (NumberFormatException e) {
            log.warn("좌표 보강 중 파싱 오류: cid={}", place.getContentId());
            return null;
        } catch (Exception e) {
            log.error("좌표 보강 실패: cid={}, error={}", place.getContentId(), e.getMessage());
            return null;
        }
    }

    public Place saveOrUpdatePlace(String cid) {
        String redisKey = "place_detail:" + cid;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            // Object 타입 RedisTemplate은 타입 정보 없이 저장되어 Place가 아닌
            // LinkedHashMap으로 역직렬화되므로 ObjectMapper로 명시적으로 변환한다.
            try {
                return objectMapper.convertValue(cached, Place.class);
            } catch (IllegalArgumentException e) {
                log.warn("Redis 캐시 변환 실패, DB에서 다시 조회합니다. cid: {}", cid);
            }
        }

        Optional<Place> existingPlace = placeRepository.findByContentId(cid);

        // 상세정보가 이미 있으면 DB에서 반환
        if (existingPlace.isPresent() && StringUtils.hasText(existingPlace.get().getLongDescription())) {
            redisTemplate.opsForValue().set(redisKey, existingPlace.get(), 1, TimeUnit.DAYS);
            return existingPlace.get();
        }

        // 상세정보 없을 때만 VisitSeoul API 호출
        TourApiDetailDto response = tourApiService.getPlaceDetailByCid(cid);
        if (response == null || response.getData() == null) {
            log.warn("장소 상세 정보를 가져올 수 없습니다. cid: {}", cid);
            return existingPlace.orElse(null);
        }

        TourApiDto.Data d = response.getData();
        String cleanDesc = "";
        if (d.getPost_desc() != null) {
            cleanDesc = d.getPost_desc()
                    .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                    .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                    .replaceAll("<[^>]*>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
        }

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

    /**
     * VisitSeoul 콘텐츠 목록 API는 위치 기반 검색을 지원하지 않고 서울 전역 콘텐츠 카탈로그를 페이지 단위로 줄 뿐이라,
     * 사용자 요청 시점에 매번 1페이지만 불러오면 추천 후보가 항상 같은 좁은 풀로 고정된다.
     * 이 메서드는 스케줄러에서 주기적으로 호출되어 카탈로그 전체 페이지를 순회하며 신규 장소를 저장하고,
     * 좌표가 없는 장소들의 좌표를 상세 API로 보강해 DB의 지리적 커버리지를 넓힌다.
     * 요청 경로가 아니라 배치 작업이므로 하나의 트랜잭션으로 묶지 않고, 각 저장 호출이 개별 트랜잭션으로 커밋되게 둔다.
     */
    public void syncFullCatalog() {
        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        int savedCount = 0;

        while ((pageNo - 1) * CATALOG_SYNC_PAGE_SIZE < totalCount && pageNo <= MAX_CATALOG_SYNC_PAGES) {
            TourApiDto response = tourApiService.getContentsPage(pageNo, CATALOG_SYNC_PAGE_SIZE);
            List<TourApiDto.Data> pageData = response.getData();

            if (pageData == null || pageData.isEmpty()) {
                break;
            }

            if (response.getPaging() != null && response.getPaging().getTotalCount() != null) {
                totalCount = response.getPaging().getTotalCount();
            }

            for (TourApiDto.Data data : pageData) {
                if (saveNewPlaceFromApi(data) != null) {
                    savedCount++;
                }
            }

            pageNo++;
        }

        log.info("장소 카탈로그 동기화: {}페이지 순회, {}건 저장/확인 완료", pageNo - 1, savedCount);

        List<Place> placesWithoutCoords = placeRepository.findByLatitudeIsNullOrLongitudeIsNull().stream()
                .limit(MAX_CATALOG_SYNC_ENRICH_CALLS)
                .collect(Collectors.toList());

        if (placesWithoutCoords.isEmpty()) {
            log.info("장소 카탈로그 동기화: 좌표 보강이 필요한 장소가 없습니다.");
            return;
        }

        List<CompletableFuture<Place>> enrichFutures = placesWithoutCoords.stream()
                .map(place -> CompletableFuture.supplyAsync(() -> enrichPlaceCoordinates(place), externalApiExecutor))
                .collect(Collectors.toList());

        List<Place> enrichedPlaces = enrichFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .map(placeRepository::save)
                .collect(Collectors.toList());

        log.info("장소 카탈로그 동기화: 좌표 없는 장소 {}건 중 {}건 좌표 보강 성공",
                placesWithoutCoords.size(), enrichedPlaces.size());
    }

    /**
     * 키워드로 VisitSeoul 실제 콘텐츠를 검색하고, 각 후보의 실제 좌표를 상세 API로 조회해
     * 기준 좌표로부터 가까운 순으로 정렬해 반환한다. DB에 저장하지 않는 읽기 전용 조회다.
     * (예: 동네 이름으로 검색해서 그 근처에 실제로 있는 콘텐츠를 찾을 때 사용)
     */
    public List<PlaceCandidateDto> searchNearbyContent(String keyword, double latitude, double longitude, int limit) {
        TourApiDto response = tourApiService.searchContentsByKeyword(keyword, 30);
        List<TourApiDto.Data> candidates = response.getData();

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<PlaceCandidateDto>> futures = candidates.stream()
                .map(data -> CompletableFuture.supplyAsync(() -> {
                    TourApiDetailDto detailResponse = tourApiService.getPlaceDetailByCid(data.getCid());
                    if (detailResponse == null || detailResponse.getData() == null) {
                        return null;
                    }

                    TourApiDto.Data detail = detailResponse.getData();
                    if (detail.getTraffic() == null
                            || !StringUtils.hasText(detail.getTraffic().getMap_position_y())
                            || !StringUtils.hasText(detail.getTraffic().getMap_position_x())) {
                        return null;
                    }

                    try {
                        double lat = Double.parseDouble(detail.getTraffic().getMap_position_y());
                        double lon = Double.parseDouble(detail.getTraffic().getMap_position_x());
                        double distance = DistanceCalculator.calculateDistance(latitude, longitude, lat, lon);

                        return PlaceCandidateDto.builder()
                                .cid(data.getCid())
                                .title(detail.getPost_sj())
                                .category(detail.getCate_depth())
                                .address(detail.getTraffic().getNew_adres())
                                .latitude(lat)
                                .longitude(lon)
                                .distanceKm(DistanceCalculator.roundDistance(distance))
                                .build();
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }, externalApiExecutor))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(PlaceCandidateDto::getDistanceKm))
                .limit(limit)
                .collect(Collectors.toList());
    }
}