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
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "온보딩 API")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "온보딩 옵션 조회", description = "온보딩에 필요한 선택 옵션 목록을 조회합니다.")
    @GetMapping("/options")
    public BaseResponse<OnboardingDto.OnboardingOptionsResponse> getOnboardingOptions() {
        OnboardingDto.OnboardingOptionsResponse response = onboardingService.getOnboardingOptions();
        return BaseResponse.success("온보딩 옵션 조회 성공", response);
    }

    @Operation(summary = "온보딩 정보 저장", description = "사용자의 온보딩 정보를 저장합니다.")
    @PostMapping
    public BaseResponse<OnboardingDto.OnboardingResponse> saveOnboarding(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody OnboardingDto.OnboardingRequest request
    ) {
        // Bearer 토큰에서 실제 토큰 추출
        String token = authorizationHeader.replace("Bearer ", "");

        // 토큰 검증 및 userId 추출
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.OnboardingResponse response = onboardingService.saveOnboarding(userId, request);
        return BaseResponse.success("온보딩 정보 저장 성공", response);
    }

    @Operation(summary = "온보딩 정보 조회", description = "사용자의 온보딩 정보를 조회합니다.")
    @GetMapping
    public BaseResponse<OnboardingDto.OnboardingResponse> getOnboardingInfo(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Bearer 토큰에서 실제 토큰 추출
        String token = authorizationHeader.replace("Bearer ", "");

        // 토큰 검증 및 userId 추출
        jwtUtil.validateToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        OnboardingDto.OnboardingResponse response = onboardingService.getOnboardingInfo(userId);
        return BaseResponse.success("온보딩 정보 조회 성공", response);
    }
}
