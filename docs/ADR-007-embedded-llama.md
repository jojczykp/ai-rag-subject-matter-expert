# ADR-007: Embedded Llama

## Status

Accepted.

## Context

The application should support fully offline chat models after model assets are
present on the local machine. The previously accepted direction is llama.cpp
with GGUF model files, but the concrete integration mechanism still needed to
be selected.

The mechanism should keep the Spring/Kotlin application simple, avoid binding
the JVM directly to unstable native APIs too early, and make runtime failures
observable through the same model availability and provider-error handling used
by other chat runtimes.

llama.cpp provides `llama-server`, an HTTP server for llama.cpp models, with
OpenAI-compatible chat-completion endpoints. See the upstream llama.cpp server
documentation:
https://github.com/ggml-org/llama.cpp/tree/master/tools/server

## Decision

Use `llama-server` as an embedded child process managed by the application.

The application will:

- [x] Require GGUF model files as external local assets, not files bundled into
      the executable JAR.
- [x] Configure the embedded llama behind an explicit `enabled` flag;
      when enabled, require asset directory, `llama-server` executable path,
      and model metadata.
- [x] Configure per-model GGUF file paths, runtime arguments, checksum,
      and context size.
- [x] Start the configured `llama-server` process for embedded offline models
      when the embedded runtime is enabled.
- [x] Allocate loopback ports internally instead of exposing host or port in
      application configuration.
- [x] Wait for a local readiness endpoint before marking the model available.
- [x] Send chat requests to the local `llama-server` OpenAI-compatible HTTP API
      using the existing provider-neutral `AiModelClient` abstraction.
- [x] Introduce structured application logging for embedded runtime process
      lifecycle events and collect managed `llama-server` stdout and stderr
      into those logs.
- [x] Stop the child process during application shutdown.
- [ ] Treat startup, readiness, timeout, and non-zero-exit failures as embedded
      runtime availability or provider errors.

The first implementation should be synchronous and non-streaming, consistent
with the current chat API.

## Rationale

This approach is pragmatic for the first embedded runtime implementation:

- [x] Avoids JNI/JNA/native wrapper complexity in the Spring application.
- [x] Uses llama.cpp's maintained executable boundary instead of binding to
      internal native APIs.
- [x] Reuses the existing OpenAI-compatible request/response mapping style.
- [x] Keeps offline operation possible after the executable and GGUF model
      assets are installed locally.
- [x] Makes runtime process lifecycle, logs, readiness, and exit status
      explicit.

The tradeoff is that the application must manage a child process and local port
allocation. That is acceptable because it keeps the initial JVM integration
simple and debuggable.

## Alternatives Considered

### Native/JVM Wrapper

Use a direct llama.cpp JVM/native wrapper.

- [ ] Potentially lower overhead than HTTP.
- [ ] Avoids managing a separate process.
- [ ] Adds JNI/JNA/native packaging complexity.
- [ ] Couples the application more tightly to wrapper maturity and native ABI
      changes.

This remains a future option if process-based integration becomes too limiting.

### User-Managed Local Server

Require users to start `llama-server` themselves and configure it like another
local OpenAI-compatible server.

- [ ] Simpler application implementation.
- [ ] Less embedded because the application does not manage the runtime.
- [ ] Worse offline user experience because startup and availability are
      outside the application.

This is useful as a fallback, but it does not satisfy the embedded runtime goal
as well as an application-managed process.

### ONNX Runtime

Use ONNX Runtime for chat model inference.

- [ ] Already used for embeddings.
- [ ] Better fit for supported embedding/classification workloads.
- [ ] Not the selected first path for GGUF LLM chat inference.

ONNX remains an option for future supported local model workloads, but not the
first embedded chat runtime.

## Consequences

- [x] Embedded runtime asset configuration is disabled by default; enabled
      configuration includes asset directory, executable path, and model
      metadata.
- [x] Embedded model metadata must include model file paths, runtime arguments,
      and metadata.
- [x] Availability checks verify executable and model file existence before
      attempting runtime loadability.
- [x] Availability checks verify configured GGUF SHA-256 checksums at startup
      when model metadata provides `sha256`.
- [x] Embedded offline asset availability is calculated once at checker
      creation; local asset changes require application restart.
- [x] Managed process readiness is checked with the local `llama-server`
      health endpoint before embedded models are marked available.
- [x] Managed process stdout and stderr should be captured through application
      logging instead of being discarded or printed directly to the console.
- [ ] Container-level acceptance testing should verify embedded offline runtime
      behavior with bundled or mounted model assets.
- [x] Documentation explains that model files and binaries are local assets
      that users install separately.
- [ ] The application should avoid committing model files, binaries, or runtime
      logs.
