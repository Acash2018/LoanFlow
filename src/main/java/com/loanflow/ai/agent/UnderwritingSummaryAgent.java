package com.loanflow.ai.agent;

import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanRagService;
import org.springframework.stereotype.Service;

@Service
public class UnderwritingSummaryAgent {

    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public UnderwritingSummaryAgent(LoanRagService loanRagService, LoanInsightService loanInsightService) {
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    public LoanInsight run(Long loanApplicationId) {
        LoanAskResponse response = loanRagService.ask(loanApplicationId, """
                Produce a human-readable underwriting summary for this loan file.
                Include borrower, property, key documents found, income observations,
                completeness concerns, notable risks, and recommended next actions.
                """);

        return loanInsightService.save(
                loanApplicationId,
                "UNDERWRITING_SUMMARY",
                "Underwriting summary",
                response.answer(),
                "INFO"
        );
    }
}
