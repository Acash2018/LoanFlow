package com.loanflow.ai.dto;

public record LoanProcessingResponse(
        String processingStatus,
        int documentsProcessed,
        int insightsGenerated,
        String finalUnderwritingSummary
) {
}
