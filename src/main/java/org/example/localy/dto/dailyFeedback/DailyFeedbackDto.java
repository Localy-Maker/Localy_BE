package org.example.localy.dto.dailyFeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyFeedbackDto {
    private String date;
    private List<WindowScoreDto> scores;
    private Integer mostFrequentSection;
    private String mostFrequentEmotion;
}