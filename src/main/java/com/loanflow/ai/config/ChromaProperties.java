package com.loanflow.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loanflow.chroma")
public record ChromaProperties(
        String baseUrl,
        String collectionName
) {
}
