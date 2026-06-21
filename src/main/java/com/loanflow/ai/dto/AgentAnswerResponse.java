package com.loanflow.ai.dto;

import java.time.Instant;
import java.util.List;

public record AgentAnswerResponse(
        String answer,
        List<String> citations,
        Instant answeredAt
) {
}
