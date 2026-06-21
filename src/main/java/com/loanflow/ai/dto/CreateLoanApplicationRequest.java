package com.loanflow.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLoanApplicationRequest(
        @NotBlank String loanNumber,
        @NotBlank String borrowerName,
        @NotBlank String propertyAddress,
        @NotBlank String loanType
) {
}
