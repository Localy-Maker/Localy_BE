package org.example.localy.repository.shop;

import org.example.localy.entity.Users;
import org.example.localy.entity.shop.Item;
import org.example.localy.entity.shop.ItemCategory;
import org.example.localy.entity.shop.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    boolean existsByUserAndItem(Users user, Item item);

    @Query("SELECT ui.item.id FROM UserItem ui WHERE ui.user = :user")
    Set<Long> findOwnedItemIdsByUser(@Param("user") Users user);

    @Query("""
        SELECT ui FROM UserItem ui
        JOIN FETCH ui.item i
        WHERE ui.user = :user
        ORDER BY
            CASE i.category
                WHEN 'BACKGROUND' THEN 1
                WHEN 'HAT' THEN 2
                WHEN 'ACCESSORY' THEN 3
                WHEN 'ETC' THEN 4
            END ASC,
            ui.purchasedAt DESC
        """)
    List<UserItem> findAllByUserOrderedByCategoryAndPurchasedAt(@Param("user") Users user);
}
