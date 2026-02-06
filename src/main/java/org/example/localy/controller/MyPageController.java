package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.AuthDto;
import org.example.localy.dto.MyPageDto;
import org.example.localy.dto.OnboardingDto;
import org.example.localy.entity.Users;
import org.example.localy.repository.UserRepository;
import org.example.localy.service.AuthService;
import org.example.localy.service.EmailVerificationService;
import org.example.localy.service.MyPageService;
import org.example.localy.service.OnboardingService;
import org.example.localy.service.mission.MissionService;
import org.example.localy.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final OnboardingService onboardingService;
    private final JwtUtil jwtUtil;
    private final MissionService missionService;
    private final UserRepository userRepository;

    /**
     * 마이페이지 홈 - 프로필 조회
     */
    @Operation(summary = "마이페이지 홈", description = "사용자 프로필 정보 조회 (닉네임, 이메일, 포인트)")
    @GetMapping("/profile")
    public BaseResponse<MyPageDto.ProfileResponse> getProfile(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        MyPageDto.ProfileResponse response = myPageService.getProfile(userId);
        return BaseResponse.success("프로필 조회 성공", response);
    }

    /**
     * 회원정보 수정 - 비밀번호/닉네임 수정
     * 사전에 /auth/email/verification/send와 /auth/email/verification/confirm으로 이메일 인증 완료 필요
     */
    @Operation(
            summary = "회원정보 수정",
            description = "이메일 인증 후 비밀번호 및 닉네임 수정 (비밀번호와 닉네임 중 하나 이상 필수). " +
                    "사전에 /auth/email/verification/send와 /auth/email/verification/confirm으로 이메일 인증 완료 필요"
    )
    @PutMapping("/profile")
    public BaseResponse<MyPageDto.UpdateProfileResponse> updateProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String verificationCode,
            @Valid @RequestBody MyPageDto.UpdateProfileRequest request
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        String email = myPageService.getEmailByUserId(userId);

        MyPageDto.UpdateProfileResponse response = myPageService.updateProfile(
                userId, email, verificationCode, request
        );

        return BaseResponse.success(response.getMessage(), response);
    }

    /**
     * 표시 언어/국적 수정
     */
    @Operation(summary = "표시 언어/국적 수정", description = "마이페이지에서 표시 언어와 국적 수정")
    @PutMapping("/settings/nationality")
    public BaseResponse<OnboardingDto.BasicInfoResponse> updateNationality(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OnboardingDto.BasicInfoRequest request
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        // OnboardingService의 기존 메서드 재사용
        OnboardingDto.BasicInfoResponse response = onboardingService.saveBasicInfo(userId, request);
        return BaseResponse.success("언어/국적 정보가 수정되었습니다.", response);
    }

    /**
     * 관심사 조회
     */
    @Operation(summary = "관심사 조회", description = "마이페이지에서 기존 선택한 관심사 조회")
    @GetMapping("/interests")
    public BaseResponse<OnboardingDto.OnboardingResponse> getInterests(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        // OnboardingService의 기존 메서드 재사용
        OnboardingDto.OnboardingResponse response = onboardingService.getOnboardingInfo(userId);
        return BaseResponse.success("관심사 조회 성공", response);
    }

    /**
     * 관심사 변경
     */
    @Operation(summary = "관심사 변경", description = "마이페이지에서 관심사 수정")
    @PutMapping("/interests")
    public BaseResponse<OnboardingDto.InterestsResponse> updateInterests(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OnboardingDto.InterestsRequest request
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        // OnboardingService의 기존 메서드 재사용
        OnboardingDto.InterestsResponse response = onboardingService.saveInterests(userId, request);
        return BaseResponse.success("관심사가 수정되었습니다.", response);
    }

    /**
     * 프리미엄 플랜 정보 조회
     */
    @Operation(summary = "프리미엄 플랜 조회", description = "사용자의 현재 프리미엄 구독 상태 및 만료일 조회")
    @GetMapping("/premium")
    public BaseResponse<Users> getPremiumPlan(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        Long userId = getUserIdFromHeader(authorizationHeader);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return BaseResponse.success("프리미엄 상태 조회 성공", user);
    }

    /**
     * 프리미엄 구독 구매
     */
    @Operation(summary = "프리미엄 구독 구매", description = "포인트를 사용하여 구독권 구매 (7DAYS: 50P, 30DAYS: 200P)")
    @PostMapping("/premium/purchase")
    public BaseResponse<String> purchasePremium(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam String planType
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        missionService.purchasePremium(user, planType);
        return BaseResponse.success("프리미엄 구독 구매가 완료되었습니다.", null);
    }

    /**
     * 회원 탈퇴
     */
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴 처리")
    @DeleteMapping("/account")
    public BaseResponse<MyPageDto.DeleteAccountResponse> deleteAccount(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        MyPageDto.DeleteAccountResponse response = myPageService.deleteAccount(userId);
        return BaseResponse.success(response.getMessage(), response);
    }

    private Long getUserIdFromHeader(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        return jwtUtil.getUserIdFromToken(token);
    }
}