package com.loanflow.ai.service;

import com.loanflow.ai.dto.LoanInsightResponse;
import com.loanflow.ai.dto.LoanSummaryResponse;
import com.loanflow.ai.model.LoanInsight;
import com.loanflow.ai.repository.LoanInsightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class LoanInsightService {

    private final LoanInsightRepository loanInsightRepository;

    public LoanInsightService(LoanInsightRepository loanInsightRepository) {
        this.loanInsightRepository = loanInsightRepository;
    }

    @Transactional
    public LoanInsight save(Long loanApplicationId, String insightType, String title, String description, String severity) {
        LoanInsight insight = new LoanInsight();
        insight.setLoanApplicationId(loanApplicationId);
        insight.setInsightType(insightType);
        insight.setTitle(title);
        insight.setDescription(description);
        insight.setSeverity(severity);
        return loanInsightRepository.save(insight);
    }

    @Transactional(readOnly = true)
    public List<LoanInsightResponse> list(Long loanApplicationId) {
        return loanInsightRepository.findByLoanApplicationId(loanApplicationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanSummaryResponse summary(Long loanApplicationId) {
        String summary = loanInsightRepository.findByLoanApplicationId(loanApplicationId).stream()
                .filter(insight -> "UNDERWRITING_SUMMARY".equals(insight.getInsightType()))
                .max(Comparator.comparing(LoanInsight::getGeneratedAt))
                .map(LoanInsight::getDescription)
                .orElse("");

        return new LoanSummaryResponse(loanApplicationId, summary);
    }

    public LoanInsightResponse toResponse(LoanInsight insight) {
        return new LoanInsightResponse(
                insight.getId(),
                insight.getLoanApplicationId(),
                insight.getInsightType(),
                insight.getTitle(),
                insight.getDescription(),
                insight.getSeverity(),
                insight.getGeneratedAt()
        );
    }
}
