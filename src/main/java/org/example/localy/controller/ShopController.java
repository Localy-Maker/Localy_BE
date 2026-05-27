package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.shop.ShopDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.shop.ItemCategory;
import org.example.localy.service.shop.ShopService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shop", description = "포인트 상점 API")
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @Operation(summary = "상점 상태 조회", description = "현재 보유 포인트, 선택된 캐릭터 및 착용 아이템 조회")
    @GetMapping("/state")
    public BaseResponse<ShopDto.ShopStateResponse> getShopState(
            @AuthenticationPrincipal Users user
    ) {
        return BaseResponse.success("상점 상태 조회 성공", shopService.getShopState(user));
    }

    @Operation(
        summary = "카테고리별 아이템 목록 조회",
        description = "category: BACKGROUND | HAT | ACCESSORY | ETC. 잘못된 값 입력 시 SHOP006 반환."
    )
    @GetMapping("/items")
    public BaseResponse<ShopDto.ItemListResponse> getItemsByCategory(
            @AuthenticationPrincipal Users user,
            @RequestParam ItemCategory category
    ) {
        return BaseResponse.success("아이템 목록 조회 성공", shopService.getItemsByCategory(user, category));
    }

    @Operation(summary = "보유 아이템 목록 조회", description = "카테고리 순 → 최근 구매순 정렬")
    @GetMapping("/items/owned")
    public BaseResponse<ShopDto.ItemListResponse> getOwnedItems(
            @AuthenticationPrincipal Users user
    ) {
        return BaseResponse.success("보유 아이템 조회 성공", shopService.getOwnedItems(user));
    }

    @Operation(
        summary = "아이템 구매",
        description = "포인트 차감(20P) + 인벤토리 추가 + 현재 캐릭터에 자동 착용. 포인트 부족: SHOP003, 이미 보유: SHOP002"
    )
    @PostMapping("/items/{itemId}/purchase")
    public BaseResponse<ShopDto.PurchaseResponse> purchaseItem(
            @AuthenticationPrincipal Users user,
            @PathVariable Long itemId
    ) {
        return BaseResponse.success("아이템 구매가 완료되었습니다.", shopService.purchaseItem(user, itemId));
    }

    @Operation(
        summary = "아이템 착용/해제",
        description = "보유한 아이템을 현재 캐릭터에 착용. itemId=null이면 해당 카테고리 해제. 미보유 아이템 착용 시도: SHOP005, 잘못된 카테고리: SHOP006"
    )
    @PatchMapping("/equip")
    public BaseResponse<ShopDto.EquipResponse> equip(
            @AuthenticationPrincipal Users user,
            @RequestBody ShopDto.EquipRequest request
    ) {
        return BaseResponse.success("착용 상태가 변경되었습니다.", shopService.equip(user, request));
    }
}
