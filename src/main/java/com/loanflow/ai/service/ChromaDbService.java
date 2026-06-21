package com.loanflow.ai.service;

import com.loanflow.ai.config.ChromaProperties;
import com.loanflow.ai.dto.ChromaQueryResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChromaDbService {

    private final RestClient restClient;
    private final OllamaClient ollamaClient;

    public ChromaDbService(RestClient.Builder builder, ChromaProperties chromaProperties, OllamaClient ollamaClient) {
        this.restClient = builder.baseUrl(chromaProperties.baseUrl()).build();
        this.ollamaClient = ollamaClient;
    }

    public void createCollection(String collectionName) {
        try {
            restClient.post()
                    .uri("/api/v1/collections")
                    .body(Map.of("name", collectionName))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 409) {
                throw e;
            }
        }
    }

    public void addDocument(String collectionName, String id, String text, Map<String, Object> metadata) {
        List<Double> embedding = ollamaClient.embed(text);
        restClient.post()
                .uri("/api/v1/collections/{collectionName}/add", collectionName)
                .body(Map.of(
                        "ids", List.of(id),
                        "documents", List.of(text),
                        "embeddings", List.of(embedding),
                        "metadatas", List.of(metadata)
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public List<ChromaQueryResult> query(String collectionName, String question, int topK) {
        List<Double> embedding = ollamaClient.embed(question);
        Map<?, ?> response = restClient.post()
                .uri("/api/v1/collections/{collectionName}/query", collectionName)
                .body(Map.of(
                        "query_embeddings", List.of(embedding),
                        "n_results", topK
                ))
                .retrieve()
                .body(Map.class);

        return parseQueryResults(response);
    }

    private List<ChromaQueryResult> parseQueryResults(Map<?, ?> response) {
        if (response == null) {
            return List.of();
        }

        List<?> ids = firstNestedList(response.get("ids"));
        List<?> documents = firstNestedList(response.get("documents"));
        List<?> metadatas = firstNestedList(response.get("metadatas"));
        List<?> distances = firstNestedList(response.get("distances"));

        List<ChromaQueryResult> results = new ArrayList<>();
        for (int index = 0; index < documents.size(); index++) {
            String id = valueAt(ids, index);
            String text = valueAt(documents, index);
            Map<String, Object> metadata = metadataAt(metadatas, index);
            Double distance = distanceAt(distances, index);
            results.add(new ChromaQueryResult(id, text, metadata, distance));
        }
        return results;
    }

    private List<?> firstNestedList(Object value) {
        if (!(value instanceof List<?> outer) || outer.isEmpty()) {
            return List.of();
        }
        Object first = outer.get(0);
        return first instanceof List<?> nested ? nested : outer;
    }

    private String valueAt(List<?> values, int index) {
        if (index >= values.size() || values.get(index) == null) {
            return "";
        }
        return values.get(index).toString();
    }

    private Map<String, Object> metadataAt(List<?> values, int index) {
        if (index >= values.size() || !(values.get(index) instanceof Map<?, ?> metadata)) {
            return Map.of();
        }

        Map<String, Object> typed = new LinkedHashMap<>();
        metadata.forEach((key, value) -> typed.put(key.toString(), value));
        return typed;
    }

    private Double distanceAt(List<?> values, int index) {
        if (index >= values.size() || !(values.get(index) instanceof Number number)) {
            return null;
        }
        return number.doubleValue();
    }
}
