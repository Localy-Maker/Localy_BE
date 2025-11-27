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
import org.example.localy.util.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceRecommendService {

    private final TourApiService tourApiService;
    private final EmotionDataService emotionDataService;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;

    // TODO: AiApiService 관련
    // private final AiApiService aiApiService;

    // 감정 기반 장소 추천
    @Transactional
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        RecommendDto.EmotionData emotionData = emotionDataService.getCurrentEmotion(user);

        if (emotionData == null || emotionData.getEmotions() == null) {
            throw new CustomException(PlaceErrorCode.EMOTION_DATA_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(emotionData.getIsHomesickMode())) {
            return recommendHomesickPlaces(user, latitude, longitude, emotionData);
        }

        return recommendEmotionBasedPlaces(user, latitude, longitude, emotionData);
    }

    // 고향 관련 장소 추천
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

        emotionDataService.deactivateHomesickMode(user);

        // TODO: [AI API 구현] 고향 관련 미션 생성하기
        // aiApiService.generateHomesickMissions(recommendedPlaces, nationality)
        List<RecommendDto.MissionItem> missions = Collections.emptyList();

        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason("고향의 정취를 느낄 수 있는 장소입니다.")
                        .matchScore(0.95)
                        .build())
                .collect(Collectors.toList());

        return RecommendDto.RecommendResponse.builder()
                .recommendedPlaces(result)
                .missions(missions)
                .build();
    }

    // 감정 기반 명소 추천
    private RecommendDto.RecommendResponse recommendEmotionBasedPlaces(
            Users user, Double latitude, Double longitude, RecommendDto.EmotionData emotionData) {

        String dominantEmotion = emotionData.getDominantEmotion();

        List<TourApiDto.LocationBasedItem> apiPlaces =
                tourApiService.getLocationBasedList(latitude, longitude, 5000, null);

        List<Place> allPlaces = new ArrayList<>();
        for (TourApiDto.LocationBasedItem apiPlace : apiPlaces) {
            Place place = saveOrUpdatePlace(apiPlace);
            allPlaces.add(place);
        }

        // TODO: [AI API 구현] 감정 기반 장소 매칭 및 추천 이유 생성하기
        // aiApiService.matchPlacesByEmotion(allPlaces, emotionData, user.getInterests())
        List<Place> recommendedPlaces = allPlaces.stream()
                .limit(7)
                .collect(Collectors.toList());

        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason(generateRecommendReason(dominantEmotion, place.getCategory()))
                        .matchScore(0.85)
                        .build())
                .collect(Collectors.toList());

        // TODO: [AI API 구현] 감정 기반 미션 생성하기
        // aiApiService.generateEmotionBasedMissions(recommendedPlaces, dominantEmotion)
        List<RecommendDto.MissionItem> missions = Collections.emptyList();

        return RecommendDto.RecommendResponse.builder()
                .recommendedPlaces(result)
                .missions(missions)
                .build();
    }

    // 장소 저장 또는 업데이트
    private Place saveOrUpdatePlace(TourApiDto.LocationBasedItem apiPlace) {
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

    // TODO: 제거
    private String generateShortDescription(String category) {
        return "새로운 경험을 시작해보세요";
    }

    // TODO: 제거
    private String generateRecommendReason(String emotion, String category) {
        return String.format("%s에 어울리는 장소입니다", emotion);
    }

    // TODO: 제거
    private List<String> getHomesickKeywords(String nationality) {
        return Arrays.asList("아시아", "국제");
    }
}