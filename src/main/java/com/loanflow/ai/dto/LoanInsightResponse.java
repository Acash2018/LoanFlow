package com.loanflow.ai.dto;

import java.time.Instant;

public record LoanInsightResponse(
        Long id,
        Long loanApplicationId,
        String insightType,
        String title,
        String description,
        String severity,
        Instant generatedAt
) {
}
