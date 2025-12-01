//package org.example.localy.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.example.localy.service.EmotionWindowQueryService;
//import org.example.localy.dto.response.EmotionWindowResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/emotion")
//@RequiredArgsConstructor
//public class EmotionWindowController {
//
//    private final EmotionWindowQueryService queryService;
//
//    @GetMapping("/windows")
//    public ResponseEntity<?> getEmotionWindows() {
//        EmotionWindowResponse response = queryService.getTodayEmotionWindows();
//
//        return ResponseEntity.ok(
//                new ApiResponse<>(true, "요청이 성공적으로 처리되었습니다.", response)
//        );
//    }
//
//    record ApiResponse<T>(boolean success, String message, T data) {}
//}