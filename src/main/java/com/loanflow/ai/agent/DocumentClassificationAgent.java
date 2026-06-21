package com.loanflow.ai.agent;

import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanRagService;
import org.springframework.stereotype.Service;

@Service
public class DocumentClassificationAgent {

    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public DocumentClassificationAgent(LoanRagService loanRagService, LoanInsightService loanInsightService) {
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    public LoanInsight run(Long loanApplicationId) {
        LoanAskResponse response = loanRagService.ask(loanApplicationId, """
                Classify every document in this loan file as one of:
                paystub, W2, tax return, bank statement, credit report, appraisal, title,
                purchase agreement, or other. Include brief evidence for each classification.
                """);

        return loanInsightService.save(
                loanApplicationId,
                "DOCUMENT_CLASSIFICATION",
                "Document classification",
                response.answer(),
                "INFO"
        );
    }
}
