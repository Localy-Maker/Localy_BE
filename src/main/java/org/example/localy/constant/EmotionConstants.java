package org.example.localy.constant;

import java.util.HashMap;
import java.util.Map;

// TODO: 수정 필요
public class EmotionConstants {

    private static final Map<String, String> EMOTION_KOREAN_MAP = new HashMap<>();

    static {
        EMOTION_KOREAN_MAP.put("loneliness", "외로움");
        EMOTION_KOREAN_MAP.put("joy", "기쁨");
        EMOTION_KOREAN_MAP.put("sadness", "슬픔");
        EMOTION_KOREAN_MAP.put("anger", "분노");
        EMOTION_KOREAN_MAP.put("fear", "두려움");
        EMOTION_KOREAN_MAP.put("surprise", "놀람");
        EMOTION_KOREAN_MAP.put("neutral", "평온");
    }

    public static String toKorean(String emotionCode) {
        if (emotionCode == null || emotionCode.isEmpty()) {
            return "평온";
        }
        return EMOTION_KOREAN_MAP.getOrDefault(emotionCode.toLowerCase(), "평온");
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
