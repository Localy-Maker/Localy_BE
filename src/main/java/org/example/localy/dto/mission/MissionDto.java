package org.example.localy.dto.mission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class MissionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 홈 응답")
    public static class MissionHomeResponse {
        @Schema(description = "사용자 포인트 정보")
        private PointInfo pointInfo;

        @Schema(description = "참여 가능한 미션 목록")
        private List<MissionItem> availableMissions;

        @Schema(description = "참여 완료한 미션 목록")
        private List<MissionItem> completedMissions;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "포인트 정보")
    public static class PointInfo {
        @Schema(description = "지금까지 모은 포인트")
        private Integer totalPoints;

        @Schema(description = "사용 가능한 포인트")
        private Integer availablePoints;

        @Schema(description = "할당받은 미션 개수")
        private Integer assignedMissions;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 아이템")
    public static class MissionItem {
        @Schema(description = "미션 ID")
        private Long missionId;

        @Schema(description = "미션 이름")
        private String missionTitle;

        @Schema(description = "미션 마감기한")
        private LocalDateTime expiresAt;

        @Schema(description = "획득 가능한 포인트")
        private Integer points;

        @Schema(description = "완료 여부")
        private Boolean isCompleted;

        @Schema(description = "NEW 태그 표시 여부")
        private Boolean isNew;

        @Schema(description = "생성된 감정")
        private String emotion;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 상세 응답")
    public static class MissionDetailResponse {
        @Schema(description = "미션 ID")
        private Long missionId;

        @Schema(description = "미션 이름")
        private String missionTitle;

        @Schema(description = "미션 설명")
        private String missionDescription;

        @Schema(description = "미션 마감기한")
        private LocalDateTime expiresAt;

        @Schema(description = "획득 가능한 포인트")
        private Integer points;

        @Schema(description = "장소 정보")
        private PlaceInfo placeInfo;

        @Schema(description = "인증 가능 여부")
        private Boolean canVerify;

        @Schema(description = "사용자 현재 위치에서의 거리(km)")
        private Double distance;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 장소 정보")
    public static class PlaceInfo {
        @Schema(description = "장소 ID")
        private Long placeId;

        @Schema(description = "장소 이름")
        private String placeName;

        @Schema(description = "카테고리")
        private String category;

        @Schema(description = "주소")
        private String address;

        @Schema(description = "위도")
        private Double latitude;

        @Schema(description = "경도")
        private Double longitude;

        @Schema(description = "영업 시간")
        private String openingHours;

        @Schema(description = "한줄 소개")
        private String shortDescription;

        @Schema(description = "이미지 목록 (최대 3개)")
        private List<String> images;

        @Schema(description = "카카오맵 URL")
        private String kakaoMapUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "미션 인증 요청")
    public static class VerifyRequest {
        @Schema(description = "사용자 현재 위도")
        private Double latitude;

        @Schema(description = "사용자 현재 경도")
        private Double longitude;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "미션 인증 응답")
    public static class VerifyResponse {
        @Schema(description = "인증 성공 여부")
        private Boolean success;

        @Schema(description = "미션 이름")
        private String missionTitle;

        @Schema(description = "획득한 포인트")
        private Integer earnedPoints;

        @Schema(description = "현재 총 포인트")
        private Integer totalPoints;
    }
}