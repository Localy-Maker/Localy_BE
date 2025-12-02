package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.util.JwtUtil;
import org.example.localy.dto.OnboardingDto;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.service.OnboardingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "온보딩 및 마이페이지 API")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "온보딩 정보 조회", description = "온보딩 완료 여부, 현재 설정 및 선택 옵션 목록 조회")
    @GetMapping("/onboarding/info")
    public BaseResponse<OnboardingDto.OnboardingResponse> getOnboardingInfo(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.OnboardingResponse response = onboardingService.getOnboardingInfo(userId);
        return BaseResponse.success("온보딩 정보 조회 성공", response);
    }

    // 언어/국적 선택
    @Operation(summary = "언어/국적 선택", description = "온보딩 1단계 - 사용자의 표시 언어와 국적 저장")
    @PutMapping("/users/nationality")
    public BaseResponse<OnboardingDto.BasicInfoResponse> saveNationality(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OnboardingDto.BasicInfoRequest request
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.BasicInfoResponse response = onboardingService.saveBasicInfo(userId, request);
        return BaseResponse.success(response.getMessage(), response);
    }

    // 온보딩 관심사 선택
    @Operation(
            summary = "관심사 선택",
            description = "온보딩 2단계 및 마이페이지 관심사 수정"
    )
    @PutMapping("/users/interests")
    public BaseResponse<OnboardingDto.InterestsResponse> saveInterests(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OnboardingDto.InterestsRequest request
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.InterestsResponse response = onboardingService.saveInterests(userId, request);
        return BaseResponse.success(response.getMessage(), response);
    }

    // 마이페이지 관심사 변경
    @Operation(
            summary = "온보딩/마이페이지 정보 조회",
            description = "마이페이지에서 기존 선택 정보 조회"
    )
    @GetMapping("/auth/interests/info")
    public BaseResponse<OnboardingDto.OnboardingResponse> getInterestsInfo(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String token = authorizationHeader.replace("Bearer ", "");
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.OnboardingResponse response = onboardingService.getOnboardingInfo(userId);
        return BaseResponse.success("정보 조회 성공", response);
    }
}