package org.example.localy.repository.shop;

import org.example.localy.entity.shop.Item;
import org.example.localy.entity.shop.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    boolean existsByNameAndCategory(String name, ItemCategory category);

    List<Item> findAllByCategory(ItemCategory category);
}
