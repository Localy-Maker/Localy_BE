package org.example.localy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.example.localy.entity.place.Place;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService {

    @Value("${openai.api.key}")
    private String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateReply(String userMessage) {
        OpenAiService service = new OpenAiService(apiKey);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("user", "사용자 입력: " + userMessage + "\n친절하고 권유형으로 답변해줘:")
                ))
                .temperature(0.7)
                .maxTokens(150)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent().trim();
    }

    public MissionCreationResult createMissionContent(String placeName, String category, String emotion) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API Key가 설정되지 않았습니다. 미션 생성을 위해 Key를 설정하세요.");
        }

        OpenAiService service = new OpenAiService(apiKey);

        String systemPrompt = String.format(
                "미션은 해당 장소에서 어떤 행동을 할지 구체적으로 제시해야 하며, 문화 체험이나 감정 해소에 초점을 맞춰" +
                        "장소: %s (%s), 사용자 감정: %s. " +
                        "응답은 반드시 {\"title\":\"...\",\"description\":\"...\"} 형태의 JSON 문자열만 포함해야함",
                placeName, category, emotion
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", "해당 장소에서 할만한 행동을 미션으로 생성해줘.")
                ))
                .temperature(0.8)
                .maxTokens(200)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        String jsonContent = result.getChoices().get(0).getMessage().getContent().trim();
        return parseMissionJson(jsonContent);
    }

    // 장소 추천 요청
    public PlaceRecommendationResult getRecommendedPlacesByEmotion(
            List<Place> availablePlaces, String emotion, String interests) {

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API Key가 설정되지 않았습니다. 장소 추천을 위해 Key를 설정하세요.");
        }
        if (availablePlaces.isEmpty()) {
            return new PlaceRecommendationResult(List.of());
        }

        // 1. Place 엔티티 목록을 GPT가 처리할 수 있는 JSON 형태로 변환
        String placesJson = availablePlaces.stream()
                .map(p -> String.format("{\"id\":%d, \"name\":\"%s\", \"category\":\"%s\"}",
                        p.getId(), p.getTitle(), p.getCategory()))
                .collect(Collectors.joining(", ", "[", "]"));

        String systemPrompt = String.format(
                "너는 사용자 맞춤형 여행 가이드 AI야. 다음 정보를 참고하여 가장 적합한 최대 5개의 장소 ID와 추천 이유, 매칭 점수를 JSON 형식으로 반환해줘" +
                        "사용자 감정: %s. 관심사: %s. " +
                        "응답은 반드시 {\"recommendedPlaces\": [{id: Long, reason: String, matchScore: Double}]} 형태여야함" +
                        "입력된 장소 목록: %s",
                emotion, interests != null ? interests : "없음", placesJson
        );

        OpenAiService service = new OpenAiService(apiKey);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", "감정에 맞는 최대 5곳을 추천하고 이유와 매칭 점수를 포함한 JSON을 반환해줘.")
                ))
                .temperature(0.7)
                .maxTokens(400)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        String jsonContent = result.getChoices().get(0).getMessage().getContent().trim();
        return parseRecommendationJson(jsonContent);
    }

    @Getter
    public static class MissionCreationResult {
        private final String title;
        private final String description;

        public MissionCreationResult(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    @Getter
    public static class PlaceRecommendationResult {
        private final List<RecommendedPlace> recommendedPlaces;

        public PlaceRecommendationResult(List<RecommendedPlace> recommendedPlaces) {
            this.recommendedPlaces = recommendedPlaces;
        }

        @Getter
        public static class RecommendedPlace {
            private Long placeId;
            private String reason;
            private Double matchScore;

            public RecommendedPlace() {}

            public RecommendedPlace(Long placeId, String reason, Double matchScore) {
                this.placeId = placeId;
                this.reason = reason;
                this.matchScore = matchScore;
            }
        }
    }

    private MissionCreationResult parseMissionJson(String json) {
        Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
        Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");

        Matcher titleMatcher = titlePattern.matcher(json);
        Matcher descMatcher = descPattern.matcher(json);

        String title = "제목 파싱 오류";
        String description = "설명 파싱 오류";

        if (titleMatcher.find()) {
            title = titleMatcher.group(1);
        }
        if (descMatcher.find()) {
            description = descMatcher.group(1);
        }

        return new MissionCreationResult(title, description);
    }

    private PlaceRecommendationResult parseRecommendationJson(String json) {
        try {
            TypeReference<java.util.Map<String, List<PlaceRecommendationResult.RecommendedPlace>>> typeRef =
                    new TypeReference<java.util.Map<String, List<PlaceRecommendationResult.RecommendedPlace>>>() {};

            java.util.Map<String, List<PlaceRecommendationResult.RecommendedPlace>> map =
                    objectMapper.readValue(json, typeRef);

            List<PlaceRecommendationResult.RecommendedPlace> recommendedList =
                    map.getOrDefault("recommendedPlaces", List.of());

            return new PlaceRecommendationResult(recommendedList);

        } catch (Exception e) {
            log.error("GPT 추천 응답 JSON 파싱 실패: {}", json, e);
            return new PlaceRecommendationResult(List.of());
        }
    }
}
