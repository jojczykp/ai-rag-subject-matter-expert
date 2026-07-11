# ADR-008: Operational Logging

## Status

Accepted.

## Context

The application performs several decisions during startup and request
processing that should be observable while the service is running:

- bundled document discovery, validation, chunking, and indexing
- embedding refresh decisions
- model catalog construction
- model availability checks and cache usage
- chat model selection, context retrieval, provider calls, and failures

Logs should help operators understand what the application is doing without
exposing user prompts, model responses, document content, credentials, or full
provider payloads.

## Decision

Use application logs for privacy-safe operational observability.

The application will:

- [x] Use SLF4J parameterized logging through `LoggerFactory.getLogger`.
- [x] Log startup and request-processing decisions at `INFO` level.
- [x] Log recoverable unavailable or misconfigured runtime decisions at `WARN`
      level.
- [x] Log model ids, runtimes, modes, availability states, counts, and elapsed
      durations.
- [x] Avoid logging raw prompts, model responses, document chunk content,
      provider payloads, API keys, or generated prompt logs.
- [x] Keep managed embedded `llama-server` stdout and stderr in application
      logs, as decided in [ADR-007: Embedded Llama](ADR-007-embedded-llama.md).

## Rationale

This keeps the service debuggable without introducing a tracing or metrics
stack before the operational needs are clear. Structured metadata in logs is
enough for the current backend application and is compatible with future log
aggregation.

The approach also keeps privacy behavior explicit. The application can show
what decision was made and why without copying sensitive AI inputs or outputs
into log storage.

## Consequences

- [x] Startup logs describe document loading, chunking, indexing, and model
      catalog setup.
- [x] Request logs describe selected model id, runtime, retrieved chunk count,
      provider call outcome, and elapsed time.
- [x] Availability logs describe checker use, cache hits, and resulting
      availability.
- [x] Provider adapter logs describe calls by model id and provider type, but
      not request or response bodies.
- [ ] Metrics and distributed tracing can be added later if logs are not enough
      for production operations.
