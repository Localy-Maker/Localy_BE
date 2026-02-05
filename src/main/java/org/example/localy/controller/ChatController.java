package org.example.localy.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.config.jwt.JwtAuthenticationFilter;
import org.example.localy.dto.chatBot.response.ChatMessageResponse;
import org.example.localy.entity.Users;
import org.example.localy.repository.UserRepository;
import org.example.localy.service.Chat.ChatService;
import org.example.localy.util.JwtUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.example.localy.common.exception.errorCode.JwtErrorCode.JWT_MISSING;

@Slf4j
@Tag(name = "Chat", description = "챗봇 api")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final JwtUtil jwtUtil;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Operation(
            summary = "챗봇 - 오늘 대화 내역을 가져옵니다.",
            description = """
          """
    )
    @GetMapping("/chatMessages/today")
    public BaseResponse<List<ChatMessageResponse>> todayMessages(@AuthenticationPrincipal Users user) {

        Long userId =  user.getId();

        if(userId!=null){
            List<ChatMessageResponse> todayMessages = chatService.getTodayChat(userId);
            return BaseResponse.success(todayMessages);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }

    @Operation(
            summary = "챗봇 - 과거 대화내역을 가져옵니다.",
            description = """
          `date` : 채팅내역을 보고 싶은 날짜 - 양식:YYYY-MM-DD
          """
    )
    @GetMapping("/chatMessages/past/{date}")
    public BaseResponse<List<ChatMessageResponse>> pastMessages(@AuthenticationPrincipal Users user, @PathVariable LocalDate date) {

        Long userId = user.getId();

        if(userId!=null){
            List<ChatMessageResponse> pastMessages = chatService.getPastChat(userId, date);
            return BaseResponse.success(pastMessages);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }

    @Operation(
            summary = "챗봇 - 대화내역이 있는 과거 날짜를 보여줍니다.(일반-1개, 프리미엄-5개 (오늘 날짜 제외))",
            description = """
          해당 API로 대화가 존재하는 날짜 우선적으로 확인 후, 해당 날짜 중 하나를 선택하여 위 전날 채팅 내역을 가져오는 API 호출하는 방식
          """
    )
    @GetMapping("/chatMessages/dateList")
    public BaseResponse<List<LocalDate>> chatDateList(@AuthenticationPrincipal Users user) {

        Long userId = user.getId();

        if(userId!=null){
            List<LocalDate> dateList = chatService.chatDateList(userId);
            return BaseResponse.success(dateList);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }
}
