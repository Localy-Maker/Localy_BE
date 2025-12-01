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

    private final ChatBotRepository chatBotRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    @Scheduled(cron = "0 0 0 * * *") // 매일 08:18
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

            if (dates.size() <= 1) continue; // 최근 1개만 유지 → 삭제할 게 없음

            // 최근 1개 제외한 나머지 날짜
            List<LocalDate> toDelete = dates.subList(1, dates.size());

            for (LocalDate date : toDelete) {
                chatBotRepository.deleteMessagesByUserIdAndDate(user.getId(), date);
            }

            log.info("💀 유저 {}의 삭제 대상 날짜: {}", user.getId(), toDelete);
        }

        log.info("✨ DB 청소 완료");
    }

    @Scheduled(cron = "0 0 0 * * *") // 매일 08:18
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

