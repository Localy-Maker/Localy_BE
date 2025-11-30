package org.example.localy.constant;

import java.util.HashMap;
import java.util.Map;

public class EmotionConstants {

    private static final Map<String, String> EMOTION_KOREAN_MAP = new HashMap<>();

    static {
        // 사용자 지정 핵심 6가지 감정
        EMOTION_KOREAN_MAP.put("happy", "행복");
        EMOTION_KOREAN_MAP.put("sadness", "슬픔");
        EMOTION_KOREAN_MAP.put("depressed", "우울");
        EMOTION_KOREAN_MAP.put("anger", "분노");
        EMOTION_KOREAN_MAP.put("anxiety", "불안");
        EMOTION_KOREAN_MAP.put("neutral", "중립");

        EMOTION_KOREAN_MAP.put("joy", "기쁨");
    }

    public static String toKorean(String emotionCode) {
        if (emotionCode == null || emotionCode.isEmpty()) {
            return "중립";
        }
        return EMOTION_KOREAN_MAP.getOrDefault(emotionCode.toLowerCase(), "중립");
    }

    public static String toCode(String koreanKeyword) {
        return EMOTION_KOREAN_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().equals(koreanKeyword))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("neutral");
    }

    public static Map<String, String> getAllEmotions() {
        return new HashMap<>(EMOTION_KOREAN_MAP);
    }

    public static boolean isValidEmotionCode(String emotionCode) {
        return emotionCode != null && EMOTION_KOREAN_MAP.containsKey(emotionCode.toLowerCase());
    }
}
