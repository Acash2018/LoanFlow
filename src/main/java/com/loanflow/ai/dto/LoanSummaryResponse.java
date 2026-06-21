package com.loanflow.ai.dto;

public record LoanSummaryResponse(
        Long loanApplicationId,
        String summary
) {
}
