package org.example.localy.service;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GPTService {

    @Value("${openai.api.key}")
    private String apiKey;

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
}
