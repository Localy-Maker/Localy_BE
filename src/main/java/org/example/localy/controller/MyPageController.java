package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.AuthDto;
import org.example.localy.dto.MyPageDto;
import org.example.localy.dto.OnboardingDto;
import org.example.localy.service.AuthService;
import org.example.localy.service.EmailVerificationService;
import org.example.localy.service.MyPageService;
import org.example.localy.service.OnboardingService;
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
     * 프리미엄 플랜 (미구현)
     */
    @Operation(summary = "프리미엄 플랜", description = "프리미엄 플랜 정보 조회 (추후 구현 예정)")
    @GetMapping("/premium")
    public BaseResponse<Void> getPremiumPlan(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);

        return BaseResponse.success("프리미엄 플랜 기능은 준비 중입니다.", null);
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
}