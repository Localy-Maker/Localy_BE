package org.example.localy.service.Chat;

import com.google.cloud.translate.v3.*;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.dto.chatBot.response.TranslateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;

@Slf4j
@Service
public class TranslationService {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    private TranslationServiceClient client;
    private boolean isAvailable = false;
    private final String location = "global";

    @PostConstruct
    public void init() {
        try {
            this.client = TranslationServiceClient.create();
            this.isAvailable = true;
            log.info("✅ Google Translation Service initialized successfully");
        } catch (IOException e) {
            log.warn("⚠️ Google Translation Service initialization failed: {}", e.getMessage());
            log.warn("⚠️ Translation features will be disabled. This is normal in test environments.");
            this.isAvailable = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (client != null) {
            client.close();
            log.info("🛑 Google Translation Service closed");
        }
    }

    public TranslateResponse translateToKorean(String text) {
        return translateText(text, "ko");
    }

    public String translate(String text, String targetLang) {
        TranslateResponse response = translateText(text, targetLang);
        return response.getTranslatedText();
    }

    private TranslateResponse translateText(String text, String targetLang) {
        // Translation Service가 사용 불가능한 경우 원본 텍스트 반환
        if (!isAvailable) {
            log.warn("⚠️ Translation service not available, returning original text");
            return TranslateResponse.builder()
                    .translatedText(text)
                    .language("unknown")
                    .build();
        }

        try {
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
        } catch (Exception e) {
            log.error("❌ Translation failed: {}", e.getMessage());
            // 번역 실패 시 원본 텍스트 반환
            return TranslateResponse.builder()
                    .translatedText(text)
                    .language("unknown")
                    .build();
        }
    }
}