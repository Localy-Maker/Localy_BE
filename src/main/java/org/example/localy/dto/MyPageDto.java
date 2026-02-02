package org.example.localy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MyPageDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "마이페이지 홈 응답")
    public static class ProfileResponse {
        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "닉네임")
        private String nickname;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "포인트")
        private Integer points;

        @Schema(description = "멤버십 등급 (BASIC, PREMIUM)")
        private String membershipLevel;

        @Schema(description = "프리미엄 만료일")
        private LocalDateTime premiumExpiryDate;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "회원정보 수정 요청")
    public static class UpdateProfileRequest {
        @Schema(description = "새 비밀번호 (영문 소문자 8~16자, 특수문자 !#$%&*@^ 포함)")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[!#$%&*@^])[a-z!#$%&*@^0-9]{8,16}$",
                message = "영문 소문자 8~16자, 특수문자(!#$%&*@^) 포함 필수"
        )
        private String newPassword;

        @Schema(description = "새 비밀번호 재입력")
        private String newPasswordConfirm;

        @Schema(description = "새 닉네임")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 이내여야 합니다.")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "회원정보 수정 응답")
    public static class UpdateProfileResponse {
        @Schema(description = "성공 여부")
        private boolean success;

        @Schema(description = "메시지")
        private String message;

        @Schema(description = "업데이트된 닉네임")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "회원 탈퇴 응답")
    public static class DeleteAccountResponse {
        @Schema(description = "성공 여부")
        private boolean success;

        @Schema(description = "메시지")
        private String message;
    }
}