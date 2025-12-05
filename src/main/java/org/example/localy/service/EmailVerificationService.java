package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.exception.CustomException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final long VERIFICATION_CODE_EXPIRATION_MINUTES = 5;

    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

    //이메일 인증번호 생성 및 전송
    public void sendVerificationCode(String email) {
        String verificationCode = generateVerificationCode();
        String redisKey = EMAIL_VERIFICATION_PREFIX + email;

        // Redis에 인증번호 저장 (5분 유효)
        redisTemplate.opsForValue().set(
                redisKey,
                verificationCode,
                VERIFICATION_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 이메일 전송
        sendEmail(email, verificationCode);

        log.info("이메일 인증번호 전송 완료: {}", email);
    }

    // 인증 코드의 유효성만 확인, 데이터 삭제x
    public void checkCode(String email, String code) {
        String redisKey = EMAIL_VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_CODE_NOT_FOUND);
        }

        if (!storedCode.equals(code)) {
            throw new CustomException(AuthErrorCode.EMAIL_VERIFICATION_CODE_MISMATCH);
        }
    }

    // 인증 코드 확인, 유효하면 Redis에서 삭제
    public void verifyAndConsumeCode(String email, String code) {
        // 먼저 코드를 확인합니다.
        checkCode(email, code);

        // 코드가 유효하면, 최종적으로 삭제(소모)합니다.
        redisTemplate.delete(EMAIL_VERIFICATION_PREFIX + email);
        log.info("이메일 인증 성공 및 코드 소모: {}", email);
    }

    // 6자리 랜덤 인증번호 생성
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // 이메일 전송
    private void sendEmail(String to, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[Localy] 이메일 인증번호");
            message.setText(String.format(
                    "안녕하세요, Localy입니다.\n" +
                            "회원가입을 위한 인증번호는 다음과 같습니다.\n" +
                            "인증번호: %s\n" +
                            "인증번호는 5분간 유효합니다.\n" +
                            "감사합니다.",
                    verificationCode
            ));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }
}