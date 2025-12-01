package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.entity.EmotionDayResult;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.repository.EmotionDayResultRepository;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayEmotionScheduler {

    private final EmotionWindowResultRepository windowRepository;
    private final EmotionDayResultRepository dayResultRepository;

    // 매일 00시 00분에 실행
    @Scheduled(cron = "0 12 21 * * *")
    @Transactional
    public void aggregateDailyEmotions() {
        log.info("=== 일일 감정 집계 시작 ===");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime endOfYesterday = yesterday.plusDays(1).atStartOfDay();

        log.info("집계 대상 날짜: {}", yesterday);

        // 어제의 모든 윈도우 결과 조회
        List<EmotionWindowResult> yesterdayResults = windowRepository
                .findByCreatedAtBetween(startOfYesterday, endOfYesterday);

        if (yesterdayResults.isEmpty()) {
            log.info("어제 데이터 없음. 집계 종료.");
            return;
        }

        // userId별로 그룹화
        Map<Long, List<EmotionWindowResult>> userResultsMap = yesterdayResults.stream()
                .collect(Collectors.groupingBy(EmotionWindowResult::getUserId));

        log.info("집계 대상 유저 수: {}", userResultsMap.size());

        // 각 유저별로 일일 결과 저장
        for (Map.Entry<Long, List<EmotionWindowResult>> entry : userResultsMap.entrySet()) {
            Long userId = entry.getKey();
            List<EmotionWindowResult> userResults = entry.getValue();

            try {
                // 이미 저장된 데이터가 있는지 확인
                if (dayResultRepository.existsByUserIdAndDate(userId, yesterday)) {
                    log.info("userId={} 의 {} 데이터는 이미 존재함. 스킵.", userId, yesterday);
                    continue;
                }

                // 평균 점수 계산
                double avgScore = userResults.stream()
                        .mapToDouble(EmotionWindowResult::getAvgScore)
                        .average()
                        .orElse(0.0);

                // 섹션 계산
                int section = calculateSection(avgScore);

                // 일일 결과 저장
                EmotionDayResult dayResult = EmotionDayResult.builder()
                        .userId(userId)
                        .date(yesterday)
                        .avgScore(avgScore)
                        .section(section)
                        .build();

                dayResultRepository.save(dayResult);

                log.info("userId={} 일일 감정 저장 완료 - avgScore={}, section={}",
                        userId, avgScore, section);

            } catch (Exception e) {
                log.error("userId={} 일일 감정 집계 실패", userId, e);
            }
        }

        log.info("=== 일일 감정 집계 완료 ===");
    }

    private int calculateSection(double score) {
        if (score <= 16) return 1;  // VERY_NEG
        if (score <= 33) return 2;  // NEG
        if (score <= 50) return 3;  // NEUTRAL
        if (score <= 66) return 4;  // POS_LIGHT
        if (score <= 83) return 5;  // POS
        return 6;                    // VERY_POS
    }
}
