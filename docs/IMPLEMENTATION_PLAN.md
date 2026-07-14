# Implementation Plan

## Purpose

This document tracks the planned delivery order for AI Subject Matter Expert.
The plan is based on the current product requirements, architecture, and
accepted ADRs.

All implementation items are TODO until delivered and verified.

## Milestone 1: Static Subject And Document Loading

- [x] Add bundled document location configuration.
- [x] Use Spring Boot Actuator health and info endpoints for service
      availability checks.
- [x] Add recursive `.txt` discovery under `subject-documents/`.
- [x] Use relative classpath resource paths as stable document identities.
- [x] Sort discovered resource paths lexicographically before indexing.
- [x] Add fail-fast startup behavior for missing folder, no supported
      documents, unreadable documents, and empty documents.
- [x] Add fail-fast startup behavior for chunking failures when document
      chunking is implemented.
- [x] Add statically configurable chunk size and chunk overlap.
- [x] Configure initial chunk size as `700` characters.
- [x] Configure initial chunk overlap as `100` characters.
- [x] Add deterministic document chunking.
- [x] Add unit tests for static resource discovery, validation, and ordering.
- [x] Add unit tests for deterministic document chunking when chunking is
      implemented.

## Milestone 2: Persistence, Embeddings, And Retrieval

- [x] Add Flyway migrations under `backend/src/main/resources/db/migration`.
- [x] Add PostgreSQL + pgvector persistence support.
- [x] Add `source_document`, `document_chunk`, and `chunk_embedding` schema.
- [x] Add Spring Data JDBC repositories for ordinary persistence.
- [x] Add Spring `JdbcClient` queries for pgvector retrieval.
- [x] Add statically configured `aisme.embedding-runtimes` and
      `aisme.embedding-models` configuration.
- [x] Add local ONNX embedding model integration.
- [x] Configure local model and tokenizer paths for `BAAI/bge-small-en-v1.5`.
- [x] Store embedding model id, version, dimensions, and chunking strategy
      version with indexed embeddings.
- [x] Add re-indexing behavior for stale or missing embeddings.
- [x] Add `RelevantChunkRetriever`.
- [x] Add Testcontainers integration tests with the pgvector image selected in
      ADR-002.

## Milestone 3: Provider-Neutral Chat API

- [x] Add provider-neutral chat request and response DTOs.
- [x] Add `AiModelClient` interface.
- [x] Add fake model client for deterministic application-flow tests.
- [x] Add `ChatModelRegistry` backed by static `aisme.chat-models` configuration.
- [x] Require `modelId` on chat requests.
- [x] Add `GET /models`.
- [x] Add `POST /chat`.
- [x] Add `AiChatService`.
- [x] Add model availability status and privacy labels to model responses.
- [x] Support `CONFIGURED`, `AVAILABLE`, `UNAVAILABLE`, and `MISCONFIGURED`
      model availability states.
- [x] Add consistent error responses with `code`, `message`, and optional
      `details`.
- [x] Add configurable chat timeout with `60s` default.
- [x] Add configurable model availability timeout with `5s` default.
- [x] Add `ChatModelAvailabilityService`.
- [x] Use `ChatModelAvailabilityService` in `GET /models`.
- [x] Use `ChatModelAvailabilityService` in `POST /chat`.
- [x] Add short-lived availability caching if checks are slow or noisy.
- [x] Do not retry chat generation automatically.
- [x] Add integration tests for `/models`, `/chat`, static document indexing,
      retrieval, and fake model routing.
- [x] Keep Kover coverage at or above 80% in default verification.

## Milestone 4: Local Ollama Runtime

- [x] Add Spring AI Ollama dependency or adapter dependency selected during
      implementation.
- [x] Add local Ollama model configuration under `aisme.chat-models`.
- [x] Add Ollama adapter behind `AiModelClient`.
- [x] Support Ollama running at `http://localhost:11434`.
- [x] Support user-configured Ollama base URLs.
- [x] Add Ollama model availability checker behind
      `ChatModelAvailabilityService`.
- [x] Add local mock or fake-client tests for adapter behavior where practical.
- [x] Add optional tagged Testcontainers Ollama tests.
- [x] Add optional model-backed Ollama container test for application chat flow
      after choosing how test models are supplied.

## Milestone 5: Cloud Runtime Adapters

- [x] Add OpenAI-compatible cloud adapter.
- [x] Add Hugging Face Inference Endpoint / TGI adapter.
- [x] Add runtime-specific chat model configuration validation.
- [x] Add timeout and provider-error mapping.
- [x] Add tagged OpenAI-compatible application-flow test with a local mock
      provider.
- [x] Add tagged Hugging Face TGI application-flow test with a local mock
      provider.
- [x] Keep real cloud provider tests out of default verification; add only as
      explicitly enabled manual smoke tests.

## Milestone 6: Embedded Offline Runtime

- [x] Decide the concrete llama.cpp integration mechanism.
- [x] Add GGUF model asset directory configuration.
- [x] Add offline model metadata support.
- [x] Add embedded offline model availability checker behind
      `ChatModelAvailabilityService`.
- [x] Extend embedded offline availability checks to verify configured model
      asset existence.
- [x] Extend embedded offline availability checks to verify model metadata
      validity.
- [x] Add embedded adapter behind `AiModelClient`.
- [x] Start managed `llama-server` processes for enabled embedded offline
      models using ephemeral loopback ports.
- [x] Introduce structured logging for embedded runtime lifecycle events and
      collect managed `llama-server` stdout and stderr.
- [x] Extend embedded offline availability checks to verify runtime loadability
      once the embedded adapter exists.
- [x] Add embedded offline availability tests for `GET /models`.
- [x] Add embedded offline availability tests for `POST /chat`.
- [x] Document that embedded offline availability requires static asset
      validation and managed `llama-server` readiness.

## Future Milestone: Docker Packaging And Acceptance

- [ ] Add Docker image and container-level acceptance test for embedded offline
      runtime with bundled or mounted model assets.
- [ ] Add per-model embedded llama prompt mode configuration, keeping
      plain `/completion` as the default and allowing `/v1/chat/completions`
      only for GGUF models with reliable chat-template metadata.

## Future Milestone: Frontend UI

- [x] Add a separate `frontend/` React + TypeScript + Vite project.
- [x] Add frontend linting, formatting, and test tooling.
- [x] Add Vitest for frontend unit and component tests.
- [x] Add React Testing Library for behavior-focused UI tests.
- [x] Add MSW for frontend API mocking.
- [x] Add Vitest V8 coverage reporting with an initial 70% coverage threshold.
- [x] Add an API client for `GET /models`.
- [x] Add an API client for `POST /chat`.
- [x] Build the first application screen around model selection and chat.
- [x] Show model display name, availability, runtime mode, runtime
      requirements, and whether prompts may leave the local machine.
- [x] Require selecting a model before sending a chat message.
- [x] Keep chat history in browser memory only.
- [x] Display loading, validation, unavailable-model, and provider-error
      states.
- [x] Add frontend tests for model loading, model selection, chat submission,
      and API error states.
- [x] Add Playwright end-to-end tests after the first usable UI screen exists.
- [x] Add local development configuration so the Vite dev server can call the
      Spring Boot API.
- [x] Add Gradle multi-project integration so frontend and backend build as
      separate subprojects.
- [x] Add configurable frontend backend API base URL with localhost default.
- [x] Add backend CORS configuration for the local frontend origin.
- [x] Add frontend verification to the default project verification workflow
      once the frontend project exists.
- [x] Document frontend development and production packaging in `README.md`.

## Future Milestone: Dynamic Subjects And Documents

- [ ] Add first-class structured CSV document support.
- [ ] Parse structured CSV rows into deterministic searchable chunks.
- [ ] Add multiple subject support.
- [ ] Add subject creation, update, and deletion.
- [ ] Add runtime document upload.
- [ ] Add runtime document deletion.
- [ ] Add tests for subject isolation.
