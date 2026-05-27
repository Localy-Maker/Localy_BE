package org.example.localy.entity.shop;

import jakarta.persistence.*;
import lombok.*;
import org.example.localy.entity.Users;

@Entity
@Table(
    name = "user_character_states",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "character_code"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserCharacterState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "character_code", nullable = false, length = 20)
    private String characterCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_background_id")
    private Item equippedBackground;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_hat_id")
    private Item equippedHat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_accessory_id")
    private Item equippedAccessory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipped_etc_id")
    private Item equippedEtc;

    public static UserCharacterState emptyState(Users user, String characterCode) {
        return UserCharacterState.builder()
                .user(user)
                .characterCode(characterCode)
                .build();
    }
}
