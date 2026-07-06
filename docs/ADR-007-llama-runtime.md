# ADR-007: Llama Runtime

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
- [x] Configure the llama runtime behind an explicit `enabled` flag;
      when enabled, require asset directory, `llama-server` executable path,
      host, and port.
- [x] Configure per-model GGUF file paths, runtime arguments, checksum,
      license, and hardware requirement metadata.
- [ ] Start the configured `llama-server` process for embedded offline models
      when the embedded runtime is enabled.
- [ ] Wait for a local readiness endpoint before marking the model available.
- [ ] Send chat requests to the local `llama-server` OpenAI-compatible HTTP API
      using the existing provider-neutral `AiModelClient` abstraction.
- [ ] Stop the child process during application shutdown.
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
      configuration includes asset directory, executable path, host, and port.
- [x] Embedded model metadata must include model file paths, runtime arguments,
      and metadata.
- [x] Availability checks verify executable and model file existence before
      attempting runtime loadability.
- [x] Embedded offline asset availability is calculated once at checker
      creation; local asset changes require application restart.
- [ ] Optional embedded runtime tests should be tagged because they may require
      native executables and model files.
- [ ] Documentation must explain that model files and binaries are local assets
      that users install separately.
- [ ] The application should avoid committing model files, binaries, or runtime
      logs.
