package org.example.localy.dto.dailyFeedback;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmotionDayDto {
    private int day;        // 날짜 (1~31)
    private int emotion;    // 감정 코드 (1~6)
}
