package org.example.localy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.ChatMessage;
import org.example.localy.service.Chat.ChatBotService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @MessageMapping("/send")
    public void sendMessage(ChatMessage payload) {
        Long userId = payload.getUserId();
        String text = payload.getText();

        log.info("[ChatController] userId={}, message={}", userId, text);

        // Redis Stream에 저장 & 1:1 Bot 처리
        chatBotService.sendMessage(userId, text);
    }
}
