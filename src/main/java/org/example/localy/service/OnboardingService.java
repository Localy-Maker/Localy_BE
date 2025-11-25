package org.example.localy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.users.Users;
import org.example.localy.dto.OnboardingDto;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.AuthErrorCode;
import org.example.localy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 온보딩 옵션
    private static final List<String> DISPLAY_LANGUAGES = Arrays.asList(
            "영어 (English)",
            "한국어 (Korean)",
            "중국어 (Chinese)",
            "일본어 (Japanese)",
            "베트남어 (Vietnamese)"
    );

    private static final List<String> NATIONALITIES = Arrays.asList(
            "중국 (China)",
            "베트남 (Vietnam)",
            "일본 (Japan)",
            "미국 (USA)",
            "캐나다 (Canada)",
            "태국 (Thailand)",
            "몽골리아 (Mongolia)",
            "러시아 (Russia)",
            "직접 입력"
    );

    private static final List<String> INTEREST_OPTIONS = Arrays.asList(
            "쇼핑",
            "음식",
            "문화",
            "자연",
            "언어교환",
            "관광"
    );

    /**
     * 온보딩 옵션 조회
     */
    public OnboardingDto.OnboardingOptionsResponse getOnboardingOptions() {
        log.info("온보딩 옵션 조회");

        return OnboardingDto.OnboardingOptionsResponse.builder()
                .displayLanguages(DISPLAY_LANGUAGES)
                .nationalities(NATIONALITIES)
                .interestOptions(INTEREST_OPTIONS)
                .build();
    }

    /**
     * 온보딩 정보 저장
     */
    @Transactional
    public OnboardingDto.OnboardingResponse saveOnboarding(Long userId, OnboardingDto.OnboardingRequest request) {
        try {
            // 사용자 조회
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 이미 온보딩을 완료한 사용자인지 확인
            if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
                log.warn("이미 온보딩을 완료한 사용자입니다: userId={}", userId);
                throw new CustomException(AuthErrorCode.ONBOARDING_ALREADY_COMPLETED);
            }

            // 관심사 목록을 JSON 문자열로 변환
            String interestsJson = objectMapper.writeValueAsString(request.getInterests());

            // 온보딩 정보 저장
            user.setDisplayLanguage(request.getDisplayLanguage());
            user.setNationality(request.getNationality());
            user.setInterests(interestsJson);
            user.setOnboardingCompleted(true);

            userRepository.save(user);

            log.info("온보딩 정보 저장 완료: userId={}", userId);

            return OnboardingDto.OnboardingResponse.builder()
                    .userId(user.getId())
                    .displayLanguage(user.getDisplayLanguage())
                    .nationality(user.getNationality())
                    .interests(request.getInterests())
                    .onboardingCompleted(user.getOnboardingCompleted())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("관심사 JSON 변환 실패: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_SAVE_FAILED);
        }
    }

    /**
     * 온보딩 정보 조회
     */
    @Transactional(readOnly = true)
    public OnboardingDto.OnboardingResponse getOnboardingInfo(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 관심사 JSON 문자열을 List로 변환
            List<String> interests = null;
            if (user.getInterests() != null) {
                interests = objectMapper.readValue(user.getInterests(), new TypeReference<List<String>>() {});
            }

            return OnboardingDto.OnboardingResponse.builder()
                    .userId(user.getId())
                    .displayLanguage(user.getDisplayLanguage())
                    .nationality(user.getNationality())
                    .interests(interests)
                    .onboardingCompleted(user.getOnboardingCompleted())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("관심사 JSON 파싱 실패: {}", e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_LOAD_FAILED);
        }
    }
}