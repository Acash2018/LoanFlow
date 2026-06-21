package com.loanflow.ai.service;

import com.loanflow.ai.dto.ChromaQueryResult;
import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.dto.LoanSourceResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LoanRagService {

    private final ChromaDbService chromaDbService;
    private final DocumentChunkingService documentChunkingService;
    private final OllamaClient ollamaClient;

    public LoanRagService(
            ChromaDbService chromaDbService,
            DocumentChunkingService documentChunkingService,
            OllamaClient ollamaClient
    ) {
        this.chromaDbService = chromaDbService;
        this.documentChunkingService = documentChunkingService;
        this.ollamaClient = ollamaClient;
    }

    public LoanAskResponse ask(Long loanApplicationId, String question) {
        List<ChromaQueryResult> results = retrieve(loanApplicationId, question, 5);
        String answer = ollamaClient.chat(buildPrompt(question, results));
        return new LoanAskResponse(answer, sources(results));
    }

    public List<ChromaQueryResult> retrieve(Long loanApplicationId, String question, int topK) {
        String collectionName = documentChunkingService.collectionName(loanApplicationId);
        return chromaDbService.query(collectionName, question, topK);
    }

    private String buildPrompt(String question, List<ChromaQueryResult> results) {
        StringBuilder context = new StringBuilder();
        for (ChromaQueryResult result : results) {
            Map<String, Object> metadata = result.metadata();
            context.append("Source: ")
                    .append(metadata.getOrDefault("documentName", "unknown document"))
                    .append(" chunk ")
                    .append(metadata.getOrDefault("chunkIndex", "unknown"))
                    .append('\n')
                    .append(result.text())
                    .append("\n\n");
        }

        return """
                You are LoanFlow AI, an underwriting assistant for single-family mortgage files.
                Answer the user's question using only the retrieved context.
                Cite document names and chunk numbers in the answer when possible.
                If the answer is not present in the context, say that it is not available in the provided loan file.

                Question:
                %s

                Retrieved context:
                %s
                """.formatted(question, context);
    }

    private List<LoanSourceResponse> sources(List<ChromaQueryResult> results) {
        return results.stream()
                .map(result -> {
                    Map<String, Object> metadata = result.metadata();
                    return new LoanSourceResponse(
                            longValue(metadata.get("loanDocumentId")),
                            stringValue(metadata.get("documentName")),
                            intValue(metadata.get("chunkIndex")),
                            stringValue(metadata.get("embeddingId")),
                            excerpt(result.text())
                    );
                })
                .toList();
    }

    private String excerpt(String text) {
        if (text == null || text.length() <= 240) {
            return text;
        }
        return text.substring(0, 240) + "...";
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
