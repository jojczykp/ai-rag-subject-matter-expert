# Product Requirements Document

## Product Summary

AI Subject Matter Expert is a backend service that answers questions about one
predefined subject. The subject's knowledge base is built from static document
resources bundled with the application. The first supported content format is
plain text.

The initial product provides REST APIs for chatting with an AI model using
bundled documents as the reasoning base.

All requirements are TODO until implemented and verified.

## Problem

Users need a backend application that can answer questions using a curated set
of domain-specific documents. At this stage, the content is maintained by the
application owner and bundled with the application, rather than uploaded or
managed dynamically by API users.

## Initial Scope

- [ ] Support exactly one predefined subject.
- [ ] Load subject source documents bundled with the application.
- [ ] Support static bundled `.txt` documents.
- [ ] Provide a REST endpoint for chatting with the predefined subject.
- [ ] Use bundled subject documents as the basis for AI reasoning.
- [ ] Allow the user to choose which configured model to use for a given chat
      request.
- [ ] Keep implementation testable with at least 80% unit test coverage.

## Future Scope

- [ ] Support multiple subjects.
- [ ] Let users create, update, and delete subjects.
- [ ] Let users upload content to subjects at runtime.
- [ ] Support user authentication and authorization.
- [ ] Support collaborative multi-user permissions.
- [ ] Support dynamic document ingestion pipelines.
- [ ] Support Markdown documents.
- [ ] Support PDF documents.
- [ ] Support Word documents.
- [ ] Support citations and source references in chat answers.
- [ ] Support streaming chat responses.
- [ ] Support persisted chat history when there is a clear product need and
      retention policy.
- [ ] Build a frontend UI.

## Non-Goals

- [ ] Do not provide runtime subject creation in the initial scope.
- [ ] Do not provide runtime document upload in the initial scope.
- [ ] Do not expose original bundled files for download in the initial scope.
- [ ] Do not include document citations or source references in chat responses
      in the initial scope.
- [ ] Do not support streaming chat responses in the initial scope.
- [ ] Do not persist chat prompts, responses, or conversation history in the
      initial scope.
- [ ] Do not require user authentication in the initial scope unless requested
      separately.
- [ ] Do not guarantee perfect factual accuracy from model responses.
- [ ] Do not hide when the model lacks enough document context to answer.
- [ ] Do not couple the product to a single model provider.

## Users

### API Consumer

An API consumer integrates with the backend to ask questions. This may be a
future frontend, script, internal tool, or another backend service.

Needs:

- [ ] Ask questions against the predefined subject.
- [ ] Receive predictable JSON responses and errors.

## Core Concepts

### Subject

The initial application has one predefined subject. The subject represents the
knowledge area covered by the bundled documents.

Requirements:

- [ ] The subject is implicit and does not need to be identified by id.
- [ ] The subject does not require display name or description metadata in the
      initial scope.
- [ ] The initial API does not create, update, or delete subjects.

### Static Source Documents

Static source documents are files bundled with the application. They are
maintained by the application owner and packaged with the service.

Supported initial document types:

- [ ] Plain text `.txt` files.

Requirements:

- [ ] Each bundled document belongs to the predefined subject.
- [ ] Each bundled document has a stable id or resource path.
- [ ] The service fails startup when configured documents cannot be loaded.
- [ ] The service fails startup when no supported documents are found.
- [ ] The initial API does not upload, update, or delete documents at runtime.
- [ ] The initial API does not expose document-management endpoints.

### Chat

Chat lets a user ask a question against the predefined subject. The answer
should be generated using relevant context retrieved from the bundled subject
documents.

Requirements:

- [ ] A chat request includes a user message.
- [ ] A chat request includes a selected model id.
- [ ] The service retrieves only relevant indexed document chunks for the
      request.
- [ ] The service sends the user message and retrieved chunks to the selected
      model.
- [ ] The response includes the generated answer.
- [ ] The response identifies the model used.
- [ ] The response should indicate when there is not enough document context to
      answer confidently.

## REST API Requirements

Endpoint names are proposed and may be refined during technical design.

### Chat API

- [x] `POST /chat` asks a question against the configured subject.
- [x] Request body includes `message`.
- [x] Request body includes `modelId`.
- [x] Response body includes `answer`.
- [x] Response body includes `modelId`.
- [x] Response is returned as a single non-streaming JSON response.
- [ ] The initial API does not create or update persisted chat history.

### Model API

- [x] `GET /models` lists available models.
- [x] Model list indicates online, local server, or embedded offline mode.
- [x] Model list indicates availability status.
- [x] Model availability status is one of `CONFIGURED`, `AVAILABLE`,
      `UNAVAILABLE`, or `MISCONFIGURED`.
- [x] Model list indicates whether prompts may leave the local machine.

### Error Responses

Errors should use a consistent JSON shape:

```json
{
  "code": "MODEL_NOT_FOUND",
  "message": "Configured model was not found.",
  "details": {}
}
```

## Functional Requirements

### Static Document Loading

- [ ] Discover configured documents bundled with the application.
- [ ] Validate configured document files at startup or during indexing.
- [ ] Reject empty documents.
- [ ] Extract or load text from supported document types.
- [ ] Split extracted text into searchable chunks.
- [ ] Use configurable chunking behavior.
- [ ] Index bundled documents once at startup or application initialization.
- [ ] Preserve enough metadata for audit and retrieval.
- [ ] Prepare document chunks for retrieval-augmented generation.

### Subject-Aware Chat

- [x] Require a non-empty user message.
- [ ] Use only bundled documents from the predefined subject.
- [ ] Retrieve only the chunks relevant to the user's message.
- [ ] Return a useful response when no documents are configured.
- [ ] Return a useful response when the selected model is unavailable.
- [ ] Avoid exposing internal resource paths unnecessarily.

### Model Selection

- [x] Require `modelId` for chat requests.
- [x] Validate requested `modelId`.
- [x] Return a validation error when `modelId` is missing.
- [ ] Allow model availability to vary by environment.
- [ ] Support future online, local server, and embedded offline model runtimes.

## Non-Functional Requirements

### Reliability

- [ ] Return consistent error response shapes.
- [ ] Error responses include `code`, `message`, and optional `details`.
- [ ] Handle model provider failures gracefully.
- [ ] Keep service health independent from model availability.

### Security And Privacy

- [ ] Do not log raw document content by default.
- [ ] Do not log raw chat prompts or model responses by default.
- [ ] Do not store raw chat prompts, model responses, or conversation history
      by default.
- [ ] Clearly distinguish online models from offline models.
- [ ] Keep API credentials out of source control.
- [ ] Avoid exposing full internal file paths in API responses.

### Performance

- [ ] Non-chat endpoints should respond without invoking AI models.
- [ ] Chat APIs should have configurable timeouts.
- [ ] Do not retry chat generation automatically in the initial scope.
- [ ] Document extraction and indexing should avoid repeated expensive work per
      chat request.
- [ ] Chat requests should not send all bundled document content to the model by
      default.
- [ ] Large bundled documents should be designed for future asynchronous
      indexing if needed.

### Observability

- [ ] Expose health information through Actuator.
- [ ] Track request failures by endpoint.
- [ ] Track document loading or indexing failures.
- [ ] Track model runtime failures without exposing prompt content.
- [ ] Track selected model id where safe.

### Testability

- [ ] Unit test static document discovery.
- [ ] Unit test chat routing behavior.
- [ ] Unit test error handling.
- [ ] Add integration tests for REST endpoints and the static document
      indexing/chat flow.
- [ ] Add integration tests covering persistence and retrieval behavior.
- [x] Use fake model clients in tests.
- [ ] Maintain at least 80% unit test coverage.

## Acceptance Criteria

### Minimum Backend API

- [ ] A user can ask a question against the configured subject.
- [ ] The chat answer uses only bundled documents for the configured subject.
- [ ] Tests cover static document loading and chat flow.
- [ ] Kover coverage verification passes.

### Model Runtime Readiness

- [ ] The API can list configured models.
- [ ] The chat endpoint can reject an unknown model id.
- [ ] The system design allows online, local server, and embedded offline
      models.
- [ ] Embedded offline model support uses llama.cpp with GGUF model files.

## Related Documents

- [Architecture](ARCHITECTURE.md)
