package com.loanflow.ai.repository;

import com.loanflow.ai.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByLoanDocumentIdOrderByChunkIndexAsc(Long loanDocumentId);
}
