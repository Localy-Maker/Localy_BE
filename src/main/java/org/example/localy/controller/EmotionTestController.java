package org.example.localy.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "EmotionTest", description = "감정 테스트 API (감정 테스트용이니 테스트 이외의 용도로 사용하시면 안 됩니다!")
@RestController
@RequestMapping("/api/emotionTest")
@RequiredArgsConstructor
public class EmotionTestController {

    private final RedisTemplate<String, String> redisTemplate;

    @Operation(
            summary = "테스트용 - 특정 사용자의 감정 수치를 50으로 설정합니다.",
            description = """
          """
    )
    @GetMapping("/create/{userId}")
    public String createEmotion(@PathVariable String userId) {
        String key = "localy:emotion:"+userId;
        redisTemplate.opsForValue().set(key, "50");

        return "해당 유저 현재 감정 50으로 설정 완료";
    }

    @Operation(
            summary = "테스트용 - 특정 사용자의 감정 수치를 figure만큼 변경합니다.",
            description = """
          """
    )
    @PutMapping("/update/{userId}")
    public String updateEmotion(@PathVariable String userId, @RequestParam int figure) {
        String key = "localy:emotion:"+userId;
        redisTemplate.opsForValue().increment(key, figure);

        return "해당 유저 현재 감정 "+figure+"만큼 변경 완료\n현재 감정:"+redisTemplate.opsForValue().get(key);
    }

    @Operation(
            summary = "테스트용 - 특정 사용자의 현재 감정 수치를 반환합니다.",
            description = """
          """
    )
    @GetMapping("/status/{userId}")
    public String statusEmotion(@PathVariable String userId) {
        String key = "localy:emotion:"+userId;
        String figure = redisTemplate.opsForValue().get(key);

        return "해당 유저의 현재 감정 수치 : "+figure;
    }

    @Operation(
            summary = "테스트용 - 특정 사용자의 현재 감정 수치를 삭제합니다.",
            description = """
          """
    )
    @DeleteMapping("/delete/{userId}")
    public String deleteEmotion(@PathVariable String userId) {
        String key = "localy:emotion:"+userId;
        redisTemplate.delete(key);

        return "해당 유저의 현재 감정 수치 삭제 완료";
    }
    
}
