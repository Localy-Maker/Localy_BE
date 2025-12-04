package org.example.localy.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.NotificationDto;
import org.example.localy.entity.Users;
import org.example.localy.repository.NotificationReadRepository;
import org.example.localy.repository.NotificationRepository;
import org.example.localy.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Alarm", description = "알람 관련 api")
@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final NotificationService notificationService;

    @GetMapping("/readAlarm")
    public BaseResponse<List<NotificationDto>> readAllAlarm(@AuthenticationPrincipal Users user) {

        List<NotificationDto> notifications = notificationService.readAllAlarm(user);

        return BaseResponse.success(notifications);

    }
}
