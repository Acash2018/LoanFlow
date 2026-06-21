package com.loanflow.ai.service;

import com.loanflow.ai.agent.DocumentClassificationAgent;
import com.loanflow.ai.agent.IncomeAnalysisAgent;
import com.loanflow.ai.agent.LoanCompletenessAgent;
import com.loanflow.ai.agent.RiskReviewAgent;
import com.loanflow.ai.agent.UnderwritingSummaryAgent;
import com.loanflow.ai.dto.LoanDocumentResponse;
import com.loanflow.ai.dto.LoanProcessingResponse;
import com.loanflow.ai.model.LoanApplication;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LoanProcessingOrchestrator {

    private static final String PROCESSING = "PROCESSING";
    private static final String PROCESSED = "PROCESSED";
    private static final String FAILED = "FAILED";

    private final LoanApplicationRepository loanApplicationRepository;
    private final S3DocumentIngestionService s3DocumentIngestionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentClassificationAgent documentClassificationAgent;
    private final LoanCompletenessAgent loanCompletenessAgent;
    private final IncomeAnalysisAgent incomeAnalysisAgent;
    private final RiskReviewAgent riskReviewAgent;
    private final UnderwritingSummaryAgent underwritingSummaryAgent;

    public LoanProcessingOrchestrator(
            LoanApplicationRepository loanApplicationRepository,
            S3DocumentIngestionService s3DocumentIngestionService,
            DocumentChunkingService documentChunkingService,
            DocumentClassificationAgent documentClassificationAgent,
            LoanCompletenessAgent loanCompletenessAgent,
            IncomeAnalysisAgent incomeAnalysisAgent,
            RiskReviewAgent riskReviewAgent,
            UnderwritingSummaryAgent underwritingSummaryAgent
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.s3DocumentIngestionService = s3DocumentIngestionService;
        this.documentChunkingService = documentChunkingService;
        this.documentClassificationAgent = documentClassificationAgent;
        this.loanCompletenessAgent = loanCompletenessAgent;
        this.incomeAnalysisAgent = incomeAnalysisAgent;
        this.riskReviewAgent = riskReviewAgent;
        this.underwritingSummaryAgent = underwritingSummaryAgent;
    }

    public LoanProcessingResponse process(Long loanApplicationId) {
        LoanApplication loanApplication = loanApplicationRepository.findById(loanApplicationId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanApplicationId));

        try {
            loanApplication.setStatus(PROCESSING);
            loanApplicationRepository.save(loanApplication);

            List<LoanDocumentResponse> documents = s3DocumentIngestionService.ingestLoanDocuments(loanApplicationId);
            documentChunkingService.chunkLoanDocuments(loanApplicationId);

            List<LoanInsight> insights = new ArrayList<>();
            insights.add(documentClassificationAgent.run(loanApplicationId));
            insights.add(loanCompletenessAgent.run(loanApplicationId));
            insights.add(incomeAnalysisAgent.run(loanApplicationId));
            insights.add(riskReviewAgent.run(loanApplicationId));
            LoanInsight summary = underwritingSummaryAgent.run(loanApplicationId);
            insights.add(summary);

            loanApplication.setStatus(PROCESSED);
            loanApplicationRepository.save(loanApplication);
            return new LoanProcessingResponse(
                    PROCESSED,
                    documents.size(),
                    insights.size(),
                    summary.getDescription()
            );
        } catch (RuntimeException e) {
            loanApplication.setStatus(FAILED);
            loanApplicationRepository.save(loanApplication);
            throw e;
        }
    }
}
