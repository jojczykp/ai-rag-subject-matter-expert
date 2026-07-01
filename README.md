# AI Subject Matter Expert

AI Subject Matter Expert is a Kotlin Spring Boot service. The current
application exposes Spring Boot Actuator health and info endpoints.

This project is vibe-coded with Codex using GPT-5, project agents, and
iteratively improved skills. The workflow starts by documenting requirements,
architecture, and decisions, then implements code according to that documented
plan.

## Requirements

- JDK 26
- Gradle Wrapper included in this repository

The Gradle build uses the Java 26 toolchain, Kotlin 2.4.0, Spring Boot 4.1.0,
and Kover for coverage verification. The wrapper currently uses Gradle 9.5.1.

## Build And Run

Run commands from the repository root.

Start the database:

```bash
docker compose up -d db
```

Download the local embedding model if it is not already present:

```bash
mkdir -p models/bge-small-en-v1.5
curl -L \
  https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/onnx/model.onnx \
  -o models/bge-small-en-v1.5/model.onnx
curl -L \
  https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/tokenizer.json \
  -o models/bge-small-en-v1.5/tokenizer.json
```

Run the application:

```bash
./gradlew bootRun
```

The service starts on the default Spring Boot port, `8080`.

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

Actuator endpoints exposed over HTTP:

- `/actuator/health`
- `/actuator/info`

The default local database connection is:

```text
jdbc:postgresql://localhost:5432/aisme
```

Override it with `AISME_DATASOURCE_URL`, `AISME_DATASOURCE_USERNAME`, and
`AISME_DATASOURCE_PASSWORD` when needed.

## Local Embedding Model

The default embedding runtime is local ONNX. Model files are configured outside
the application JAR and are ignored by git:

```yaml
aisme:
  embedding-model:
    model-path: ./models/bge-small-en-v1.5/model.onnx
    tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json
```

The example uses [BAAI/bge-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5).
See `Build And Run` for the download commands.

These files are local runtime assets. They are intentionally excluded from git
because model binaries are large and can be replaced independently from
application code.

The ONNX client loads these files during startup, so the application fails fast
when the configured model or tokenizer file is missing.

## Local Ollama Model

The default chat model entry points to Ollama at `http://localhost:11434`:

```yaml
aisme:
  chat-models:
    - id: local-ollama-llama
      runtime: OLLAMA
      mode: LOCAL_SERVER
      base-url: http://localhost:11434
      model-name: llama3.2
```

The application-owned `aisme.chat-models` configuration is the source of truth
for selectable chat models. Spring AI Ollama settings are handled by the future
Ollama adapter instead of duplicated in a separate Spring profile.

## Verification

Run the unit tests:

```bash
./gradlew test
```

Some tests named `*IntegrationTest` use Testcontainers and run through the same
test task when Docker is available.

Docker is required to execute PostgreSQL/pgvector Testcontainers tests.

This project uses Kover for code coverage and enforces a minimum 80% coverage
threshold for production code:

```bash
./gradlew koverVerify
```

Generate the HTML coverage report when a local report is useful:

```bash
./gradlew koverHtmlReport
```

Run the full Gradle check before final handoff when practical. The `check` task
depends on coverage verification:

```bash
./gradlew check
```

## Run

To view the models available:

```bash
curl http://localhost:8080/models
```


To send sample query:

```bash
curl http://localhost:8080/chat -d '
{
  "modelId": "local-ollama-llama",
  "message": "How should I cook rice?"
}'
```


## Project Agents

Project-scoped Codex agents live in `.codex/agents/` and are described in
`AGENTS.md`.

- `orchestrator` coordinates end-to-end feature work across the other agents.
- `architect` proposes system design changes and implementation plans.
- `developer` implements production code and fixes production defects.
- `tester` creates tests, runs verification, and triages failures.
- `documenter` keeps Markdown documentation and project guidance in sync.

## Design Documents

- [Product Requirements Document](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [ADR-001: Embedding Generation Strategy](docs/ADR-001-embedding-generation-strategy.md)
- [ADR-002: Persistence And Vector Search](docs/ADR-002-persistence-and-vector-search.md)
- [ADR-003: Model Runtime Integration](docs/ADR-003-model-runtime-integration.md)
- [ADR-004: Integration Testing Strategy](docs/ADR-004-integration-testing-strategy.md)
- [ADR-005: Subject Document Scope](docs/ADR-005-subject-document-scope.md)
- [ADR-006: Local Embedding Runtime](docs/ADR-006-local-embedding-runtime.md)
