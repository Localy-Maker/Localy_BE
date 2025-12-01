package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.chatBot.response.ChatMessageResponse;
import org.example.localy.dto.emotion.DailyFeedbackDto;
import org.example.localy.service.DailyFeedbackService;
import org.example.localy.util.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static org.example.localy.common.exception.errorCode.JwtErrorCode.JWT_MISSING;

@Slf4j
@RestController
@Tag(name = "DailyFeedback", description = "데일리피드백 api")
@RequestMapping("/dailyFeedback")
@RequiredArgsConstructor
public class DailyFeedbackController {

    private final DailyFeedbackService dailyFeedbackService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "하루 데일리피드백 - 오늘 하루 3시간씩 감정수치변화를 보여줍니다.",
            description = """
          """
    )
    @GetMapping("/day")
    public BaseResponse<DailyFeedbackDto> getDailyFeedback(
            HttpServletRequest request
    ) {
        // date가 없으면 오늘 날짜 사용
        LocalDate targetDate = LocalDate.now();

        String token = jwtUtil.getTokenFromHeader(request);
        Long userId = jwtUtil.getUserIdFromToken(token);

        if(userId!=null){
            log.info("일일 피드백 조회 - userId: {}, date: {}", userId, targetDate);

            DailyFeedbackDto feedback = dailyFeedbackService.getDailyFeedback(userId, targetDate);

            return BaseResponse.success(feedback);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }
}
