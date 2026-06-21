package com.loanflow.ai.service;

import com.loanflow.ai.config.S3Properties;
import com.loanflow.ai.dto.LoanDocumentResponse;
import com.loanflow.ai.model.LoanApplication;
import com.loanflow.ai.model.LoanDocument;
import com.loanflow.ai.repository.LoanApplicationRepository;
import com.loanflow.ai.repository.LoanDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class S3DocumentIngestionService {

    private static final String PDF_TYPE = "PDF";
    private static final String TEXT_TYPE = "TEXT";

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final DocumentTextExtractor documentTextExtractor;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDocumentRepository loanDocumentRepository;

    public S3DocumentIngestionService(
            S3Client s3Client,
            S3Properties s3Properties,
            DocumentTextExtractor documentTextExtractor,
            LoanApplicationRepository loanApplicationRepository,
            LoanDocumentRepository loanDocumentRepository
    ) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.documentTextExtractor = documentTextExtractor;
        this.loanApplicationRepository = loanApplicationRepository;
        this.loanDocumentRepository = loanDocumentRepository;
    }

    @Transactional
    public List<LoanDocumentResponse> ingestLoanDocuments(Long loanId) {
        LoanApplication loanApplication = loanApplicationRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found: " + loanId));

        String prefix = "loans/%s/documents/".formatted(loanApplication.getLoanNumber());
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(s3Properties.bucketName())
                .prefix(prefix)
                .build();

        return StreamSupport.stream(s3Client.listObjectsV2Paginator(listRequest).contents().spliterator(), false)
                .filter(s3Object -> isSupportedDocument(s3Object.key()))
                .map(s3Object -> ingestObject(loanApplication, s3Object))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanDocumentResponse> getLoanDocuments(Long loanId) {
        if (!loanApplicationRepository.existsById(loanId)) {
            throw new IllegalArgumentException("Loan application not found: " + loanId);
        }

        return loanDocumentRepository.findByLoanApplicationId(loanId).stream()
                .map(this::toResponse)
                .toList();
    }

    private LoanDocument ingestObject(LoanApplication loanApplication, S3Object s3Object) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucketName())
                .key(s3Object.key())
                .build();

        try (ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(getRequest)) {
            String extractedText = documentTextExtractor.extract(objectStream);

            LoanDocument document = new LoanDocument();
            document.setLoanApplicationId(loanApplication.getId());
            document.setDocumentName(documentNameFromKey(s3Object.key()));
            document.setDocumentType(documentTypeFromKey(s3Object.key()));
            document.setS3Bucket(s3Properties.bucketName());
            document.setS3Key(s3Object.key());
            document.setExtractedText(extractedText);
            document.setProcessed(true);

            return loanDocumentRepository.save(document);
        } catch (Exception e) {
            throw new DocumentProcessingException("Unable to ingest S3 document: " + s3Object.key(), e);
        }
    }

    private boolean isSupportedDocument(String s3Key) {
        String normalized = s3Key.toLowerCase();
        return normalized.endsWith(".pdf")
                || normalized.endsWith(".txt")
                || normalized.endsWith(".text");
    }

    private String documentNameFromKey(String s3Key) {
        int lastSlash = s3Key.lastIndexOf('/');
        return lastSlash >= 0 ? s3Key.substring(lastSlash + 1) : s3Key;
    }

    private String documentTypeFromKey(String s3Key) {
        String normalized = s3Key.toLowerCase();
        if (normalized.endsWith(".pdf")) {
            return PDF_TYPE;
        }
        return TEXT_TYPE;
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
