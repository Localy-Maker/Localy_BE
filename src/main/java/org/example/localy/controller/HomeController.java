package org.example.localy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.localy.common.response.BaseResponse;
import org.example.localy.dto.HomeResponseDto;
import org.example.localy.util.JwtUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Home", description = "홈 API")
@RequestMapping("/api/home")
public class HomeController {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "홈 - 주간 감정 트렌드 요약 가져오기",
            description = """
          """
    )
    @GetMapping
    public BaseResponse<HomeResponseDto> getHomeData(HttpServletRequest request) {

        Long userId = jwtUtil.getUserIdFromToken(jwtUtil.getTokenFromHeader(request));

        String emotion = redisTemplate.opsForValue().get("localy:home:emotion:" + userId);
        String diff = redisTemplate.opsForValue().get("localy:home:happiness_diff:" + userId);

        return BaseResponse.success(
                new HomeResponseDto(
                        emotion != null ? emotion : "없음",
                        diff != null ? diff : "0.0"
                )
        );
    }
}
