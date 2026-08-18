package org.example.localy.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.place.PlaceCandidateDto;
import org.example.localy.entity.place.Place;
import org.example.localy.service.place.PlaceRecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/place")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "AdminPlace", description = "어드민 장소 카탈로그 동기화/조회 수동 트리거")
public class PlaceAdminController {

    private final PlaceRecommendService placeRecommendService;

    // 매일 새벽 3시 자동 실행되는 장소 카탈로그 동기화를 즉시 한 번 실행
    @PostMapping("/sync-catalog")
    public BaseResponse<String> syncCatalog() {
        log.info("장소 카탈로그 동기화 수동 트리거");
        placeRecommendService.syncFullCatalog();
        return BaseResponse.success("카탈로그 동기화 완료");
    }

    // 키워드로 VisitSeoul 실제 콘텐츠를 검색해, 기준 좌표에서 가까운 순으로 후보를 보여줌 (DB 저장 안 함)
    @GetMapping("/search")
    public BaseResponse<List<PlaceCandidateDto>> search(
            @RequestParam String keyword,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10") int limit) {
        List<PlaceCandidateDto> candidates =
                placeRecommendService.searchNearbyContent(keyword, latitude, longitude, limit);
        return BaseResponse.success(candidates);
    }

    // 특정 cid를 실제 VisitSeoul 데이터로 DB에 저장/갱신
    @PostMapping("/ingest")
    public BaseResponse<PlaceCandidateDto> ingest(@RequestParam String cid) {
        Place saved = placeRecommendService.saveOrUpdatePlace(cid);
        if (saved == null) {
            return BaseResponse.failure("PLACE_INGEST_FAILED", "VisitSeoul에서 해당 cid 데이터를 가져오지 못했습니다: " + cid);
        }

        // JPA 엔티티를 그대로 반환하면 지연 로딩된 연관관계(images) 직렬화 시 에러가 날 수 있어 DTO로 변환
        PlaceCandidateDto dto = PlaceCandidateDto.builder()
                .cid(saved.getContentId())
                .title(saved.getTitle())
                .category(saved.getCategory())
                .address(saved.getAddress())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .distanceKm(null)
                .build();
        return BaseResponse.success(dto);
    }
}
