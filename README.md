# LoanFlow AI

LoanFlow AI is a Java 21 Spring Boot application for processing single-family mortgage loan documents stored in AWS S3 and supporting an agentic retrieval-augmented generation workflow.

The application is designed to:

- Register mortgage loan documents by loan application and S3 object key.
- Download documents from an AWS S3 bucket.
- Extract text from PDFs and other loan file artifacts with Apache Tika.
- Chunk and index extracted content into ChromaDB for vector search.
- Use Ollama locally for embeddings and LLM-based answers.
- Track loan applications, documents, chunks, insights, and agent tasks in PostgreSQL with Spring Data JPA.

## Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- AWS SDK for S3
- Apache Tika
- ChromaDB
- Ollama

## Project Structure

```text
src/main/java/com/loanflow/ai
|-- agent
|-- config
|-- controller
|-- dto
|-- model
|-- repository
`-- service
```

## File Guide

### Root Files

- `pom.xml` defines the Maven project, Java 21 version, Spring Boot parent, dependencies, and build plugins.
- `README.md` documents the purpose, structure, local setup, and current implementation status of LoanFlow AI.

### Application Entry Point

- `LoanFlowAiApplication.java` starts the Spring Boot application and enables configuration property scanning.

### `agent`

- `LoanReviewAgent.java` coordinates the agentic RAG flow. It retrieves relevant document context from vector search, builds a loan-review prompt, calls Ollama, and returns an answer with citations/context.
- `DocumentClassificationAgent.java` classifies loan documents as paystub, W2, tax return, bank statement, credit report, appraisal, title, purchase agreement, or other.
- `LoanCompletenessAgent.java` checks whether required underwriting documents are present and flags missing items.
- `IncomeAnalysisAgent.java` reviews income-related documents, summarizes borrower income, and flags inconsistencies.
- `RiskReviewAgent.java` looks for risks such as missing pages, mismatched names or addresses, unclear income, and unusual deposits.
- `UnderwritingSummaryAgent.java` produces the final human-readable loan review summary.

### `config`

- `AwsS3Config.java` creates the AWS SDK `S3Client` bean used by services that download loan documents from S3.
- `ChromaProperties.java` maps `loanflow.chroma.*` settings from `application.yml`, including the ChromaDB base URL and collection name.
- `OllamaProperties.java` maps `loanflow.ollama.*` settings from `application.yml`, including the Ollama base URL, embedding model, and chat model.
- `S3Properties.java` maps `loanflow.s3.*` settings from `application.yml`, including the default S3 bucket configuration.

### `controller`

- `LoanDocumentController.java` exposes REST endpoints for ingesting, listing, and retrieving mortgage loan documents.
- `LoanController.java` exposes loan-scoped endpoints for ingesting all supported documents from a loan's S3 folder and listing documents for a loan.
- `RagController.java` exposes the agent question endpoint used to ask loan-file questions through the RAG workflow.

### `dto`

- `AgentAnswerResponse.java` is the response shape returned by the agent, including the answer, citations/context, and timestamp.
- `AgentQueryRequest.java` is the request shape for asking the agent a question about a loan.
- `AskLoanRequest.java` is the frontend request shape for asking a loan-specific RAG question.
- `ChromaQueryResult.java` represents one vector search result returned from ChromaDB.
- `CreateLoanApplicationRequest.java` is the request shape for creating a loan application from the dashboard.
- `IngestDocumentRequest.java` is the request shape for registering and processing a document from S3.
- `LoanApplicationResponse.java` is the dashboard response shape for loan application records.
- `LoanAskResponse.java` is the response shape for loan-specific Q&A, including answer and sources.
- `LoanDocumentResponse.java` is the API response shape for loan document metadata and processing state.
- `LoanInsightResponse.java` is the dashboard response shape for AI-generated loan insights.
- `LoanProcessingResponse.java` summarizes the result of the full processing workflow.
- `LoanSourceResponse.java` represents a cited source document and chunk used in an answer.
- `LoanSummaryResponse.java` returns the latest underwriting summary for a loan.

### `model`

- `LoanApplication.java` represents the mortgage loan application, including borrower, property, loan type, status, and audit timestamps.
- `LoanDocument.java` represents a document attached to a loan application, including S3 location, extracted text, upload timestamp, and processed flag.
- `DocumentChunk.java` represents a chunk of extracted document text that can be indexed in ChromaDB with an embedding ID.
- `LoanInsight.java` represents an AI-generated insight for a loan, such as a risk, missing item, compliance note, or summary.
- `AgentTask.java` represents work performed by an agent for a loan application, including task status, result, and completion timestamp.

### `repository`

- `LoanApplicationRepository.java` provides database access for `LoanApplication` records, including lookup by loan number.
- `LoanDocumentRepository.java` provides database access for `LoanDocument` records, including lookup by loan application ID.
- `DocumentChunkRepository.java` provides database access for `DocumentChunk` records, ordered by chunk index for a document.
- `LoanInsightRepository.java` provides database access for `LoanInsight` records by loan application ID.
- `AgentTaskRepository.java` provides database access for `AgentTask` records by loan application ID.

### `service`

- `LoanDocumentService.java` handles document ingestion: records metadata, downloads from S3, extracts text, indexes content, and updates processing state.
- `S3DocumentIngestionService.java` handles loan-folder ingestion from S3. It lists objects under `loans/{loanNumber}/documents/`, downloads supported PDF and text files, extracts text, saves document records, and marks them processed.
- `LoanApplicationService.java` creates, lists, and retrieves loan application records.
- `DocumentChunkingService.java` splits extracted document text into overlapping chunks, saves `DocumentChunk` records, and stores vectors in loan-specific ChromaDB collections.
- `ChromaDbService.java` creates ChromaDB collections, adds embedded documents, and queries relevant chunks.
- `LoanRagService.java` performs loan-specific retrieval-augmented Q&A and returns answers with cited sources.
- `LoanInsightService.java` saves and reads AI-generated insights and underwriting summaries.
- `LoanProcessingOrchestrator.java` runs the full processing workflow from S3 ingestion through all loan review agents.
- `DocumentTextExtractor.java` uses Apache Tika to extract text from uploaded mortgage documents such as PDFs.
- `DocumentProcessingException.java` is the custom runtime exception used when document extraction or ingestion fails.
- `VectorIndexService.java` owns chunking, embedding, and ChromaDB vector-search integration points.
- `OllamaClient.java` wraps local Ollama calls for embeddings and LLM completions.

### Resources And Tests

- `src/main/resources/application.yml` contains local configuration for PostgreSQL, ChromaDB, Ollama, and S3.
- `LoanFlowAiApplicationTests.java` is the basic Spring Boot context-load test.

## API Sketch

- `POST /api/loans` creates a loan application.
- `GET /api/loans` lists loan applications.
- `GET /api/loans/{loanId}` returns one loan application.
- `POST /api/loan-documents/ingest` registers and ingests a document from S3.
- `GET /api/loan-documents` lists known loan documents.
- `GET /api/loan-documents/{id}` returns one loan document.
- `POST /api/loans/{loanId}/ingest` ingests all PDF and text documents from `loans/{loanNumber}/documents/` in S3.
- `POST /api/loans/{loanId}/process` runs ingestion, chunking, embeddings, vector storage, classification, completeness review, income analysis, risk review, and underwriting summary generation.
- `POST /api/loans/{loanId}/ask` asks a loan-specific RAG question.
- `GET /api/loans/{loanId}/documents` lists documents saved for a loan application.
- `GET /api/loans/{loanId}/insights` lists AI-generated insights for a loan.
- `GET /api/loans/{loanId}/summary` returns the latest underwriting summary.
- `POST /api/agent/ask` asks LoanFlow AI a loan-file question using retrieved context.

Example ask request:

```json
{
  "question": "Does this loan file contain income verification?"
}
```

Example ask response:

```json
{
  "answer": "Yes. The retrieved context includes income verification evidence...",
  "sources": [
    {
      "documentId": 12,
      "documentName": "paystub.pdf",
      "chunkIndex": 0,
      "embeddingId": "loan_1_doc_12_chunk_0",
      "excerpt": "..."
    }
  ]
}
```

## Local Services

Expected defaults are configured in `src/main/resources/application.yml`:

- PostgreSQL: `jdbc:postgresql://localhost:5432/loanflow_ai`
- ChromaDB: `http://localhost:8000`
- Ollama: `http://localhost:11434`
- S3 bucket: `loanflow-documents`

## Run

```bash
mvn spring-boot:run
```

Before running, create the PostgreSQL database and ensure AWS credentials, ChromaDB, and Ollama are available locally.

## Current Status

This is the initial application scaffold. Controllers, persistence, document extraction, S3 download, and the agent entry point are in place. ChromaDB write/query calls and Ollama embedding response handling are intentionally stubbed for the next implementation step.
