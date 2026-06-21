package com.loanflow.ai.dto;

import java.util.Map;

public record ChromaQueryResult(
        String id,
        String text,
        Map<String, Object> metadata,
        Double distance
) {
}
