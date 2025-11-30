package org.example.localy.worker;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.response.PredictResponse;
import org.example.localy.dto.chatBot.response.TranslateResponse;
import org.example.localy.entity.ChatMessage;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.service.GPTService;
import org.example.localy.service.PredictClientService;
import org.example.localy.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWorker {

    private final ChatBotRepository chatBotRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final GPTService gptService;   // 여기에 GPTService 추가
    private final PredictClientService predictClient;
    private final TranslationService translationService;

    private static final List<String> LONGING_KEYWORDS = List.of(
            "그리워", "그리움", "보고싶", "보고 싶", "그립", "허전", "외롭", "쓸쓸"
    );


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

                // 0️⃣ 한국어로 번역 - OK
                TranslateResponse translateResponse=translationService.translateToKorean(text);
                String text_ko = translateResponse.getTranslatedText();
                String language = translateResponse.getLanguage();
                log.info("💬 한국어로 번역 완료 / 언어 : "+language);

                // 1️⃣ KoBERT 감정 분석 → 감정 수치 업데이트 - OK
                PredictResponse res = predictClient.requestEmotion(text_ko);

                log.info("😭 감정 분석 결과 라벨 : " + res.getEmotion_name());

                int score = switch (res.getPredicted_label()) {
                    case 1 -> -10;
                    case 2 -> -7;
                    case 3 -> -3;
                    case 4 -> -1;
                    case 5 -> 0;
                    case 6 -> 10;
                    default -> 0;
                };

                String key1 = "localy:emotion:" + userId;

                if(!redisTemplate.hasKey(key1)){
                    redisTemplate.opsForValue().set(key1, "50");
                }

                redisTemplate.opsForValue().increment(key1, score);
                log.info("😆 감정 수치 조절 완료 ");


                // 2️⃣ 그리움 키워드 후보 감지 -> 필요시 GPT 호출 - OK
                boolean hasKeyword = containsLongingKeyword(text_ko);

                if (hasKeyword) {

                    String loging = gptService.logingCheck(text_ko);
                    log.info("☑️ 그리움 단어 체크 : "+loging);
                    if (Objects.equals(loging, "true")) {
                        String key = "localy:emotion:"+userId+":longing";
                        redisTemplate.opsForValue().set(key, "true", 3, TimeUnit.HOURS);
                        log.info("📄 그리움 업데이트 완료");

                    }
                }

                // 3️⃣ GPT로 답변 호출 - OK
                String botReply = gptService.generateReply(text_ko, language); // 실제 GPT 호출로 대체

                // 4️⃣ Redis Pub/Sub로 WebSocket 서버에 알림 - OK
                redisTemplate.convertAndSend("localy:chat:bot:" + userId, botReply);
                log.info("🤖 봇 답변 : " + botReply + " -> localy:chat:bot:" + userId);


                // 5️⃣ DB 저장 (MySQL) - OK

                ChatMessage userMessage = ChatMessage.builder()
                        .userId(userId)
                        .text(text)
                        .role(ChatMessage.Role.USER)
                        .createdAt(createdAt)
                        .emotionDelta(score)
                        .emotionAfter(Integer.parseInt(redisTemplate.opsForValue().get(key1)))
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
                log.info("📄 DB 저장 완료");
            }
        }
    }

    private boolean containsLongingKeyword(String text) {
        String lower = text.toLowerCase();
        return LONGING_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
