package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.localy.entity.Notification;
import org.example.localy.entity.Users;
import org.example.localy.repository.UserRepository;
import org.example.localy.service.NotificationService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InactiveUserScheduler {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    // 6시간마다 실행
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void checkDormantUsers() {

        LocalDateTime time = LocalDateTime.now().minusHours(48);

        List<Users> dormantCandidates =
                userRepository.findByLastLoginTimeIsNullOrLastLoginTimeBefore(time);

        for (Users user : dormantCandidates) {
            if (Boolean.FALSE.equals(redisTemplate.opsForSet().isMember("user:noActivity", user.getId().toString()))) {
                notificationService.createGeneralNotice(Notification.GeneralNoticeType.LASTLOGINTIME, user.getId());
            }
            redisTemplate.opsForSet().add("user:noActivity", String.valueOf(user.getId()));
        }
    }
}
