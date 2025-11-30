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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmotionDataService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final List<EmotionRange> EMOTION_RANGES = List.of(
            new EmotionRange(15, "depressed", 0.45), // 0-15: 우울 (매우 부정)
            new EmotionRange(35, "sadness", 0.4),    // 16-35: 슬픔 (부정)
            new EmotionRange(55, "anger", 0.35),     // 36-55: 분노 (부정) - 사용자 요청의 '분노' 반영
            new EmotionRange(75, "anxiety", 0.3),    // 56-75: 불안 (중립/부정)
            new EmotionRange(90, "neutral", 0.5),    // 76-90: 중립 (Stable)
            new EmotionRange(100, "happy", 0.6)      // 91-100: 행복 (매우 긍정)
    );

    // Redis 키 형식
    private static final String EMOTION_KEY_PREFIX = "localy:emotion:";
    private static final String NOSTALGIA_KEY_SUFFIX = ":longing";

    // 사용자 현재 감정 확인
    public RecommendDto.EmotionData getCurrentEmotion(Users user) {
        String emotionKey = EMOTION_KEY_PREFIX + user.getId();
        String nostalgiaKey = emotionKey + NOSTALGIA_KEY_SUFFIX;

        Boolean isHomesickMode = false;
        LocalDateTime homesickActivatedAt = null;

        String nostalgiaValue = redisTemplate.opsForValue().get(nostalgiaKey);
        if ("true".equals(nostalgiaValue)) {
            isHomesickMode = true;
            homesickActivatedAt = LocalDateTime.now();
            log.info("그리움 모드 활성 상태: userId={}", user.getId());
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

    // 감정 데이터 조회
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
        Map<String, Double> emotions = getDefaultEmotions();
        String dominantKeyword = "neutral";
        double foundDominantWeight = 0.2;

        for (EmotionRange range : EMOTION_RANGES) {
            if (score <= range.rangeEnd) {
                dominantKeyword = range.keyword;
                foundDominantWeight = range.weight;
                break;
            }
        }

        final double finalDominantWeight = foundDominantWeight;

        emotions.replaceAll((k, v) -> v * (1 - finalDominantWeight));

        emotions.put(dominantKeyword, finalDominantWeight);

        return emotions;
    }

    private Map<String, Double> getDefaultEmotions() {
        Map<String, Double> emotions = new HashMap<>();
        emotions.put("happy", 0.1);
        emotions.put("sadness", 0.1);
        emotions.put("depressed", 0.1);
        emotions.put("anger", 0.1);
        emotions.put("anxiety", 0.1);
        emotions.put("neutral", 0.1);
        return emotions;
    }

    // 고향 모드 활성화
    public void activateHomesickMode(Users user) {
        String nostalgiaKey = EMOTION_KEY_PREFIX + user.getId() + NOSTALGIA_KEY_SUFFIX;
        redisTemplate.opsForValue().set(nostalgiaKey, "true", Duration.ofHours(5));
        log.info("고향 모드 활성화: userId={}, TTL=5시간", user.getId());
    }

    // 고향 모드 비활성화
    public void deactivateHomesickMode(Users user) {
        String nostalgiaKey = EMOTION_KEY_PREFIX + user.getId() + NOSTALGIA_KEY_SUFFIX;
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

    // 감정 구간 정의를 위한 내부 클래스
    private static class EmotionRange {
        int rangeEnd;
        String keyword;
        double weight;

        public EmotionRange(int rangeEnd, String keyword, double weight) {
            this.rangeEnd = rangeEnd;
            this.keyword = keyword;
            this.weight = weight;
        }
    }
}