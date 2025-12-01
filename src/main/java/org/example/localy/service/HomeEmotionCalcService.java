package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.repository.EmotionDayResultRepository;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.example.localy.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeEmotionCalcService {

    private final EmotionWindowResultRepository windowRepo;
    private final EmotionDayResultRepository dayRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository; // 모든 유저 ID 조회용

    // 이번 주 시작 ~ 끝
    private LocalDateTime getStartOfWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    }

    private LocalDateTime getEndOfWeek() {
        return LocalDate.now().with(DayOfWeek.SUNDAY).atTime(23, 59, 59);
    }

    // 지난 주
    private LocalDate getLastWeekStart() {
        return LocalDate.now().minusWeeks(1).with(DayOfWeek.MONDAY);
    }

    private LocalDate getLastWeekEnd() {
        return LocalDate.now().minusWeeks(1).with(DayOfWeek.SUNDAY);
    }

    // 이번 주 (어제까지)
    private LocalDate getThisWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    private LocalDate getThisWeekEnd() {
        return LocalDate.now().minusDays(1); // 어제까지
    }

    /**
     * 1) 이번주 최빈 감정 계산
     */
    private String calcMostFrequentEmotion(Long userId) {
        List<String> emotions = windowRepo.findEmotionsByUserAndWeek(
                userId,
                getStartOfWeek(),
                getEndOfWeek()
        );

        if (emotions.isEmpty()) return "없음";

        return emotions.stream()
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    /**
     * 2) 지난주 vs 이번주 평균 행복지수 변화율
     */
    private double calcHappinessDiff(Long userId) {

        List<Double> lastWeek = dayRepo.findAvgScoresByDateRange(
                userId, getLastWeekStart(), getLastWeekEnd()
        );

        log.info("lastWeek: {}", lastWeek);

        List<Double> thisWeek = dayRepo.findAvgScoresByDateRange(
                userId, getThisWeekStart(), getThisWeekEnd()
        );

        log.info("thisWeek: {}", thisWeek);

        double lastAvg = lastWeek.stream().mapToDouble(Double::doubleValue).average().orElse(50);
        double thisAvg = thisWeek.stream().mapToDouble(Double::doubleValue).average().orElse(50);

        log.info("lastAvg: {}, thisAvg: {}", lastAvg, thisAvg);

        if (lastAvg == 0) return 0.0;

        return (thisAvg - lastAvg);
    }

    /**
     * 3) Redis 저장
     */
    private void saveToRedis(Long userId, String emotion, double diff) {
        redisTemplate.opsForValue().set("localy:home:emotion:" + userId, emotion);
        redisTemplate.opsForValue().set("localy:home:happiness_diff:" + userId, String.valueOf(diff));
    }

    /**
     * 4) 전체 유저 처리
     */
    @Transactional
    public void updateAllUsers() {
        List<Long> userIds = userRepository.findAllUserIds();

        for (Long userId : userIds) {
            String emotion = calcMostFrequentEmotion(userId);
            log.info("emotion={}", emotion);
            double diff = calcHappinessDiff(userId);
            log.info("diff={}", diff);
            saveToRedis(userId, emotion, diff);
        }
    }
}
