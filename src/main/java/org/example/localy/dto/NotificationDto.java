package org.example.localy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.localy.entity.Notification;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class NotificationDto {
    private Long id;
    private Notification.NotificationType type;
    private String title;
    private String body;
    private java.time.LocalDateTime createdAt;
    private boolean isRead;
}
