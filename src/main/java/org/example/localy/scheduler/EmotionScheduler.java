package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.exception.errorCode.ChatErrorCode;
import org.example.localy.dto.emotion.EmotionLogDto;
import org.example.localy.dto.emotion.EmotionLogMapper;
import org.example.localy.entity.ChatMessage;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.example.localy.service.EmotionAnalysisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Service
@RequiredArgsConstructor
public class EmotionScheduler {

    private final ChatBotRepository chatBotRepository;
    private final EmotionAnalysisService emotionAnalysisService;
    private final EmotionWindowResultRepository windowRepository;
    private final RedisTemplate<String, String> redisTemplate;

    // 3분마다 실행 (실 환경에서는 3시간)
    @Scheduled(cron = "0 0 * * * *")
    public void analyzeWindow() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(1);
        String window = getCurrentWindow();

        // 최근 3시간 메시지
        List<ChatMessage> recentMessages =
                chatBotRepository.findByCreatedAtBetween(start, now);

        // 오늘 날짜 기준 모든 유저 중 "오늘 메시지를 남긴 유저만" 대상
        List<Long> todayUserIds =
                chatBotRepository.findDistinctUserIdsToday(LocalDate.now().atStartOfDay(), now);

        for (Long userId : todayUserIds) {

            // 유저의 최근 3시간 메시지
            List<ChatMessage> userRecentMsgs = recentMessages.stream()
                    .filter(m -> m.getUserId().equals(userId) && m.getRole() == ChatMessage.Role.USER)
                    .toList();

            if (!userRecentMsgs.isEmpty()) {
                // 1️⃣ 최근 3시간 메시지가 있을 경우 정상 분석
                List<EmotionLogDto> logs = userRecentMsgs.stream()
                        .map(EmotionLogMapper::from)
                        .filter(l -> l.getEmotionAfter() != null)
                        .toList();

                /*if (!logs.isEmpty()) {
                    // ⭐ 평균 점수 계산
                    double avgScore = logs.stream()
                            .mapToDouble(EmotionLogDto::getEmotionAfter)
                            .average()
                            .orElse(0.0);

                    // ⭐ 변경된 메서드에 맞게 avgScore 전달
                    emotionAnalysisService.saveWindowResult(userId, window, avgScore);
                    continue;
                }*/

                if (!logs.isEmpty()) {

                    // 가중치와 점수를 함께 계산
                    class WeightedScore {
                        double score;
                        double weight;
                        WeightedScore(double score, double weight) {
                            this.score = score;
                            this.weight = weight;
                        }
                    }

                    List<WeightedScore> weightedScores = logs.stream()
                            .map(log -> {
                                long minutesAgo = Duration.between(log.getCreatedAt(), now).toMinutes();
                                double weight = Math.max(0, 60 - minutesAgo);
                                return new WeightedScore(log.getEmotionAfter(), weight);
                            })
                            .toList();

                    double totalWeightedScore = weightedScores.stream()
                            .mapToDouble(ws -> ws.score * ws.weight)
                            .sum();

                    double totalWeight = weightedScores.stream()
                            .mapToDouble(ws -> ws.weight)
                            .sum();

                    double avgScore = totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;

                    emotionAnalysisService.saveWindowResult(userId, window, avgScore);
                    continue;
                }
            }

            // 2️⃣ 최근 3시간 메시지가 없는 경우 (오늘 하루 과거 기록은 있음)
            handleEmptyWindow(userId, window);
        }
    }


    private void handleEmptyWindow(Long userId, String window) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        log.info("Start of day: {} now: {} userId: {} window: {}",
                startOfDay, now, userId, window);

        Long todayAllTextCounts = chatBotRepository.countTodayUserMessages(userId, startOfDay, now);
        log.info("todayAllTextCounts: {}", todayAllTextCounts);

        if (todayAllTextCounts == 0) {
            // 오늘 메시지가 없으면 저장 X
            log.info("오늘 메시지 없음 -> 저장 X");
            return;
        }

        // 오늘 저장된 윈도우 감정 기록 조회
        Optional<EmotionWindowResult> todayResults =
                windowRepository.findFirstByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        userId, startOfDay, now
                );

        // Redis에서 최신 감정 점수 가져오기
        String latestEmotion = redisTemplate.opsForValue().get("localy:emotion:" + userId);
        log.info("현재 기분 (Redis): {}", latestEmotion);

        int nowEmotion = (latestEmotion != null) ? Integer.parseInt(latestEmotion) : 0;

        double newAvg;

        if (todayResults.isPresent()) {
            // ✅ 이전 윈도우 결과가 있으면 평균 계산
            EmotionWindowResult todayResult = todayResults.get();
            newAvg = (todayResult.getAvgScore() + nowEmotion) / 2.0;
            log.info("이전 결과 있음. 평균 계산: ({} + {}) / 2 = {}",
                    todayResult.getAvgScore(), nowEmotion, newAvg);
        } else {
            // ✅ 오늘 첫 윈도우 분석이면 현재 감정만 사용
            newAvg = nowEmotion;
            log.info("오늘 첫 윈도우 분석. 현재 감정 사용: {}", newAvg);
        }

        emotionAnalysisService.saveWindowResult(userId, window, newAvg);
    }


    public String getCurrentWindow() {
        int hour = LocalDateTime.now().getHour();
        int start = (hour / 3) * 3;
        int end = start + 1;
        return String.format("%02d-%02d", start, end);
    }
}