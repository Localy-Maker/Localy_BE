package org.example.localy.service;

import com.google.cloud.translate.v3.*;
import org.example.localy.dto.chatBot.response.TranslateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Service
public class TranslationService {

    @Value("${gcp.project-id}")
    private String projectId; // application.yml에서만 읽음

    private TranslationServiceClient client;
    private final String location = "global";

    @PostConstruct
    public void init() throws IOException {
        // GOOGLE_APPLICATION_CREDENTIALS 환경 변수를 그대로 사용
        this.client = TranslationServiceClient.create();
    }

    public TranslateResponse translateToKorean(String text) {
        return translateText(text, "ko");
    }

    public String translate(String text, String targetLang) {
        TranslateResponse response = translateText(text, targetLang);
        return response.getTranslatedText();
    }

    private TranslateResponse translateText(String text, String targetLang) {
        LocationName parent = LocationName.of(projectId, location);

        TranslateTextRequest request = TranslateTextRequest.newBuilder()
                .setParent(parent.toString())
                .addContents(text)
                .setTargetLanguageCode(targetLang)
                .setMimeType("text/plain")
                .build();

        TranslateTextResponse response = client.translateText(request);
        Translation translation = response.getTranslations(0);

        return TranslateResponse.builder()
                .translatedText(translation.getTranslatedText())
                .language(translation.getDetectedLanguageCode())
                .build();
    }
}
