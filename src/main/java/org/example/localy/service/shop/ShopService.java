package org.example.localy.service.shop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.exception.errorCode.ShopErrorCode;
import org.example.localy.dto.shop.ShopDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.shop.Character;
import org.example.localy.entity.shop.Item;
import org.example.localy.entity.shop.ItemCategory;
import org.example.localy.entity.shop.UserCharacterState;
import org.example.localy.entity.shop.UserItem;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.shop.CharacterRepository;
import org.example.localy.repository.shop.ItemRepository;
import org.example.localy.repository.shop.UserCharacterStateRepository;
import org.example.localy.repository.shop.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserCharacterStateRepository userCharacterStateRepository;

    @Transactional
    public ShopDto.ShopStateResponse getShopState(Users user) {
        String characterCode = user.getCurrentCharacterCode();

        Character character = characterRepository.findByCode(characterCode)
                .orElseThrow(() -> new CustomException(ShopErrorCode.CHARACTER_NOT_FOUND));

        UserCharacterState state = ensureCharacterStateExists(user, characterCode);

        return ShopDto.ShopStateResponse.builder()
                .currentPoint(user.getPoints())
                .currentCharacter(ShopDto.CurrentCharacterDto.builder()
                        .code(character.getCode())
                        .name(character.getName())
                        .imageUrl(character.getImageUrl())
                        .equipped(toEquippedDto(state))
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public ShopDto.ItemListResponse getItemsByCategory(Users user, ItemCategory category) {
        String characterCode = user.getCurrentCharacterCode();

        List<Item> items = itemRepository.findAllByCategory(category);
        Set<Long> ownedIds = userItemRepository.findOwnedItemIdsByUser(user);
        Set<Long> equippedIds = getEquippedItemIds(user, characterCode);

        List<ShopDto.ItemResponse> responses = items.stream()
                .map(item -> toItemResponse(item, ownedIds, equippedIds))
                .collect(Collectors.toList());

        return ShopDto.ItemListResponse.builder().items(responses).build();
    }

    @Transactional(readOnly = true)
    public ShopDto.ItemListResponse getOwnedItems(Users user) {
        String characterCode = user.getCurrentCharacterCode();

        List<UserItem> userItems = userItemRepository
                .findAllByUserOrderedByCategoryAndPurchasedAt(user);
        Set<Long> ownedIds = userItems.stream()
                .map(ui -> ui.getItem().getId())
                .collect(Collectors.toSet());
        Set<Long> equippedIds = getEquippedItemIds(user, characterCode);

        List<ShopDto.ItemResponse> responses = userItems.stream()
                .map(ui -> toItemResponse(ui.getItem(), ownedIds, equippedIds))
                .collect(Collectors.toList());

        return ShopDto.ItemListResponse.builder().items(responses).build();
    }

    @Transactional
    public ShopDto.PurchaseResponse purchaseItem(Users user, Long itemId) {
        // 비관적 락으로 재조회 → 동시 요청 시 포인트 음수 방지
        Users lockedUser = userRepository.findByIdWithLock(user.getId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));

        if (userItemRepository.existsByUserAndItem(lockedUser, item)) {
            throw new CustomException(ShopErrorCode.ITEM_ALREADY_OWNED);
        }

        if (lockedUser.getPoints() < item.getPrice()) {
            throw new CustomException(ShopErrorCode.INSUFFICIENT_POINTS);
        }

        lockedUser.deductPoints(item.getPrice());
        userRepository.save(lockedUser);

        userItemRepository.save(UserItem.builder()
                .user(lockedUser)
                .item(item)
                .purchasedAt(LocalDateTime.now())
                .build());

        // 구매 즉시 해당 카테고리에 자동 착용
        String characterCode = lockedUser.getCurrentCharacterCode();
        UserCharacterState state = ensureCharacterStateExists(lockedUser, characterCode);
        equipItemToState(state, item);
        userCharacterStateRepository.save(state);

        return ShopDto.PurchaseResponse.builder()
                .currentPoint(lockedUser.getPoints())
                .purchasedItemId(item.getId())
                .autoEquipped(true)
                .build();
    }

    @Transactional
    public ShopDto.EquipResponse equip(Users user, ShopDto.EquipRequest request) {
        ItemCategory category = parseCategory(request.getCategory());
        String characterCode = user.getCurrentCharacterCode();

        UserCharacterState state = userCharacterStateRepository
                .findByUserAndCharacterCode(user, characterCode)
                .orElseGet(() -> userCharacterStateRepository.save(
                        UserCharacterState.emptyState(user, characterCode)));

        if (request.getItemId() == null) {
            unequipCategoryFromState(state, category);
        } else {
            Item item = itemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new CustomException(ShopErrorCode.ITEM_NOT_FOUND));

            if (!userItemRepository.existsByUserAndItem(user, item)) {
                throw new CustomException(ShopErrorCode.ITEM_NOT_OWNED);
            }

            if (!item.getCategory().equals(category)) {
                throw new CustomException(ShopErrorCode.INVALID_CATEGORY);
            }

            equipItemToState(state, item);
        }

        userCharacterStateRepository.save(state);

        return ShopDto.EquipResponse.builder()
                .equipped(toEquippedDto(state))
                .build();
    }

    @Transactional
    public ShopDto.CurrentCharacterDto changeCurrentCharacter(Users user, String characterCode) {
        Character character = characterRepository.findByCode(characterCode)
                .orElseThrow(() -> new CustomException(ShopErrorCode.CHARACTER_NOT_FOUND));

        user.setCurrentCharacterCode(characterCode);
        userRepository.save(user);

        UserCharacterState state = ensureCharacterStateExists(user, characterCode);

        return ShopDto.CurrentCharacterDto.builder()
                .code(character.getCode())
                .name(character.getName())
                .imageUrl(character.getImageUrl())
                .equipped(toEquippedDto(state))
                .build();
    }

    @Transactional(readOnly = true)
    public ShopDto.CharacterListResponse getCharacters(Users user) {
        String currentCode = user.getCurrentCharacterCode();

        List<ShopDto.CharacterResponse> characters = characterRepository.findAll().stream()
                .map(c -> ShopDto.CharacterResponse.builder()
                        .code(c.getCode())
                        .name(c.getName())
                        .imageUrl(c.getImageUrl())
                        .isCurrent(c.getCode().equals(currentCode))
                        .build())
                .collect(Collectors.toList());

        return ShopDto.CharacterListResponse.builder().characters(characters).build();
    }

    // 캐릭터 변경 시 + GET /api/shop/state 진입 시 두 곳에서만 호출
    UserCharacterState ensureCharacterStateExists(Users user, String characterCode) {
        return userCharacterStateRepository
                .findByUserAndCharacterCode(user, characterCode)
                .orElseGet(() -> {
                    UserCharacterState empty = UserCharacterState.emptyState(user, characterCode);
                    log.info("캐릭터 상태 row 신규 생성: userId={}, characterCode={}", user.getId(), characterCode);
                    return userCharacterStateRepository.save(empty);
                });
    }

    void equipItemToState(UserCharacterState state, Item item) {
        switch (item.getCategory()) {
            case BACKGROUND -> state.setEquippedBackground(item);
            case HAT        -> state.setEquippedHat(item);
            case ACCESSORY  -> state.setEquippedAccessory(item);
            case ETC        -> state.setEquippedEtc(item);
        }
    }

    void unequipCategoryFromState(UserCharacterState state, ItemCategory category) {
        switch (category) {
            case BACKGROUND -> state.setEquippedBackground(null);
            case HAT        -> state.setEquippedHat(null);
            case ACCESSORY  -> state.setEquippedAccessory(null);
            case ETC        -> state.setEquippedEtc(null);
        }
    }

    private Set<Long> getEquippedItemIds(Users user, String characterCode) {
        return userCharacterStateRepository
                .findByUserAndCharacterCode(user, characterCode)
                .map(state -> {
                    Set<Long> ids = new java.util.HashSet<>();
                    if (state.getEquippedBackground() != null) ids.add(state.getEquippedBackground().getId());
                    if (state.getEquippedHat() != null)        ids.add(state.getEquippedHat().getId());
                    if (state.getEquippedAccessory() != null)  ids.add(state.getEquippedAccessory().getId());
                    if (state.getEquippedEtc() != null)        ids.add(state.getEquippedEtc().getId());
                    return ids;
                })
                .orElse(Set.of());
    }

    private ShopDto.ItemResponse toItemResponse(Item item, Set<Long> ownedIds, Set<Long> equippedIds) {
        return ShopDto.ItemResponse.builder()
                .itemId(item.getId())
                .category(item.getCategory().name())
                .name(item.getName())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .isOwned(ownedIds.contains(item.getId()))
                .isEquipped(equippedIds.contains(item.getId()))
                .build();
    }

    private ShopDto.EquippedDto toEquippedDto(UserCharacterState state) {
        return ShopDto.EquippedDto.builder()
                .background(toEquippedItemDto(state.getEquippedBackground()))
                .hat(toEquippedItemDto(state.getEquippedHat()))
                .accessory(toEquippedItemDto(state.getEquippedAccessory()))
                .etc(toEquippedItemDto(state.getEquippedEtc()))
                .build();
    }

    private ShopDto.EquippedItemDto toEquippedItemDto(Item item) {
        if (item == null) return null;
        return ShopDto.EquippedItemDto.builder()
                .itemId(item.getId())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private ItemCategory parseCategory(String categoryStr) {
        try {
            return ItemCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ShopErrorCode.INVALID_CATEGORY);
        }
    }
}
