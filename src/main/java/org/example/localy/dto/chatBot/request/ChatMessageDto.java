package org.example.localy.dto.chatBot.request;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
@Builder
public class ChatMessageDto {
    private Long userId;
    private String text;
}