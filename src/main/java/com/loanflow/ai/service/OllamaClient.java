package com.loanflow.ai.service;

import com.loanflow.ai.config.OllamaProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(RestClient.Builder builder, OllamaProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    public List<Double> embed(String text) {
        Map<?, ?> response = restClient.post()
                .uri("/api/embeddings")
                .body(Map.of(
                        "model", properties.embeddingModel(),
                        "prompt", text
                ))
                .retrieve()
                .body(Map.class);

        Object embedding = response == null ? null : response.get("embedding");
        if (!(embedding instanceof List<?> values)) {
            return List.of();
        }

        List<Double> doubles = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Number number) {
                doubles.add(number.doubleValue());
            }
        }
        return doubles;
    }

    public String chat(String prompt) {
        Map<?, ?> response = restClient.post()
                .uri("/api/generate")
                .body(Map.of(
                        "model", properties.chatModel(),
                        "prompt", prompt,
                        "stream", false
                ))
                .retrieve()
                .body(Map.class);

        Object answer = response == null ? null : response.get("response");
        return answer == null ? "" : answer.toString();
    }
}
