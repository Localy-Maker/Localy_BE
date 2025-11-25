package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.AuthDto;
import org.example.localy.service.AuthService;
import org.example.localy.service.EmailVerificationService;
import org.example.localy.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "이메일 인증번호 요청", description = "회원가입 시 이메일 인증번호를 요청합니다.")
    @PostMapping("/email/verification/send")
    public BaseResponse<AuthDto.EmailVerificationResponse> sendVerificationCode(
            @Valid @RequestBody AuthDto.EmailVerificationRequest request
    ) {
        emailVerificationService.sendVerificationCode(request.getEmail());

        AuthDto.EmailVerificationResponse response = AuthDto.EmailVerificationResponse.builder()
                .verified(false)
                .message("인증번호가 이메일로 전송되었습니다. 5분 이내에 입력해주세요.")
                .build();

        return BaseResponse.success("이메일 인증번호 전송 완료", response);
    }

    @Operation(summary = "이메일 인증번호 확인", description = "입력한 인증번호가 올바른지 확인합니다.")
    @PostMapping("/email/verification/confirm")
    public BaseResponse<AuthDto.EmailVerificationResponse> confirmVerificationCode(
            @Valid @RequestBody AuthDto.EmailVerificationConfirm request
    ) {
        emailVerificationService.verifyCode(request.getEmail(), request.getCode());

        AuthDto.EmailVerificationResponse response = AuthDto.EmailVerificationResponse.builder()
                .verified(true)
                .message("이메일 인증이 완료되었습니다.")
                .build();

        return BaseResponse.success("이메일 인증 완료", response);
    }

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public BaseResponse<AuthDto.AuthResponse> signUp(
            @Valid @RequestBody AuthDto.SignUpRequest request
    ) {
        AuthDto.AuthResponse response = authService.signUp(request);
        return BaseResponse.success("회원가입 완료", response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public BaseResponse<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        AuthDto.AuthResponse response = authService.login(request);
        return BaseResponse.success("로그인 완료", response);
    }

    @Operation(summary = "Google 소셜 로그인", description = "Google 계정으로 로그인합니다.")
    @PostMapping("/login/google")
    public BaseResponse<AuthDto.AuthResponse> googleLogin(
            @Valid @RequestBody AuthDto.GoogleLoginRequest request
    ) {
        AuthDto.AuthResponse response = authService.googleLogin(request.getIdToken());
        return BaseResponse.success("Google 로그인 완료", response);
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
    @PostMapping("/logout")
    public BaseResponse<AuthDto.LogoutResponse> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");

        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        AuthDto.LogoutResponse response = authService.logout(userId);
        return BaseResponse.success("로그아웃 완료", response);
    }
}