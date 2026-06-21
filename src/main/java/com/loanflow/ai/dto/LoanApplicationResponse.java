package com.loanflow.ai.dto;

import java.time.Instant;

public record LoanApplicationResponse(
        Long id,
        String loanNumber,
        String borrowerName,
        String propertyAddress,
        String loanType,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
