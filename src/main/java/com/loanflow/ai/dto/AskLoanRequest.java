package com.loanflow.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AskLoanRequest(
        @NotBlank String question
) {
}
