package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.place.Mission;
import org.example.localy.repository.place.MissionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCleanupScheduler {

    private final MissionRepository missionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String REDIS_KEY = "mission:expiringSoon";

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredMissions() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 삭제될 미션의 ID만 조회 (효율적)
        List<Long> expiredMissionIds = missionRepository.findExpiredMissionIds(now);

        if (expiredMissionIds.isEmpty()) {
            log.info("삭제할 만료된 미션이 없습니다.");
            return;
        }

        int deletedCount = missionRepository.deleteExpiredMissions(now);

        // 3. Redis Set에서 일괄 제거
        String[] idsToRemove = expiredMissionIds.stream()
                .map(String::valueOf)
                .toArray(String[]::new);

        Long removedCount = redisTemplate.opsForSet().remove(REDIS_KEY, (Object[]) idsToRemove);

        log.info("만료된 미션 정리 완료. DB 삭제: {}개, Redis 제거: {}개",
                deletedCount, removedCount);
    }
}