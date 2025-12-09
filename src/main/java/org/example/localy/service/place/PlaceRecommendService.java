package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.constant.EmotionConstants;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.dto.place.TourApiDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.util.CategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final GPTService gptService;

    // 감정 기반 장소 추천
    @Transactional
    public RecommendDto.RecommendResponse recommendPlaces(Users user, Double latitude, Double longitude) {
        // EmotionDataService가 Score(0-100)와 DominantEmotion(GPT가 뽑은 세부 감정 단어)를 반환한다고 가정
        RecommendDto.EmotionData emotionData = emotionDataService.getCurrentEmotion(user);

        if (Boolean.TRUE.equals(emotionData.getIsHomesickMode())) {
            log.info("고향 기반 장소 추천 시작: userId={}", user.getId());
            return recommendHomesickPlaces(user, latitude, longitude, emotionData);
        }

        // dominantEmotion은 이제 GPT가 뽑은 세부 감정 단어이거나, Score에 따른 기존 감정 키워드 중 하나임
        log.info("일반 감정 기반 장소 추천 시작: userId={}, emotionScore={}",
                user.getId(), emotionData.getEmotionScore());
        return recommendEmotionBasedPlaces(user, latitude, longitude, emotionData);
    }

    private RecommendDto.RecommendResponse recommendHomesickPlaces(
            Users user, Double latitude, Double longitude, RecommendDto.EmotionData emotionData) {

        String nationality = user.getNationality() != null ? user.getNationality() : "아시아";

        String baseNationalityKeyword = nationality.split(" ")[0];

        List<String> keywordsToSearch;
        if (baseNationalityKeyword.equals("직접") || baseNationalityKeyword.equals("아시아") || baseNationalityKeyword.equals("기타")) {
            keywordsToSearch = List.of("아시아 마트", "국제", "외국인 거리", "전통 시장");
        } else {
            keywordsToSearch = List.of(baseNationalityKeyword);
        }

        List<Place> recommendedPlaces = new ArrayList<>();

        for (String keyword : keywordsToSearch) {
            List<TourApiDto.LocationBasedItem> apiPlaces = tourApiService.searchByKeyword(keyword, null);
            for (TourApiDto.LocationBasedItem apiPlace : apiPlaces) {
                try {
                    Place place = saveOrUpdatePlace(apiPlace);
                    recommendedPlaces.add(place);
                    if (recommendedPlaces.size() >= 5) break;
                } catch (CustomException e) {
                    log.warn("장소 저장/업데이트 중 오류 발생 (TourAPI 상세조회 실패): {}", e.getMessage());
                }
            }
            if (recommendedPlaces.size() >= 5) break;
        }

        List<RecommendDto.MissionItem> missions = List.of();

        // 추천 이유 개인화
        String personalizedReason = (baseNationalityKeyword.equals("아시아") || baseNationalityKeyword.equals("기타"))
                ? "고향의 정취를 느낄 수 있는 장소입니다."
                : baseNationalityKeyword + "의 정취를 느낄 수 있는 장소입니다.";


        // 응답 DTO 생성
        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason(personalizedReason)
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

    //@Transactional
    private RecommendDto.RecommendResponse recommendEmotionBasedPlaces(
            Users user, Double latitude, Double longitude, RecommendDto.EmotionData emotionData) {

        // dominantEmotion은 이제 GPT가 대화에서 추출한 가장 가까운 '세부 감정 단어'일 수 있습니다.
        String dominantEmotionKeyword = emotionData.getDominantEmotion();

        List<TourApiDto.LocationBasedItem> apiPlaces =
                tourApiService.getLocationBasedList(latitude, longitude, 5000, null);

        List<Place> allPlaces = new ArrayList<>();
        for (TourApiDto.LocationBasedItem apiPlace : apiPlaces) {

            try {
                Place place = saveOrUpdatePlace(apiPlace);
                allPlaces.add(place);
            } catch (CustomException e) {
                log.warn("장소 저장/업데이트 중 오류 발생 (TourAPI 상세조회 실패): {}", e.getMessage());

            }
        }

        if (allPlaces.isEmpty()) {
            return RecommendDto.RecommendResponse.builder()
                    .recommendedPlaces(List.of())
                    .missions(List.of())
                    .build();
        }

        GPTService.PlaceRecommendationResult aiResult =
                gptService.getRecommendedPlacesByEmotion(
                        allPlaces, dominantEmotionKeyword, user.getInterests());

        List<GPTService.PlaceRecommendationResult.RecommendedPlace> aiRecommendedList =
                aiResult.getRecommendedPlaces();

        List<Long> recommendedPlaceIds = aiRecommendedList.stream()
                .map(GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId)
                .filter(Objects::nonNull) // null ID 필터링
                .collect(Collectors.toList());

        Map<Long, Place> placeMap = placeRepository.findAllById(recommendedPlaceIds).stream()
                .collect(Collectors.toMap(Place::getId, p -> p));

        List<Place> recommendedPlaces = recommendedPlaceIds.stream()
                .filter(placeMap::containsKey)
                .map(placeMap::get)
                .collect(Collectors.toList());

        List<RecommendDto.MissionItem> missions = List.of();

        // 응답 DTO 매핑
        Map<Long, String> reasonMap = aiRecommendedList.stream()
                .filter(rec -> rec.getPlaceId() != null)
                .collect(Collectors.toMap(
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId,
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getReason
                ));
        Map<Long, Double> scoreMap = aiRecommendedList.stream()
                .filter(rec -> rec.getPlaceId() != null)
                .collect(Collectors.toMap(
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getPlaceId,
                        GPTService.PlaceRecommendationResult.RecommendedPlace::getMatchScore
                ));

        List<RecommendDto.RecommendedPlace> result = recommendedPlaces.stream()
                .map(place -> RecommendDto.RecommendedPlace.builder()
                        .placeId(place.getId())
                        .reason(reasonMap.getOrDefault(place.getId(), generateRecommendReason(dominantEmotionKeyword, place.getCategory())))
                        .matchScore(scoreMap.getOrDefault(place.getId(), 0.85))
                        .build())
                .collect(Collectors.toList());

        return RecommendDto.RecommendResponse.builder()
                .recommendedPlaces(result)
                .missions(missions)
                .build();
    }

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

        return switch (contentTypeId) {
            case "12" -> introItem.getUsetime();
            case "14" -> introItem.getUsetimeculture();
            case "39" -> introItem.getOpentimefood();
            case "38" -> introItem.getOpentime();
            default -> null;
        };
    }

    // 테스트용
    private String generateShortDescription(String category) {
        return "새로운 경험을 시작해보세요";
    }

    // 테스트용
    // 세부 감정 단어를 사용하여 추천 이유 생성
    private String generateRecommendReason(String emotionKeyword, String category) {
        return String.format("%s 느낌에 어울리는 장소입니다", emotionKeyword);
    }
}