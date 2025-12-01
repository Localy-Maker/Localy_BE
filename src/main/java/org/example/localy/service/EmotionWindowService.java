package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import org.example.localy.repository.EmotionWindowResultRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmotionWindowService {

    private final EmotionWindowResultRepository repository;

    public Map<String, Object> analyzeUserEmotion(Long userId) {

        // 1) 가장 많이 등장한 section 찾기
        List<Object[]> sectionCounts = repository.findSectionCountByUser(userId);

        if (sectionCounts.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("mostFrequentSection", null);
            result.put("mostFrequentEmotion", null);
            return result;
        }

        Integer mostFrequentSection = (Integer) sectionCounts.get(0)[0];

        // 2) 해당 section에서 emotion 최빈값 찾기
        List<Object[]> emotionCounts =
                repository.findEmotionCountByUserAndSection(userId, mostFrequentSection);

        String mostFrequentEmotion = (String) emotionCounts.get(0)[0];

        return Map.of(
                "mostFrequentSection", mostFrequentSection,
                "mostFrequentEmotion", mostFrequentEmotion
        );
    }
}