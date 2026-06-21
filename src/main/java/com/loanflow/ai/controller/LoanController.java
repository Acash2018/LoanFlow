package com.loanflow.ai.controller;

import com.loanflow.ai.dto.AskLoanRequest;
import com.loanflow.ai.dto.CreateLoanApplicationRequest;
import com.loanflow.ai.dto.LoanApplicationResponse;
import com.loanflow.ai.dto.LoanAskResponse;
import com.loanflow.ai.dto.LoanDocumentResponse;
import com.loanflow.ai.dto.LoanInsightResponse;
import com.loanflow.ai.dto.LoanProcessingResponse;
import com.loanflow.ai.dto.LoanSummaryResponse;
import com.loanflow.ai.service.LoanApplicationService;
import com.loanflow.ai.service.LoanInsightService;
import com.loanflow.ai.service.LoanProcessingOrchestrator;
import com.loanflow.ai.service.LoanRagService;
import com.loanflow.ai.service.S3DocumentIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanApplicationService loanApplicationService;
    private final S3DocumentIngestionService ingestionService;
    private final LoanProcessingOrchestrator loanProcessingOrchestrator;
    private final LoanRagService loanRagService;
    private final LoanInsightService loanInsightService;

    public LoanController(
            LoanApplicationService loanApplicationService,
            S3DocumentIngestionService ingestionService,
            LoanProcessingOrchestrator loanProcessingOrchestrator,
            LoanRagService loanRagService,
            LoanInsightService loanInsightService
    ) {
        this.loanApplicationService = loanApplicationService;
        this.ingestionService = ingestionService;
        this.loanProcessingOrchestrator = loanProcessingOrchestrator;
        this.loanRagService = loanRagService;
        this.loanInsightService = loanInsightService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplicationResponse createLoan(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return loanApplicationService.create(request);
    }

    @GetMapping
    public List<LoanApplicationResponse> listLoans() {
        return loanApplicationService.list();
    }

    @GetMapping("/{loanId}")
    public LoanApplicationResponse getLoan(@PathVariable Long loanId) {
        return loanApplicationService.get(loanId);
    }

    @PostMapping("/{loanId}/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public List<LoanDocumentResponse> ingestDocuments(@PathVariable Long loanId) {
        return ingestionService.ingestLoanDocuments(loanId);
    }

    @GetMapping("/{loanId}/documents")
    public List<LoanDocumentResponse> getDocuments(@PathVariable Long loanId) {
        return ingestionService.getLoanDocuments(loanId);
    }

    @PostMapping("/{loanId}/process")
    public LoanProcessingResponse processLoan(@PathVariable Long loanId) {
        return loanProcessingOrchestrator.process(loanId);
    }

    @PostMapping("/{loanId}/ask")
    public LoanAskResponse askLoan(@PathVariable Long loanId, @Valid @RequestBody AskLoanRequest request) {
        return loanRagService.ask(loanId, request.question());
    }

    @GetMapping("/{loanId}/insights")
    public List<LoanInsightResponse> getInsights(@PathVariable Long loanId) {
        return loanInsightService.list(loanId);
    }

    @GetMapping("/{loanId}/summary")
    public LoanSummaryResponse getSummary(@PathVariable Long loanId) {
        return loanInsightService.summary(loanId);
    }
}
