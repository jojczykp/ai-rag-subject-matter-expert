# Product Requirements Document

## Product Summary

AI RAG Subject Matter Expert answers questions about predefined subjects using
static bundled `.txt` documents as the knowledge base. The application exposes
REST APIs and a lightweight browser UI. Users choose a subject, an embedding
model for retrieval, and a chat model for answer generation.

The product supports multiple model runtime styles: local Ollama models,
embedded offline GGUF chat models through managed `llama-server`, and
configurable online provider adapters.

## Problem

Users need a small backend-oriented RAG application that can answer questions
from curated domain documents while making model choice, local/offline behavior,
and provider availability visible. For this project stage, documents are owned
by the application and bundled as static resources rather than uploaded at
runtime.

## Current Scope

- [x] Support multiple predefined static subjects.
- [x] Load subject source documents bundled with the application.
- [x] Support static bundled `.txt` documents.
- [x] Provide REST endpoints for subjects, embedding models, chat models, and
      chat.
- [x] Provide a React UI for selecting subject, embedding model, and chat model.
- [x] Use bundled subject documents as the basis for RAG context retrieval.
- [x] Allow the user to choose which configured chat model to use.
- [x] Allow the user to choose which configured embedding model to use.
- [x] Keep implementation testable with backend Kover coverage at or above 80%.
- [x] Provide a runtime-only Docker image that serves the UI and API through one
      HTTP port.
- [x] Provide a Compose workflow with PostgreSQL and persistent model and
      database volumes.

## Future Scope

- [ ] Expand container-level acceptance testing across supported architectures
      and first-start model download flows.
- [ ] Let users create, update, and delete subjects at runtime.
- [ ] Let users upload and delete content at runtime.
- [ ] Add first-class structured CSV document support.
- [ ] Support Markdown documents.
- [ ] Support PDF documents.
- [ ] Support Word documents.
- [ ] Support citations and source references in chat answers.
- [ ] Support streaming chat responses.
- [ ] Support persisted chat history when there is a clear retention policy.
- [ ] Support user authentication and authorization.
- [ ] Add per-model embedded llama prompt mode configuration.

## Non-Goals

- [x] Do not provide runtime subject creation in the current scope.
- [x] Do not provide runtime document upload in the current scope.
- [x] Do not expose original bundled files for download in the current scope.
- [x] Do not include document citations or source references in chat responses
      in the current scope.
- [x] Do not support streaming chat responses in the current scope.
- [x] Do not persist chat prompts, responses, or conversation history by default.
- [x] Do not require user authentication in the current scope.
- [x] Do not guarantee perfect factual accuracy from model responses.
- [x] Do not couple the product to a single model provider.

## Users

### Application User

Uses the frontend to choose a subject and model combination, then asks
questions against the selected subject.

Needs:

- [x] See available subjects.
- [x] See available embedding and chat models.
- [x] Understand whether selected models are available.
- [x] Understand whether prompts stay local or may leave the machine.
- [x] Send a question and receive a generated answer.

### API Consumer

Integrates directly with the backend REST API.

Needs:

- [x] Ask questions against a selected predefined subject.
- [x] Receive predictable JSON responses and errors.
- [x] Discover configured subjects and models.

## Core Concepts

### Subject

Subjects are configured by application owners. Each enabled subject has a
display name, display order, default question, and bundled document folder.

Requirements:

- [x] A subject has a configured id.
- [x] A subject has a configured display name.
- [x] A subject has a configured display order for API and UI selectors.
- [x] A subject has a configured default question.
- [x] A subject can be enabled or disabled in configuration.
- [x] The initial API does not create, update, or delete subjects.

### Static Source Documents

Static source documents are files bundled with the application. They are
maintained by the application owner and packaged with the service.

Requirements:

- [x] Plain text `.txt` files are supported.
- [x] Each bundled document belongs to one predefined subject.
- [x] Each bundled document has a stable resource path identity.
- [x] The service fails startup when configured documents cannot be loaded.
- [x] The service fails startup when no supported documents are found.
- [x] The initial API does not upload, update, or delete documents at runtime.
- [x] The initial API does not expose document-management endpoints.

### Chat

Chat lets a user ask a question against a selected predefined subject. The
answer is generated using relevant chunks retrieved from that subject's bundled
documents.

Requirements:

- [x] A chat request includes a selected subject id.
- [x] A chat request includes a selected chat model id.
- [x] A chat request includes a selected embedding model id when more than one
      embedding model is enabled.
- [x] A chat request includes a non-empty user message.
- [x] The service retrieves only relevant indexed document chunks for the
      request.
- [x] The service sends the user message and retrieved chunks to the selected
      model.
- [x] The response includes the generated answer.
- [x] The response identifies the chat model used.

## REST API Requirements

### Chat API

- [x] `GET /subjects` lists indexed predefined subjects.
- [x] `POST /chat` asks a question against the selected subject.
- [x] Request body includes `subjectId`.
- [x] Request body includes `message`.
- [x] Request body includes `modelId`.
- [x] Request body can include `embeddingModelId` for retrieval model selection.
- [x] Response body includes `answer`.
- [x] Response body includes `modelId`.
- [x] Response is returned as a single non-streaming JSON response.
- [x] The initial API does not create or update persisted chat history.

### Model API

- [x] `GET /chat-models` lists configured chat models.
- [x] `GET /embedding-models` lists configured embedding models.
- [x] Model list indicates online, local server, or embedded offline mode.
- [x] Model list indicates availability status.
- [x] Model availability status is one of `CONFIGURED`, `AVAILABLE`,
      `UNAVAILABLE`, or `MISCONFIGURED`.
- [x] Model list indicates whether prompts may leave the local machine.

### Error Responses

Errors use a consistent JSON shape:

```json
{
  "code": "MODEL_NOT_FOUND",
  "message": "Configured model was not found.",
  "details": {}
}
```

## Non-Functional Requirements

### Reliability

- [x] Return consistent error response shapes.
- [x] Handle unavailable or misconfigured models gracefully.
- [x] Keep service health independent from optional model availability.
- [x] Use startup validation for bundled document configuration.

### Security And Privacy

- [x] Do not log raw document content by default.
- [x] Do not log raw chat prompts or model responses by default.
- [x] Do not store raw chat prompts, model responses, or conversation history
      by default.
- [x] Clearly distinguish online models from local and embedded offline models.
- [x] Keep API credentials out of source control.

### Performance

- [x] Non-chat endpoints do not invoke chat generation.
- [x] Chat APIs have configurable timeouts.
- [x] Chat generation is not retried automatically in the current scope.
- [x] Document extraction and indexing avoid repeated work per chat request.
- [x] Chat requests send retrieved chunks rather than all bundled document
      content.

### Observability

- [x] Expose health and info through Spring Boot Actuator.
- [x] Log startup asset downloads, indexing, model runtime lifecycle, and
      provider failures without logging prompt bodies.

### Testability

- [x] Unit test static document discovery and chunking.
- [x] Unit test chat routing behavior.
- [x] Unit test error handling.
- [x] Add integration tests for REST endpoints and static document
      indexing/chat flow.
- [x] Add integration tests covering persistence and retrieval behavior.
- [x] Use fake model clients in tests.
- [x] Maintain at least 80% backend coverage through Kover verification.

## Acceptance Criteria

### Minimum Application

- [x] A user can ask a question against a configured subject.
- [x] The chat answer uses retrieved chunks from the selected subject.
- [x] The UI can select subject, embedding model, and chat model.
- [x] Tests cover static document loading, indexing, retrieval, and chat flow.
- [x] Kover coverage verification passes.

### Model Runtime Readiness

- [x] The API can list configured chat and embedding models.
- [x] The chat endpoint rejects unknown or unavailable model ids.
- [x] The system supports online, local server, and embedded offline model
      runtime patterns.
- [x] Embedded offline model support uses llama.cpp with GGUF model files.

## Related Documents

- [Architecture](ARCHITECTURE.md)
- [Configuration Reference](CONFIGURATION.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
