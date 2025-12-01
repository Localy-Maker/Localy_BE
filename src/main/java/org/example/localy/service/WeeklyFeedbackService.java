package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import org.example.localy.dto.dailyFeedback.DailyEmotionDto;
import org.example.localy.dto.dailyFeedback.WeekRangeDto;
import org.example.localy.dto.dailyFeedback.WeeklyEmotionDto;
import org.example.localy.entity.EmotionDayResult;
import org.example.localy.repository.EmotionDayResultRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WeeklyFeedbackService {

    private final EmotionDayResultRepository emotionDayResultRepository;

    public WeeklyEmotionDto getWeeklyEmotion(Long userId, LocalDate startDate) {

        LocalDate endDate = startDate.plusDays(6);

        // 요일 리스트
        List<String> days = List.of("월", "화", "수", "목", "금", "토", "일");

        // 기본값 score = 0
        Map<String, Integer> dayScoreMap = new HashMap<>();
        days.forEach(d -> dayScoreMap.put(d, 0));

        // 👉 DB에서 가져오기
        List<EmotionDayResult> records =
                emotionDayResultRepository.findByUserIdAndDateBetween(userId, startDate, endDate);

        // 조회한 데이터를 요일별로 매핑
        for (EmotionDayResult record : records) {
            DayOfWeek dow = record.getDate().getDayOfWeek(); // MONDAY~SUNDAY
            String dayName = convertDayOfWeek(dow);          // "월","화"... 변환

            dayScoreMap.put(dayName, record.getAvgScore().intValue());
        }

        // DTO 리스트 변환
        List<DailyEmotionDto> emotions = new ArrayList<>();
        for (String day : days) {
            emotions.add(
                    DailyEmotionDto.builder()
                            .day(day)
                            .score(dayScoreMap.get(day))
                            .build()
            );
        }

        WeekRangeDto weekRange = WeekRangeDto.builder()
                .start(startDate.toString())
                .end(endDate.toString())
                .build();

        return WeeklyEmotionDto.builder()
                .weekRange(weekRange)
                .emotions(emotions)
                .build();
    }

    // 요일 변환기
    private String convertDayOfWeek(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}
