package org.example.localy.worker;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.localy.entity.ChatMessage;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.service.GPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatWorker {

    private final ChatBotRepository chatBotRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final GPTService gptService;   // 여기에 GPTService 추가


    @PostConstruct
    public void start() {
        // Worker 시작 전에 Stream과 Group이 있는지 확인
        setupStreamAndGroup();

        new Thread(this::consume).start();
    }

    private void setupStreamAndGroup() {
        String streamKey = "localy:chat:stream";
        String groupName = "chat-consumer-group";

        try {
            // Stream이 이미 존재하면 groups() 호출 가능
            objectRedisTemplate.opsForStream().groups(streamKey);
        } catch (Exception e) {
            // Stream 없으면 그룹 생성
            objectRedisTemplate.opsForStream().createGroup(streamKey, groupName);
        }
    }

    private void consume() {
        while (true) {
            List<MapRecord<String, Object, Object>> messages = objectRedisTemplate.opsForStream()
                    .read(Consumer.from("chat-consumer-group", "worker-1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                            StreamOffset.create("localy:chat:stream", ReadOffset.lastConsumed()));

            if (messages == null) continue;

            for (MapRecord<String, Object, Object> record : messages) {
                Long userId = Long.valueOf(record.getValue().get("userId").toString());
                String text = record.getValue().get("text").toString();

                long epochMilli = ((Number) record.getValue().get("createdAt")).longValue();
                LocalDateTime createdAt = Instant.ofEpochMilli(epochMilli)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                // 1️⃣ KoBERT 감정 분석 → 감정 수치 업데이트

                // 2️⃣ 그리움 키워드 후보 감지 -> 필요시 GPT 호출

                // 3️⃣ GPT로 답변 호출
                String botReply = gptService.generateReply(text); // 실제 GPT 호출로 대체

                // 4️⃣ Redis Pub/Sub로 WebSocket 서버에 알림 - OK
                redisTemplate.convertAndSend("localy:chat:bot:" + userId, botReply);
                System.out.println("발행됨: " + botReply + " -> localy:chat:bot:" + userId);

                // 5️⃣ DB 저장 (MySQL) - OK

                ChatMessage userMessage = ChatMessage.builder()
                        .userId(userId)
                        .text(text)
                        .role(ChatMessage.Role.USER)
                        .createdAt(createdAt)
                        .emotionDelta(-5)
                        .emotionAfter(40)
                        .build();

                ChatMessage botMessage = ChatMessage.builder()
                        .userId(userId)
                        .text(botReply)
                        .role(ChatMessage.Role.BOT)
                        .createdAt(LocalDateTime.now())
                        .emotionDelta(null)
                        .emotionAfter(null)
                        .build();

                chatBotRepository.save(userMessage);
                chatBotRepository.save(botMessage);
            }
        }
    }
}
