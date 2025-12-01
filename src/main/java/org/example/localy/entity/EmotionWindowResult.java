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
@Table(name = "emotion_window_result")
public class EmotionWindowResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "time_window", nullable = false, length = 20)
    private String window;

    @Column(nullable = false)
    private Double avgScore;

    @Column(nullable = false, length = 50)
    private String emotion;

    @Column(nullable = false)
    private Integer section;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
