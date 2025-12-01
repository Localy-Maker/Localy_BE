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
import org.example.localy.repository.UserRepository;
import org.example.localy.service.Chat.ChatService;
import org.example.localy.util.JwtUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public BaseResponse<List<ChatMessageResponse>> todayMessages(HttpServletRequest request) {

        String token = jwtUtil.getTokenFromHeader(request);
        Long userId = jwtUtil.getUserIdFromToken(token);

        if(userId!=null){
            List<ChatMessageResponse> todayMessages = chatService.getTodayChat(userId);
            return BaseResponse.success(todayMessages);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }

    @Operation(
            summary = "챗봇 - 전날 대화내역을 가져옵니다.",
            description = """
          """
    )
    @GetMapping("/chatMessages/past")
    public BaseResponse<List<ChatMessageResponse>> pastMessages(HttpServletRequest request) {

        String token = jwtUtil.getTokenFromHeader(request);
        Long userId = jwtUtil.getUserIdFromToken(token);

        if(userId!=null){
            List<ChatMessageResponse> pastMessages = chatService.getPastChat(userId);
            return BaseResponse.success(pastMessages);
        }
        else{
            throw new CustomException(JWT_MISSING);
        }
    }
}
