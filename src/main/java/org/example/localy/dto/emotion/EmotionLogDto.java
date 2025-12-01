package org.example.localy.dto.emotion;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class EmotionLogDto {

    private Long userId;
    private String text;
    private Integer emotionAfter;
    private LocalDateTime createdAt;
}

