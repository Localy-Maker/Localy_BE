package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.dto.MyPageDto;
import org.example.localy.entity.Users;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.repository.NotificationReadRepository;
import org.example.localy.repository.EmotionDayResultRepository;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.example.localy.repository.place.BookmarkRepository;
import org.example.localy.repository.place.MissionRepository;
import org.example.localy.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.localy.repository.place.MissionArchiveRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private final ChatBotRepository chatBotRepository;
    private final MissionRepository missionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final EmotionDayResultRepository emotionDayResultRepository;
    private final EmotionWindowResultRepository emotionWindowResultRepository;
    private final MissionArchiveRepository missionArchiveRepository;

    public String getEmailByUserId(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
        return user.getEmail();
    }

    @Transactional(readOnly = true)
    public MyPageDto.ProfileResponse getProfile(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        log.info("프로필 조회: userId={}, membership={}, email={}", userId, user.getMembershipLevel(), user.getEmail());

        return MyPageDto.ProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .points(user.getPoints())
                .membershipLevel(user.getMembershipLevel().toString())
                .premiumExpiryDate(user.getPremiumExpiryDate())
                .build();
    }

    @Transactional
    public MyPageDto.UpdateProfileResponse updateProfile(
            Long userId,
            String email,
            String verificationCode,
            MyPageDto.UpdateProfileRequest request
    ) {
        // 이메일 인증번호 확인
        emailVerificationService.verifyAndConsumeCode(email, verificationCode);

        // 사용자 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        // 이메일이 본인 것인지 확인
        if (!user.getEmail().equals(email)) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        boolean updated = false;
        String updatedNickname = user.getNickname();

        // 비밀번호 변경
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            // 소셜 로그인 사용자는 비밀번호 변경 불가
            if (user.getAuthProvider() != Users.AuthProvider.LOCAL) {
                throw new CustomException(AuthErrorCode.OAUTH_USER_EMAIL_MISMATCH);
            }

            // 비밀번호 확인
            if (request.getNewPasswordConfirm() == null ||
                    !request.getNewPassword().equals(request.getNewPasswordConfirm())) {
                throw new CustomException(AuthErrorCode.PASSWORD_MISMATCH);
            }

            user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
            updated = true;
            log.info("비밀번호 변경 완료: userId={}", userId);
        }

        // 닉네임 변경
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            // 기존 닉네임과 다른 경우에만 중복 체크
            if (!user.getNickname().equals(request.getNickname())) {
                if (userRepository.existsByNickname(request.getNickname())) {
                    throw new CustomException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
                }
                user.updateNickname(request.getNickname());
                updatedNickname = request.getNickname();
                updated = true;
                log.info("닉네임 변경 완료: userId={}, newNickname={}", userId, updatedNickname);
            }
        }

        if (updated) {
            userRepository.save(user);
        }

        return MyPageDto.UpdateProfileResponse.builder()
                .success(true)
                .message(updated ? "회원정보가 성공적으로 수정되었습니다." : "변경된 정보가 없습니다.")
                .nickname(updatedNickname)
                .build();
    }

    @Transactional
    public MyPageDto.DeleteAccountResponse deleteAccount(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        try {
            // Redis에서 Refresh Token 삭제
            String redisKey = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.delete(redisKey);

            missionRepository.deleteAllByUser(user);
            missionArchiveRepository.deleteAllByUser(user);
            bookmarkRepository.deleteAllByUser(user);
            notificationReadRepository.deleteAllByUser(user);
            chatBotRepository.deleteAllByUserId(userId);
            emotionDayResultRepository.deleteAllByUserId(userId);
            emotionWindowResultRepository.deleteAllByUserId(userId);
            // 사용자 삭제
            userRepository.delete(user);

            log.info("회원 탈퇴 완료: userId={}, email={}", userId, user.getEmail());

            return MyPageDto.DeleteAccountResponse.builder()
                    .success(true)
                    .message("회원 탈퇴가 완료되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("회원 탈퇴 처리 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}