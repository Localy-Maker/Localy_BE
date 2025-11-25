package org.example.localy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

public class OnboardingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "온보딩 정보 저장 요청")
    public static class OnboardingRequest {

        @Schema(description = "표시 언어", example = "영어 (English)")
        @NotBlank(message = "표시 언어를 선택해주세요.")
        private String displayLanguage;

        @Schema(description = "국적", example = "중국 (China)")
        @NotBlank(message = "국적을 선택해주세요.")
        private String nationality;

        @Schema(description = "관심사 목록", example = "[\"쇼핑\", \"음식\", \"문화\"]")
        @NotEmpty(message = "최소 1개 이상의 관심사를 선택해주세요.")
        private List<String> interests;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "온보딩 정보 저장 응답")
    public static class OnboardingResponse {

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "표시 언어")
        private String displayLanguage;

        @Schema(description = "국적")
        private String nationality;

        @Schema(description = "관심사 목록")
        private List<String> interests;

        @Schema(description = "온보딩 완료 여부")
        private Boolean onboardingCompleted;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "온보딩 옵션 조회 응답")
    public static class OnboardingOptionsResponse {

        @Schema(description = "표시 언어 목록")
        private List<String> displayLanguages;

        @Schema(description = "국적 목록")
        private List<String> nationalities;

        @Schema(description = "관심사 목록")
        private List<String> interestOptions;
    }
}