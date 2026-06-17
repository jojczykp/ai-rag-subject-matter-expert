# ADR-002: Persistence And Vector Search

## Status

Accepted.

## Context

The application needs to store static document metadata, extracted chunks,
embeddings, and retrieval-related metadata. Retrieval depends on efficient
similarity search over embeddings, while ordinary application data should remain
simple to read, test, and evolve.

The persistence approach should fit Spring Boot, support PostgreSQL-specific
vector search, and avoid unnecessary ORM complexity.

## Decision

Use PostgreSQL with pgvector for document chunks, embeddings, and similarity
search. Use Flyway for schema migrations. Use Spring Data JDBC for ordinary
persistence and Spring `JdbcClient` for explicit pgvector queries.

Use a versioned pgvector Docker image for local and integration-test databases.
The selected initial image is `pgvector/pgvector:0.8.2-pg18`, matching the
latest pgvector version verified when this ADR was accepted.

## Selected Stack

- [ ] PostgreSQL as the application database.
- [ ] pgvector for embedding storage and vector similarity search.
- [ ] Flyway for database schema migrations.
- [ ] Spring Data JDBC for ordinary persistence of simple application data.
- [ ] Spring `JdbcClient` for explicit pgvector queries in
      `RelevantChunkRetriever`.
- [ ] Flyway migrations under `src/main/resources/db/migration`.
- [ ] Testcontainers with `pgvector/pgvector:0.8.2-pg18` for persistence
      integration tests.

## Schema Shape

Use separate tables for source documents, chunks, and embeddings:

```text
source_document
document_chunk
chunk_embedding
```

Keep chunk text and embedding vectors separate because chunk metadata and vector
data change for different reasons. If the embedding model, dimensions, or
chunking strategy changes, embeddings can be rebuilt without rewriting all
document and chunk metadata.

Initial table responsibilities:

- [ ] `source_document` stores bundled resource identity and document metadata.
- [ ] `document_chunk` stores extracted chunk text and chunk metadata.
- [ ] `chunk_embedding` stores one vector per chunk for the statically
      configured embedding model.
- [ ] `chunk_embedding` stores embedding model id, embedding model version,
      embedding dimensions, and chunking strategy version.

Because ADR-001 selects one statically configured embedding model, the initial
schema should use one embedding table rather than provider-specific embedding
tables.

## Vector Search

- [ ] Use cosine distance as the initial similarity metric.
- [ ] Start with exact vector search.
- [ ] Add an approximate pgvector index, such as HNSW, only when data size or
      latency makes it necessary.
- [ ] Define the pgvector column dimensions from the statically configured
      embedding model.
- [ ] Treat embedding dimension changes as a migration and re-indexing event.

Exact search is preferred initially because the first product scope uses static
bundled `.txt` documents. It keeps the first implementation simpler and easier
to verify before introducing approximate-search tuning.

## Indexing And Transactions

- [ ] Read bundled documents from application resources.
- [ ] Create or update `source_document` and `document_chunk` rows during
      indexing.
- [ ] Create embeddings when they are missing or stale.
- [ ] Treat embeddings as stale when embedding model id, embedding model
      version, embedding dimensions, or chunking strategy version changes.
- [ ] Write document metadata and chunks transactionally.
- [ ] Rebuild embeddings transactionally per document or per indexing run.
- [ ] Avoid marking a new embedding version active until its indexing work has
      completed successfully.

## Rationale

Spring Data JDBC fits the existing Spring Boot application without introducing a
heavy ORM model. It is enough for simple tables such as document metadata,
chunks, model metadata, and future chat history if that becomes a product
requirement.

Vector retrieval should stay SQL-visible. pgvector queries depend on
PostgreSQL-specific operators, indexes, ordering, and limits. Keeping those
queries in `JdbcClient` makes retrieval behavior easier to inspect, tune, and
test.

## Options Considered

### Option 1: Spring Data JDBC Plus JdbcClient

Benefits:

- [ ] Integrates naturally with Spring Boot configuration, transactions, and
      tests.
- [ ] Avoids JPA lazy-loading and persistence-context complexity.
- [ ] Allows simple repository-style persistence where useful.
- [ ] Keeps pgvector SQL explicit where precision matters.

Tradeoffs:

- [ ] Requires hand-written SQL for vector search.
- [ ] Provides less automatic relationship management than JPA.

This is the selected option.

### Option 2: JPA

Benefits:

- [ ] Strong fit for complex relational domains with rich relationships.
- [ ] Common Spring persistence abstraction.

Tradeoffs:

- [ ] Adds ORM behavior that is not needed for the initial data model.
- [ ] PostgreSQL-specific vector queries are less natural through ORM mapping.
- [ ] Can obscure SQL behavior that should remain visible for retrieval.

JPA is not the default choice unless the domain later needs richer relational
mapping behavior.

### Option 3: Jdbi

Benefits:

- [ ] Good SQL-first persistence library.
- [ ] Provides direct control over queries and mapping.

Tradeoffs:

- [ ] Adds another persistence framework beside Spring Data.
- [ ] Requires more project-specific integration decisions in a Spring Boot app.
- [ ] Provides less value while the application can use Spring JDBC tools
      directly.

Jdbi should only be added if the project intentionally moves toward a SQL-first
persistence style outside Spring Data conventions.

## Consequences

- [ ] Schema changes go through Flyway migrations.
- [ ] Vector search SQL stays close to `RelevantChunkRetriever`.
- [ ] Embedding model metadata is stored with indexed document chunks.
- [ ] Document chunks are re-indexed when the configured embedding model
      changes.
- [ ] Document chunks are re-indexed when embedding dimensions or chunking
      strategy changes.
- [ ] Integration tests verify migrations, chunk persistence, and vector
      similarity queries with Testcontainers.
