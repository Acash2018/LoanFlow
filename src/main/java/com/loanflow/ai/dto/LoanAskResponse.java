package com.loanflow.ai.dto;

import java.util.List;

public record LoanAskResponse(
        String answer,
        List<LoanSourceResponse> sources
) {
}
