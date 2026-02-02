package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.mission.MissionDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.example.localy.service.mission.MissionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Mission", description = "미션 관련 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(summary = "미션 홈 조회", description = "포인트 정보와 미션 목록을 조회합니다. 위치 정보를 포함하면 감정 변화에 따라 새로운 미션을 생성합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<MissionDto.MissionHomeResponse>> getMissionHome(
            @AuthenticationPrincipal Users user,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        MissionDto.MissionHomeResponse response = missionService.getMissionHome(user, latitude, longitude);
        return ResponseEntity.ok(BaseResponse.success("미션 홈 조회 성공", response));
    }

    @Operation(summary = "미션 상세 조회", description = "미션 상세 정보와 장소 정보를 조회합니다.")
    @GetMapping("/{missionId}")
    public ResponseEntity<BaseResponse<MissionDto.MissionDetailResponse>> getMissionDetail(
            @AuthenticationPrincipal Users user,
            @PathVariable Long missionId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        MissionDto.MissionDetailResponse response = missionService.getMissionDetail(
                user, missionId, latitude, longitude);
        return ResponseEntity.ok(BaseResponse.success("미션 상세 조회 성공", response));
    }

    @Operation(summary = "미션 인증", description = "사용자 위치를 확인하여 미션을 인증하고 포인트를 지급합니다.")
    @PostMapping("/{missionId}/verify")
    public ResponseEntity<BaseResponse<MissionDto.VerifyResponse>> verifyMission(
            @AuthenticationPrincipal Users user,
            @PathVariable Long missionId,
            @Valid @RequestBody MissionDto.VerifyRequest request) {

        MissionDto.VerifyResponse response = missionService.verifyMission(user, missionId, request);
        return ResponseEntity.ok(BaseResponse.success("미션 인증 완료", response));
    }

    @Operation(summary = "프리미엄 구독 구매", description = "포인트를 사용하여 프리미엄 구독권을 구매합니다. (7DAYS: 50P, 30DAYS: 200P)")
    @PostMapping("/premium/purchase")
    public ResponseEntity<BaseResponse<Void>> purchasePremium(
            @AuthenticationPrincipal Users user,
            @RequestParam String planType) {

        missionService.purchasePremium(user, planType);
        return ResponseEntity.ok(BaseResponse.success("프리미엄 구독 구매 완료", null));
    }

    @Operation(summary = "미션 캘린더 아카이빙 저장", description = "수행한 미션의 사진을 아카이빙합니다. 저장된 결과를 반환하므로 스웨거에서 URL 확인이 가능합니다.")
    @PostMapping("/{missionId}/archive")
    public ResponseEntity<BaseResponse<MissionArchive>> archiveMission(
            @AuthenticationPrincipal Users user,
            @PathVariable Long missionId,
            @RequestParam String imageUrl,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate photoStoredDate) {

        MissionArchive response = missionService.archiveMission(user, missionId, imageUrl, targetDate, photoStoredDate);
        return ResponseEntity.ok(BaseResponse.success("캘린더 아카이빙 성공", response));
    }

    @Operation(summary = "월별 아카이브 조회", description = "특정 년/월의 아카이빙 목록을 조회하여 캘린더를 채웁니다.")
    @GetMapping("/archive")
    public ResponseEntity<BaseResponse<List<MissionArchive>>> getMonthlyArchives(
            @AuthenticationPrincipal Users user,
            @RequestParam int year,
            @RequestParam int month) {

        List<MissionArchive> response = missionService.getMonthlyArchives(user, year, month);
        return ResponseEntity.ok(BaseResponse.success("월별 아카이브 조회 성공", response));
    }
}