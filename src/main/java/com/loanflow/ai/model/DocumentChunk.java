package com.loanflow.ai.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanDocumentId;

    @Lob
    @Column(nullable = false)
    private String chunkText;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false)
    private String chromaCollection;

    @Column(nullable = false)
    private String embeddingId;

    public Long getId() {
        return id;
    }

    public Long getLoanDocumentId() {
        return loanDocumentId;
    }

    public void setLoanDocumentId(Long loanDocumentId) {
        this.loanDocumentId = loanDocumentId;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getChromaCollection() {
        return chromaCollection;
    }

    public void setChromaCollection(String chromaCollection) {
        this.chromaCollection = chromaCollection;
    }

    public String getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(String embeddingId) {
        this.embeddingId = embeddingId;
    }
}
