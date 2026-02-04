package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.Notification;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Mission;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.place.MissionRepository;
import org.example.localy.service.NotificationService;
import org.example.localy.service.mission.MissionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionTimeScheduler {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    private final MissionRepository missionRepository;

    @Scheduled(cron = "0 */2 * * * *")
    @Transactional
    public void checkMissionTime() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeHoursLater = now.plusHours(3);

        List<Mission> expiringSoonMissions = missionRepository.findCompletedMissionsExpiringSoon(now, threeHoursLater);


        for (Mission mission : expiringSoonMissions) {

            if (Boolean.FALSE.equals(redisTemplate.opsForSet().isMember("mission:expiringSoon", mission.getId().toString()))) {
                notificationService.createGeneralNotice(Notification.GeneralNoticeType.MISSIONTIME, mission.getId());
            }
            redisTemplate.opsForSet().add("mission:expiringSoon", mission.getId().toString());

            // 알림 발송 등의 처리
            log.info("미션 {} 3시간 이내 만료 예정", mission.getTitle());
        }
    }
}

