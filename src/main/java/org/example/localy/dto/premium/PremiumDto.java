package org.example.localy.dto.premium;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PremiumDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랜 목록 조회 응답")
    public static class PlansResponse {
        @Schema(description = "현재 보유 포인트")
        private Integer currentPoint;

        @Schema(description = "프리미엄 구독 중 여부")
        private Boolean isPremium;

        @Schema(description = "현재 구독 정보 (비구독 시 null)", nullable = true)
        private CurrentSubscriptionDto currentSubscription;

        @Schema(description = "구매 가능 플랜 목록")
        private List<PlanItem> plans;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "현재 구독 정보")
    public static class CurrentSubscriptionDto {
        @Schema(description = "구독 만료 일시")
        private LocalDateTime expiresAt;

        @Schema(description = "남은 일수 (당일 만료 시 0)")
        private Integer remainingDays;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "플랜 단건 정보")
    public static class PlanItem {
        @Schema(description = "플랜 코드", example = "7day")
        private String code;

        @Schema(description = "플랜 이름", example = "7일권")
        private String name;

        @Schema(description = "구독 기간 (일)", example = "7")
        private Integer durationDays;

        @Schema(description = "가격 (포인트)", example = "50")
        private Integer price;

        @Schema(description = "7일 환산 가격. durationDays > 7 인 경우만, 그 외 null", nullable = true, example = "46")
        private Integer weeklyPrice;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구독 요청")
    public static class SubscribeRequest {
        @Schema(description = "플랜 코드", example = "7day")
        private String planCode;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구독 완료 응답")
    public static class SubscribeResponse {
        @Schema(description = "차감 후 잔여 포인트")
        private Integer currentPoint;

        @Schema(description = "구독 만료 일시")
        private LocalDateTime expiresAt;

        @Schema(description = "남은 일수")
        private Integer remainingDays;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "구독 상태 조회 응답")
    public static class StatusResponse {
        @Schema(description = "프리미엄 구독 중 여부")
        private Boolean isPremium;

        @Schema(description = "현재 구독 정보 (비구독 시 null)", nullable = true)
        private CurrentSubscriptionDto currentSubscription;
    }
}
