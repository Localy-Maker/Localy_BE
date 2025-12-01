package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import org.example.localy.dto.dailyFeedback.EmotionDayDto;
import org.example.localy.dto.dailyFeedback.MonthlyEmotionDto;
import org.example.localy.dto.dailyFeedback.MonthlyStatsDto;
import org.example.localy.entity.EmotionDayResult;
import org.example.localy.repository.EmotionDayResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonthlyEmotionService {

    private final EmotionDayResultRepository repository;

    public MonthlyEmotionDto getMonthlyEmotion(Long userId, String yearMonthStr) {

        // 1) yearMonth 파싱
        YearMonth yearMonth;
        if (yearMonthStr != null && !yearMonthStr.isEmpty()) {
            yearMonth = YearMonth.parse(yearMonthStr);  // "2025-11" 형태라면 parse OK
        } else {
            yearMonth = YearMonth.now();
        }

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 2) DB 조회
        List<EmotionDayResult> list =
                repository.findByUserIdAndDateBetween(userId, startDate, endDate);

        // 3) days 배열 만들기
        List<EmotionDayDto> dayDtos = list.stream()
                .map(r -> EmotionDayDto.builder()
                        .day(r.getDate().getDayOfMonth())
                        .emotion(r.getSection())
                        .build())
                .toList();

        // 4) monthlyStats 계산 (section 1~6 카운트)
        int[] stats = new int[7];  // index 1~6 사용
        for (EmotionDayResult r : list) {
            int sec = r.getSection();
            if (sec >= 1 && sec <= 6) stats[sec]++;
        }

        MonthlyStatsDto monthlyStats = MonthlyStatsDto.builder()
                .e1(stats[1])
                .e2(stats[2])
                .e3(stats[3])
                .e4(stats[4])
                .e5(stats[5])
                .e6(stats[6])
                .build();

        // 5) 최종 Response 생성
        return MonthlyEmotionDto.builder()
                .yearMonth(yearMonth.toString().replace("-", "")) // "202511"
                .days(dayDtos)
                .monthlyStats(monthlyStats)
                .build();
    }
}
