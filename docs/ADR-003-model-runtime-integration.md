# ADR-003: Model Runtime Integration

## Status

Accepted.

## Context

The application should let a user choose which configured model handles a chat
request. Supported runtime directions include online cloud providers, local
model servers, and future fully offline embedded models.

The application domain should not depend directly on one provider SDK or one
runtime. Provider-specific code should stay behind adapters.

## Decision

Use a project-owned provider-neutral model abstraction for application logic.
Use Spring AI where it fits provider-backed integrations, and add runtime
adapters behind the internal `AiModelClient` interface.

Configured models are defined statically in application configuration. Chat
requests must include `modelId`; there is no default chat model in the initial
scope. The model list is configured directly under `aisme.chat-models`.

Runtime integration follows the component flow documented in
[Architecture](ARCHITECTURE.md).

## Runtime Directions

### Online Cloud

- [ ] Use Spring AI where possible.
- [ ] Support OpenAI-compatible chat providers.
- [x] Support OpenAI-compatible chat-completion providers.
- [x] Support Hugging Face Inference Endpoint / TGI-compatible `/generate`.
- [x] Configure credentials through application configuration, with environment
      variables or secret storage as the intended source.
- [ ] Return clear errors for missing API keys or unavailable endpoints.
- [x] Mark online models so users know prompts may leave the local machine.

### Local Server

- [x] Use Ollama as the first supported local server runtime.
- [x] Support Ollama running at `http://localhost:11434`.
- [x] Support user-configured Ollama base URLs.
- [ ] Support selecting different Ollama models per request.
- [ ] Add model availability and health checks.

### Embedded Offline

- [ ] Use llama.cpp with GGUF model files as the selected embedded offline
      direction.
- [ ] Keep model files as external application assets, not bundled inside the
      executable JAR.
- [ ] Add model asset directory configuration.
- [ ] Add model metadata files for bundled or predownloaded models.
- [ ] Validate model file checksums before use.
- [ ] Expose model license and redistribution metadata.
- [ ] Report hardware requirements before loading a model.
- [ ] Support offline startup without network access.

Future embedded runtime options:

- [ ] Evaluate llama.cpp through a native/JVM wrapper.
- [ ] Evaluate ONNX Runtime for supported local model workloads.
- [ ] Evaluate running an embedded inference binary as a child process.

## Open Decisions

- [ ] Decide the exact Spring AI dependencies once implementation starts.
- [x] Decide the first OpenAI-compatible provider configuration shape.
- [x] Use the TGI-compatible `/generate` API as the first Hugging Face
      integration path.
- [ ] Decide the concrete embedded llama.cpp integration approach.

## Runtime Defaults

- [ ] Use synchronous non-streaming chat initially.
- [ ] Keep streaming chat responses as future scope.
- [x] Use configurable chat timeouts with `60s` as the initial default.
- [x] Use configurable model availability timeouts with `5s` as the initial
      default.
- [x] Do not retry chat generation automatically in the initial implementation.
- [ ] Keep retry behavior for availability checks as a future enhancement.
- [ ] Keep request cancellation support as a future enhancement.

## JSON Serialization

- [x] Use Jackson for Spring MVC request and response binding.
- [ ] Keep `kotlinx.serialization` as a future option only if the application
      explicitly standardizes on Kotlin-first serialization.

Rationale:

Spring MVC uses Jackson as its default JSON mapper, and the current REST DTO
tests should verify the same binding behavior that controllers will use at
runtime. `kotlinx.serialization` is more Kotlin-native, but using it for Spring
MVC would require explicit converter configuration and separate verification of
request validation and error responses. Jackson is the pragmatic default for
this Spring Boot backend.

## Model Availability States

Use these provider-neutral availability states in model responses. Until runtime
availability checks are implemented, configured models are reported as
`CONFIGURED`.

- [x] `CONFIGURED`: model is configured, but runtime availability has not been
      confirmed.
- [x] `AVAILABLE`: model passed the current availability check.
- [x] `UNAVAILABLE`: model is configured but the runtime is not reachable or
      not currently usable.
- [x] `MISCONFIGURED`: required model configuration is missing or invalid.

## Configuration Shape

Use a direct list for configured models:

```yaml
aisme:
  chat-models:
    - id: local-ollama-llama
      display-name: Local Ollama Llama
      runtime: OLLAMA
      mode: LOCAL_SERVER
      base-url: http://localhost:11434
      model-name: llama3.2
      available-offline: false
```

Use one statically configured embedding model:

```yaml
aisme:
  embedding-model:
    id: local-bge-small
    version: "1.5"
    dimensions: 384
    runtime: ONNX
    model-path: ./models/bge-small-en-v1.5/model.onnx
    tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json
```

Rationale:

- [x] `modelId` is required on chat requests, so a default model is not needed.
- [x] The current model registry only needs a list of configured models.

## Consequences

- [x] Controllers and use-case services depend on internal DTOs and
      `AiModelClient`, not provider SDKs.
- [x] Provider-specific classes stay outside controller and domain code.
- [x] `ChatModelRegistry` becomes the source of truth for configured models,
      runtime mode, availability, and user-facing labels.
- [x] `ChatModelRegistry` reads configured models from `aisme.chat-models`.
- [x] Chat requests fail validation when `modelId` is missing.
- [x] Model responses use the provider-neutral availability states defined in
      this ADR.
- [x] Tests can use fake model clients for application flow and adapter-level
      tests for provider behavior.
