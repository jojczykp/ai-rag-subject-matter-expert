# Architecture

## Purpose

This document describes the planned model integration architecture for AI
Subject Matter Expert. The goal is to let users choose between online models,
local server models, and fully offline embedded models without coupling the
application domain logic to any single model provider or runtime.

All implementation items are currently TODO. As features are delivered, update
this document by marking the matching checklist items as complete.

## Decision Records

- [ADR-001: Embedding Generation Strategy](ADR-001-embedding-generation-strategy.md)
- [ADR-002: Persistence And Vector Search](ADR-002-persistence-and-vector-search.md)
- [ADR-003: Model Runtime Integration](ADR-003-model-runtime-integration.md)
- [ADR-004: Integration Testing Strategy](ADR-004-integration-testing-strategy.md)
- [ADR-005: Subject Document Scope](ADR-005-subject-document-scope.md)
- [ADR-006: Local Embedding Runtime](ADR-006-local-embedding-runtime.md)

## Current Product Scope

The initial product scope is intentionally smaller than the full model-routing
vision. See
[ADR-005: Subject Document Scope](ADR-005-subject-document-scope.md)
for the accepted static subject and bundled document scope decision.

The first implementation is a single-subject backend using static bundled
documents as the knowledge base.

## Goals

- [ ] Let users choose which model to use for each interaction.
- [ ] Support online cloud-hosted models.
- [ ] Support locally running model servers, including Ollama on localhost.
- [ ] Support Hugging Face-hosted models through managed inference endpoints.
- [ ] Support fully offline use with predownloaded embedded models.
- [ ] Keep application code independent from provider-specific SDKs.
- [ ] Track model capabilities, availability, and runtime requirements.
- [ ] Make online/offline behavior explicit and visible to users.

## Non-Goals

- [ ] Do not bundle multi-GB model files inside the application JAR.
- [ ] Do not make one model provider a hard dependency of the domain layer.
- [ ] Do not require network access for embedded offline model usage.
- [ ] Do not hide model licensing, hardware, or memory requirements.

## Recommended Architecture

Use Spring AI for provider-backed models and a project-owned model abstraction
for routing. The internal application should call a stable service interface;
provider adapters should translate that request to Spring AI, Ollama, Hugging
Face, OpenAI-compatible APIs, or an embedded inference runtime. See
[ADR-003: Model Runtime Integration](ADR-003-model-runtime-integration.md) for
the accepted runtime integration decision.

```text
User model selection
  -> ChatModelRegistry
  -> AiChatService
  -> RelevantChunkRetriever
  -> AiModelClient
       -> SpringAiModelClient
            -> provider-backed model runtime
       -> EmbeddedModelClient
            -> offline model runtime
```

## Runtime Modes

The architecture supports online cloud, local server, and embedded offline
model runtimes. Runtime-specific decisions are documented in
[ADR-003: Model Runtime Integration](ADR-003-model-runtime-integration.md).

## Core Components

### ChatModelRegistry

The model registry is the source of truth for available models. It should know
which models are configured, which are currently available, and what each model
can do. The initial subject is implicit and represented by the bundled resource
documents.

- [x] Add `ChatModelRegistry`.
- [x] Add static configuration for known models.
- [ ] Add runtime availability checks.
- [ ] Add model capability metadata.
- [x] Add online/offline mode metadata.
- [x] Add user-facing display names.
- [ ] Add user-facing descriptions.
- [ ] Add tests for model discovery and filtering.

Example model descriptor:

```kotlin
data class ChatModelDescriptor(
    val id: String,
    val displayName: String,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val availability: ChatModelAvailability,
    val baseUrl: String?,
)

enum class ChatModelRuntime {
    SPRING_AI,
    OLLAMA,
    HUGGING_FACE_ENDPOINT,
    EMBEDDED_OFFLINE,
}

enum class ChatModelMode {
    ONLINE,
    LOCAL_SERVER,
    EMBEDDED_OFFLINE,
}

enum class ChatModelAvailability {
    CONFIGURED,
    AVAILABLE,
    UNAVAILABLE,
    MISCONFIGURED,
}
```

### AiChatService

`AiChatService` should own use-case level chat behavior. Controllers and future
UI endpoints should call this service instead of provider clients directly. For
the initial product scope, it routes every chat request through the single
configured subject and relevant chunks retrieved from bundled resource
documents.

- [ ] Add `AiChatService`.
- [ ] Require a selected model id with each chat request.
- [ ] Retrieve relevant chunks from the single configured subject's bundled
      resource documents.
- [ ] Resolve the selected model through `ChatModelRegistry`.
- [ ] Route chat requests to the matching `AiModelClient`.
- [ ] Return provider-neutral responses.
- [ ] Add tests for model selection and routing.

### StaticSubjectDocumentService

`StaticSubjectDocumentService` should load and describe the static documents
bundled with the application.

- [ ] Add `StaticSubjectDocumentService`.
- [ ] Discover configured documents in application resources.
- [ ] Extract or load text from supported resource files.
- [ ] Split extracted text into searchable chunks.
- [ ] Index bundled documents once at startup or application initialization.
- [ ] Expose document metadata for API responses.
- [ ] Provide indexed chunks to the chat context retrieval layer.
- [ ] Add tests for resource discovery and unsupported document types.

### RelevantChunkRetriever

`RelevantChunkRetriever` should select a small, relevant subset of indexed
document chunks for each user message. The initial architecture should use
retrieval-augmented generation rather than sending every bundled document to the
model for each request.

- [ ] Add `RelevantChunkRetriever`.
- [ ] Retrieve relevant chunks for a user message.
- [ ] Limit retrieved context to a configurable budget.
- [ ] Return enough chunk metadata to support future citations.
- [ ] Avoid sending all bundled document content to the model by default.
- [ ] Add tests for retrieval behavior and empty-result handling.

### AiModelClient

`AiModelClient` is the internal provider-neutral interface. Each runtime gets
an adapter that implements this interface.

- [x] Add `AiModelClient`.
- [x] Add request and response DTOs.
- [ ] Add synchronous chat support.
- [ ] Add structured error handling.
- [ ] Add configurable timeout behavior.
- [ ] Add tests for each adapter contract.

Draft interface:

```kotlin
interface AiModelClient {
    fun chat(request: AiModelChatRequest): AiModelChatResponse
}
```

Streaming chat support can be added later when the API shape is clear.

### Provider Adapters

Provider adapters translate internal requests to the selected runtime. Runtime
integration details are owned by
[ADR-003: Model Runtime Integration](ADR-003-model-runtime-integration.md).

## Grounding Strategy

Use retrieval-augmented generation as the initial grounding strategy. See
[ADR-001: Embedding Generation Strategy](ADR-001-embedding-generation-strategy.md)
for the accepted statically configured embedding model decision and
[ADR-006: Local Embedding Runtime](ADR-006-local-embedding-runtime.md) for the
local embedding runtime decision.

## Persistence And Database Access

Use PostgreSQL with pgvector for document chunks, embeddings, and similarity
search. Keep database access simple and explicit. See
[ADR-002: Persistence And Vector Search](ADR-002-persistence-and-vector-search.md)
for the accepted database access and vector search decision.

## Data Model Overview

The initial database model stores bundled source document metadata, deterministic
chunks, and one embedding per chunk for the statically configured embedding
model. Startup indexing refreshes missing or stale embeddings when the configured
embedding model metadata or chunking strategy changes. The schema diagram is maintained in
[Database Schema](DATABASE_SCHEMA.md).

## Configuration

Configuration should make model availability and runtime mode explicit.

- [ ] Add bundled document resource folder configuration.
- [ ] Add `application-local.yml` for local server models.
- [ ] Add `application-cloud.yml` for cloud-hosted models.
- [ ] Add `application-offline.yml` for embedded offline models.
- [ ] Add configuration properties for `aisme.chat-models`.
- [ ] Add environment variable support for cloud credentials.
- [ ] Add validation for missing required provider settings.
- [ ] Document all configuration properties in README.md.

Example profile intent:

```yaml
spring:
  profiles:
    active: local
```

```yaml
aisme:
  documents-location: classpath:/subject-documents/
  chat-models:
    - id: local-ollama-llama
      display-name: Local Ollama Llama
      runtime: OLLAMA
      mode: LOCAL_SERVER
      base-url: http://localhost:11434
      available-offline: false
  embedding-model:
    id: local-bge-small
    version: "1.5"
    dimensions: 384
    runtime: ONNX
    model-path: ./models/bge-small-en-v1.5/model.onnx
    tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json
  chat:
    timeout: 60s
  model-availability:
    timeout: 5s
```

## User Experience

Users should understand whether a model requires network access and whether
their data leaves the local machine.

- [ ] Show all configured models.
- [ ] Show model availability status.
- [ ] Label models as online, local server, or embedded offline.
- [ ] Warn before sending prompts to online providers.
- [ ] Explain when a local model server is unavailable.
- [ ] Explain when an embedded model cannot be loaded.
- [ ] Optionally remember the user's last selected model as a future user
      convenience feature.

## Security And Privacy

Online and offline modes have different privacy expectations. The application
should make those differences explicit.

- [ ] Never log raw prompts or model responses by default.
- [ ] Keep API keys in environment variables or secret storage.
- [ ] Clearly mark online providers that receive user prompts over the network.
- [ ] Support fully offline operation for embedded offline models.
- [ ] Add tests for configuration that disables online providers.

## Observability

Operational visibility should avoid leaking sensitive prompt data.

- [ ] Track selected model id.
- [ ] Track runtime type.
- [ ] Track latency and failure counts.
- [ ] Track token usage when providers report it.
- [ ] Avoid recording prompt and response bodies by default.
- [ ] Add health indicators for configured model runtimes.

## Testing Strategy

Tests should cover routing and configuration without requiring real cloud
credentials or locally running model servers in the default test path. Provider
integration tests should use Docker-backed services or HTTP protocol mocks. See
[ADR-004: Integration Testing Strategy](ADR-004-integration-testing-strategy.md)
for the accepted integration testing decision.

- [ ] Unit test static resource document discovery.
- [ ] Unit test `ChatModelRegistry`.
- [ ] Unit test `AiChatService` routing.
- [ ] Add Spring Boot configuration tests for each profile.
- [ ] Add integration tests for REST endpoints and the static document
      indexing/chat flow.
- [ ] Keep Kover coverage at or above 80%.

## Documentation Tasks

- [ ] Document the single-subject static-resource scope in README.md.
- [ ] Document where bundled subject documents live.
- [ ] Document supported model runtimes in README.md.
- [ ] Document local Ollama setup in README.md.
- [ ] Document cloud provider environment variables in README.md.
- [ ] Document embedded offline model installation in README.md.
- [ ] Document model selection behavior in README.md.
- [ ] Keep this design document updated as implementation progresses.

Implementation sequencing is tracked in
[Implementation Plan](IMPLEMENTATION_PLAN.md).
