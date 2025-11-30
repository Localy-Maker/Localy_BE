package org.example.localy.dto.chatBot.response;

import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TranslateResponse {
    private String translatedText;
    private String language;
}
