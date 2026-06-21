package com.loanflow.ai.service;

import com.loanflow.ai.dto.IngestDocumentRequest;
import com.loanflow.ai.dto.LoanDocumentResponse;
import com.loanflow.ai.model.LoanDocument;
import com.loanflow.ai.repository.LoanDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;

@Service
public class LoanDocumentService {

    private final LoanDocumentRepository loanDocumentRepository;
    private final S3Client s3Client;
    private final DocumentTextExtractor documentTextExtractor;
    private final VectorIndexService vectorIndexService;

    public LoanDocumentService(
            LoanDocumentRepository loanDocumentRepository,
            S3Client s3Client,
            DocumentTextExtractor documentTextExtractor,
            VectorIndexService vectorIndexService
    ) {
        this.loanDocumentRepository = loanDocumentRepository;
        this.s3Client = s3Client;
        this.documentTextExtractor = documentTextExtractor;
        this.vectorIndexService = vectorIndexService;
    }

    @Transactional
    public LoanDocumentResponse ingest(IngestDocumentRequest request) {
        LoanDocument document = new LoanDocument();
        document.setLoanApplicationId(request.loanApplicationId());
        document.setDocumentName(request.documentName());
        document.setDocumentType(request.documentType());
        document.setS3Bucket(request.s3Bucket());
        document.setS3Key(request.s3Key());
        document.setProcessed(request.processed());

        LoanDocument saved = loanDocumentRepository.save(document);

        try (ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                .bucket(request.s3Bucket())
                .key(request.s3Key())
                .build())) {
            String text = documentTextExtractor.extract(object);
            saved.setExtractedText(text);
            vectorIndexService.index(saved, text);
            saved.setProcessed(true);
        } catch (Exception e) {
            saved.setProcessed(false);
            throw new DocumentProcessingException("Unable to ingest S3 document: " + request.s3Key(), e);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponse> listDocuments() {
        return loanDocumentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanDocumentResponse getDocument(Long id) {
        return loanDocumentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Loan document not found: " + id));
    }

    private LoanDocumentResponse toResponse(LoanDocument document) {
        return new LoanDocumentResponse(
                document.getId(),
                document.getLoanApplicationId(),
                document.getDocumentName(),
                document.getDocumentType(),
                document.getS3Bucket(),
                document.getS3Key(),
                textPreview(document.getExtractedText()),
                document.getUploadedAt(),
                document.isProcessed()
        );
    }

    private String textPreview(String text) {
        if (text == null || text.length() <= 300) {
            return text;
        }
        return text.substring(0, 300) + "...";
    }
}
