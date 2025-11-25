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
            "English",
            "Korean",
            "Chinese",
            "Japanese",
            "Vietnamese"
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
     * 온보딩 기본 정보 저장 (언어/국적)
     */
    @Transactional
    public OnboardingDto.BasicInfoResponse saveBasicInfo(Long userId, OnboardingDto.BasicInfoRequest request) {
        try {
            // 사용자 조회
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 기본 정보 저장
            user.setDisplayLanguage(request.getLanguage());
            user.setNationality(request.getNationality());

            // 명시적으로 save 호출
            Users savedUser = userRepository.save(user);

            log.info("온보딩 기본 정보 저장 완료: userId={}, language={}, nationality={}",
                    userId, savedUser.getDisplayLanguage(), savedUser.getNationality());

            return OnboardingDto.BasicInfoResponse.builder()
                    .status(200)
                    .message("기본 정보가 저장되었습니다")
                    .userId(savedUser.getId())
                    .language(savedUser.getDisplayLanguage())
                    .nationality(savedUser.getNationality())
                    .build();

        } catch (Exception e) {
            log.error("기본 정보 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_SAVE_FAILED);
        }
    }

    /**
     * 관심사 저장/수정 (온보딩 & 마이페이지 공통 사용)
     */
    @Transactional
    public OnboardingDto.InterestsResponse saveInterests(Long userId, OnboardingDto.InterestsRequest request) {
        try {
            // 사용자 조회
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 관심사 목록을 JSON 문자열로 변환
            String interestsJson = objectMapper.writeValueAsString(request.getInterests());

            // 관심사 저장
            user.setInterests(interestsJson);

            // 온보딩 완료 처리 (아직 완료 안했다면)
            if (!user.getOnboardingCompleted()) {
                user.setOnboardingCompleted(true);
                log.info("온보딩 완료 처리: userId={}", userId);
            }

            // 명시적으로 save 호출
            Users savedUser = userRepository.save(user);

            // 저장된 관심사를 다시 파싱하여 확인
            List<String> savedInterests = objectMapper.readValue(
                    savedUser.getInterests(),
                    new TypeReference<List<String>>() {}
            );

            log.info("관심사 저장 완료: userId={}, interests={}, onboardingCompleted={}",
                    userId, savedInterests, savedUser.getOnboardingCompleted());

            return OnboardingDto.InterestsResponse.builder()
                    .status(200)
                    .message("관심사가 저장되었습니다")
                    .userId(savedUser.getId())
                    .interests(savedInterests)
                    .build();

        } catch (JsonProcessingException e) {
            log.error("관심사 JSON 변환 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_SAVE_FAILED);
        } catch (Exception e) {
            log.error("관심사 저장 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_SAVE_FAILED);
        }
    }

    /**
     * 온보딩/마이페이지 정보 조회
     */
    @Transactional(readOnly = true)
    public OnboardingDto.OnboardingResponse getOnboardingInfo(Long userId) {
        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

            // 관심사 JSON 문자열을 List로 변환
            List<String> interests = null;
            if (user.getInterests() != null && !user.getInterests().isEmpty()) {
                interests = objectMapper.readValue(user.getInterests(), new TypeReference<List<String>>() {});
            }

            log.info("온보딩 정보 조회: userId={}, language={}, nationality={}, interests={}, completed={}",
                    userId, user.getDisplayLanguage(), user.getNationality(), interests, user.getOnboardingCompleted());

            return OnboardingDto.OnboardingResponse.builder()
                    .userId(user.getId())
                    .language(user.getDisplayLanguage())
                    .nationality(user.getNationality())
                    .interests(interests)
                    .onboardingCompleted(user.getOnboardingCompleted())
                    .build();

        } catch (JsonProcessingException e) {
            log.error("관심사 JSON 파싱 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_LOAD_FAILED);
        } catch (Exception e) {
            log.error("온보딩 정보 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new CustomException(AuthErrorCode.ONBOARDING_LOAD_FAILED);
        }
    }
}