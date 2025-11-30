package org.example.localy.dto.chatBot.response;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Data
@Builder
@Getter
@Setter
public class PredictResponse {
    private String text;
    private int predicted_label;
    private String emotion_name;
    private double confidence;
    private Map<String, Double> probabilities;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPredicted_label() {
        return predicted_label;
    }

    public void setPredicted_label(int predicted_label) {
        this.predicted_label = predicted_label;
    }

    public String getEmotion_name() {
        return emotion_name;
    }

    public void setEmotion_name(String emotion_name) {
        this.emotion_name = emotion_name;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Map<String, Double> probabilities) {
        this.probabilities = probabilities;
    }
}
