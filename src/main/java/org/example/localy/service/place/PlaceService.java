package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.PlaceDto;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.entity.place.Bookmark;
import org.example.localy.entity.place.Mission;
import org.example.localy.entity.place.Place;
import org.example.localy.entity.place.PlaceImage;
import org.example.localy.entity.Users;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.PlaceErrorCode;
import org.example.localy.repository.place.BookmarkRepository;
import org.example.localy.repository.place.MissionRepository;
import org.example.localy.repository.place.PlaceImageRepository;
import org.example.localy.repository.place.PlaceRepository;
import org.example.localy.util.DistanceCalculator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {

    private final PlaceRecommendService recommendService;
    private final EmotionDataService emotionDataService;
    private final PlaceRepository placeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PlaceImageRepository placeImageRepository;
    private final MissionRepository missionRepository;

    // 로컬가이드 홈 조회
    @Transactional(readOnly = true)
    public PlaceDto.HomeResponse getHomeData(Users user, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new CustomException(PlaceErrorCode.LOCATION_REQUIRED);
        }

        // 미션 배너 데이터
        PlaceDto.MissionBanner missionBanner = getMissionBanner(user);

        // 미션 장소 (활성 미션의 장소들)
        List<PlaceDto.PlaceSimple> missionPlaces = getMissionPlaces(user, latitude, longitude);

        // 감정 기반 추천 장소
        List<PlaceDto.PlaceSimple> recommendedPlaces = getRecommendedPlaces(user, latitude, longitude);

        // 최근 북마크한 장소
        List<PlaceDto.BookmarkItem> recentBookmarks = getRecentBookmarks(user);

        return PlaceDto.HomeResponse.builder()
                .missionBanner(missionBanner)
                .missionPlaces(missionPlaces)
                .recommendedPlaces(recommendedPlaces)
                .recentBookmarks(recentBookmarks)
                .build();
    }

    // 미션 배너 데이터
    private PlaceDto.MissionBanner getMissionBanner(Users user) {
        RecommendDto.EmotionData emotionData = emotionDataService.getCurrentEmotion(user);

        LocalDateTime now = LocalDateTime.now();
        long totalMissions = missionRepository.countActiveByUser(user, now);
        long completedMissions = missionRepository.countCompletedByUser(user, now);

        int progressPercent = totalMissions > 0
                ? (int) ((completedMissions * 100) / totalMissions)
                : 0;

        return PlaceDto.MissionBanner.builder()
                .emotionKeyword(getEmotionKeyword(emotionData.getDominantEmotion()))
                .totalMissions((int) totalMissions)
                .completedMissions((int) completedMissions)
                .progressPercent(progressPercent)
                .build();
    }

    // 미션 장소 목록
    private List<PlaceDto.PlaceSimple> getMissionPlaces(Users user, Double latitude, Double longitude) {
        List<Mission> activeMissions = missionRepository.findActiveByUser(user, LocalDateTime.now());

        return activeMissions.stream()
                .map(Mission::getPlace)
                .map(place -> convertToPlaceSimple(place, latitude, longitude))
                .collect(Collectors.toList());
    }

    // 추천 장소 (미션 제외)
    private List<PlaceDto.PlaceSimple> getRecommendedPlaces(Users user, Double latitude, Double longitude) {
        try {
            RecommendDto.RecommendResponse recommendation =
                    recommendService.recommendPlaces(user, latitude, longitude);

            List<Long> missionPlaceIds = missionRepository.findActiveByUser(user, LocalDateTime.now())
                    .stream()
                    .map(mission -> mission.getPlace().getId())
                    .collect(Collectors.toList());

            return recommendation.getRecommendedPlaces().stream()
                    .filter(rec -> !missionPlaceIds.contains(rec.getPlaceId()))
                    .limit(5)
                    .map(rec -> {
                        Place place = placeRepository.findById(rec.getPlaceId())
                                .orElse(null);
                        return place != null ? convertToPlaceSimple(place, latitude, longitude) : null;
                    })
                    .filter(place -> place != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("추천 장소 조회 실패", e);
            return List.of();
        }
    }

    // 최근 북마크 생성 (최대 5개)
    private List<PlaceDto.BookmarkItem> getRecentBookmarks(Users user) {
        List<Bookmark> bookmarks = bookmarkRepository.findTop5ByUserOrderByCreatedAtDesc(
                user, PageRequest.of(0, 5));

        return bookmarks.stream()
                .map(this::convertToBookmarkItem)
                .collect(Collectors.toList());
    }

    // 장소 상세페이지 조회
    @Transactional(readOnly = true)
    public PlaceDto.PlaceDetail getPlaceDetail(Users user, Long placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(PlaceErrorCode.PLACE_NOT_FOUND));

        // 이미지 목록 조회
        List<PlaceImage> images = placeImageRepository.findByPlaceOrderByDisplayOrder(place);
        List<String> imageUrls = images.stream()
                .map(PlaceImage::getImageUrl)
                .collect(Collectors.toList());

        // 북마크 여부 확인
        boolean isBookmarked = bookmarkRepository.existsByUserAndPlace(user, place);

        return PlaceDto.PlaceDetail.builder()
                .placeId(place.getId())
                .placeName(place.getTitle())
                .category(place.getCategory())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .phoneNumber(place.getPhoneNumber())
                .openingHours(place.getOpeningHours())
                .images(imageUrls.isEmpty() ? List.of(place.getThumbnailImage()) : imageUrls)
                .shortDescription(place.getShortDescription())
                .longDescription(place.getLongDescription())
                .isBookmarked(isBookmarked)
                .bookmarkCount(place.getBookmarkCount())
                .build();
    }

    // 북마크 목록 조회
    @Transactional(readOnly = true)
    public PlaceDto.BookmarkListResponse getBookmarks(
            Users user, String sortType, Long lastBookmarkId, Integer size) {

        if (size == null || size <= 0) {
            size = 20;
        }

        PageRequest pageRequest = PageRequest.of(0, size);
        List<Bookmark> bookmarks;

        // 조회 (정렬 최신순/인기순)
        if ("POPULAR".equalsIgnoreCase(sortType)) {
            if (lastBookmarkId != null) {
                bookmarks = bookmarkRepository.findByUserAndIdLessThanOrderByPopularity(
                        user, lastBookmarkId, pageRequest);
            } else {
                bookmarks = bookmarkRepository.findByUserOrderByPopularity(user, pageRequest);
            }
        } else { // 기본값
            if (lastBookmarkId != null) {
                bookmarks = bookmarkRepository.findByUserAndIdLessThanOrderByCreatedAtDesc(
                        user, lastBookmarkId, pageRequest);
            } else {
                bookmarks = bookmarkRepository.findByUserOrderByCreatedAtDesc(user, pageRequest);
            }
        }

        // 다음 페이지 존재 여부 확인
        boolean hasNext = bookmarks.size() >= size;
        Long newLastBookmarkId = bookmarks.isEmpty() ? null : bookmarks.get(bookmarks.size() - 1).getId();

        // DTO 변환
        List<PlaceDto.BookmarkItem> bookmarkItems = bookmarks.stream()
                .map(this::convertToBookmarkItem)
                .collect(Collectors.toList());

        long totalElements = bookmarkRepository.countByUser(user);

        return PlaceDto.BookmarkListResponse.builder()
                .bookmarks(bookmarkItems)
                .hasNext(hasNext)
                .totalElements(totalElements)
                .lastBookmarkId(newLastBookmarkId)
                .build();
    }

    // 북마크 추가/취소
    @Transactional
    public PlaceDto.BookmarkResponse toggleBookmark(
            Users user, Long placeId, PlaceDto.BookmarkRequest request) {

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(PlaceErrorCode.PLACE_NOT_FOUND));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndPlace(user, place);

        if (existingBookmark.isPresent()) {
            // 북마크 취소
            bookmarkRepository.delete(existingBookmark.get());
            place.decrementBookmarkCount();
            placeRepository.save(place);

            return PlaceDto.BookmarkResponse.builder()
                    .isBookmarked(false)
                    .bookmarkCount(place.getBookmarkCount())
                    .build();
        } else {
            // 북마크 추가
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .place(place)
                    .bookmarkedEmotion(request.getCurrentEmotion())
                    .build();
            bookmarkRepository.save(bookmark);

            place.incrementBookmarkCount();
            placeRepository.save(place);

            return PlaceDto.BookmarkResponse.builder()
                    .isBookmarked(true)
                    .bookmarkCount(place.getBookmarkCount())
                    .build();
        }
    }

     // Place를 PlaceSimple로 변환
    private PlaceDto.PlaceSimple convertToPlaceSimple(Place place, Double latitude, Double longitude) {
        double distance = DistanceCalculator.calculateDistance(
                latitude, longitude, place.getLatitude(), place.getLongitude());

        return PlaceDto.PlaceSimple.builder()
                .placeId(place.getId())
                .placeName(place.getTitle())
                .category(place.getCategory())
                .address(place.getAddress())
                .thumbnailImage(place.getThumbnailImage())
                .distance(DistanceCalculator.roundDistance(distance))
                .build();
    }

    //북마크 변환
    private PlaceDto.BookmarkItem convertToBookmarkItem(Bookmark bookmark) {
        Place place = bookmark.getPlace();

        return PlaceDto.BookmarkItem.builder()
                .bookmarkId(bookmark.getId())
                .placeId(place.getId())
                .placeName(place.getTitle())
                .category(place.getCategory())
                .address(place.getAddress())
                .thumbnailImage(place.getThumbnailImage())
                .bookmarkedAt(bookmark.getCreatedAt())
                .bookmarkCount(place.getBookmarkCount())
                .bookmarkedEmotion(bookmark.getBookmarkedEmotion())
                .build();
    }

    // 감정 키워드 변환
    private String getEmotionKeyword(String emotion) {
        switch (emotion) {
            case "loneliness":
                return "외로움";
            case "joy":
                return "기쁨";
            case "sadness":
                return "슬픔";
            case "anger":
                return "분노";
            case "fear":
                return "두려움";
            case "surprise":
                return "놀람";
            default:
                return "평온";
        }
    }
}