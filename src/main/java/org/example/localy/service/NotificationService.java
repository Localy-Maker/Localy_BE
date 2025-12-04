package org.example.localy.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.NotificationDto;
import org.example.localy.dto.admin.CreateAnnouncementRequest;
import org.example.localy.entity.Notification;
import org.example.localy.entity.NotificationRead;
import org.example.localy.entity.Users;
import org.example.localy.repository.NotificationReadRepository;
import org.example.localy.repository.NotificationRepository;
import org.example.localy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    public void createAnnouncement(CreateAnnouncementRequest dto) {
        Notification notification=Notification.builder()
                .title(dto.getTitle())
                .body(dto.getContent())
                .type(Notification.NotificationType.ANNOUNCEMENT)
                .build();

        notificationRepository.save(notification);

        Long notificationId = notification.getId();  // ← 생성된 ID
        Notification savedNotification = notificationRepository.getReferenceById(notificationId);

        // 2) 모든 사용자 조회
        List<Users> allUsers = userRepository.findAll();

        // 3) NotificationRead 생성
        List<NotificationRead> readRows = allUsers.stream()
                .map(user -> NotificationRead.builder()
                        .user(user)
                        .notification(savedNotification)
                        .isRead(false)
                        .build())
                .toList();

        // 4) Batch insert (saveAll)
        notificationReadRepository.saveAll(readRows);

        String key = "localy:alarm:unread:";

        for(Users user:allUsers){
            if(!redisTemplate.hasKey(key+user.getId())){
                // DB에서 읽지 않은 알림 개수 조회
                Long unreadCount = notificationReadRepository.countByUserIdAndIsReadFalse(user.getId());
                log.info("Unread count for user {} is {}", user.getId(), unreadCount);

                // Redis에 초기값 세팅
                redisTemplate.opsForValue().set(key+user.getId(), unreadCount.toString());
            }else{
                redisTemplate.opsForValue().increment(key+user.getId(), 1);
            }

            String redisValue = redisTemplate.opsForValue().get(key + user.getId());
            String unreadCounts = redisValue == null ? "0" : redisValue;



            log.info("Connected users: {}", simpUserRegistry.getUsers());


            /*new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                messagingTemplate.convertAndSendToUser(
                        user.getId().toString(),
                        "/queue/alarm/unreadCount",
                        unreadCounts
                );
            }).start();*/

            messagingTemplate.convertAndSend(
                    "/topic/alarm/unreadCount/" + user.getId(),
                    unreadCounts
            );
        }

        messagingTemplate.convertAndSend("/topic/alarm/receiveNotice", dto);

    }

    @Transactional
    public List<NotificationDto> readAllAlarm(Users user){
        // 1️⃣ 해당 유저가 받은 공지 + 읽음 여부 조회 (날짜순)
        List<NotificationRead> reads = notificationReadRepository
                .findAllByUserOrderByNotificationCreatedAtDesc(user);

        // 2️⃣ DTO로 변환
        List<NotificationDto> notifications = reads.stream()
                .map(nr -> new NotificationDto(
                        nr.getNotification().getId(),
                        nr.getNotification().getType(),
                        nr.getNotification().getTitle(),
                        nr.getNotification().getBody(),
                        nr.getNotification().getCreatedAt(),
                        nr.isRead()
                ))
                .collect(Collectors.toList());

        // 3️⃣ 아직 읽지 않은 공지 읽음 처리
        reads.stream()
                .filter(nr -> !nr.isRead())
                .forEach(NotificationRead::markAsRead);

        redisTemplate.opsForValue().set("localy:alarm:unread:"+user.getId(),"0");

        return notifications;
    }
}
