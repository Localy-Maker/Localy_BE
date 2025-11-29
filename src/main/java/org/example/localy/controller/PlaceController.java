package org.example.localy.controller;

import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.place.PlaceDto;
import org.example.localy.entity.Users;
import org.example.localy.service.place.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    // 로컬가이드 홈 조회
    @GetMapping("/home")
    public ResponseEntity<BaseResponse<PlaceDto.HomeResponse>> getHome(
            @AuthenticationPrincipal Users user,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        PlaceDto.HomeResponse response = placeService.getHomeData(user, latitude, longitude);
        return ResponseEntity.ok(BaseResponse.success("장소추천 홈 조회 성공", response));
    }

    // 장소 상세 페이지 조회
    @GetMapping("/{placeId}")
    public ResponseEntity<BaseResponse<PlaceDto.PlaceDetail>> getPlaceDetail(
            @AuthenticationPrincipal Users user,
            @PathVariable Long placeId) {

        PlaceDto.PlaceDetail response = placeService.getPlaceDetail(user, placeId);
        return ResponseEntity.ok(BaseResponse.success("장소 상세 조회 성공", response));
    }

    // 북마크 조회
    @GetMapping("/bookmarks")
    public ResponseEntity<BaseResponse<PlaceDto.BookmarkListResponse>> getBookmarks(
            @AuthenticationPrincipal Users user,
            @RequestParam(required = false, defaultValue = "RECENT") String sortType,
            @RequestParam(required = false) Long lastBookmarkId,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        PlaceDto.BookmarkListResponse response = placeService.getBookmarks(
                user, sortType, lastBookmarkId, size);
        return ResponseEntity.ok(BaseResponse.success("북마크 목록 조회 성공", response));
    }

    // 북마크 추가/취소
    @PostMapping("/{placeId}/bookmarks")
    public ResponseEntity<BaseResponse<PlaceDto.BookmarkResponse>> toggleBookmark(
            @AuthenticationPrincipal Users user,
            @PathVariable Long placeId,
            @RequestBody PlaceDto.BookmarkRequest request) {

        PlaceDto.BookmarkResponse response = placeService.toggleBookmark(user, placeId, request);

        String message = response.getIsBookmarked()
                ? "북마크가 추가되었습니다."
                : "북마크가 취소되었습니다.";

        return ResponseEntity.ok(BaseResponse.success(message, response));
    }
}