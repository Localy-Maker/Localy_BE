package org.example.localy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.admin.CreateAnnouncementRequest;
import org.example.localy.repository.NotificationRepository;
import org.example.localy.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/admin/notice")
@Slf4j
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NotificationService notificationService;

    @PostMapping("/announcement")
    public BaseResponse<CreateAnnouncementRequest> createAnnouncement(@RequestBody CreateAnnouncementRequest dto) {
        notificationService.createAnnouncement(dto);
        return BaseResponse.success(dto);
    }
}
