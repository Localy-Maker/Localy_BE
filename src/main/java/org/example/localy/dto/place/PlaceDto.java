package org.example.localy.dto.place;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PlaceDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceSimple {
        private Long placeId;
        private String placeName;
        private String category;
        private String address;
        private String thumbnailImage;
        private Double distance;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceDetail {
        private Long placeId;
        private String placeName;
        private String category;
        private String address;
        private Double latitude;
        private Double longitude;
        private String phoneNumber;
        private String openingHours;
        private List<String> images;
        private String shortDescription;
        private String longDescription;
        private Boolean isBookmarked;
        private Integer bookmarkCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookmarkItem {
        private Long bookmarkId;
        private Long placeId;
        private String placeName;
        private String category;
        private String address;
        private String thumbnailImage;
        private LocalDateTime bookmarkedAt;
        private Integer bookmarkCount;
        private String bookmarkedEmotion;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MissionBanner {
        private String emotionKeyword;
        private Integer totalMissions;
        private Integer completedMissions;
        private Integer progressPercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookmarkRequest {
        private String currentEmotion;
        private Double emotionScore;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookmarkResponse {
        private Boolean isBookmarked;
        private Integer bookmarkCount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookmarkListResponse {
        private List<BookmarkItem> bookmarks;
        private Boolean hasNext;
        private Long totalElements;
        private Long lastBookmarkId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HomeResponse {
        private MissionBanner missionBanner;
        private List<PlaceSimple> missionPlaces;
        private List<PlaceSimple> recommendedPlaces;
        private List<BookmarkItem> recentBookmarks;
    }
}