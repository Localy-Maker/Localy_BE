package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.dailyFeedback.DailyFeedbackDto;
import org.example.localy.dto.dailyFeedback.WindowScoreDto;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyFeedbackService {

    private final EmotionWindowResultRepository windowRepository;
    private final EmotionWindowService emotionWindowService;

    public DailyFeedbackDto getDailyFeedback(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // 해당 날짜의 모든 윈도우 결과 조회
        List<EmotionWindowResult> results = windowRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, startOfDay, endOfDay);

        Map<String, Object> mostFre = emotionWindowService.analyzeUserEmotion(userId);

        Integer mostFrequentSection = (Integer) mostFre.get("mostFrequentSection");
        String mostFrequentEmotion = (String) mostFre.get("mostFrequentEmotion");

        System.out.println("Section 최빈값: " + mostFrequentSection);
        System.out.println("Emotion 최빈값: " + mostFrequentEmotion);


        if (results.isEmpty()) {
            return DailyFeedbackDto.builder()
                    .date(date.toString())
                    .scores(new ArrayList<>())
                    .mostFrequentSection(null)
                    .mostFrequentEmotion(null)
                    .build();
        }

        // WindowScoreDto 리스트 생성
        List<WindowScoreDto> scores = results.stream()
                .map(r -> WindowScoreDto.builder()
                        .window(r.getWindow())
                        .avgScore(r.getAvgScore())
                        .emotion(r.getEmotion())
                        .build())
                .collect(Collectors.toList());

        // 가장 빈번한 감정 찾기
        Map<String, Long> emotionFrequency = results.stream()
                .collect(Collectors.groupingBy(
                        EmotionWindowResult::getEmotion,
                        Collectors.counting()
                ));

//        String mostFrequentEmotion = emotionFrequency.entrySet().stream()
//                .max(Map.Entry.comparingByValue())
//                .map(Map.Entry::getKey)
//                .orElse(null);
//
//        // 가장 빈번한 감정의 첫 번째 구간 번호 찾기
//        Integer mostFrequentSection = null;
//        if (mostFrequentEmotion != null) {
//            for (int i = 0; i < results.size(); i++) {
//                if (results.get(i).getEmotion().equals(mostFrequentEmotion)) {
//                    mostFrequentSection = i + 1; // 1부터 시작
//                    break;
//                }
//            }
//        }

        return DailyFeedbackDto.builder()
                .date(date.toString())
                .scores(scores)
                .mostFrequentSection(mostFrequentSection)
                .mostFrequentEmotion(mostFrequentEmotion)
                .build();
    }
}
