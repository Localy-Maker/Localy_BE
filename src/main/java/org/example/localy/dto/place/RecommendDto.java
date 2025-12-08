package org.example.localy.dto.place;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RecommendDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendRequest {
        private Long userId;
        private Map<String, Double> currentEmotions;
        private String dominantEmotion;
        private List<ConversationItem> conversationHistory;
        private LocationInfo location;
        private int emotionScore;
        private Boolean isHomesickMode;
        private String userNationality;
        private List<AvailablePlace> availablePlaces;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationItem {
        private String message;
        private LocalDateTime timestamp;
        private String emotion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AvailablePlace {
        private Long placeId;
        private String placeName;
        private String category;
        private List<String> keywords;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendResponse {
        private List<RecommendedPlace> recommendedPlaces;
        private List<MissionItem> missions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedPlace {
        private Long placeId;
        private String reason;
        private Double matchScore;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MissionItem {
        private Long placeId;
        private String missionTitle;
        private String missionDescription;
        private Integer points;
        private LocalDateTime expiresAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmotionData {
        private Map<String, Double> emotions;
        private String dominantEmotion;
        private int emotionScore;
        private LocalDateTime lastUpdated;
        private Boolean isHomesickMode;
        private LocalDateTime homesickActivatedAt;
    }
}
