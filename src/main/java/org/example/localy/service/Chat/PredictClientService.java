package org.example.localy.service.Chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.request.PredictRequest;
import org.example.localy.dto.chatBot.response.PredictResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictClientService {

    private final WebClient webClient;

    public PredictResponse requestEmotion(String text) {
        PredictRequest request = new PredictRequest(text);

        return webClient.post()
                .uri("http://localhost:8000/textEmotion/predict")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictResponse.class)
                .block();   // 동기 방식
    }
}

