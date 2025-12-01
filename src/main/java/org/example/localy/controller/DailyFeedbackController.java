package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.dailyFeedback.DailyFeedbackDto;
import org.example.localy.dto.dailyFeedback.MonthlyEmotionDto;
import org.example.localy.dto.dailyFeedback.WeeklyEmotionDto;
import org.example.localy.service.DailyFeedbackService;
import org.example.localy.service.MonthlyEmotionService;
import org.example.localy.service.WeeklyFeedbackService;
import org.example.localy.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.example.localy.common.exception.errorCode.JwtErrorCode.JWT_MISSING;

@Slf4j
@RestController
@Tag(name = "DailyFeedback", description = "데일리피드백 api")
@RequestMapping("/dailyFeedback")
@RequiredArgsConstructor
public class DailyFeedbackController {

    private final DailyFeedbackService dailyFeedbackService;
    private final JwtUtil jwtUtil;
    private final WeeklyFeedbackService weeklyFeedbackService;
    private final MonthlyEmotionService monthlyEmotionService;

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

    @Operation(
            summary = "일주일 데일리피드백 - 일주일동안의 데일리피드백을 반환합니다.",
            description = """
          """
    )
    @GetMapping("/week")
    public BaseResponse<WeeklyEmotionDto> getDailyFeedbackWeek(
            HttpServletRequest request
    ) {

        String token = jwtUtil.getTokenFromHeader(request);
        Long userId = jwtUtil.getUserIdFromToken(token);

        LocalDate targetStartDate = LocalDate.now().with(DayOfWeek.MONDAY);

        if(userId!=null){
            log.info("한 주 피드백 조회 - userId: {}, date: {}", userId, targetStartDate);

            WeeklyEmotionDto weeklyEmotion = weeklyFeedbackService.getWeeklyEmotion(userId, targetStartDate);

            return BaseResponse.success(weeklyEmotion);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }

    @Operation(
            summary = "이번 달 데일리피드백 - 한 달 동안의 데일리피드백을 반환합니다.",
            description = """
          """
    )
    @GetMapping("/month/{yearMonth}")
    public BaseResponse<MonthlyEmotionDto> getDailyFeedbackMonth(
            HttpServletRequest request,
            @PathVariable String yearMonth
    ) {

        String token = jwtUtil.getTokenFromHeader(request);
        Long userId = jwtUtil.getUserIdFromToken(token);


        if(userId!=null){
            log.info("한 달 피드백 조회 - userId: {}", userId);

            MonthlyEmotionDto monthlyEmotionDto = monthlyEmotionService.getMonthlyEmotion(userId, yearMonth);

            return BaseResponse.success(monthlyEmotionDto);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }
}
