package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.AuthDto;
import org.example.localy.entity.Users;
import org.example.localy.repository.UserRepository;
import org.example.localy.service.AuthService;
import org.example.localy.service.EmailVerificationService;
import org.example.localy.subscriber.RedisSubscriber;
import org.example.localy.subscriber.RedisSubscriberInitializer;
import org.example.localy.util.JwtUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisSubscriber redisSubscriber;
    private final RedisSubscriberInitializer subscriberInitializer;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Operation(summary = "이메일 인증번호 요청", description = "회원가입 시 이메일 인증번호를 요청합니다.")
    @Tags({
            @Tag(name = "Auth"),
            @Tag(name = "MyPage")
    })
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

    @Operation(summary = "Google Client ID 조회", description = "Google OAuth를 위한 Client ID를 반환합니다.")
    @GetMapping("/google/client-id")
    public BaseResponse<ClientResponse> getGoogleClientId() {
        ClientResponse response = new ClientResponse(googleClientId);
        return BaseResponse.success("Google Client ID 조회 성공", response);
    }

    private record ClientResponse(String clientId) {}

    @Operation(summary = "이메일 인증번호 확인", description = "입력한 인증번호가 올바른지 확인합니다.")
    @Tags({
            @Tag(name = "Auth"),
            @Tag(name = "MyPage")
    })
    @PostMapping("/email/verification/confirm")
    public BaseResponse<AuthDto.EmailVerificationResponse> confirmVerificationCode(
            @Valid @RequestBody AuthDto.EmailVerificationConfirm request
    ) {
        emailVerificationService.checkCode(request.getEmail(), request.getCode());

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
        // 신규 가입한 유저의 1대1 채널 구독 시작
        String channel = "localy:chat:bot:" + response.getUserId();
        redisSubscriber.subscribe(channel);
        return BaseResponse.success("회원가입 완료", response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public BaseResponse<AuthDto.AuthResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request
    ) {
        AuthDto.AuthResponse response = authService.login(request);

        // 신규 가입한 유저의 1대1 채널 구독 시작
        String channel = "localy:chat:bot:" + response.getUserId();
        redisSubscriber.subscribe(channel);

        authService.updateLastLoginTime(response);

        return BaseResponse.success("로그인 완료", response);
    }

    @Operation(summary = "Google 소셜 로그인", description = "Google 계정으로 로그인합니다.")
    @PostMapping("/login/google")
    public BaseResponse<AuthDto.AuthResponse> googleLogin(
            @Valid @RequestBody AuthDto.GoogleLoginRequest request
    ) {
        AuthDto.AuthResponse response = authService.googleLogin(request.getIdToken());

        // 신규 가입한 유저의 1대1 채널 구독 시작
        String channel = "localy:chat:bot:" + response.getUserId();
        redisSubscriber.subscribe(channel);

        authService.updateLastLoginTime(response);

        return BaseResponse.success("Google 로그인 완료", response);
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃합니다.")
    @Tags({
            @Tag(name = "Auth"),
            @Tag(name = "MyPage")
    })
    @PostMapping("/logout")
    public BaseResponse<AuthDto.LogoutResponse> logout(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");

        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        AuthDto.LogoutResponse response = authService.logout(userId);

        // 🔹 로그아웃 시 구독 해제 코드 필요 시 여기에 작성 가능 🔹
        redisSubscriber.unsubscribe("localy:chat:bot:" + userId);

        return BaseResponse.success("로그아웃 완료", response);
    }

    @Operation(summary = "비밀번호 재설정 (비밀번호 찾기)", description = "이메일 인증 후 비밀번호를 재설정합니다.")
    @PostMapping("/password/reset")
    public BaseResponse<AuthDto.PasswordResetResponse> resetPassword(
            @Valid @RequestBody AuthDto.PasswordResetRequest request
    ) {
        AuthDto.PasswordResetResponse response = authService.resetPassword(request);
        return BaseResponse.success("비밀번호 재설정 완료", response);
    }
}