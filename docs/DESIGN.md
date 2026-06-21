# LoanFlow AI Design Document

## 1. Overview

LoanFlow AI is a Spring Boot backend for processing single-family mortgage loan files. It ingests documents from AWS S3, extracts text, chunks document content, stores vector embeddings in ChromaDB, and uses Ollama for local embedding and LLM calls.

The system is designed around an agentic RAG workflow. Documents are retrieved by loan, converted into searchable context, and then reviewed by specialized agents that produce underwriting insights for a frontend dashboard.

## 2. Goals

- Provide a Java 21 Spring Boot backend for mortgage document processing.
- Store loan applications, documents, chunks, agent tasks, and insights in PostgreSQL.
- Ingest PDF and text documents from S3 using loan-specific folder prefixes.
- Extract text from loan documents with Apache Tika.
- Chunk extracted text into overlapping chunks for retrieval.
- Generate embeddings locally with Ollama.
- Store searchable document vectors in ChromaDB.
- Support loan-specific RAG question answering.
- Run a full automated loan review workflow through specialized agents.
- Expose clean REST APIs for a frontend dashboard.

## 3. Non-Goals For The Current Version

- Production authentication and authorization.
- Full ChromaDB API hardening across all server versions.
- Human review workflow states beyond basic status fields.
- OCR for scanned image-only PDFs.
- Formal underwriting rule engine.
- Full observability, tracing, retries, and dead-letter handling.
- Database migrations with Flyway or Liquibase.

## 4. Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- AWS SDK for S3
- Apache Tika
- ChromaDB
- Ollama
- Maven

## 5. High-Level Architecture

```text
Frontend Dashboard
        |
        v
Spring Boot REST API
        |
        |-- LoanController
        |-- LoanDocumentController
        |-- RagController
        |
        v
Application Services
        |
        |-- S3DocumentIngestionService
        |-- DocumentTextExtractor
        |-- DocumentChunkingService
        |-- ChromaDbService
        |-- OllamaClient
        |-- LoanRagService
        |-- LoanProcessingOrchestrator
        |-- Agent services
        |
        +------------------+
        |                  |
        v                  v
   PostgreSQL         External Systems
                      |-- AWS S3
                      |-- ChromaDB
                      |-- Ollama
```

## 6. Package Responsibilities

### `controller`

The controller layer exposes REST APIs for frontend and integration clients.

- `LoanController` is the main dashboard API surface for loans, ingestion, processing, Q&A, insights, and summaries.
- `LoanDocumentController` exposes lower-level document ingestion/list/detail endpoints.
- `RagController` exposes a general agent ask endpoint from the initial scaffold.

### `service`

The service layer contains application workflow and external integration logic.

- `LoanApplicationService` creates, lists, and retrieves loan applications.
- `S3DocumentIngestionService` lists S3 objects under a loan folder, downloads supported files, extracts text, and saves processed documents.
- `DocumentTextExtractor` wraps Apache Tika text extraction.
- `DocumentChunkingService` splits document text into overlapping chunks, saves chunk records, sends chunks to ChromaDB, and stores embedding IDs.
- `ChromaDbService` creates collections, adds documents, and queries relevant chunks.
- `OllamaClient` calls local Ollama endpoints for embeddings and chat completions.
- `LoanRagService` performs loan-specific retrieval-augmented question answering.
- `LoanInsightService` saves and retrieves agent-generated insights.
- `LoanProcessingOrchestrator` runs the full workflow end to end.

### `agent`

The agent package contains domain-specific review services.

- `DocumentClassificationAgent` classifies uploaded loan documents.
- `LoanCompletenessAgent` checks whether required documents are present.
- `IncomeAnalysisAgent` reviews income documents and flags inconsistencies.
- `RiskReviewAgent` identifies loan file risks.
- `UnderwritingSummaryAgent` produces the final summary.

### `model`

The model package contains JPA entities persisted to PostgreSQL.

### `repository`

The repository package contains Spring Data JPA repositories for database access.

### `dto`

The DTO package contains frontend-friendly request and response types.

### `config`

The config package contains Spring configuration and property binding classes.

## 7. Data Model

### LoanApplication

Represents a mortgage loan file.

Important fields:

- `id`
- `loanNumber`
- `borrowerName`
- `propertyAddress`
- `loanType`
- `status`
- `createdAt`
- `updatedAt`

### LoanDocument

Represents a document attached to a loan application.

Important fields:

- `id`
- `loanApplicationId`
- `documentName`
- `documentType`
- `s3Bucket`
- `s3Key`
- `extractedText`
- `uploadedAt`
- `processed`

### DocumentChunk

Represents a chunk of extracted document text stored in ChromaDB.

Important fields:

- `id`
- `loanDocumentId`
- `chunkText`
- `chunkIndex`
- `chromaCollection`
- `embeddingId`

### LoanInsight

Represents an AI-generated review output.

Important fields:

- `id`
- `loanApplicationId`
- `insightType`
- `title`
- `description`
- `severity`
- `generatedAt`

### AgentTask

Represents agent work tracking. It is available in the model for future workflow tracking.

Important fields:

- `id`
- `loanApplicationId`
- `agentName`
- `taskType`
- `status`
- `result`
- `createdAt`
- `completedAt`

## 8. S3 Document Layout

Each loan is expected to have documents under this prefix:

```text
loans/{loanNumber}/documents/
```

Example:

```text
loans/LN-1001/documents/paystub.pdf
loans/LN-1001/documents/w2.pdf
loans/LN-1001/documents/bank_statement.txt
```

Supported file types in the current service:

- `.pdf`
- `.txt`
- `.text`

## 9. Main Processing Workflow

The full workflow is triggered by:

```http
POST /api/loans/{loanId}/process
```

Processing steps:

1. Load the `LoanApplication` by `loanId`.
2. Mark the loan status as `PROCESSING`.
3. Build the S3 prefix: `loans/{loanNumber}/documents/`.
4. List PDF and text documents in S3.
5. Download each supported document.
6. Extract text with Apache Tika.
7. Save `LoanDocument` metadata and extracted text.
8. Mark documents as processed.
9. Create a loan-specific ChromaDB collection named `loan_{loanApplicationId}`.
10. Split extracted text into chunks of about 800 tokens with 100-token overlap.
11. Save each chunk as a `DocumentChunk`.
12. Generate embeddings through Ollama.
13. Store chunk vectors and metadata in ChromaDB.
14. Save the ChromaDB embedding ID on each `DocumentChunk`.
15. Run document classification.
16. Run completeness review.
17. Run income analysis.
18. Run risk review.
19. Generate underwriting summary.
20. Save all agent outputs as `LoanInsight` records.
21. Mark the loan status as `PROCESSED`.
22. Return processing status, document count, insight count, and summary.

If a runtime failure occurs, the orchestrator marks the loan as `FAILED`.

## 10. RAG Workflow

The loan-specific ask endpoint is:

```http
POST /api/loans/{loanId}/ask
```

Request:

```json
{
  "question": "Does this loan file contain income verification?"
}
```

Workflow:

1. Convert `loanId` into collection name `loan_{loanId}`.
2. Generate an embedding for the user question through Ollama.
3. Query ChromaDB for the top matching chunks.
4. Build a prompt using retrieved document text.
5. Ask Ollama to answer only from retrieved context.
6. Return an answer plus cited sources.

Response shape:

```json
{
  "answer": "...",
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

## 11. Agent Workflow

Each agent accepts a `loanApplicationId`, uses `LoanRagService`, and saves its result as a `LoanInsight`.

### DocumentClassificationAgent

Classifies documents as:

- paystub
- W2
- tax return
- bank statement
- credit report
- appraisal
- title
- purchase agreement
- other

### LoanCompletenessAgent

Checks whether required underwriting documents are present and flags missing items.

### IncomeAnalysisAgent

Reviews income-related documents, summarizes borrower income, and flags inconsistencies.

### RiskReviewAgent

Looks for:

- missing pages
- inconsistent borrower names
- mismatched addresses
- unclear income
- unusual deposits
- incomplete signatures
- conflicting document facts

### UnderwritingSummaryAgent

Produces a human-readable final loan review summary.

## 12. REST API Design

### Loan Dashboard APIs

```http
POST /api/loans
GET /api/loans
GET /api/loans/{loanId}
POST /api/loans/{loanId}/ingest
POST /api/loans/{loanId}/process
POST /api/loans/{loanId}/ask
GET /api/loans/{loanId}/documents
GET /api/loans/{loanId}/insights
GET /api/loans/{loanId}/summary
```

### Document APIs

```http
POST /api/loan-documents/ingest
GET /api/loan-documents
GET /api/loan-documents/{id}
```

### Agent API

```http
POST /api/agent/ask
```

## 13. Frontend-Friendly Response Principles

The API is shaped for dashboards:

- Loan responses contain identifiers, borrower/property details, status, and timestamps.
- Document responses return metadata and a short `textPreview` instead of full extracted text.
- Ask responses return `answer` and `sources`.
- Insight responses contain severity, type, title, description, and generated timestamp.
- Process responses summarize the workflow outcome instead of exposing internal logs.

## 14. Configuration

Local defaults live in:

```text
src/main/resources/application.yml
```

Important configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/loanflow_ai

loanflow:
  s3:
    bucket-name: loanflow-documents
  chroma:
    base-url: http://localhost:8000
    collection-name: loan_documents
  ollama:
    base-url: http://localhost:11434
    embedding-model: nomic-embed-text
    chat-model: llama3.1
```

## 15. External Dependencies

### AWS S3

Stores source mortgage documents. The Spring service uses the default AWS SDK credential provider chain.

### PostgreSQL

Stores structured application state:

- loan records
- document metadata
- extracted text
- document chunks
- insight outputs
- agent task records

### ChromaDB

Stores vectorized document chunks by loan-specific collection.

Collection naming:

```text
loan_{loanApplicationId}
```

### Ollama

Runs locally for:

- embedding generation
- LLM prompt completion

## 16. Error Handling

Current behavior:

- Missing loan IDs throw `IllegalArgumentException`.
- Document ingestion and extraction failures throw `DocumentProcessingException`.
- The orchestrator marks loans as `FAILED` when runtime exceptions occur.

Recommended next step:

- Add a global `@ControllerAdvice` to map exceptions into consistent JSON error responses.

## 17. Current Implementation Notes

- Maven is expected but was not available in the local execution environment during scaffolding.
- ChromaDB endpoint paths may need adjustment depending on the deployed ChromaDB server version.
- The chunking implementation approximates tokens by whitespace splitting.
- The current vector flow stores embedding IDs on `DocumentChunk` before adding records to ChromaDB.
- `AgentTask` is modeled but not yet fully used by the orchestrator.

## 18. Future Enhancements

- Add Flyway or Liquibase migrations.
- Add global exception handling.
- Add validation for duplicate S3 document ingestion.
- Add OCR support for scanned PDFs.
- Add authentication and role-based authorization.
- Add async processing for long-running loan workflows.
- Add retry policies for S3, ChromaDB, and Ollama calls.
- Add OpenAPI/Swagger documentation.
- Add integration tests with Testcontainers for PostgreSQL.
- Add contract tests for the dashboard APIs.
- Add structured logging and observability.
- Persist agent task execution history in `AgentTask`.
- Improve ChromaDB client compatibility across Chroma API versions.

## 19. Deployment Assumptions

The initial deployment assumes:

- PostgreSQL is reachable from the Spring Boot app.
- AWS credentials are available through environment variables, profile, IAM role, or another AWS SDK-supported provider.
- ChromaDB is running and reachable.
- Ollama is running and has the configured embedding and chat models pulled locally.

## 20. Summary

LoanFlow AI is designed as a modular Spring Boot backend for mortgage document intelligence. It separates REST APIs, persistence, S3 ingestion, text extraction, vector indexing, RAG answering, and agentic review into clear packages. The current design provides a strong starting point for a frontend dashboard and can evolve toward production with stronger error handling, migrations, security, observability, and async workflow execution.
