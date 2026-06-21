package com.loanflow.ai.agent;

import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanRagService;
import org.springframework.stereotype.Service;

@Service
public class IncomeAnalysisAgent {

    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public IncomeAnalysisAgent(LoanRagService loanRagService, LoanInsightService loanInsightService) {
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    public LoanInsight run(Long loanApplicationId) {
        LoanAskResponse response = loanRagService.ask(loanApplicationId, """
                Review income-related documents such as paystubs, W2s, tax returns,
                employment verification, and bank statements. Summarize borrower income
                and flag inconsistencies or unclear income evidence.
                """);

        return loanInsightService.save(
                loanApplicationId,
                "INCOME_ANALYSIS",
                "Income analysis",
                response.answer(),
                "WARNING"
        );
    }
}
