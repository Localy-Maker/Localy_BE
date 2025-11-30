package org.example.localy.dto.chatBot.request;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Data
@Setter
@Getter
@Builder
public class PredictRequest {
    private String text;

    public PredictRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

