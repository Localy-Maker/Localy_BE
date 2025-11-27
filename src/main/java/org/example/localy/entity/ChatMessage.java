package org.example.localy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;  // USER or BOT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "emotion_delta")
    private Integer emotionDelta;     // KoBERT 감정 변화량 (예: +15, -10)

    @Column(name = "emotion_after")
    private Integer emotionAfter;     // 변화 후 감정 점수 (0~100)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum Role {
        USER,
        BOT
    }
}