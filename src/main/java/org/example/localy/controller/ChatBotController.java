package org.example.localy.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.request.ChatMessageDto;
import org.example.localy.entity.ChatMessage;
import org.example.localy.service.Chat.ChatBotService;
import org.example.localy.util.JwtUtil;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.Header;


@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;
    private final JwtUtil jwtUtil;

    @MessageMapping("/send")
    public void sendMessage(@Header("Authorization") String authHeader,
                            String text) {
//        Long userId = payload.getUserId();
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
//        String text = payload.getText();
//        String text = chatMessageDto.getText();

        log.info("[ChatController] userId={}, message={}", userId, text);

        // Redis Stream에 저장 & 1:1 Bot 처리
        chatBotService.sendMessage(userId, text);
    }
}
