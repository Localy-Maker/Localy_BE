package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.mission.MissionDto;
import org.example.localy.entity.Users;
import org.example.localy.service.mission.MissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}