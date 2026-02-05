package org.example.localy.service.Chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.dto.chatBot.response.ChatMessageResponse;
import org.example.localy.entity.ChatMessage;
import org.example.localy.entity.Users;
import org.example.localy.repository.ChatBotRepository;
import org.example.localy.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.example.localy.entity.Users.MembershipLevel.PREMIUM;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatBotRepository chatBotRepository;
    private final UserRepository userRepository;

    public List<ChatMessageResponse> getTodayChat(Long userId){
        List<ChatMessage> todayMessages = chatBotRepository.findTodayMessagesByUserId(userId);
        log.debug(todayMessages.toString());

        return todayMessages.stream()
                .map(ChatMessageResponse::from) // 엔티티 → DTO 변환
                .toList();

    }

    public List<ChatMessageResponse> getPastChat(Long userId, LocalDate date) {

        /*Pageable limitOne = PageRequest.of(0, 1);
        List<LocalDateTime> dates = chatBotRepository.findLastChatDate(userId, limitOne);

        if (dates.isEmpty()) return null;*/

        // 가장 최근 날짜 (어제 또는 그 이전)
//        LocalDate targetDate = dates.get(0).toLocalDate();

        // 해당 날짜의 모든 메시지 가져오기
        List<ChatMessage> pastMessages =
                chatBotRepository.findMessagesByUserIdAndDate(userId, date);

        return pastMessages.stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    public List<LocalDate> chatDateList(Long userId) {

        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));

        Pageable recent;

        if(user.getMembershipLevel()==PREMIUM) {
            recent = PageRequest.of(0, 5);
        }else {
            recent = PageRequest.of(0, 1);
        }
        List<LocalDate> dates = chatBotRepository.findLastChatDate(userId, recent);

        log.info("유저 {}의 채팅 최근 일자 : {}", userId, dates);

        return dates;

    }

}
