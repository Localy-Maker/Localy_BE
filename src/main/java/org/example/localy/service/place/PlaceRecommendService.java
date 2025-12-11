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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
                    if (place != null) {
                        recommendedPlaces.add(place);
                        if (recommendedPlaces.size() >= 5) break;
                    }
                } catch (Exception e) {
                    log.warn("장소 저장/업데이트 중 오류 발생 - contentId: {}, error: {}",
                            apiPlace.getContentid(), e.getMessage());
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
                if (place != null) {
                    allPlaces.add(place);
                }
            } catch (Exception e) {
                log.warn("장소 저장/업데이트 중 오류 발생 - contentId: {}, error: {}",
                        apiPlace.getContentid(), e.getMessage());
            }
        }

        if (allPlaces.isEmpty()) {
            log.warn("주변 API에서 유효한 장소를 찾지 못했습니다. GPT 추천을 건너뜁니다.");
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

        if (recommendedPlaces.isEmpty()) {
            log.warn("GPT가 추천한 장소 {}개 중 DB에 존재하는 장소가 없어 미션 생성을 건너뜁니다.", aiRecommendedList.size());
            return RecommendDto.RecommendResponse.builder()
                    .recommendedPlaces(List.of())
                    .missions(List.of())
                    .build();
        }

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

    public Place saveOrUpdatePlace(TourApiDto.LocationBasedItem apiPlace) {

        // 1. contentId 유효성 검사
        if (!StringUtils.hasText(apiPlace.getContentid())) {
            log.warn("TourAPI 응답에서 contentId 누락. title={}", apiPlace.getTitle());
            throw new CustomException(PlaceErrorCode.TOUR_API_ERROR);
        }

        String contentId = apiPlace.getContentid();

        // 2. 전화번호 정제
        String cleanedTel = cleanPhoneNumber(apiPlace.getTel());

        // 3. 기존 장소 있는지 확인
        Optional<Place> existing = placeRepository.findByContentId(contentId);
        if (existing.isPresent()) return existing.get();

        try {
            // 4. 상세 정보 조회
            TourApiDto.CommonItem commonItem = tourApiService.getCommonDetail(contentId);
            TourApiDto.IntroItem introItem = tourApiService.getIntroDetail(contentId, apiPlace.getContenttypeid());

            // 5. 카테고리 처리
            String category = CategoryMapper.getCategoryName(apiPlace.getContenttypeid(), apiPlace.getCat3());

            // 6. 오프닝 아워 파싱 (1번 방식: 그냥 텍스트 저장)
            String openingHours = extractOpeningHoursSafe(introItem, apiPlace.getContenttypeid());

            // 7. Place 엔티티 생성
            Place place = Place.builder()
                    .contentId(contentId)
                    .contentTypeId(String.valueOf(apiPlace.getContenttypeid()))
                    .title(apiPlace.getTitle())
                    .category(category)
                    .address(apiPlace.getAddr1() != null ? apiPlace.getAddr1() : "")
                    .addressDetail(apiPlace.getAddr2() != null ? apiPlace.getAddr2() : "")
                    .latitude(parseDouble(apiPlace.getMapy()))
                    .longitude(parseDouble(apiPlace.getMapx()))
                    .phoneNumber(cleanedTel)
                    .openingHours(openingHours != null ? openingHours : "")
                    .thumbnailImage(selectThumbnail(apiPlace))
                    .longDescription(commonItem != null && commonItem.getOverview() != null
                            ? commonItem.getOverview()
                            : "")
                    .bookmarkCount(0)
                    .build();

            // 8. 저장
            Place saved = placeRepository.save(place);
            log.info("Place 저장 성공 - placeId={}, contentId={}", saved.getId(), contentId);

            // 9. 이미지 저장
            saveImagesSafe(saved, contentId);

            return saved;

        } catch (Exception e) {
            log.error("Place 저장 실패 - contentId={}, error={}", contentId, e.getMessage(), e);
            throw e;
        }
    }


    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        return phoneNumber.trim();
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Double 파싱 실패: {}", value);
            return 0.0;
        }
    }

    private String selectThumbnail(TourApiDto.LocationBasedItem apiPlace) {
        if (StringUtils.hasText(apiPlace.getFirstimage2())) {
            return apiPlace.getFirstimage2();
        }
        if (StringUtils.hasText(apiPlace.getFirstimage())) {
            return apiPlace.getFirstimage();
        }
        return null;
    }

    private void saveImagesSafe(Place place, String contentId) {
        try {
            List<TourApiDto.ImageItem> images = tourApiService.getImages(contentId);

            if (images == null || images.isEmpty()) return;

            int order = 0;
            for (TourApiDto.ImageItem img : images) {
                if (!StringUtils.hasText(img.getOriginimgurl())) continue;

                PlaceImage pi = PlaceImage.builder()
                        .place(place)
                        .imageUrl(img.getOriginimgurl())
                        .thumbnailUrl(img.getSmallimageurl())
                        .displayOrder(order++)
                        .build();

                placeImageRepository.save(pi);
            }
        } catch (Exception e) {
            log.warn("이미지 저장 실패 - placeId={}, message={}", place.getId(), e.getMessage());
        }
    }


    private String extractOpeningHoursSafe(TourApiDto.IntroItem introItem, String typeId) {
        if (introItem == null) return "";

        String c = String.valueOf(typeId);

        return switch (c) {
            case "12" -> Optional.ofNullable(introItem.getUsetime()).orElse("");
            case "14" -> Optional.ofNullable(introItem.getUsetimeculture()).orElse("");
            case "39" -> Optional.ofNullable(introItem.getOpentimefood()).orElse("");
            case "38" -> Optional.ofNullable(introItem.getOpentime()).orElse("");
            default -> "";
        };
    }


    private String generateRecommendReason(String emotionKeyword, String category) {
        return String.format("%s 느낌에 어울리는 장소입니다", emotionKeyword);
    }
}