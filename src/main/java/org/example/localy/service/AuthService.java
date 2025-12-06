package org.example.localy.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.exception.CustomException;
import org.example.localy.dto.AuthDto;
import org.example.localy.entity.Users;
import org.example.localy.repository.UserRepository;
import org.example.localy.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    // 회원가입
    @Transactional
    public AuthDto.AuthResponse signUp(AuthDto.SignUpRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // 비밀번호 확인
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 사용자 생성
        Users user = Users.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .authProvider(Users.AuthProvider.LOCAL)
                .points(0)
                .build();

        Users savedUser = userRepository.save(user);
        log.info("회원가입 완료: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();
    }

   // 로그인
    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        // 사용자 조회
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIALS));

        // 소셜 로그인 사용자인 경우
        if (user.getAuthProvider() != Users.AuthProvider.LOCAL) {
            throw new CustomException(AuthErrorCode.OAUTH_USER_EMAIL_MISMATCH);
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        log.info("로그인 성공: userId={}, email={}", user.getId(), user.getEmail());

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return AuthDto.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    // Google OAuth 로그인
    @Transactional
    public AuthDto.AuthResponse googleLogin(String idToken) {
        try {
            // Google ID Token 검증
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.error("Google ID Token 검증 실패");
                throw new CustomException(AuthErrorCode.OAUTH_PROCESSING_FAILED);
            }

            // 사용자 정보 추출
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String providerId = payload.getSubject();

            log.info("Google 토큰 검증 성공: email={}, providerId={}", email, providerId);

            // 기존 사용자 조회 또는 신규 생성
            Users user = userRepository.findByAuthProviderAndProviderId(
                    Users.AuthProvider.GOOGLE,
                    providerId
            ).orElseGet(() -> createGoogleUser(email, providerId));

            log.info("Google 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());

            // JWT 토큰 생성
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            return AuthDto.AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .onboardingCompleted(user.getOnboardingCompleted())
                    .build();

        } catch (Exception e) {
            log.error("Google 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.OAUTH_PROCESSING_FAILED);
        }
    }

    // Google 사용자 생성
    private Users createGoogleUser(String email, String providerId) {
        // 닉네임 생성 (이메일 앞부분 사용, 중복 시 숫자 추가)
        String baseNickname = email.split("@")[0];
        String nickname = generateUniqueNickname(baseNickname);

        Users user = Users.builder()
                .email(email)
                .nickname(nickname)
                .authProvider(Users.AuthProvider.GOOGLE)
                .providerId(providerId)
                .points(0)
                .build();

        Users savedUser = userRepository.save(user);
        log.info("Google 신규 사용자 생성: userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return savedUser;
    }

    //중복되지 않는 닉네임 생성
    private String generateUniqueNickname(String baseNickname) {
        String nickname = baseNickname;
        int suffix = 1;

        while (userRepository.existsByNickname(nickname)) {
            nickname = baseNickname + suffix;
            suffix++;
        }

        return nickname;
    }

    // 로그아웃
    @Transactional
    public AuthDto.LogoutResponse logout(Long userId) {
        try {
            // Redis에서 Refresh Token 삭제
            String redisKey = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.delete(redisKey);

            log.info("로그아웃 완료: userId={}", userId);

            return AuthDto.LogoutResponse.builder()
                    .success(true)
                    .message("로그아웃이 완료되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("로그아웃 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.LOGOUT_FAILED);
        }
    }

    // 비밀번호 찾기
    @Transactional
    public AuthDto.PasswordResetResponse resetPassword(AuthDto.PasswordResetRequest request) {
        try {
            // 이메일 인증번호 확인
            emailVerificationService.verifyAndConsumeCode(request.getEmail(), request.getCode());

            // 사용자 조회
            Users user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 소셜 로그인 사용자 체크
            if (user.getAuthProvider() != Users.AuthProvider.LOCAL) {
                throw new CustomException(AuthErrorCode.OAUTH_USER_EMAIL_MISMATCH);
            }

            // 새 비밀번호 확인
            if (!request.getNewPassword().equals(request.getNewPasswordConfirm())) {
                throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
            }

            // 비밀번호 업데이트
            user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("비밀번호 재설정 완료: userId={}, email={}", user.getId(), user.getEmail());

            return AuthDto.PasswordResetResponse.builder()
                    .success(true)
                    .message("비밀번호가 성공적으로 변경되었습니다.")
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}