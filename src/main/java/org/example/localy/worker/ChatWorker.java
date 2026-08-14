package org.example.localy.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.response.PredictResponse;
import org.example.localy.dto.chatBot.response.TranslateResponse;
import org.example.localy.entity.ChatMessage;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.service.Chat.GPTService;
import org.example.localy.service.Chat.PredictClientService;
import org.example.localy.service.Chat.TranslationService;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
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

/**
 * 챗봇 메시지를 비동기로 처리하는 Worker
 * Redis Stream을 통해 메시지를 수신하고, 감정 분석 및 GPT 답변을 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWorker {

    private final ChatBotRepository chatBotRepository;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final GPTService gptService;
    private final PredictClientService predictClient;
    private final TranslationService translationService;

    // Worker 실행 상태 플래그 (종료 시 false로 변경)
    private volatile boolean running = true;

    // Worker 스레드 객체
    private Thread workerThread;

    // 그리움 관련 키워드 목록
    private static final List<String> LONGING_KEYWORDS = List.of(
            "그리워", "그리움", "보고싶", "보고 싶", "그립", "허전", "외롭", "쓸쓸"
    );

    /**
     * 애플리케이션 시작 시 Worker 스레드 실행
     */
    @PostConstruct
    public void start() {
        setupStreamAndGroup();
        workerThread = new Thread(this::consume, "Chat-Worker-Thread");
        workerThread.start();
        log.info("🚀 ChatWorker started.");
    }

    /**
     * 애플리케이션 종료 시 Worker를 안전하게 정리
     */
    @PreDestroy
    public void stop() {
        log.info("🛑 ChatWorker shutting down...");

        running = false; // 루프 중단 신호

        if (workerThread != null) {
            try {
                workerThread.interrupt(); // 블로킹 상태 즉시 해제
                workerThread.join(5000);  // 최대 5초 대기

                if (workerThread.isAlive()) {
                    log.warn("⚠️ Worker thread did not stop in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Interrupted while waiting for worker thread");
            }
        }

        log.info("✅ ChatWorker stopped safely.");
    }

    /**
     * Redis Stream과 Consumer Group 초기 설정
     */
    private void setupStreamAndGroup() {
        String streamKey = "localy:chat:stream";
        String groupName = "chat-consumer-group";

        try {
            objectRedisTemplate.opsForStream().groups(streamKey);
        } catch (Exception e) {
            try {
                objectRedisTemplate.opsForStream().createGroup(streamKey, groupName);
                log.info("📌 Redis Stream & Group 생성 완료");
            } catch (Exception ex) {
                log.warn("⚠️ Stream group creation failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * Redis Stream에서 메시지를 지속적으로 읽어서 처리하는 메인 루프
     */
    private void consume() {
        log.info("🔄 ChatWorker consume loop started");

        while (running) {
            try {
                // Redis Stream에서 메시지 읽기 (2초 블로킹)
                List<MapRecord<String, Object, Object>> messages = readMessages();

                if (!running) break; // 종료 신호 확인
                if (messages == null || messages.isEmpty()) continue;

                // 메시지 처리
                processMessages(messages);

            } catch (QueryTimeoutException e) {
                // Redis 타임아웃은 정상적인 상황 (메시지가 없을 때)
                if (running) {
                    log.debug("⏱️ Redis read timeout (no messages) - this is normal");
                } else {
                    break;
                }

            } catch (RedisConnectionFailureException | RedisSystemException e) {
                // Redis 연결 오류 처리
                if (running) {
                    log.warn("⚠️ Redis connection error (will retry): {}", e.getMessage());
                    sleepSafely(1000); // 1초 후 재시도
                } else {
                    log.debug("Redis connection closed during shutdown - this is expected");
                    break;
                }
            } catch (IllegalStateException e) {
                // LettuceConnectionFactory는 SmartLifecycle 단계에서 ChatWorker의 @PreDestroy보다
                // 먼저 정지될 수 있어, running=false로 바뀌기 전에 이 예외가 발생할 수 있다.
                // Redis 연결이 이미 끊긴 상태라 재시도해도 의미가 없으므로 바로 루프를 종료한다.
                log.info("🛑 Redis 연결이 종료되어 ChatWorker consume loop를 종료합니다: {}", e.getMessage());
                break;
            } catch (Exception e) {
                // 기타 예외 처리
                if (running) {
                    log.error("❌ Unexpected error in worker loop", e);
                    sleepSafely(1000);
                } else {
                    // 종료 중 발생한 예외는 무시
                    if (e.getCause() instanceof InterruptedException ||
                            Thread.currentThread().isInterrupted()) {
                        log.debug("Interrupted during shutdown - this is expected");
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }
        }

        log.info("🛑 ChatWorker consume loop exited");
    }

    /**
     * Redis Stream에서 메시지 읽기
     * @return 읽어온 메시지 목록
     */
    private List<MapRecord<String, Object, Object>> readMessages() {
        try {
            return objectRedisTemplate.opsForStream().read(
                    Consumer.from("chat-consumer-group", "worker-1"),
                    StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                    StreamOffset.create("localy:chat:stream", ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            if (!running) {
                return null; // 종료 중이면 null 반환
            }
            throw e; // 실행 중이면 예외 전파
        }
    }

    /**
     * 여러 메시지를 순회하며 처리
     * @param messages 처리할 메시지 목록
     */
    private void processMessages(List<MapRecord<String, Object, Object>> messages) {
        for (MapRecord<String, Object, Object> record : messages) {
            if (!running) break; // 종료 신호 확인

            try {
                processMessage(record);
            } catch (Exception e) {
                log.error("❌ Failed to process message: {}", record.getId(), e);
                // 개별 메시지 처리 실패 시 다음 메시지 계속 처리
            }
        }
    }

    /**
     * 단일 메시지 처리 파이프라인
     * 번역 → 감정 분석 → 그리움 감지 → GPT 답변 생성 → 알림 → DB 저장
     * @param record 처리할 메시지
     */
    private void processMessage(MapRecord<String, Object, Object> record) {
        // 메시지 데이터 추출
        Long userId = Long.valueOf(record.getValue().get("userId").toString());
        String text = record.getValue().get("text").toString();

        long epochMilli = ((Number) record.getValue().get("createdAt")).longValue();
        LocalDateTime createdAt = Instant.ofEpochMilli(epochMilli)
                .atZone(ZoneId.systemDefault()).toLocalDateTime();

        // 1. 번역 (외국어 → 한국어)
        TranslateResponse translateResponse = translationService.translateToKorean(text);
        String text_ko = translateResponse.getTranslatedText();
        String language = translateResponse.getLanguage();
        log.info("💬 한국어 번역 완료 / 언어 : {}", language);

        // 2. 감정 분석 (KoBERT)
        PredictResponse res = predictClient.requestEmotion(text_ko);
        log.info("😭 감정 분석 라벨 : {}", res.getEmotion_name());

        int score = calculateEmotionScore(res.getPredicted_label());
        updateEmotionScore(userId, score);

        // 3. 그리움 감지
        checkLonging(userId, text_ko);

        // 4. GPT 답변 생성
        String botReply = gptService.generateReply(text_ko, language);

        // 5. WebSocket으로 사용자에게 알림
        notifyUser(userId, botReply);

        // 6. DB에 메시지 저장
        saveMessages(userId, text, botReply, createdAt, score);
    }

    /**
     * 감정 라벨을 점수로 변환
     * @param label 감정 라벨 (1-6)
     * @return 감정 점수 (-10 ~ +10)
     */
    private int calculateEmotionScore(int label) {
        return switch (label) {
            case 1 -> -15; // 매우 부정
            case 2 -> -10;
            case 3 -> -5;
            case 4 -> 0;
            case 5 -> 7;   // 중립
            case 6 -> 15;  // 매우 긍정
            default -> 0;
        };
    }

    /**
     * 사용자의 감정 수치를 Redis에 업데이트
     * @param userId 사용자 ID
     * @param score 변경할 감정 점수
     */
    private void updateEmotionScore(Long userId, int score) {
        String key = "localy:emotion:" + userId;

        try {
            // 감정 수치 초기화 (없으면 기본값 50)
            if (!redisTemplate.hasKey(key)) {
                redisTemplate.opsForValue().set(key, "50");
            }
            // 점수 증감
            redisTemplate.opsForValue().increment(key, score);
            log.info("😆 감정 수치 조절 완료");
        } catch (Exception e) {
            log.error("❌ Failed to update emotion score", e);
        }
    }

    /**
     * 그리움 키워드 감지 및 GPT로 재확인
     * @param userId 사용자 ID
     * @param text_ko 한국어 텍스트
     */
    private void checkLonging(Long userId, String text_ko) {
        if (containsLongingKeyword(text_ko)) {
            try {
                // GPT로 그리움 여부 재확인
                String longing = gptService.logingCheck(text_ko);
                log.info("☑️ 그리움 단어 체크 : {}", longing);

                if (Objects.equals(longing, "true")) {
                    // Redis에 그리움 상태 저장 (3시간 TTL)
                    redisTemplate.opsForValue().set(
                            "localy:emotion:" + userId + ":longing",
                            "true",
                            3,
                            TimeUnit.HOURS
                    );
                    log.info("📄 그리움 상태 업데이트 완료");
                }
            } catch (Exception e) {
                log.error("❌ Failed to check longing", e);
            }
        }
    }

    /**
     * Redis Pub/Sub을 통해 사용자에게 봇 응답 전송
     * @param userId 사용자 ID
     * @param botReply 봇 응답 메시지
     */
    private void notifyUser(Long userId, String botReply) {
        try {
            redisTemplate.convertAndSend("localy:chat:bot:" + userId, botReply);
            log.info("🤖 유저에게 전달 : {}", botReply);
        } catch (Exception e) {
            log.error("❌ Failed to notify user", e);
        }
    }

    /**
     * 사용자 메시지와 봇 응답을 DB에 저장
     * @param userId 사용자 ID
     * @param text 원본 메시지
     * @param botReply 봇 응답
     * @param createdAt 메시지 생성 시간
     * @param score 감정 점수 변화량
     */
    private void saveMessages(Long userId, String text, String botReply,
                              LocalDateTime createdAt, int score) {
        try {
            String key = "localy:emotion:" + userId;
            int emotionAfter = Integer.parseInt(
                    redisTemplate.opsForValue().get(key)
            );

            // 사용자 메시지 엔티티 생성
            ChatMessage userMessage = ChatMessage.builder()
                    .userId(userId)
                    .text(text)
                    .role(ChatMessage.Role.USER)
                    .createdAt(createdAt)
                    .emotionDelta(score)
                    .emotionAfter(emotionAfter)
                    .build();

            // 봇 메시지 엔티티 생성
            ChatMessage botMessage = ChatMessage.builder()
                    .userId(userId)
                    .text(botReply)
                    .role(ChatMessage.Role.BOT)
                    .createdAt(LocalDateTime.now())
                    .build();

            // DB 저장
            chatBotRepository.save(userMessage);
            chatBotRepository.save(botMessage);

            log.info("📄 DB 저장 완료");
        } catch (Exception e) {
            log.error("❌ Failed to save messages to DB", e);
        }
    }

    /**
     * 텍스트에 그리움 키워드가 포함되어 있는지 확인
     * @param text 확인할 텍스트
     * @return 키워드 포함 여부
     */
    private boolean containsLongingKeyword(String text) {
        String lower = text.toLowerCase();
        return LONGING_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * InterruptedException을 처리하며 안전하게 대기
     * @param millis 대기 시간(밀리초)
     */
    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // interrupt 상태 복원
        }
    }
}