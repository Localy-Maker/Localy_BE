package org.example.localy.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtStompInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        log.info("[STOMP] preSend called, command={}, sessionAttributes={}",
                accessor.getCommand(), accessor.getSessionAttributes());

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 🔹 CONNECT 프레임 헤더에서 JWT 읽기
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    jwtUtil.validateToken(token);
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String email = jwtUtil.getEmailFromToken(token);

                    accessor.setUser(new StompUser(userId, email));

                    log.info("[STOMP] CONNECT JWT 검증 성공: userId={}, email={}", userId, email);
                } catch (Exception e) {
                    log.warn("[STOMP] JWT 검증 실패: {}", e.getMessage());
                    throw new RuntimeException("Invalid JWT");
                }
            } else {
                log.warn("[STOMP] CONNECT Authorization header 없음");
                throw new RuntimeException("JWT 필요");
            }
        }

        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
}
