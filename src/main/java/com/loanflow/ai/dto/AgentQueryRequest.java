package com.loanflow.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentQueryRequest(
        @NotBlank String loanNumber,
        @NotBlank String question
) {
}
