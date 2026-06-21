package com.loanflow.ai.dto;

public record LoanSourceResponse(
        Long documentId,
        String documentName,
        Integer chunkIndex,
        String embeddingId,
        String excerpt
) {
}
