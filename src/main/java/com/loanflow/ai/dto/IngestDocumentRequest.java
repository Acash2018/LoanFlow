package com.loanflow.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IngestDocumentRequest(
        @NotNull Long loanApplicationId,
        @NotBlank String documentName,
        @NotBlank String documentType,
        @NotBlank String s3Bucket,
        @NotBlank String s3Key,
        boolean processed
) {
}
