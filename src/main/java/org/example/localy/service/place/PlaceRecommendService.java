package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.constant.EmotionConstants;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendService {
    private final TourApiService tourApiService;
    private final EmotionDataService emotionDataService;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final GPTService gptService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final org.example.localy.repository.EmotionWindowResultRepository emotionWindowResultRepository;

    // 기존 서비스(MissionService, PlaceService)가 호출하는 메서드 이름에 맞춰 추가/수정
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        // 1. 최신 감정 분석 결과 가져오기
        org.example.localy.entity.EmotionWindowResult latestEmotion = emotionWindowResultRepository
                .findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        user.getId(),
                        java.time.LocalDate.now().atStartOfDay(),
                        java.time.LocalDateTime.now()
                ).orElseThrow(() -> new CustomException(PlaceErrorCode.EMOTION_DATA_NOT_FOUND));

        // 2. DB에서 장소 조회
        List<Place> nearbyPlaces = placeRepository.findAll();
        log.info("DB에서 조회된 장소 개수: {}", nearbyPlaces.size());

        // 3. DB가 비어있으면 API로 장소 수집
        if (nearbyPlaces.isEmpty()) {
            log.info("DB가 비어있어 VisitSeoul API로 장소를 수집합니다.");
            List<TourApiDto.Data> apiList = tourApiService.getContentsList();

            if (apiList == null || apiList.isEmpty()) {
                log.error("VisitSeoul API에서 장소를 가져오지 못했습니다. 더미 데이터를 생성합니다.");

                // 더미 장소 생성 (테스트용)
                Place dummyPlace = Place.builder()
                        .contentId("DUMMY_001")
                        .title("서울 명동")
                        .category("관광지")
                        .thumbnailImage("https://example.com/dummy.jpg")
                        .latitude(37.5636)
                        .longitude(126.9838)
                        .shortDescription("테스트용 더미 장소")
                        .build();
                nearbyPlaces = List.of(placeRepository.save(dummyPlace));
            } else {
                log.info("API에서 {}개의 장소를 가져왔습니다. DB에 저장합니다.", apiList.size());
                nearbyPlaces = apiList.stream()
                        .map(this::saveNewPlaceFromApi)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                log.info("DB에 저장된 장소 개수: {}", nearbyPlaces.size());
            }
        }

        // 4. GPT 호출
        log.info("GPT에 전달할 장소 개수: {}", nearbyPlaces.size());
        GPTService.PlaceRecommendationResult aiResult = gptService.getRecommendedPlacesByEmotion(
                nearbyPlaces,
                latestEmotion.getEmotion(),
                user.getInterests()
        );

        log.info("GPT가 추천한 장소 개수: {}", aiResult.getRecommendedPlaces().size());

        // 5. 추천 결과 가공
        List<RecommendDto.PlaceRecommendation> updatedList = aiResult.getRecommendedPlaces().stream()
                .map(rec -> {
                    Place p = placeRepository.findById(rec.getPlaceId()).orElse(null);
                    if (p == null) {
                        log.warn("GPT가 추천한 Place ID={}를 DB에서 찾을 수 없습니다.", rec.getPlaceId());
                        return null;
                    }

                    Place detailedPlace = saveOrUpdatePlace(p.getContentId());

                    return RecommendDto.PlaceRecommendation.builder()
                            .placeId(p.getId())
                            .contentId(detailedPlace.getContentId())
                            .title(detailedPlace.getTitle())
                            .category(detailedPlace.getCategory())
                            .description(detailedPlace.getShortDescription())
                            .reason(rec.getReason())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("최종 추천 장소 개수: {}", updatedList.size());

        return RecommendDto.RecommendResponse.builder()
                .emotion(latestEmotion.getEmotion())
                .score(latestEmotion.getAvgScore())
                .recommendations(updatedList)
                .build();
    }

    private Place saveNewPlaceFromApi(TourApiDto.Data d) {
        if (d == null || d.getCid() == null) {
            log.warn("유효하지 않은 API 데이터입니다.");
            return null;
        }

        return placeRepository.findByContentId(d.getCid())
                .orElseGet(() -> {
                    Place newPlace = Place.builder()
                            .contentId(d.getCid())
                            .title(d.getPost_sj() != null ? d.getPost_sj() : "제목 없음")
                            .category(d.getCate_depth() != null ? d.getCate_depth() : "기타")
                            .thumbnailImage(d.getMain_img())
                            .shortDescription(d.getSumry())
                            .build();

                    Place saved = placeRepository.save(newPlace);
                    log.info("새 장소 저장 완료. cid: {}, title: {}", saved.getContentId(), saved.getTitle());
                    return saved;
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
        if (response == null || response.getData() == null) {
            log.warn("장소 상세 정보를 가져올 수 없습니다. cid: {}", cid);
            return existingPlace.orElse(null);
        }

        TourApiDto.Data d = response.getData();
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