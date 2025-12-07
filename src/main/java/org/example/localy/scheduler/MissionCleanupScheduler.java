package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.repository.place.MissionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCleanupScheduler {

    private final MissionRepository missionRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredMissions() {
        LocalDateTime now = LocalDateTime.now();

        int deletedCount = missionRepository.deleteExpiredMissions(now);

        log.info("만료된 미션 정리 완료. 삭제된 미션 개수: {}", deletedCount);
    }
}