package org.example.localy.service.Chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.request.PredictRequest;
import org.example.localy.dto.chatBot.response.PredictResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictClientService {

    private final WebClient webClient;
    @Value("${emotion.url}")
    private String fastApiUrl;  // 추가

    public PredictResponse requestEmotion(String text) {
        PredictRequest request = new PredictRequest(text);
          // 환경변수 주입

        return webClient.post()
                .uri(fastApiUrl + "/textEmotion/predict")  // 환경변수 사용
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictResponse.class)
                .block();   // 동기 방식
    }
}

