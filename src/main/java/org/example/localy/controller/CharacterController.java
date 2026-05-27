package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.shop.ShopDto;
import org.example.localy.entity.Users;
import org.example.localy.service.shop.ShopService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Character", description = "캐릭터 선택/변경 API")
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final ShopService shopService;

    @Operation(summary = "캐릭터 목록 조회", description = "6종 캐릭터 리스트 + 현재 선택 여부 반환")
    @GetMapping
    public BaseResponse<ShopDto.CharacterListResponse> getCharacters(
            @AuthenticationPrincipal Users user
    ) {
        return BaseResponse.success("캐릭터 목록 조회 성공", shopService.getCharacters(user));
    }

    @Operation(
        summary = "현재 캐릭터 변경",
        description = "users.currentCharacterCode만 변경. 기존 착용 상태 데이터 유지. 없는 코드 입력 시 SHOP004."
    )
    @PatchMapping("/current")
    public BaseResponse<ShopDto.CurrentCharacterDto> changeCurrentCharacter(
            @AuthenticationPrincipal Users user,
            @RequestBody ShopDto.ChangeCharacterRequest request
    ) {
        return BaseResponse.success("캐릭터가 변경되었습니다.",
                shopService.changeCurrentCharacter(user, request.getCharacterCode()));
    }
}
