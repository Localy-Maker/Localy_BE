package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.response.ChatMessageResponse;
import org.example.localy.entity.ChatMessage;
import org.example.localy.repository.ChatBotRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatBotRepository chatBotRepository;

    public List<ChatMessageResponse> getTodayChat(Long userId){
        List<ChatMessage> todayMessages = chatBotRepository.findTodayMessagesByUserId(userId);
        log.debug(todayMessages.toString());

        return todayMessages.stream()
                .map(ChatMessageResponse::from) // 엔티티 → DTO 변환
                .toList();

    }

    public List<ChatMessageResponse> getPastChat(Long userId) {

        Pageable limitOne = PageRequest.of(0, 1);
        List<LocalDateTime> dates = chatBotRepository.findLastChatDate(userId, limitOne);

        if (dates.isEmpty()) return null;

        // 가장 최근 날짜 (어제 또는 그 이전)
        LocalDate targetDate = dates.get(0).toLocalDate();

        // 해당 날짜의 모든 메시지 가져오기
        List<ChatMessage> pastMessages =
                chatBotRepository.findMessagesByUserIdAndDate(userId, targetDate);

        return pastMessages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

}
