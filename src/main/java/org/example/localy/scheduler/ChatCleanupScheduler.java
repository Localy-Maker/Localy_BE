package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.Users;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCleanupScheduler {

    private static final int BASIC_CHAT_RETENTION_DAYS = 1;
    private static final int PREMIUM_CHAT_RETENTION_DAYS = 5;

    private final ChatBotRepository chatBotRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00
    @Transactional
    public void cleanupOldChats() {

        log.info("✨ DB 청소 시작");

        List<Users> users = userRepository.findAll();

        for (Users user : users) {
            // JPQL에서 LocalDateTime으로 받고 서비스에서 LocalDate로 변환
            List<LocalDate> dates = chatBotRepository.findPastChatDates(user.getId())
                    .stream()
                    .map(LocalDateTime::toLocalDate) // LocalDateTime → LocalDate
                    .distinct() // 중복 제거
                    .toList();

            int keepDays = user.isPremium() ? PREMIUM_CHAT_RETENTION_DAYS : BASIC_CHAT_RETENTION_DAYS;

            if (dates.size() <= keepDays) continue;

            // keepDays 이후 인덱스 = 오래된 날짜들 → 삭제
            List<LocalDate> toDelete = dates.subList(keepDays, dates.size());

            for (LocalDate date : toDelete) {
                chatBotRepository.deleteMessagesByUserIdAndDate(user.getId(), date);
            }

            log.info("💀 유저 {}의 삭제 대상 날짜: {}", user.getId(), toDelete);
        }

        log.info("✨ DB 청소 완료");
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 00:00
    @Transactional
    public void resetEmotion() {
        String keyPrefix = "localy:emotion:";

        List<Users> users = userRepository.findAll();

        for (Users user : users) {
            String key = keyPrefix + user.getId();

            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                redisTemplate.delete(key);
                log.info("🗑️ Redis key 삭제 완료: {}", key);
            }
        }

        // 스트림 키 TTL 1일 설정 (24시간)
        objectRedisTemplate.opsForStream().trim("localy:chat:stream", 0);

        log.info("✨ 모든 유저 감정 키 초기화 완료");
    }
}

