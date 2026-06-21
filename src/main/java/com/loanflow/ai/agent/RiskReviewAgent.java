package com.loanflow.ai.agent;

import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanRagService;
import org.springframework.stereotype.Service;

@Service
public class RiskReviewAgent {

    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public RiskReviewAgent(LoanRagService loanRagService, LoanInsightService loanInsightService) {
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    public LoanInsight run(Long loanApplicationId) {
        LoanAskResponse response = loanRagService.ask(loanApplicationId, """
                Review the loan file for potential risks such as missing pages,
                inconsistent borrower names, mismatched addresses, unclear income,
                unusual deposits, incomplete signatures, or conflicting document facts.
                """);

        return loanInsightService.save(
                loanApplicationId,
                "RISK_REVIEW",
                "Risk review",
                response.answer(),
                "HIGH"
        );
    }
}
