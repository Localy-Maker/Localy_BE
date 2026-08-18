package org.example.localy.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.service.place.PlaceRecommendService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceCatalogSyncScheduler {

    private final PlaceRecommendService placeRecommendService;

    // 매일 새벽 3시. 카테고리/콘텐츠 구조가 자주 바뀌지 않아 하루 1회면 충분.
    @Scheduled(cron = "0 0 3 * * *")
    public void syncPlaceCatalog() {
        log.info("장소 카탈로그 동기화 스케줄러 시작");
        try {
            placeRecommendService.syncFullCatalog();
        } catch (Exception e) {
            log.error("장소 카탈로그 동기화 중 오류 발생", e);
        }
        log.info("장소 카탈로그 동기화 스케줄러 종료");
    }
}
