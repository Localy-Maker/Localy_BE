package org.example.localy.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ShopDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캐릭터 목록 응답")
    public static class CharacterListResponse {
        private java.util.List<CharacterResponse> characters;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "캐릭터 단건 정보")
    public static class CharacterResponse {
        @Schema(description = "캐릭터 코드", example = "happy")
        private String code;

        @Schema(description = "캐릭터 이름", example = "행복")
        private String name;

        @Schema(description = "캐릭터 기본 이미지 URL")
        private String imageUrl;

        @Schema(description = "현재 선택된 캐릭터 여부")
        private Boolean isCurrent;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "현재 캐릭터 변경 요청")
    public static class ChangeCharacterRequest {
        @Schema(description = "변경할 캐릭터 코드", example = "sad")
        private String characterCode;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "착용/해제 요청")
    public static class EquipRequest {
        @Schema(description = "카테고리 (BACKGROUND | HAT | ACCESSORY | ETC)", example = "HAT")
        private String category;

        @Schema(description = "착용할 아이템 ID. null이면 해당 카테고리 아이템 해제", nullable = true)
        private Long itemId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "착용/해제 후 갱신된 착용 상태")
    public static class EquipResponse {
        @Schema(description = "갱신된 착용 상태")
        private EquippedDto equipped;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 구매 응답")
    public static class PurchaseResponse {
        @Schema(description = "구매 후 잔여 포인트")
        private Integer currentPoint;

        @Schema(description = "구매한 아이템 ID")
        private Long purchasedItemId;

        @Schema(description = "구매 즉시 자동 착용 여부")
        private Boolean autoEquipped;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 목록 응답")
    public static class ItemListResponse {
        private java.util.List<ItemResponse> items;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "아이템 단건 정보")
    public static class ItemResponse {
        @Schema(description = "아이템 ID")
        private Long itemId;

        @Schema(description = "카테고리", example = "HAT")
        private String category;

        @Schema(description = "아이템명")
        private String name;

        @Schema(description = "가격", example = "20")
        private Integer price;

        @Schema(description = "아이템 이미지 URL")
        private String imageUrl;

        @Schema(description = "보유 여부 (true면 가격 회색 처리)")
        private Boolean isOwned;

        @Schema(description = "현재 캐릭터에 착용 중 여부")
        private Boolean isEquipped;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상점 화면 진입 시 전체 상태 응답")
    public static class ShopStateResponse {
        @Schema(description = "보유 포인트")
        private Integer currentPoint;

        @Schema(description = "현재 선택된 캐릭터 상태")
        private CurrentCharacterDto currentCharacter;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "현재 선택된 캐릭터 정보 + 착용 상태")
    public static class CurrentCharacterDto {
        @Schema(description = "캐릭터 코드", example = "happy")
        private String code;

        @Schema(description = "캐릭터 이름", example = "행복")
        private String name;

        @Schema(description = "캐릭터 기본 이미지 URL")
        private String imageUrl;

        @Schema(description = "착용 중인 아이템")
        private EquippedDto equipped;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "카테고리별 착용 중인 아이템 (없으면 null)")
    public static class EquippedDto {
        private EquippedItemDto background;
        private EquippedItemDto hat;
        private EquippedItemDto accessory;
        private EquippedItemDto etc;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "착용 중인 아이템 정보")
    public static class EquippedItemDto {
        @Schema(description = "아이템 ID")
        private Long itemId;

        @Schema(description = "아이템 이미지 URL")
        private String imageUrl;
    }
}
