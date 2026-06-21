package com.loanflow.ai.service;

import com.loanflow.ai.model.DocumentChunk;
import com.loanflow.ai.model.LoanDocument;
import com.loanflow.ai.repository.DocumentChunkRepository;
import com.loanflow.ai.repository.LoanDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentChunkingService {

    private static final int TARGET_TOKENS = 800;
    private static final int OVERLAP_TOKENS = 100;

    private final LoanDocumentRepository loanDocumentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ChromaDbService chromaDbService;

    public DocumentChunkingService(
            LoanDocumentRepository loanDocumentRepository,
            DocumentChunkRepository documentChunkRepository,
            ChromaDbService chromaDbService
    ) {
        this.loanDocumentRepository = loanDocumentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.chromaDbService = chromaDbService;
    }

    @Transactional
    public List<DocumentChunk> chunkLoanDocuments(Long loanApplicationId) {
        String collectionName = collectionName(loanApplicationId);
        chromaDbService.createCollection(collectionName);

        return loanDocumentRepository.findByLoanApplicationId(loanApplicationId).stream()
                .filter(document -> document.getExtractedText() != null && !document.getExtractedText().isBlank())
                .flatMap(document -> chunkDocument(document, collectionName).stream())
                .toList();
    }

    @Transactional
    public List<DocumentChunk> chunkDocument(LoanDocument document, String collectionName) {
        List<String> chunks = splitIntoTokenChunks(document.getExtractedText());
        List<DocumentChunk> savedChunks = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            String embeddingId = "loan_%d_doc_%d_chunk_%d".formatted(
                    document.getLoanApplicationId(),
                    document.getId(),
                    index
            );

            DocumentChunk chunk = new DocumentChunk();
            chunk.setLoanDocumentId(document.getId());
            chunk.setChunkText(chunks.get(index));
            chunk.setChunkIndex(index);
            chunk.setChromaCollection(collectionName);
            chunk.setEmbeddingId(embeddingId);

            DocumentChunk saved = documentChunkRepository.save(chunk);
            chromaDbService.addDocument(collectionName, embeddingId, saved.getChunkText(), Map.of(
                    "loanApplicationId", document.getLoanApplicationId(),
                    "loanDocumentId", document.getId(),
                    "documentName", document.getDocumentName(),
                    "documentType", document.getDocumentType(),
                    "chunkIndex", index,
                    "embeddingId", embeddingId
            ));
            savedChunks.add(saved);
        }

        return savedChunks;
    }

    public String collectionName(Long loanApplicationId) {
        return "loan_" + loanApplicationId;
    }

    private List<String> splitIntoTokenChunks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String[] tokens = text.trim().split("\\s+");
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < tokens.length) {
            int end = Math.min(start + TARGET_TOKENS, tokens.length);
            chunks.add(String.join(" ", java.util.Arrays.copyOfRange(tokens, start, end)));
            if (end == tokens.length) {
                break;
            }
            start = Math.max(end - OVERLAP_TOKENS, start + 1);
        }

        return chunks;
    }
}
