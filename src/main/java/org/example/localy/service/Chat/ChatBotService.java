package org.example.localy.service.Chat;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static org.example.localy.entity.ChatMessage.Role.USER;

@Service
@RequiredArgsConstructor
public class ChatBotService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public void sendMessage(Long userId, String message) {

        // 유저 감정 초기화
        String key = "localy:emotion:" + userId;
        if (!redisTemplate.hasKey(key)) {
            redisTemplate.opsForValue().set(key, "50");
        }

        // Redis Stream에 메시지 저장
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("text", message);
        map.put("speaker", USER.name());
        map.put("createdAt", System.currentTimeMillis());

        objectRedisTemplate.opsForStream().add("localy:chat:stream", map);

        // 1:1 구독 WebSocket으로 알림 (임시, BOT 처리 후 보내는 용도)
        // messagingTemplate.convertAndSend("/topic/chat/" + userId, message);
        // Bot 응답은 ChatWorker에서 처리
    }
}
