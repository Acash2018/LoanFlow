package com.loanflow.ai.controller;

import com.loanflow.ai.agent.LoanReviewAgent;
import com.loanflow.ai.dto.AgentAnswerResponse;
import com.loanflow.ai.dto.AgentQueryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class RagController {

    private final LoanReviewAgent loanReviewAgent;

    public RagController(LoanReviewAgent loanReviewAgent) {
        this.loanReviewAgent = loanReviewAgent;
    }

    @PostMapping("/ask")
    public AgentAnswerResponse ask(@Valid @RequestBody AgentQueryRequest request) {
        return loanReviewAgent.answer(request);
    }
}
