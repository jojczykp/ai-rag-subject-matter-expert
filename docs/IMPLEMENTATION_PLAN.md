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

- [x] Add Flyway migrations under `src/main/resources/db/migration`.
- [x] Add PostgreSQL + pgvector persistence support.
- [x] Add `source_document`, `document_chunk`, and `chunk_embedding` schema.
- [x] Add Spring Data JDBC repositories for ordinary persistence.
- [x] Add Spring `JdbcClient` queries for pgvector retrieval.
- [x] Add statically configured `aisme.embedding-model` configuration.
- [ ] Add local ONNX embedding model integration.
- [ ] Configure local model and tokenizer paths for `BAAI/bge-small-en-v1.5`.
- [ ] Store embedding model id, version, dimensions, and chunking strategy
      version with indexed embeddings.
- [ ] Add re-indexing behavior for stale or missing embeddings.
- [x] Add `RelevantChunkRetriever`.
- [x] Add Testcontainers integration tests with the pgvector image selected in
      ADR-002.

## Milestone 3: Provider-Neutral Chat API

- [ ] Add provider-neutral chat request and response DTOs.
- [ ] Add `AiModelClient` interface.
- [ ] Add fake model client for deterministic application-flow tests.
- [ ] Add `ModelRegistry` backed by static `aisme.models` configuration.
- [ ] Require `modelId` on chat requests.
- [ ] Add `GET /models`.
- [ ] Add `POST /chat`.
- [ ] Add `AiChatService`.
- [ ] Add model availability status and privacy labels to model responses.
- [ ] Support `CONFIGURED`, `AVAILABLE`, `UNAVAILABLE`, and `MISCONFIGURED`
      model availability states.
- [ ] Add consistent error responses with `code`, `message`, and optional
      `details`.
- [ ] Add configurable chat timeout with `60s` default.
- [ ] Add configurable model availability timeout with `5s` default.
- [ ] Do not retry chat generation automatically.
- [ ] Add integration tests for `/models`, `/chat`, static document indexing,
      retrieval, and fake model routing.
- [ ] Keep Kover coverage at or above 80% in default verification.

## Milestone 4: Local Ollama Runtime

- [ ] Add Spring AI Ollama dependency or adapter dependency selected during
      implementation.
- [ ] Add local profile configuration.
- [ ] Add Ollama adapter behind `AiModelClient`.
- [ ] Support Ollama running at `http://localhost:11434`.
- [ ] Support user-configured Ollama base URLs.
- [ ] Add model availability checks.
- [ ] Add MockServer or fake-client tests for adapter behavior where practical.
- [ ] Add optional tagged Testcontainers Ollama tests.

## Milestone 5: Cloud Runtime Adapters

- [ ] Add OpenAI-compatible cloud adapter.
- [ ] Add Hugging Face Inference Endpoint / TGI adapter.
- [ ] Add cloud profile configuration.
- [ ] Add credential validation.
- [ ] Add timeout and provider-error mapping.
- [ ] Add Testcontainers MockServer tests for cloud provider request/response
      mapping.
- [ ] Keep real cloud provider tests as explicitly enabled manual smoke tests
      only.

## Milestone 6: Embedded Offline Runtime

- [ ] Decide the concrete llama.cpp integration mechanism.
- [ ] Add GGUF model asset directory configuration.
- [ ] Add offline model metadata support.
- [ ] Add embedded adapter behind `AiModelClient`.
- [ ] Add model checksum validation.
- [ ] Add license and hardware requirement metadata.
- [ ] Add offline profile configuration.
- [ ] Add offline startup tests with lightweight fakes.
- [ ] Add optional tagged embedded runtime tests when a practical fixture
      exists.

## Future Milestone: Dynamic Subjects And Documents

- [ ] Add first-class structured CSV document support.
- [ ] Parse structured CSV rows into deterministic searchable chunks.
- [ ] Add multiple subject support.
- [ ] Add subject creation, update, and deletion.
- [ ] Add runtime document upload.
- [ ] Add runtime document deletion.
- [ ] Add tests for subject isolation.
