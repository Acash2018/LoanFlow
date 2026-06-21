package com.loanflow.ai.dto;

import java.time.Instant;

public record LoanDocumentResponse(
        Long id,
        Long loanApplicationId,
        String documentName,
        String documentType,
        String s3Bucket,
        String s3Key,
        String textPreview,
        Instant uploadedAt,
        boolean processed
) {
}
