package org.example.localy.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.admin.CreateAnnouncementRequest;
import org.example.localy.repository.NotificationRepository;
import org.example.localy.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notice")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "AdminNoticeTest", description = "어드민 공지사항 작성 테스트 api")
public class NoticeAdminController {

    private final NotificationService notificationService;

    @PostMapping("/announcement")
    public BaseResponse<CreateAnnouncementRequest> createAnnouncement(@RequestBody CreateAnnouncementRequest dto) {
        notificationService.createAnnouncement(dto);
        return BaseResponse.success(dto);
    }
}
