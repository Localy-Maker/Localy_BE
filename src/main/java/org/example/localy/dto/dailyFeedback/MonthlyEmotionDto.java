package org.example.localy.dto.dailyFeedback;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MonthlyEmotionDto {
    private String yearMonth;                    // 예: "202511"
    private List<EmotionDayDto> days;            // 일별 감정 데이터
    private MonthlyStatsDto monthlyStats;        // 월간 통계
}
