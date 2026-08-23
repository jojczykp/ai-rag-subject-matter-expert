# ADR-010: Container Packaging

## Status

Accepted.

## Context

The application should be runnable on a workstation that has Docker but does
not have Java, Node.js, npm, or Gradle installed. Users should build and start
the service with one Compose command, access the UI and API through one HTTP
port, and retain downloaded model assets and indexed data across application
image upgrades.

The application requires PostgreSQL + pgvector, runs ONNX embeddings in the
JVM, manages `llama-server` child processes for embedded GGUF chat models, and
can optionally use Ollama. Model weights are several gigabytes and should not
make every application image rebuild equally large.

## Decision

Package the production UI and backend as one application image and run
PostgreSQL + pgvector as a separate Compose service.

The application image will:

- use separate Node and JDK builder stages;
- package the compiled React assets into Spring Boot static resources;
- use a minimal JRE runtime stage without Node.js, npm, Gradle, a JDK, source
  code, or compiler tooling;
- include a pinned, checksum-verified `llama-server` and its shared libraries;
- build natively for Linux AMD64 and Linux ARM64;
- run as a non-root user; and
- expose Spring Boot port `8080` and PostgreSQL port `5432` for demo access.

Compose will:

- publish PostgreSQL port `5432` for convenient demo and local development
  access;
- persist PostgreSQL data, application model weights, and Ollama data in
  separate named volumes;
- start the application only after PostgreSQL is healthy;
- offer Ollama and one-shot model initialization through the `ollama` profile;
  and
- stop the application with enough grace time to terminate managed
  `llama-server` processes.

Model weights remain outside the image and are downloaded into the application
model volume on first startup. The executable `llama-server` remains in the
immutable image. Air-gapped installations can pre-populate the model volume; a
large image containing weights is not the default artifact.

The default Compose run activates the `container,minimal` Spring profiles. The
minimal profile enables local BGE embeddings and one embedded Qwen model. Full
mode activates `container,full`, inherits the complete base catalog, and starts
the Compose `ollama` profile so required Ollama models are initialized.

## Rationale

Serving static React assets through Spring Boot provides one public HTTP origin
without adding nginx or a Node runtime. Keeping PostgreSQL separate follows the
single-process container model and gives the database independent health,
persistence, and upgrade behavior.

Baking model weights into the standard image would make application releases
multi-gigabyte artifacts and force model layers through every image build and
registry transfer. Keeping executable runtime code in the image while storing
mutable weights in a volume preserves reproducibility without sacrificing
upgrade speed.

## Consequences

- `docker compose up --build` is the supported minimal container workflow.
- Full mode requires substantially more download time, disk space, and memory.
- Full mode uses the optional Ollama services because the base catalog includes
  an Ollama embedding model needed during indexing.
- The frontend uses same-origin API URLs in production while retaining the
  localhost backend default during Vite development.
- First startup is not ready until required weights are downloaded and bundled
  documents are indexed.
- Docker image builds require network access for builder dependencies and the
  pinned `llama-server` archive.
- PostgreSQL is intentionally published for the demo; Ollama and managed
  inference ports remain internal.
- Container-level acceptance coverage should continue to verify both supported
  Linux architectures, first-start downloads, volume reuse, readiness, and
  graceful shutdown.

## Alternatives Considered

### Separate Frontend Container

Running the frontend behind nginx would add another runtime, proxy
configuration, and health boundary without providing value for the current
single-release application. It remains an option if the UI later moves to a CDN
or gains an independent deployment lifecycle.

### PostgreSQL In The Application Container

Bundling PostgreSQL would require process supervision and couple database
persistence and upgrades to application replacement. This option is rejected.

### Model Weights In The Standard Image

This would improve fully offline startup but produce very large images and
expensive upgrades. A separately published offline image may be considered if
there is a concrete distribution requirement.

### Ollama In The Application Container

Ollama owns a separate server lifecycle and model store. It therefore remains
an optional Compose service rather than a second process in the application
container.
