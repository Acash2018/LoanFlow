package com.loanflow.ai.controller;

import com.loanflow.ai.dto.IngestDocumentRequest;
import com.loanflow.ai.dto.LoanDocumentResponse;
import com.loanflow.ai.service.LoanDocumentService;
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
@RequestMapping("/api/loan-documents")
public class LoanDocumentController {

    private final LoanDocumentService loanDocumentService;

    public LoanDocumentController(LoanDocumentService loanDocumentService) {
        this.loanDocumentService = loanDocumentService;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public LoanDocumentResponse ingest(@Valid @RequestBody IngestDocumentRequest request) {
        return loanDocumentService.ingest(request);
    }

    @GetMapping
    public List<LoanDocumentResponse> list() {
        return loanDocumentService.listDocuments();
    }

    @GetMapping("/{id}")
    public LoanDocumentResponse get(@PathVariable Long id) {
        return loanDocumentService.getDocument(id);
    }
}
