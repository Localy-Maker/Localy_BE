package org.example.localy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(length = 100)
    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    @Column(length = 50)
    private String country;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "display_language", length = 20)
    private String displayLanguage;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "interests", length = 500)
    private String interests;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted = false;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "current_character_code", columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'happy'")
    @Builder.Default
    private String currentCharacterCode = "happy";

    @Column(name = "premium_expires_at")
    private LocalDateTime premiumExpiresAt;

    public enum AuthProvider {
        LOCAL, GOOGLE
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateCountry(String country) {
        this.country = country;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void deductPoints(int amount) {
        if (this.points >= amount) {
            this.points -= amount;
        }
    }

    public boolean isPremium() {
        return premiumExpiresAt != null && premiumExpiresAt.isAfter(LocalDateTime.now());
    }

    public void extendPremium(int days) {
        LocalDateTime base = isPremium() ? premiumExpiresAt : LocalDateTime.now();
        this.premiumExpiresAt = base.plusDays(days);
    }
}
