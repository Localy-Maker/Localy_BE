package org.example.localy.service.Chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiService;
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
import java.util.Map;
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

    Map<String, String> languageMap = Map.ofEntries(
            Map.entry("ko", "Korean"),
            Map.entry("en", "English"),
            Map.entry("fr", "French"),
            Map.entry("es", "Spanish"),
            Map.entry("de", "German"),
            Map.entry("it", "Italian"),
            Map.entry("ja", "Japanese"),
            Map.entry("zh", "Chinese"),
            Map.entry("ru", "Russian"),
            Map.entry("pt", "Portuguese")
    );

    public String generateReply(String userMessage, String language) {

        OpenAiService service = new OpenAiService(apiKey);

        String langName = languageMap.getOrDefault(language.toLowerCase(), language); // 기본은 그냥 들어온 값

        String prompt = String.format(
                "User input: \"%s\"\n" +
                        "Please respond in %s, in a friendly and persuasive tone. " +
                        "Make sure the entire response stays within 500 tokens. " +
                        "Also, end with a follow-up question to continue the conversation.",
                userMessage,
                langName
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", prompt)))
                .temperature(0.7)
                .maxTokens(500)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent().trim();
    }

    public String logingCheck(String userMessage) {
        OpenAiService service = new OpenAiService(apiKey);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("user", "" + userMessage + "에 그리움이라는 감정이 느껴지면 true로 안 느껴진다면 false로 단답형으로 답해줘.")
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

        if (availablePlaces.isEmpty()) {
            return new PlaceRecommendationResult(List.of());
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("GPT API Key가 설정되지 않았습니다. 임시 데이터를 반환합니다.");
            Place firstPlace = availablePlaces.get(0);
            return new PlaceRecommendationResult(List.of(
                    new PlaceRecommendationResult.RecommendedPlace(
                            firstPlace.getId(),
                            "GPT API Key 누락으로 인한 임시 추천",
                            0.75
                    )
            ));
        }

        try {
            // 실제 Place ID 목록 명확히 전달
            String placesJson = availablePlaces.stream()
                    .map(p -> String.format("{\"id\":%d, \"name\":\"%s\", \"category\":\"%s\"}",
                            p.getId(), p.getTitle(), p.getCategory()))
                    .collect(Collectors.joining(", ", "[", "]"));

            // 실제 사용 가능한 ID 목록
            String availableIds = availablePlaces.stream()
                    .map(p -> String.valueOf(p.getId()))
                    .collect(Collectors.joining(", "));

            String systemPrompt = String.format(
                    "너는 사용자 맞춤형 여행 가이드 AI야. 다음 정보를 참고하여 가장 적합한 장소를 최대 5개 추천해줘.\n\n" +
                            "**중요**: 반드시 아래 '사용 가능한 ID' 목록에서만 선택해야 해:\n" +
                            "사용 가능한 ID: [%s]\n\n" +
                            "사용자 감정: %s\n" +
                            "관심사: %s\n\n" +
                            "장소 목록:\n%s\n\n" +
                            "응답 형식은 반드시 다음과 같아야 해:\n" +
                            "{\"recommendedPlaces\": [{\"id\": <실제 ID 숫자>, \"reason\": \"추천 이유\", \"matchScore\": 0.0~1.0 사이 점수}]}\n\n" +
                            "주의: id는 반드시 위의 '사용 가능한 ID' 목록에 있는 숫자만 사용해야 해!",
                    availableIds,
                    emotion,
                    interests != null ? interests : "없음",
                    placesJson
            );

            OpenAiService service = new OpenAiService(apiKey);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage("system", systemPrompt),
                            new ChatMessage("user", "감정에 맞는 장소를 최대 5곳 추천하고, 반드시 제공된 ID 목록에서만 선택해서 JSON으로 반환해줘.")
                    ))
                    .temperature(0.7)
                    .maxTokens(600)
                    .build();

            ChatCompletionResult result = service.createChatCompletion(request);
            String jsonContent = result.getChoices().get(0).getMessage().getContent().trim();

            log.info("GPT 원본 응답: {}", jsonContent);

            PlaceRecommendationResult recommendation = parseRecommendationJson(jsonContent);

            // 유효한 ID만 필터링
            List<Long> validIds = availablePlaces.stream()
                    .map(Place::getId)
                    .collect(Collectors.toList());

            List<PlaceRecommendationResult.RecommendedPlace> validRecommendations =
                    recommendation.getRecommendedPlaces().stream()
                            .filter(rec -> validIds.contains(rec.getPlaceId()))
                            .collect(Collectors.toList());

            log.info("필터링 전: {}, 필터링 후: {}",
                    recommendation.getRecommendedPlaces().size(),
                    validRecommendations.size());

            return new PlaceRecommendationResult(validRecommendations);

        } catch (Exception e) {
            log.error("GPT 장소 추천 요청 처리 중 심각한 오류 발생: {}", e.getMessage(), e);

            // 실패 시 첫 번째 장소 반환
            Place firstPlace = availablePlaces.get(0);
            return new PlaceRecommendationResult(List.of(
                    new PlaceRecommendationResult.RecommendedPlace(
                            firstPlace.getId(),
                            "GPT 호출 실패로 인한 임시 추천 (테스트용)",
                            0.75
                    )
            ));
        }
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
            @JsonProperty("id")
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

    public String pickEmotionKeyword(String category, double avgScore) {

        OpenAiService service = new OpenAiService(apiKey);

        String prompt = String.format(
                "감정 구간: %s, 평균 점수: %.2f\n" +
                        "이 감정 상태를 가장 잘 표현하는 한국어 단어를 1개만 제시해줘. 절대로 문장 금지, 단어만 답해.",
                category, avgScore
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("user", prompt)
                ))
                .temperature(0.5)
                .maxTokens(10)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent().trim();
    }
}