package org.example.localy.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.service.place.PlaceRecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/place")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "AdminPlace", description = "어드민 장소 카탈로그 동기화 수동 트리거")
public class PlaceAdminController {

    private final PlaceRecommendService placeRecommendService;

    // 매일 새벽 3시 자동 실행되는 장소 카탈로그 동기화를 즉시 한 번 실행
    @PostMapping("/sync-catalog")
    public BaseResponse<String> syncCatalog() {
        log.info("장소 카탈로그 동기화 수동 트리거");
        placeRecommendService.syncFullCatalog();
        return BaseResponse.success("카탈로그 동기화 완료");
    }
}
