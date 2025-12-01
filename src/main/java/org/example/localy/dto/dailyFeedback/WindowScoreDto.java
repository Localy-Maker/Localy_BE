package org.example.localy.dto.dailyFeedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WindowScoreDto {
    private String window;
    private Double avgScore;
    private String emotion;
}
