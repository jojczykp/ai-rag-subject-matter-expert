# Architecture

## Purpose

This document describes the planned model integration architecture for AI
Subject Matter Expert. The goal is to let users choose between online models,
local server models, and fully offline embedded models without coupling the
application domain logic to any single model provider or runtime.

As features are delivered, update this document by marking the matching
checklist items as complete.

## Decision Records

- [ADR-001: Embedding Generation Strategy](ADR-001-embedding-generation-strategy.md)
- [ADR-002: Persistence And Vector Search](ADR-002-persistence-and-vector-search.md)
- [ADR-003: Model Runtime Integration](ADR-003-model-runtime-integration.md)
- [ADR-004: Integration Testing Strategy](ADR-004-integration-testing-strategy.md)
- [ADR-005: Subject Document Scope](ADR-005-subject-document-scope.md)
- [ADR-006: Local Embedding Runtime](ADR-006-local-embedding-runtime.md)
- [ADR-007: Embedded Llama](ADR-007-embedded-llama.md)
- [ADR-008: Operational Logging](ADR-008-operational-logging.md)
- [ADR-009: Frontend UI Architecture](ADR-009-frontend-ui-architecture.md)

## Current Product Scope

The initial product scope is intentionally smaller than the full model-routing
vision. See
[ADR-005: Subject Document Scope](ADR-005-subject-document-scope.md)
for the accepted static subject and bundled document scope decision.

The first implementation is a single-subject backend using static bundled
documents as the knowledge base.

## Goals

- [x] Let users choose which model to use for each interaction.
- [x] Support online cloud-hosted models.
- [x] Support locally running model servers, including Ollama on localhost.
- [x] Support Hugging Face-hosted models through managed inference endpoints.
- [x] Support fully offline use with predownloaded embedded models.
- [x] Keep application code independent from provider-specific SDKs.
- [x] Track model capabilities, availability, and runtime requirements.
- [x] Make online/offline behavior explicit and visible to users.

## Non-Goals

- [x] Do not bundle multi-GB model files inside the application JAR.
- [x] Do not make one model provider a hard dependency of the domain layer.
- [x] Do not require network access for embedded offline model usage.

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
       -> OllamaAiModelClient
            -> Ollama local server runtime
       -> OpenAiCompatibleAiModelClient
            -> OpenAI-compatible chat-completion providers
       -> HuggingFaceTgiAiModelClient
            -> Hugging Face Inference Endpoint / TGI-compatible runtime
       -> SpringAiModelClient
            -> cloud provider-backed model runtime
       -> EmbeddedModelClient
            -> application-managed llama-server child process
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
- [x] Add runtime availability checks.
- [x] Add model capability metadata.
- [x] Add online/offline mode metadata.
- [x] Add user-facing display names.
- [x] Add user-facing descriptions.
- [x] Add tests for model discovery and filtering.

Runtime availability checks should be delegated to a separate
`ChatModelAvailabilityService`, not implemented directly in the registry or
controller. `ChatModelRegistry` should keep owning configured model metadata,
while availability checkers enrich descriptors with current runtime state for
`GET /chat-models` and chat validation. Provider-specific checkers should stay behind
that service, for example Ollama, cloud, and embedded-runtime checkers.
`GET /chat-models` should use availability checks to inform callers; `POST /chat`
should enforce that the selected model is usable before calling it. Short-lived
availability caching can be added inside `ChatModelAvailabilityService` if
checks become slow or noisy.

`GET /chat-models` exposes model capabilities and runtime requirements derived from
the configured runtime. The first implementation reports `CHAT` capability for
all selectable models and requirements such as network access, API keys,
Ollama, GGUF assets, or a managed llama-server executable. Cloud-provider
runtimes are online-only in the initial scope; local OpenAI-compatible and
local TGI-compatible servers can be added later as explicit future runtimes if
needed.

`GET /embedding-models` exposes the configured embedding model catalog. The
indexer stores embeddings for every enabled embedding model client. `POST /chat`
uses the optional `embeddingModelId` for query embedding and relevant chunk
retrieval. When more than one embedding model is enabled, callers must provide
`embeddingModelId`; otherwise the single enabled embedding model is used as the
default.

Embedding model availability follows the same status semantics as chat model
availability. `EmbeddingModelAvailabilityService` keeps configured metadata in
the registry and delegates runtime checks to provider-specific checkers. ONNX
embedding availability checks readable local model and tokenizer assets; Ollama
embedding availability checks that the configured model is present in the local
Ollama model list. Results are cached briefly because local server reachability
and local asset state can change while the application is running.

Embedded offline asset availability is computed once when the checker is
created because GGUF files and the managed `llama-server` executable are static
local assets. Updating those files requires restarting the application to
refresh embedded offline availability. The startup check verifies that local
asset paths exist and are readable, and that the configured `llama-server`
executable can be used. Online and local-server providers remain eligible for
request-time checks because their reachability can change while the application
is running.

Example model descriptor:

```kotlin
data class ChatModelDescriptor(
    val id: String,
    val displayName: String,
    val description: String?,
    val runtime: ChatModelRuntime,
    val mode: ChatModelMode,
    val availableOffline: Boolean,
    val availability: ChatModelAvailability,
    val baseUrl: String?,
    val modelName: String?,
    val apiKey: String?,
)

enum class ChatModelRuntime {
    SPRING_AI,
    OPENAI_COMPATIBLE,
    OLLAMA,
    HUGGING_FACE_TGI,
    EMBEDDED_LLAMA,
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

- [x] Add `AiChatService`.
- [x] Require a selected model id with each chat request.
- [x] Retrieve relevant chunks from the single configured subject's bundled
      resource documents.
- [x] Resolve the selected model through `ChatModelRegistry`.
- [x] Route chat requests to the matching `AiModelClient`.
- [x] Return provider-neutral responses.
- [x] Add tests for model selection and routing.

### Static Subject Document Loading

Static subject document loading components should load and describe the static
documents bundled with the application.

- [x] Add static subject document loading components.
- [x] Discover configured documents in application resources.
- [x] Extract or load text from supported resource files.
- [x] Split extracted text into searchable chunks.
- [x] Index bundled documents once at startup or application initialization.
- [x] Provide indexed chunks to the chat context retrieval layer.
- [x] Add tests for resource discovery and unsupported document types.

### RelevantChunkRetriever

`RelevantChunkRetriever` should select a small, relevant subset of indexed
document chunks for each user message. The initial architecture should use
retrieval-augmented generation rather than sending every bundled document to the
model for each request.

- [x] Add `RelevantChunkRetriever`.
- [x] Retrieve relevant chunks for a user message.
- [x] Limit retrieved context to a configurable budget.
- [x] Return enough chunk metadata to support future citations.
- [x] Avoid sending all bundled document content to the model by default.
- [x] Add tests for retrieval behavior and empty-result handling.

### AiModelClient

`AiModelClient` is the internal provider-neutral interface. Each runtime gets
an adapter that implements this interface. A client instance represents one
configured application model. Provider components can create multiple client
instances from `aisme.chat.models` when one runtime supports multiple models.

- [x] Add `AiModelClient`.
- [x] Add request and response DTOs.
- [x] Add synchronous chat support.
- [x] Add structured error handling.
- [x] Add configurable API timeout behavior.
- [x] Add tests for implemented adapter contracts.

Draft interface:

```kotlin
interface AiModelClient {
    val modelId: String

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
for the accepted dedicated embedding model catalog decision and
[ADR-006: Local Embedding Runtime](ADR-006-local-embedding-runtime.md) for the
local embedding runtime decision.

## Persistence And Database Access

Use PostgreSQL with pgvector for document chunks, embeddings, and similarity
search. Keep database access simple and explicit. See
[ADR-002: Persistence And Vector Search](ADR-002-persistence-and-vector-search.md)
for the accepted database access and vector search decision.

## Data Model Overview

The database model stores bundled source document metadata, deterministic
chunks, and one embedding per chunk per embedding model id. Startup indexing
refreshes missing or stale embeddings when enabled embedding model metadata or
chunking strategy changes. The schema diagram is maintained in
[Database Schema](DATABASE_SCHEMA.md).

## Configuration

Configuration should make model availability and runtime mode explicit.

- [x] Add bundled document resource folder configuration.
- [x] Keep model selection in a single `aisme.chat.models` catalog with keyed
      model ids, per-model `enabled` flags, and optional display order.
- [x] Add configuration properties for `aisme.chat.models`.
- [x] Add environment variable support for cloud credentials.
- [x] Add validation for missing required provider settings.
- [x] Report missing OpenAI-compatible credentials as model misconfiguration
      instead of failing application startup.
- [x] Document all configuration properties in README.md.

Example model catalog configuration:

```yaml
aisme:
  documents:
    location: classpath:/subject-documents/

  embedding:
    api-timeout: 60s
    default-model-id: ollama-nomic-embed
    model-availability:
      timeout: 5s
      cache-ttl: 5s
    runtimes:
      local-onnx:
        type: ONNX
    models:
      local-bge-small:
        enabled: true
        version: "1.5"
        dimensions: 384
        runtime:
          id: local-onnx
          model-path: ./models/bge-small-en-v1.5/model.onnx
          tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json

  chat:
    default-model-id: embedded-mistral-7b
    api-timeout: 60s
    relevant-chunk-limit: 5
    model-availability:
      timeout: 5s
      cache-ttl: 5s
    runtimes:
      local-ollama:
        type: OLLAMA
        base-url: http://localhost:11434
    models:
      local-ollama-llama:
        enabled: true
        display-order: 50
        display-name: Local Ollama Llama
        runtime:
          id: local-ollama
          model-name: llama3.2
```

## User Experience

Users should understand whether a model requires network access and whether
their data leaves the local machine.

- [x] Show all configured models.
- [x] Show model availability status.
- [x] Label models as online, local server, or embedded offline.
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

- [x] Unit test static resource document discovery.
- [x] Unit test `ChatModelRegistry`.
- [x] Unit test `AiChatService` routing.
- [x] Add Spring Boot configuration binding and validation tests.
- [x] Add integration tests for REST endpoints and the static document
      indexing/chat flow.
- [x] Keep Kover coverage at or above 80%.

## Documentation Tasks

- [ ] Document the single-subject static-resource scope in README.md.
- [ ] Document where bundled subject documents live.
- [x] Document supported model runtimes in README.md.
- [x] Document local Ollama setup in README.md.
- [x] Document cloud provider environment variables in README.md.
- [x] Document embedded offline model installation in README.md.
- [x] Document model selection behavior in README.md.
- [x] Keep this design document updated as implementation progresses.

Implementation sequencing is tracked in
[Implementation Plan](IMPLEMENTATION_PLAN.md).
