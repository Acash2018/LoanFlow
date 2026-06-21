package com.loanflow.ai.agent;

import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanRagService;
import org.springframework.stereotype.Service;

@Service
public class LoanCompletenessAgent {

    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public LoanCompletenessAgent(LoanRagService loanRagService, LoanInsightService loanInsightService) {
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    public LoanInsight run(Long loanApplicationId) {
        LoanAskResponse response = loanRagService.ask(loanApplicationId, """
                Check whether the loan file appears complete for underwriting.
                Flag missing documents, especially income, assets, credit, appraisal, title,
                purchase agreement, identity, and property documentation.
                """);

        return loanInsightService.save(
                loanApplicationId,
                "COMPLETENESS",
                "Loan completeness review",
                response.answer(),
                "WARNING"
        );
    }
}
