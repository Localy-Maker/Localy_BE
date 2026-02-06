package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.exception.CustomException;
import org.example.localy.common.exception.errorCode.AuthErrorCode;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.mission.MissionArchiveDto;
import org.example.localy.dto.mission.MissionDto;
import org.example.localy.entity.Users;
import org.example.localy.entity.place.MissionArchive;
import org.example.localy.repository.UserRepository;
import org.example.localy.service.mission.ArchiveService;
import org.example.localy.service.mission.MissionService;
import org.example.localy.util.JwtUtil;
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
    private final ArchiveService archiveService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Operation(summary = "미션 홈 조회", description = "포인트 정보와 미션 목록 조회 및 미션 생성")
    @GetMapping
    public ResponseEntity<BaseResponse<MissionDto.MissionHomeResponse>> getMissionHome(
            @AuthenticationPrincipal Users user,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {

        MissionDto.MissionHomeResponse response = missionService.getMissionHome(user, latitude, longitude);
        return ResponseEntity.ok(BaseResponse.success("미션 홈 조회 성공", response));
    }

    @Operation(summary = "미션 상세 조회", description = "미션, 장소 상세 정보 조회")
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

    @Operation(summary = "미션 인증", description = "사용자 위치로 미션 인증")
    @PostMapping("/{missionId}/verify")
    public ResponseEntity<BaseResponse<MissionDto.VerifyResponse>> verifyMission(
            @AuthenticationPrincipal Users user,
            @PathVariable Long missionId,
            @Valid @RequestBody MissionDto.VerifyRequest request) {

        MissionDto.VerifyResponse response = missionService.verifyMission(user, missionId, request);
        return ResponseEntity.ok(BaseResponse.success("미션 인증 완료", response));
    }

    @Operation(summary = "미션 캘린더 아카이빙 저장", description = "수행한 미션 사진 아카이빙")
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

    @Operation(summary = "미션 캘린더 월별 조회", description = "대표 이미지가 설정된 날짜별 요약 정보 반환")
    @GetMapping("/archive/monthly")
    public BaseResponse<MissionArchiveDto.MonthlySummaryResponse> getMonthlyArchive(
            @RequestHeader("Authorization") String token,
            @Parameter(description = "연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "월") @RequestParam(required = false) Integer month) {

        Users user = getUserFromToken(token);

        // 값이 없을 경우 현재 시간 기준으로 기본값 설정
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();

        return BaseResponse.success("월별 조회 성공", missionService.getMonthlySummary(user, targetYear, targetMonth));
    }

    // 미션 캘린더 상세 조회
    @Operation(summary = "날짜별 아카이브 상세 조회", description = "특정 날짜의 완료 미션 개수 및 사진 목록 조회")
    @GetMapping("/archive/detail")
    public BaseResponse<MissionArchiveDto.DetailResponse> getArchiveDetail(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Users user = getUserFromToken(token);
        return BaseResponse.success("상세 조회 성공", archiveService.getDayDetail(user, date));
    }

    // 미션 사진 업로드
    @Operation(summary = "미션 사진 업로드", description = "갤러리 저장 날짜와 선택한 날짜가 일치해야 업로드 가능")
    @PostMapping("/archive/upload")
    public BaseResponse<String> uploadArchivePhoto(
            @RequestHeader("Authorization") String token,
            @RequestBody MissionArchiveDto.UploadRequest request) {
        Users user = getUserFromToken(token);
        archiveService.uploadArchivePhoto(user, request);
        return BaseResponse.success("사진 업로드 성공", null);
    }

    // 썸네일 사진 선택
    @Operation(summary = "썸네일 사진 선택", description = "업로드한 사진 중 먼슬리 페이지에 보일 썸네일 지정")
    @PatchMapping("/archive/{archiveId}/thumbnail")
    public BaseResponse<String> updateThumbnail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long archiveId) {
        Users user = getUserFromToken(token);
        archiveService.setThumbnail(user, archiveId);
        return BaseResponse.success("썸네일 설정 완료", null);
    }

    private Users getUserFromToken(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.USER_NOT_FOUND));
    }
}