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

## Llama Runtime Assets

Embedded offline chat will use local llama.cpp assets. The default asset
configuration points to external files outside the application JAR:

```yaml
aisme:
  llama-runtime:
    enabled: false
    config:
      asset-directory: ./models/llama
      server-executable-path: ./bin/llama-server
      host: 127.0.0.1
      port: 18080
      models:
        - id: llama-runtime-example
          display-name: Embedded Llama
          gguf-file: models/llama.gguf
          context-size: 4096
          runtime-arguments: []
          license: TODO
          hardware-requirements: TODO
```

Set `enabled` to `true` only when the local llama.cpp runtime assets are
installed. `config.asset-directory` is the base directory for local GGUF model
files and related metadata. `config.server-executable-path` points to the local
`llama-server` binary that the application will manage when the embedded runtime
is implemented. Model metadata describes GGUF files relative to
`config.asset-directory`, runtime arguments, optional checksum, license, and
hardware requirements.

Embedded offline asset availability is checked when the application starts.
After changing local GGUF files or the configured `llama-server` binary, restart
the application to refresh embedded offline availability. If model metadata
includes `sha256`, the GGUF checksum is verified during that startup check.

## Local Ollama Model

The default chat model entry points to Ollama at `http://localhost:11434`:

```yaml
aisme:
  chat-models:
    - id: local-ollama-llama
      enabled: true
      config:
        display-name: Local Ollama Llama
        runtime: OLLAMA
        mode: LOCAL_SERVER
        available-offline: false
        base-url: http://localhost:11434
        model-name: llama3.2
```

The application-owned `aisme.chat-models` configuration is the source of truth
for selectable chat models. Spring AI Ollama settings are handled by the Ollama
adapter instead of duplicated in a separate Spring profile.

## OpenAI-Compatible Cloud Model

OpenAI-compatible chat providers can be configured as selectable online models:

```yaml
aisme:
  chat-models:
    - id: cloud-gpt
      enabled: true
      config:
        display-name: Cloud GPT
        runtime: OPENAI_COMPATIBLE
        mode: ONLINE
        available-offline: false
        base-url: https://api.openai.com/v1
        model-name: gpt-4.1-mini
        api-key: ${OPENAI_API_KEY}
```

The adapter sends non-streaming chat-completion requests to
`/chat/completions` with bearer-token authentication.

## Hugging Face Inference Endpoint / TGI Model

Hugging Face Inference Endpoints or self-hosted TGI-compatible servers can be
configured as selectable online models:

```yaml
aisme:
  chat-models:
    - id: hf-mistral
      enabled: true
      config:
        display-name: Hugging Face Mistral
        runtime: HUGGING_FACE_ENDPOINT
        mode: ONLINE
        available-offline: false
        base-url: https://example.endpoints.huggingface.cloud
        api-key: ${HF_API_KEY}
```

The adapter sends non-streaming TGI-compatible requests to `/generate`.
`api-key` is optional for unsecured local TGI servers.

## Verification

Run the unit tests:

```bash
./gradlew test
```

Some tests named `*IntegrationTest` use Testcontainers and run through the same
test task when Docker is available.

Docker is required to execute PostgreSQL/pgvector Testcontainers tests.

Optional Ollama container tests are tagged separately because they pull and
start the Ollama Docker image and may pull a small model for the model-backed
chat-flow test. Run them explicitly:

```bash
./gradlew ollamaTest
```

Optional OpenAI-compatible adapter flow tests are also tagged separately. They
use a local mock HTTP server, not a real cloud provider:

```bash
./gradlew openAiCompatibleTest
```

The default test image is `ollama/ollama:latest`, and the default model-backed
test model is `tinyllama:latest`. Override them when needed:

```bash
./gradlew ollamaTest \
  -Daisme.ollama.test.image=ollama/ollama:latest \
  -Daisme.ollama.test.model=tinyllama:latest
```

This project uses Kover for code coverage and enforces a minimum 80% coverage
threshold for production code:

```bash
./gradlew koverVerify
```

Generate the HTML coverage report when a local report is useful:

```bash
./gradlew koverHtmlReport
```

The generated report is available at:

```text
build/reports/kover/html/index.html
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
- [ADR-007: Llama Runtime](docs/ADR-007-llama-runtime.md)
