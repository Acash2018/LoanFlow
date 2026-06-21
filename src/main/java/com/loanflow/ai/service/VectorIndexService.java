package com.loanflow.ai.service;

import com.loanflow.ai.config.ChromaProperties;
import com.loanflow.ai.model.LoanDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class VectorIndexService {

    private final RestClient restClient;
    private final ChromaProperties chromaProperties;
    private final OllamaClient ollamaClient;

    public VectorIndexService(RestClient.Builder builder, ChromaProperties chromaProperties, OllamaClient ollamaClient) {
        this.restClient = builder.baseUrl(chromaProperties.baseUrl()).build();
        this.chromaProperties = chromaProperties;
        this.ollamaClient = ollamaClient;
    }

    public void index(LoanDocument document, String text) {
        List<String> chunks = chunk(text);
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            List<Double> embedding = ollamaClient.embed(chunk);
            // TODO: Persist chunk, embedding, and metadata to ChromaDB collection.
            // Metadata should include loanApplicationId, documentId, s3Key, documentType, and chunk index.
        }
    }

    public List<String> search(String loanNumber, String question) {
        // TODO: Query ChromaDB with an Ollama embedding for the question.
        return List.of();
    }

    private List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = 2_000;
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : text.lines().toList()) {
            if (!current.isEmpty() && current.length() + line.length() + 1 > chunkSize) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append('\n');
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }
}
