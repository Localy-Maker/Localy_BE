package org.example.localy.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.shop.Character;
import org.example.localy.entity.shop.Item;
import org.example.localy.entity.shop.ItemCategory;
import org.example.localy.repository.shop.CharacterRepository;
import org.example.localy.repository.shop.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopDataInitializer implements CommandLineRunner {

    private final CharacterRepository characterRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedCharacters();
        seedItems();
    }

    private void seedCharacters() {
        List<Character> characters = List.of(
            character("happy",     "행복",   "https://placeholder.local/characters/happy.png"),
            character("sad",       "슬픔",   "https://placeholder.local/characters/sad.png"),
            character("angry",     "분노",   "https://placeholder.local/characters/angry.png"),
            character("depressed", "우울",   "https://placeholder.local/characters/depressed.png"),
            character("neutral",   "무표정", "https://placeholder.local/characters/neutral.png"),
            character("calm",      "평온",   "https://placeholder.local/characters/calm.png")
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
            item(ItemCategory.BACKGROUND, "여름 해변",   "https://placeholder.local/items/background_summer_beach.png"),
            item(ItemCategory.BACKGROUND, "별빛 밤하늘", "https://placeholder.local/items/background_starry_night.png"),
            item(ItemCategory.BACKGROUND, "도시 야경",   "https://placeholder.local/items/background_city_night.png"),

            item(ItemCategory.HAT, "파란 베레모",   "https://placeholder.local/items/hat_blue_beret.png"),
            item(ItemCategory.HAT, "빨간 야구 모자", "https://placeholder.local/items/hat_red_baseball.png"),
            item(ItemCategory.HAT, "별 왕관",       "https://placeholder.local/items/hat_star_crown.png"),

            item(ItemCategory.ACCESSORY, "하트 목걸이",  "https://placeholder.local/items/accessory_heart_necklace.png"),
            item(ItemCategory.ACCESSORY, "둥근 안경",    "https://placeholder.local/items/accessory_round_glasses.png"),
            item(ItemCategory.ACCESSORY, "무지개 귀걸이", "https://placeholder.local/items/accessory_rainbow_earring.png"),

            item(ItemCategory.ETC, "작은 별 스티커", "https://placeholder.local/items/etc_star_sticker.png"),
            item(ItemCategory.ETC, "구름 풍선",      "https://placeholder.local/items/etc_cloud_balloon.png"),
            item(ItemCategory.ETC, "리본 장식",      "https://placeholder.local/items/etc_ribbon.png")
        );

        for (Item i : items) {
            if (!itemRepository.existsByNameAndCategory(i.getName(), i.getCategory())) {
                itemRepository.save(i);
                log.info("아이템 시드 등록: [{}] {}", i.getCategory(), i.getName());
            }
        }
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
