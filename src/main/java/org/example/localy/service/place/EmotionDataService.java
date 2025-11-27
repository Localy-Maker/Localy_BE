package org.example.localy.service.place;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.place.RecommendDto;
import org.example.localy.entity.Users;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// TODO: 감정 모델 구현 후 추가 수정 필요
@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionDataService {

    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String EMOTION_KEY_PREFIX = "localy:emotion:";
    private static final String HOMESICK_KEY_PREFIX = "localy:homesick:";

    // 사용자 현재 감정 확인
    public RecommendDto.EmotionData getCurrentEmotion(Users user) {
        String emotionKey = EMOTION_KEY_PREFIX + user.getId();
        String homesickKey = HOMESICK_KEY_PREFIX + user.getId();

        // 고향 모드 확인
        Boolean isHomesickMode = false;
        LocalDateTime homesickActivatedAt = null;

        String homesickData = redisTemplate.opsForValue().get(homesickKey);
        if (homesickData != null) {
            try {
                homesickActivatedAt = LocalDateTime.parse(homesickData);
                Duration duration = Duration.between(homesickActivatedAt, LocalDateTime.now());
                if (duration.toHours() < 5) {
                    isHomesickMode = true;
                } else {
                    redisTemplate.delete(homesickKey);
                }
            } catch (Exception e) {
                log.error("고향 감정 데이터 파싱 실패", e);
            }
        }

        // 감정 데이터 조회
        Map<String, Double> emotions = getEmotionsFromRedis(emotionKey);
        String dominantEmotion = findDominantEmotion(emotions);

        return RecommendDto.EmotionData.builder()
                .emotions(emotions)
                .dominantEmotion(dominantEmotion)
                .lastUpdated(LocalDateTime.now())
                .isHomesickMode(isHomesickMode)
                .homesickActivatedAt(homesickActivatedAt)
                .build();
    }

    private Map<String, Double> getEmotionsFromRedis(String emotionKey) {
        // 이후 로직 작성
        log.warn("감정 데이터 조회 미구현");
        return new HashMap<>();
    }

    // 고향 모드 활성화
    public void activateHomesickMode(Users user) {
        String homesickKey = HOMESICK_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(homesickKey, LocalDateTime.now().toString(), Duration.ofHours(5));
        log.info("고향 모드 활성화: userId={}", user.getId());
    }

    // 고향 모드 비활성화
    public void deactivateHomesickMode(Users user) {
        String homesickKey = HOMESICK_KEY_PREFIX + user.getId();
        redisTemplate.delete(homesickKey);
        log.info("고향 모드 비활성화: userId={}", user.getId());
    }

    // 주요 감정 판별
    private String findDominantEmotion(Map<String, Double> emotions) {
        if (emotions == null || emotions.isEmpty()) {
            return "neutral";
        }

        return emotions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");
    }
}