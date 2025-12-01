package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import org.example.localy.dto.emotion.EmotionLogDto;
import org.example.localy.dto.emotion.SentimentResult;
import org.example.localy.entity.EmotionWindowResult;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.example.localy.service.Chat.GPTService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private final EmotionWindowResultRepository resultRepository;
    private final GPTService gptService;

    private static final Map<String, List<String>> EMOTION_WORDS = Map.of(
            "VERY_NEG", List.of("절망", "무기력", "불안", "공허"),
            "NEG", List.of("짜증", "실망", "걱정", "예민함", "답답함"),
            "NEUTRAL", List.of("담담함", "무난함", "심심함", "어수선함"),
            "POS_LIGHT", List.of("차분함", "안정감", "편안함", "여유로움"),
            "POS", List.of("즐거움", "만족", "활기참", "설렘", "뿌듯함"),
            "VERY_POS", List.of("행복", "감동", "신남", "황홀함", "기쁨")
    );

//    public String mapScoreToCategory(double score) {
//        if (score <= 16) return "VERY_NEG";
//        if (score <= 33) return "NEG";
//        if (score <= 50) return "NEUTRAL";
//        if (score <= 66) return "POS_LIGHT";
//        if (score <= 83) return "POS";
//        return "VERY_POS";
//    }

    public SentimentResult mapScoreToCategory(double score) {
        if (score <= 16) return new SentimentResult("VERY_NEG", 1);
        if (score <= 33) return new SentimentResult("NEG", 2);
        if (score <= 50) return new SentimentResult("NEUTRAL", 3);
        if (score <= 66) return new SentimentResult("POS_LIGHT", 4);
        if (score <= 83) return new SentimentResult("POS", 5);
        return new SentimentResult("VERY_POS", 6);
    }

    // 유저별 감정 분석 후 DB 저장
    public EmotionWindowResult saveWindowResult(Long userId, String window, double avgScore) {

        SentimentResult res = mapScoreToCategory(avgScore);
        String category = res.category();
        Integer code = res.code();

        String selectedWord;
        try {
            selectedWord = gptService.pickEmotionKeyword(category, avgScore);
        } catch (Exception e) {
            selectedWord = EMOTION_WORDS.get(category).get(0);
        }

        EmotionWindowResult result = EmotionWindowResult.builder()
                .userId(userId)
                .window(window)
                .avgScore(avgScore)
                .emotion(selectedWord)
                .section(code)
                .build();

        return resultRepository.save(result);
    }

}
