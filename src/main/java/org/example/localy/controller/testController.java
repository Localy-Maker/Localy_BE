package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 카카오맵 API 테스트용 컨트롤러
 *
 * 협업자 작업 완료 전까지 카카오맵 URL 생성 테스트용
 *
 * TODO: 협업자 작업 완료 후 삭제 예정
 */
@Tag(name = "Test", description = "테스트 API (개발용)")
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class testController {

    @Operation(
            summary = "[테스트] 카카오맵 URL 생성",
            description = "장소 정보로 카카오맵 URL을 생성하여 테스트합니다."
    )
    @GetMapping("/kakaomap-url")
    public ResponseEntity<BaseResponse<Map<String, String>>> testKakaoMapUrl(
            @RequestParam String placeName,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        // 카카오맵 URL 생성
        String kakaoMapUrl = generateKakaoMapUrl(placeName, latitude, longitude);

        Map<String, String> result = new HashMap<>();
        result.put("placeName", placeName);
        result.put("latitude", String.valueOf(latitude));
        result.put("longitude", String.valueOf(longitude));
        result.put("kakaoMapUrl", kakaoMapUrl);
        result.put("instruction", "이 URL을 웹브라우저에 붙여넣으면 카카오맵으로 이동합니다.");

        return ResponseEntity.ok(BaseResponse.success("카카오맵 URL 생성 성공", result));
    }

    @Operation(
            summary = "[테스트] 여러 장소 카카오맵 URL 생성",
            description = "여러 테스트 장소의 카카오맵 URL을 한번에 생성합니다."
    )
    @GetMapping("/kakaomap-samples")
    public ResponseEntity<BaseResponse<Map<String, Object>>> testKakaoMapSamples() {

        Map<String, Object> samples = new HashMap<>();

        // 샘플 1: 카카오 판교 오피스
        samples.put("sample1", Map.of(
                "placeName", "카카오 판교오피스",
                "latitude", 37.402056,
                "longitude", 127.108212,
                "kakaoMapUrl", generateKakaoMapUrl("카카오 판교오피스", 37.402056, 127.108212)
        ));

        // 샘플 2: 서울역
        samples.put("sample2", Map.of(
                "placeName", "서울역",
                "latitude", 37.554648,
                "longitude", 126.970869,
                "kakaoMapUrl", generateKakaoMapUrl("서울역", 37.554648, 126.970869)
        ));

        // 샘플 3: 남산타워
        samples.put("sample3", Map.of(
                "placeName", "N서울타워",
                "latitude", 37.551169,
                "longitude", 126.988227,
                "kakaoMapUrl", generateKakaoMapUrl("N서울타워", 37.551169, 126.988227)
        ));

        // 샘플 4: 강남역
        samples.put("sample4", Map.of(
                "placeName", "강남역",
                "latitude", 37.498095,
                "longitude", 127.027610,
                "kakaoMapUrl", generateKakaoMapUrl("강남역", 37.498095, 127.027610)
        ));

        samples.put("instruction", "각 URL을 웹브라우저에 붙여넣어 테스트하세요.");

        return ResponseEntity.ok(BaseResponse.success("샘플 URL 생성 완료", samples));
    }

    // 카카오맵 URL 생성 (private 메서드)
    private String generateKakaoMapUrl(String placeName, Double latitude, Double longitude) {
        return String.format("https://map.kakao.com/link/map/%s,%s,%s",
                placeName,
                latitude,
                longitude);
    }
}