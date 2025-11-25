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

public class AuthDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이메일 인증번호 요청")
    public static class EmailVerificationRequest {
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Schema(description = "인증받을 이메일")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이메일 인증번호 확인")
    public static class EmailVerificationConfirm {
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Schema(description = "인증받은 이메일")
        private String email;

        @NotBlank(message = "인증번호를 입력해주세요.")
        @Size(min = 6, max = 6, message = "인증번호는 6자리입니다.")
        @Schema(description = "인증번호 (6자리)")
        private String code;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "회원가입 요청")
    public static class SignUpRequest {
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Schema(description = "이메일(아이디)")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[!#$%&*@^])[a-z!#$%&*@^0-9]{8,16}$",
                message = "영문 소문자 8~16자, 특수문자(!#$%&*@^) 포함 필수"
        )
        @Schema(description = "비밀번호 (영문 소문자 8~16자, 특수문자 !#$%&*@^ 포함)")
        private String password;

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        @Schema(description = "비밀번호 재입력")
        private String passwordConfirm;

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자 이내여야 합니다.")
        @Schema(description = "닉네임")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "로그인 요청")
    public static class LoginRequest {
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Schema(description = "이메일(아이디)")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Schema(description = "비밀번호")
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Google OAuth 로그인 요청")
    public static class GoogleLoginRequest {
        @NotBlank(message = "Google ID Token을 입력해주세요.")
        @Schema(description = "Google ID Token")
        private String idToken;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "인증 응답")
    public static class AuthResponse {
        @Schema(description = "JWT Access Token")
        private String accessToken;

        @Schema(description = "JWT Refresh Token")
        private String refreshToken;

        @Schema(description = "사용자 ID")
        private Long userId;

        @Schema(description = "이메일")
        private String email;

        @Schema(description = "닉네임")
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "이메일 인증 응답")
    public static class EmailVerificationResponse {
        @Schema(description = "인증 성공 여부")
        private boolean verified;

        @Schema(description = "메시지")
        private String message;
    }
}
