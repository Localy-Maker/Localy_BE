package org.example.localy.entity.shop;

import jakarta.persistence.*;
import lombok.*;
import org.example.localy.entity.Users;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;
}
