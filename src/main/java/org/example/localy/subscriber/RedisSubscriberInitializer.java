package org.example.localy.subscriber;

import lombok.RequiredArgsConstructor;
import org.example.localy.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSubscriberInitializer {

    private final RedisSubscriber redisSubscriber;
    private final UserRepository userRepository; // DB에서 유저 가져올 Repository

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // DB에서 모든 유저 조회
        userRepository.findAll().forEach(user -> {
            String userId = user.getId().toString();
            String channel = "localy:chat:bot:" + userId;
            redisSubscriber.subscribe(channel);
            System.out.println("Redis 구독 시작: " + channel);
        });
    }
}
