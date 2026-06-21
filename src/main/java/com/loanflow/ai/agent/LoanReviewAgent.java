package com.loanflow.ai.agent;

import com.loanflow.ai.dto.AgentAnswerResponse;
import com.loanflow.ai.dto.AgentQueryRequest;
import com.loanflow.ai.service.OllamaClient;
import com.loanflow.ai.service.VectorIndexService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class LoanReviewAgent {

    private final VectorIndexService vectorIndexService;
    private final OllamaClient ollamaClient;

    public LoanReviewAgent(VectorIndexService vectorIndexService, OllamaClient ollamaClient) {
        this.vectorIndexService = vectorIndexService;
        this.ollamaClient = ollamaClient;
    }

    public AgentAnswerResponse answer(AgentQueryRequest request) {
        List<String> context = vectorIndexService.search(request.loanNumber(), request.question());
        String prompt = """
                You are LoanFlow AI, an assistant for reviewing single-family mortgage loan files.
                Answer only from the provided retrieved context. If the answer is not present, say so.

                Loan number: %s
                Question: %s

                Retrieved context:
                %s
                """.formatted(request.loanNumber(), request.question(), String.join("\n\n", context));

        return new AgentAnswerResponse(
                ollamaClient.chat(prompt),
                context,
                Instant.now()
        );
    }
}
