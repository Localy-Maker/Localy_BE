package org.example.localy.dto.chatBot.response;

import lombok.*;
import org.example.localy.entity.ChatMessage;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {


    private Long id;
    private String role;
    private String text;
    private Integer emotionDelta;
    private Integer emotionAfter;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .role(msg.getRole().name())
                .text(msg.getText())
                .emotionDelta(msg.getEmotionDelta())
                .emotionAfter(msg.getEmotionAfter())
                .createdAt(msg.getCreatedAt())
                .build();
    }

}
