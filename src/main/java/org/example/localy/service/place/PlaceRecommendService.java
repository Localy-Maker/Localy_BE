package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.entity.Users;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.GPTService;
import org.example.localy.service.mission.MissionService;
import org.example.localy.util.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendService {

    private final TourApiService tourApiService;
    private final EmotionDataService emotionDataService;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;
    private final MissionService missionService;
    private final GPTService gptService;

    // 감정 기반 장소 추천
    @Transactional
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        RecommendDto.EmotionData emotionData = emotionDataService.getCurrentEmotion(user);

        if (emotionData == null || emotionData.getEmotions() == null) {
            throw new CustomException(PlaceErrorCode.EMOTION_DATA_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(emotionData.getIsHomesickMode())) {
            log.info("고향 기반 장소 추천 시작: userId={}", user.getId());
            return recommendHomesickPlaces(user, latitude, longitude, emotionData);
        }

        log.info("일반 감정 기반 장소 추천 시작: userId={}, emotion={}",
                user.getId(), emotionData.getDominantEmotion());
        return recommendEmotionBasedPlaces(user, latitude, longitude, emotionData);
    }

    private RecommendDto.RecommendResponse recommendHomesickPlaces(
            Users user, Double latitude, Double longitude, RecommendDto.EmotionData emotionData) {

        String nationality = user.getNationality() != null ? user.getNationality() : "아시아";
        List<String> keywords = getHomesickKeywords(nationality);

        List<Place> recommendedPlaces = new ArrayList<>();

        for (String keyword : keywords) {
            List<TourApiDto.LocationBasedItem> apiPlaces = tourApiService.searchByKeyword(keyword, null);
            for (TourApiDto.LocationBasedItem apiPlace : apiPlaces) {
                Place place = saveOrUpdatePlace(apiPlace);
                recommendedPlaces.add(place);
                if (recommendedPlaces.size() >= 5) break;
            }
            if (recommendedPlaces.size() >= 5) break;
        }

        List<RecommendDto.MissionItem> missions =
                missionService.createMissionsForRecommendedPlaces(user, recommendedPlaces, "loneliness");

        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason("고향의 정취를 느낄 수 있는 장소입니다.")
                        .matchScore(0.95)
                        .build())
                .collect(Collectors.toList());

        emotionDataService.deactivateHomesickMode(user);
        log.info("고향 모드 비활성화 완료: userId={}", user.getId());

        return RecommendDto.RecommendResponse.builder()
                .recommendedPlaces(result)
                .missions(missions)
                .build();
    }

    // CASE B: 일반 감정 기반 명소 추천 (GPT 사용 통합)
    private RecommendDto.RecommendResponse recommendEmotionBasedPlaces(
            Users user, Double latitude, Double longitude, RecommendDto.EmotionData emotionData) {

        String dominantEmotion = emotionData.getDominantEmotion();

        // 1. 위치 기반 장소 목록 조회 및 DB 저장/업데이트
        List<TourApiDto.LocationBasedItem> apiPlaces =
                tourApiService.getLocationBasedList(latitude, longitude, 5000, null);

        List<Place> allPlaces = new ArrayList<>();
        for (TourApiDto.LocationBasedItem apiPlace : apiPlaces) {
            Place place = saveOrUpdatePlace(apiPlace);
            allPlaces.add(place);
        }

        if (allPlaces.isEmpty()) {
            return RecommendDto.RecommendResponse.builder()
                    .recommendedPlaces(List.of())
                    .missions(List.of())
                    .build();
        }

        // 2. 💡 GPT를 호출하여 장소 매칭 및 추천 이유 생성
        GPTService.PlaceRecommendationResult aiResult =
                gptService.getRecommendedPlacesByEmotion(
                        allPlaces, dominantEmotion, user.getInterests());

        List<GPTService.PlaceRecommendationResult.RecommendedPlace> aiRecommendedList =
                aiResult.getRecommendedPlaces();

        // 3. GPT가 추천한 Place ID를 기반으로 실제 Place 엔티티 조회 (순서 유지를 위해 ID 순으로 다시 조회)
        List<Long> recommendedPlaceIds = aiRecommendedList.stream()
                .map(GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId)
                .collect(Collectors.toList());

        // PlaceRepository는 순서를 보장하지 않으므로, ID 순서대로 정렬하기 위해 Map 사용
        Map<Long, Place> placeMap = placeRepository.findAllById(recommendedPlaceIds).stream()
                .collect(Collectors.toMap(Place::getId, p -> p));

        List<Place> recommendedPlaces = recommendedPlaceIds.stream()
                .filter(placeMap::containsKey)
                .map(placeMap::get)
                .collect(Collectors.toList());

        // 4. 미션 생성
        List<RecommendDto.MissionItem> missions = missionService.createMissionsForRecommendedPlaces(
                user, recommendedPlaces, dominantEmotion);

        // 5. 응답 DTO 매핑
        Map<Long, String> reasonMap = aiRecommendedList.stream()
                .collect(Collectors.toMap(
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId,
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getReason
                ));
        Map<Long, Double> scoreMap = aiRecommendedList.stream()
                .collect(Collectors.toMap(
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId,
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getMatchScore
                ));

        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason(reasonMap.getOrDefault(place.getId(), generateRecommendReason(dominantEmotion, place.getCategory())))
                        .matchScore(scoreMap.getOrDefault(place.getId(), 0.85))
                        .build())
                .collect(Collectors.toList());

        return RecommendDto.RecommendResponse.builder()
                .recommendedPlaces(result)
                .missions(missions)
                .build();
    }

    // 장소 저장 또는 업데이트
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Place saveOrUpdatePlace(TourApiDto.LocationBasedItem apiPlace) {
        Optional<Place> existingPlace = placeRepository.findByContentId(apiPlace.getContentid());

        if (existingPlace.isPresent()) {
            return existingPlace.get();
        }

        TourApiDto.CommonItem commonItem = tourApiService.getCommonDetail(apiPlace.getContentid());
        TourApiDto.IntroItem introItem = tourApiService.getIntroDetail(
                apiPlace.getContentid(), apiPlace.getContenttypeid());

        String category = CategoryMapper.getCategoryName(apiPlace.getContenttypeid(), apiPlace.getCat3());
        String openingHours = extractOpeningHours(introItem, apiPlace.getContenttypeid());

        Place place = Place.builder()
                .contentId(apiPlace.getContentid())
                .contentTypeId(apiPlace.getContenttypeid())
                .title(apiPlace.getTitle())
                .category(category)
                .address(apiPlace.getAddr1())
                .addressDetail(apiPlace.getAddr2())
                .latitude(Double.parseDouble(apiPlace.getMapy()))
                .longitude(Double.parseDouble(apiPlace.getMapx()))
                .phoneNumber(apiPlace.getTel())
                .openingHours(openingHours)
                .thumbnailImage(apiPlace.getFirstimage2() != null ? apiPlace.getFirstimage2() : apiPlace.getFirstimage())
                .shortDescription(generateShortDescription(category))
                .longDescription(commonItem != null ? commonItem.getOverview() : "")
                .bookmarkCount(0)
                .build();

        Place savedPlace = placeRepository.save(place);
        saveImages(savedPlace, apiPlace.getContentid());

        return savedPlace;
    }

    // 이미지 저장
    private void saveImages(Place place, String contentId) {
        List<TourApiDto.ImageItem> images = tourApiService.getImages(contentId);

        int order = 0;
        if (images != null) {
            for (TourApiDto.ImageItem image : images) {
                PlaceImage placeImage = PlaceImage.builder()
                        .place(place)
                        .imageUrl(image.getOriginimgurl())
                        .thumbnailUrl(image.getSmallimageurl())
                        .displayOrder(order++)
                        .build();
                placeImageRepository.save(placeImage);
            }
        }
    }

    // 영업시간 추출
    private String extractOpeningHours(TourApiDto.IntroItem introItem, String contentTypeId) {
        if (introItem == null) return null;

        switch (contentTypeId) {
            case "12": return introItem.getUsetime();
            case "14": return introItem.getUsetimeculture();
            case "39": return introItem.getOpentimefood();
            case "38": return introItem.getOpentime();
            default: return null;
        }
    }

    // 테스트용
    private String generateShortDescription(String category) {
        return "새로운 경험을 시작해보세요";
    }

    // 테스트용
    private String generateRecommendReason(String emotion, String category) {
        return String.format("%s에 어울리는 장소입니다", emotion);
    }

    // 테스트용
    private List<String> getHomesickKeywords(String nationality) {
        Map<String, List<String>> keywordMap = new HashMap<>();
        keywordMap.put("중국", Arrays.asList("중국", "차이나타운", "중식"));
        keywordMap.put("일본", Arrays.asList("일본", "일식", "라멘"));
        keywordMap.put("베트남", Arrays.asList("베트남", "쌀국수", "분짜"));
        keywordMap.put("미국", Arrays.asList("미국", "햄버거", "스테이크"));

        return keywordMap.getOrDefault(nationality, Arrays.asList("아시아", "국제"));
    }
}