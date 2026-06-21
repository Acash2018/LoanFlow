package com.loanflow.ai.service;

import com.loanflow.ai.dto.CreateLoanApplicationRequest;
import com.loanflow.ai.dto.LoanApplicationResponse;
import com.loanflow.ai.model.LoanApplication;
import com.loanflow.ai.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanApplicationService {

    private static final String DEFAULT_STATUS = "NEW";

    private final LoanApplicationRepository loanApplicationRepository;

    public LoanApplicationService(LoanApplicationRepository loanApplicationRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
    }

    @Transactional
    public LoanApplicationResponse create(CreateLoanApplicationRequest request) {
        LoanApplication loanApplication = new LoanApplication();
        loanApplication.setLoanNumber(request.loanNumber());
        loanApplication.setBorrowerName(request.borrowerName());
        loanApplication.setPropertyAddress(request.propertyAddress());
        loanApplication.setLoanType(request.loanType());
        loanApplication.setStatus(DEFAULT_STATUS);

        return toResponse(loanApplicationRepository.save(loanApplication));
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> list() {
        return loanApplicationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse get(Long loanId) {
        return loanApplicationRepository.findById(loanId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));
    }

    private LoanApplicationResponse toResponse(LoanApplication loanApplication) {
        return new LoanApplicationResponse(
                loanApplication.getId(),
                loanApplication.getLoanNumber(),
                loanApplication.getBorrowerName(),
                loanApplication.getPropertyAddress(),
                loanApplication.getLoanType(),
                loanApplication.getStatus(),
                loanApplication.getCreatedAt(),
                loanApplication.getUpdatedAt()
        );
    }
}
