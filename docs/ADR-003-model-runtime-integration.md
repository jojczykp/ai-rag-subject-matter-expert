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
scope. The model list is configured directly under `aisme.models`.

Runtime integration follows the component flow documented in
[Architecture](ARCHITECTURE.md).

## Runtime Directions

### Online Cloud

- [ ] Use Spring AI where possible.
- [ ] Support OpenAI-compatible chat providers.
- [ ] Support Hugging Face Inference Endpoint / TGI.
- [ ] Configure credentials through environment variables or secret storage.
- [ ] Return clear errors for missing API keys or unavailable endpoints.
- [ ] Mark online models so users know prompts may leave the local machine.

### Local Server

- [ ] Use Ollama as the first supported local server runtime.
- [ ] Support Ollama running at `http://localhost:11434`.
- [ ] Support user-configured Ollama base URLs.
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
- [ ] Decide the first OpenAI-compatible provider configuration shape.
- [ ] Decide the first Hugging Face integration path: Inference Endpoint, TGI,
      or both.
- [ ] Decide the concrete embedded llama.cpp integration approach.

## Runtime Defaults

- [ ] Use synchronous non-streaming chat initially.
- [ ] Keep streaming chat responses as future scope.
- [ ] Use configurable chat timeouts with `60s` as the initial default.
- [ ] Use configurable model availability timeouts with `5s` as the initial
      default.
- [ ] Do not retry chat generation automatically in the initial implementation.
- [ ] Keep retry behavior for availability checks as a future enhancement.
- [ ] Keep request cancellation support as a future enhancement.

## Model Availability States

Use these provider-neutral availability states in model responses:

- [ ] `CONFIGURED`: model is configured, but runtime availability has not been
      confirmed.
- [ ] `AVAILABLE`: model passed the current availability check.
- [ ] `UNAVAILABLE`: model is configured but the runtime is not reachable or
      not currently usable.
- [ ] `MISCONFIGURED`: required model configuration is missing or invalid.

## Configuration Shape

Use a direct list for configured models:

```yaml
aisme:
  models:
    - id: local-ollama-llama
      display-name: Local Ollama Llama
      runtime: OLLAMA
      mode: LOCAL_SERVER
      base-url: http://localhost:11434
      available-offline: false
```

Use one statically configured embedding model:

```yaml
aisme:
  embedding-model:
    id: local-bge-small
    runtime: ONNX
    model-path: ./models/bge-small-en-v1.5/model.onnx
    tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json
    dimensions: 384
```

Rationale:

- [ ] `modelId` is required on chat requests, so a default model is not needed.
- [ ] The current model registry only needs a list of configured models.

## Consequences

- [ ] Controllers and use-case services depend on internal DTOs and
      `AiModelClient`, not provider SDKs.
- [ ] Provider-specific classes stay outside controller and domain code.
- [ ] `ModelRegistry` becomes the source of truth for configured models,
      runtime mode, availability, and user-facing labels.
- [ ] `ModelRegistry` reads configured models from `aisme.models`.
- [ ] Chat requests fail validation when `modelId` is missing.
- [ ] Model responses use the provider-neutral availability states defined in
      this ADR.
- [ ] Tests can use fake model clients for application flow and adapter-level
      tests for provider behavior.
