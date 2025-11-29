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

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionDataService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis 키 형식 (협업자 ChatWorker 기준)
    private static final String EMOTION_KEY_PREFIX = "localy:emotion:";
    private static final String NOSTALGIA_KEY_PREFIX = "localy:homesick:";

    // 사용자 현재 감정 확인
    public RecommendDto.EmotionData getCurrentEmotion(Users user) {
        String emotionKey = EMOTION_KEY_PREFIX + user.getId();
        String nostalgiaKey = NOSTALGIA_KEY_PREFIX + user.getId();

        // 그리움 모드 확인
        Boolean isHomesickMode = false;
        LocalDateTime homesickActivatedAt = null;

        String nostalgiaValue = redisTemplate.opsForValue().get(nostalgiaKey);
        if ("true".equals(nostalgiaValue)) {
            isHomesickMode = true;
            homesickActivatedAt = LocalDateTime.now();
            log.info("그리움 모드 활성 상태: userId={}", user.getId());
        }

        // 감정 데이터 조회 (협업자 테스트 코드: 단순 정수값)
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

    // 감정 데이터 조회 (협업자 테스트 코드 기반)
    private Map<String, Double> getEmotionsFromRedis(String emotionKey) {
        try {
            String emotionScore = redisTemplate.opsForValue().get(emotionKey);

            if (emotionScore != null) {
                int score = Integer.parseInt(emotionScore);
                return convertScoreToEmotionMap(score);
            }

            log.warn("감정 데이터 없음: {}", emotionKey);
            return getDefaultEmotions();

        } catch (Exception e) {
            log.error("Redis 감정 데이터 조회 실패: {}", emotionKey, e);
            return getDefaultEmotions();
        }
    }

    // 감정 수치(0~100)를 감정 맵으로 변환
    private Map<String, Double> convertScoreToEmotionMap(int score) {
        Map<String, Double> emotions = new HashMap<>();

        if (score <= 30) {
            // 매우 부정적 (0~30)
            emotions.put("sadness", 0.4);
            emotions.put("loneliness", 0.3);
            emotions.put("fear", 0.2);
            emotions.put("joy", 0.1);
        } else if (score <= 50) {
            // 부정적 (31~50)
            emotions.put("loneliness", 0.35);
            emotions.put("sadness", 0.25);
            emotions.put("anger", 0.2);
            emotions.put("joy", 0.2);
        } else if (score <= 70) {
            // 중립 (51~70)
            emotions.put("joy", 0.3);
            emotions.put("surprise", 0.25);
            emotions.put("loneliness", 0.25);
            emotions.put("sadness", 0.2);
        } else {
            // 긍정적 (71~100)
            emotions.put("joy", 0.5);
            emotions.put("surprise", 0.25);
            emotions.put("loneliness", 0.15);
            emotions.put("sadness", 0.1);
        }

        return emotions;
    }

    // 기본 감정 데이터
    private Map<String, Double> getDefaultEmotions() {
        Map<String, Double> emotions = new HashMap<>();
        emotions.put("joy", 0.2);
        emotions.put("sadness", 0.2);
        emotions.put("anger", 0.1);
        emotions.put("fear", 0.1);
        emotions.put("disgust", 0.1);
        emotions.put("surprise", 0.1);
        emotions.put("loneliness", 0.2);
        return emotions;
    }

    // 고향 모드 활성화
    public void activateHomesickMode(Users user) {
        String nostalgiaKey = NOSTALGIA_KEY_PREFIX + user.getId();
        redisTemplate.opsForValue().set(nostalgiaKey, "true", Duration.ofHours(5));
        log.info("고향 모드 활성화: userId={}, TTL=5시간", user.getId());
    }

    // 고향 모드 비활성화
    public void deactivateHomesickMode(Users user) {
        String nostalgiaKey = NOSTALGIA_KEY_PREFIX + user.getId();
        redisTemplate.delete(nostalgiaKey);
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