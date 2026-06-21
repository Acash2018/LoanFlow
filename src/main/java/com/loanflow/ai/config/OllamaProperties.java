package com.loanflow.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loanflow.ollama")
public record OllamaProperties(
        String baseUrl,
        String embeddingModel,
        String chatModel
) {
}
