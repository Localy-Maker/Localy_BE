package org.example.localy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.shop.Character;
import org.example.localy.entity.shop.Item;
import org.example.localy.entity.shop.ItemCategory;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.shop.CharacterRepository;
import org.example.localy.repository.shop.ItemRepository;
import org.example.localy.repository.shop.UserCharacterStateRepository;
import org.example.localy.repository.shop.UserItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopDataInitializer implements CommandLineRunner {

    @Value("${app.assets.base-url}")
    private String assetsBaseUrl;

    private final CharacterRepository characterRepository;
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final UserCharacterStateRepository userCharacterStateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (hasPlaceholderData()) {
            log.info("placeholder 데이터 감지 → 기존 상점 데이터 초기화 시작");
            cleanupShopData();
        }
        seedCharacters();
        seedItems();
    }

    private boolean hasPlaceholderData() {
        return characterRepository.findAll().stream()
                .anyMatch(c -> c.getImageUrl().contains("placeholder.local"))
                || itemRepository.findAll().stream()
                .anyMatch(i -> i.getImageUrl().contains("placeholder.local"));
    }

    private void cleanupShopData() {
        // FK 참조 순서: user_character_states → user_items → items → characters
        userCharacterStateRepository.deleteAll();
        userItemRepository.deleteAll();
        itemRepository.deleteAll();
        characterRepository.deleteAll();

        // 삭제된 캐릭터 코드(sad/angry/calm)를 가진 유저 초기화
        userRepository.findAll().forEach(u -> {
            u.setCurrentCharacterCode("happy");
            userRepository.save(u);
        });
        log.info("상점 데이터 초기화 완료");
    }

    private void seedCharacters() {
        List<Character> characters = List.of(
            character("happy",     "행복", img("characters/happy.svg")),
            character("sadness",   "슬픔", img("characters/sadness.svg")),
            character("anger",     "분노", img("characters/anger.svg")),
            character("depressed", "우울", img("characters/depressed.svg")),
            character("anxiety",   "불안", img("characters/anxiety.svg")),
            character("neutral",   "중립", img("characters/neutral.svg"))
        );

        for (Character c : characters) {
            if (!characterRepository.existsByCode(c.getCode())) {
                characterRepository.save(c);
                log.info("캐릭터 시드 등록: {}", c.getCode());
            }
        }
    }

    private void seedItems() {
        List<Item> items = List.of(
            // HAT (16)
            item(ItemCategory.HAT, "모자 1",  img("items/base/hat/hat_01.svg")),
            item(ItemCategory.HAT, "모자 2",  img("items/base/hat/hat_02.svg")),
            item(ItemCategory.HAT, "모자 3",  img("items/base/hat/hat_03.svg")),
            item(ItemCategory.HAT, "모자 4",  img("items/base/hat/hat_04.svg")),
            item(ItemCategory.HAT, "모자 5",  img("items/base/hat/hat_05.svg")),
            item(ItemCategory.HAT, "모자 6",  img("items/base/hat/hat_06.svg")),
            item(ItemCategory.HAT, "모자 7",  img("items/base/hat/hat_07.svg")),
            item(ItemCategory.HAT, "모자 8",  img("items/base/hat/hat_08.svg")),
            item(ItemCategory.HAT, "모자 9",  img("items/base/hat/hat_09.svg")),
            item(ItemCategory.HAT, "모자 10", img("items/base/hat/hat_10.svg")),
            item(ItemCategory.HAT, "모자 11", img("items/base/hat/hat_11.svg")),
            item(ItemCategory.HAT, "모자 12", img("items/base/hat/hat_12.svg")),
            item(ItemCategory.HAT, "모자 13", img("items/base/hat/hat_13.svg")),
            item(ItemCategory.HAT, "모자 14", img("items/base/hat/hat_14.svg")),
            item(ItemCategory.HAT, "모자 15", img("items/base/hat/hat_15.svg")),
            item(ItemCategory.HAT, "모자 16", img("items/base/hat/hat_16.svg")),

            // BACKGROUND (7)
            item(ItemCategory.BACKGROUND, "배경 1", img("items/base/background/bg_01.svg")),
            item(ItemCategory.BACKGROUND, "배경 2", img("items/base/background/bg_02.svg")),
            item(ItemCategory.BACKGROUND, "배경 3", img("items/base/background/bg_03.svg")),
            item(ItemCategory.BACKGROUND, "배경 4", img("items/base/background/bg_04.svg")),
            item(ItemCategory.BACKGROUND, "배경 5", img("items/base/background/bg_05.svg")),
            item(ItemCategory.BACKGROUND, "배경 6", img("items/base/background/bg_06.svg")),
            item(ItemCategory.BACKGROUND, "배경 7", img("items/base/background/bg_07.svg")),

            // ACCESSORY (10)
            item(ItemCategory.ACCESSORY, "악세서리 1",  img("items/base/accessory/acc_01.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 2",  img("items/base/accessory/acc_02.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 3",  img("items/base/accessory/acc_03.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 4",  img("items/base/accessory/acc_04.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 5",  img("items/base/accessory/acc_05.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 6",  img("items/base/accessory/acc_06.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 7",  img("items/base/accessory/acc_07.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 8",  img("items/base/accessory/acc_08.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 9",  img("items/base/accessory/acc_09.svg")),
            item(ItemCategory.ACCESSORY, "악세서리 10", img("items/base/accessory/acc_10.svg")),

            // ETC (3)
            item(ItemCategory.ETC, "기타 1", img("items/base/etc/etc_01.svg")),
            item(ItemCategory.ETC, "기타 2", img("items/base/etc/etc_02.svg")),
            item(ItemCategory.ETC, "기타 3", img("items/base/etc/etc_03.svg"))
        );

        for (Item i : items) {
            if (!itemRepository.existsByNameAndCategory(i.getName(), i.getCategory())) {
                itemRepository.save(i);
                log.info("아이템 시드 등록: [{}] {}", i.getCategory(), i.getName());
            }
        }
    }

    private String img(String path) {
        return assetsBaseUrl + "/images/" + path;
    }

    private Character character(String code, String name, String imageUrl) {
        return Character.builder()
                .code(code)
                .name(name)
                .imageUrl(imageUrl)
                .build();
    }

    private Item item(ItemCategory category, String name, String imageUrl) {
        return Item.builder()
                .category(category)
                .name(name)
                .imageUrl(imageUrl)
                .build();
    }
}
