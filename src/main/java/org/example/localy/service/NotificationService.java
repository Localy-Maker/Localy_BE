package org.example.localy.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.NotificationDto;
import org.example.localy.dto.admin.CreateAnnouncementRequest;
import org.example.localy.entity.Notification;
import org.example.localy.entity.NotificationRead;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.Mission;
import org.example.localy.repository.NotificationReadRepository;
import org.example.localy.repository.NotificationRepository;
import org.example.localy.repository.UserRepository;
import org.example.localy.repository.place.MissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.example.localy.common.exception.errorCode.AuthErrorCode.USER_NOT_FOUND;
import static org.example.localy.common.exception.errorCode.MissionErrorCode.MISSION_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MissionRepository missionRepository;

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


    public void createGeneralNotice(Notification.GeneralNoticeType type, Long id) {

        Notification notification;
        Users user;

        if (type == Notification.GeneralNoticeType.LASTLOGINTIME) {
            notification = Notification.builder()
                    .title("[Localy 알림]\n똑똑똑! 오늘 하루는 어떠신가요?")
                    .body("낯선 곳에서 고생한 당신의 오늘 하루는 어떠신가요? \nLocaly가 당신의 모든 감정 변화를 이해하고 따뜻한 위로를 드릴 준비가 되어 있어요. 😊")
                    .type(Notification.NotificationType.GENERAL)
                    .build();

            notificationRepository.save(notification);

            Long notificationId = notification.getId();  // ← 생성된 ID
            Notification savedNotification = notificationRepository.getReferenceById(notificationId);

            user = userRepository.findById(id).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

            // 3) NotificationRead 생성
            NotificationRead readRow = NotificationRead.builder()
                    .user(user)
                    .notification(savedNotification)
                    .isRead(false)
                    .build();

            notificationReadRepository.save(readRow);

            log.info("✨LastLoginTime 알림 발송 완료");

        }
        else {

            Mission mission = missionRepository.findById(id).orElseThrow(() -> new CustomException(MISSION_NOT_FOUND));

            notification = Notification.builder()
                    .title("[Localy 알림]\n⏰ 미션 마감 임박! Localy가 추천하는 미션을 확인해보세요.")
                    .body("로컬 미션 '"+ mission.getTitle() +"'완료까지 이제 3시간 남았습니다! \n간단한 인증으로 "+ mission.getPoints() +" 포인트를 획득할 기회를 놓치지 마세요.")
                    .type(Notification.NotificationType.GENERAL)
                    .build();

            notificationRepository.save(notification);

            Long notificationId = notification.getId();  // ← 생성된 ID
            Notification savedNotification = notificationRepository.getReferenceById(notificationId);

            user = userRepository.findById(mission.getUser().getId()).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

            // 3) NotificationRead 생성
            NotificationRead readRow = NotificationRead.builder()
                    .user(user)
                    .notification(savedNotification)
                    .isRead(false)
                    .build();

            notificationReadRepository.save(readRow);

            log.info("✨MissionTime 알림 발송 완료");

        }

        String key = "localy:alarm:unread:";

        if (!redisTemplate.hasKey(key + user.getId())) {
            // DB에서 읽지 않은 알림 개수 조회
            Long unreadCount = notificationReadRepository.countByUserIdAndIsReadFalse(user.getId());
            log.info("Unread count for user {} is {}", user.getId(), unreadCount);

            // Redis에 초기값 세팅
            redisTemplate.opsForValue().set(key + user.getId(), unreadCount.toString());
        } else {
            redisTemplate.opsForValue().increment(key + user.getId(), 1);
        }

        String redisValue = redisTemplate.opsForValue().get(key + user.getId());
        String unreadCounts = redisValue == null ? "0" : redisValue;


        log.info("Connected user: {}", user);


        messagingTemplate.convertAndSend(
                "/topic/alarm/unreadCount/" + user.getId(),
                unreadCounts
        );

        CreateAnnouncementRequest dto = CreateAnnouncementRequest.builder()
                .title(notification.getTitle())
                .content(notification.getBody())
                .build();

        messagingTemplate.convertAndSend("/topic/alarm/receiveNotice", dto);

    }
}
