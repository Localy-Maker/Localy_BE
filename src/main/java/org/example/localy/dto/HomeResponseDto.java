package org.example.localy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeResponseDto {
    private String mostEmotion;
    private String happinessDiff;
}