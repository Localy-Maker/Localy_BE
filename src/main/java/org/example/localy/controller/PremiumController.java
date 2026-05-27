package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.premium.PremiumDto;
import org.example.localy.entity.Users;
import org.example.localy.service.premium.PremiumService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Premium", description = "프리미엄 구독 API")
@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumController {

    private final PremiumService premiumService;

    @Operation(summary = "플랜 목록 조회", description = "현재 포인트, 구독 상태, 구매 가능 플랜 목록 반환")
    @GetMapping("/plans")
    public BaseResponse<PremiumDto.PlansResponse> getPlans(
            @AuthenticationPrincipal Users user
    ) {
        return BaseResponse.success("플랜 목록 조회 성공", premiumService.getPlans(user));
    }

    @Operation(summary = "구독 상태 조회", description = "현재 프리미엄 구독 여부 및 만료 정보 반환")
    @GetMapping("/status")
    public BaseResponse<PremiumDto.StatusResponse> getStatus(
            @AuthenticationPrincipal Users user
    ) {
        return BaseResponse.success("구독 상태 조회 성공", premiumService.getStatus(user));
    }

    @Operation(
        summary = "프리미엄 플랜 구독",
        description = "포인트 차감 후 구독 기간 부여/연장. 잘못된 플랜 코드: PREMIUM001, 포인트 부족: PREMIUM002"
    )
    @PostMapping("/subscribe")
    public BaseResponse<PremiumDto.SubscribeResponse> subscribe(
            @AuthenticationPrincipal Users user,
            @RequestBody PremiumDto.SubscribeRequest request
    ) {
        return BaseResponse.success("프리미엄 구독이 완료되었습니다.",
                premiumService.subscribe(user, request.getPlanCode()));
    }
}
