package org.example.localy.dto.emotion;

import org.example.localy.entity.ChatMessage;

public class EmotionLogMapper {

    public static EmotionLogDto from(ChatMessage msg) {
        return EmotionLogDto.builder()
                .userId(msg.getUserId())
                .text(msg.getText())
                .emotionAfter(msg.getEmotionAfter())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}