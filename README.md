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

## Build

Run commands from the repository root.

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

Download example offline llama assets if you want to enable the bundled
`embedded-llama-example` chat model:

```bash
mkdir -p models/llama/models
curl -L \
  https://huggingface.co/QuantFactory/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/TinyLlama-1.1B-Chat-v1.0.Q4_K_M.gguf \
  -o models/llama/models/llama.gguf
```

Download a prebuilt `llama-server` for macOS Apple Silicon and copy it into the
configured project-local path:

```bash
mkdir -p models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-macos-arm64.tar.gz \
  -o /tmp/llama-bin-macos-arm64.tar.gz
tar -xzf /tmp/llama-bin-macos-arm64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server models/llama/bin/llama-server
chmod +x models/llama/bin/llama-server
```

For macOS Intel, use the x64 archive instead:

```bash
mkdir -p models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-macos-x64.tar.gz \
  -o /tmp/llama-bin-macos-x64.tar.gz
tar -xzf /tmp/llama-bin-macos-x64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server models/llama/bin/llama-server
chmod +x models/llama/bin/llama-server
```

Download a prebuilt `llama-server` for Linux Ubuntu x64:

```bash
mkdir -p models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-ubuntu-x64.tar.gz \
  -o /tmp/llama-bin-ubuntu-x64.tar.gz
tar -xzf /tmp/llama-bin-ubuntu-x64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server models/llama/bin/llama-server
chmod +x models/llama/bin/llama-server
```

On Windows PowerShell, download the CPU x64 archive and copy
`llama-server.exe`:

```powershell
New-Item -ItemType Directory -Force models\llama\bin | Out-Null
Invoke-WebRequest `
  -Uri https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-win-cpu-x64.zip `
  -OutFile $env:TEMP\llama-bin-win-cpu-x64.zip
Expand-Archive -Force $env:TEMP\llama-bin-win-cpu-x64.zip $env:TEMP\llama-bin-win-cpu-x64
Copy-Item $env:TEMP\llama-bin-win-cpu-x64\llama-server.exe models\llama\bin\llama-server.exe
```

When running on Windows, set
`aisme.embedded-llama.server-executable-path` to
`./models/llama/bin/llama-server.exe`.

Optionally calculate the model checksum and copy it into
`aisme.embedded-llama.models[0].sha256`:

```bash
# macOS
shasum -a 256 models/llama/models/llama.gguf
```

```bash
# Linux
sha256sum models/llama/models/llama.gguf
```

```powershell
# Windows PowerShell
Get-FileHash models\llama\models\llama.gguf -Algorithm SHA256
```

To make the example model selectable, set both
`aisme.embedded-llama.models[0].enabled` and the
`embedded-llama-example` chat model entry to `true`.

Build the application:

```bash
./gradlew build
```

## Run

Start the database:

```bash
docker compose up -d db
```

Start Ollama locally and pull the configured chat model if it is not already
available:

```bash
ollama serve
```

In another terminal:

```bash
ollama pull llama3.2
ollama list
```

Run the application:

```bash
./gradlew bootRun
```

The service starts on the default Spring Boot port, `8080`.

Check application health:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

View application info:

```bash
curl http://localhost:8080/actuator/info
```

View the configured models:

```bash
curl http://localhost:8080/models
```

Send a sample chat request:

```bash
curl http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "modelId": "local-ollama-llama",
    "message": "How should I cook rice?"
  }'
```

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
See `Build` for the download commands.

These files are local runtime assets. They are intentionally excluded from git
because model binaries are large and can be replaced independently from
application code.

The ONNX client loads these files during startup, so the application fails fast
when the configured model or tokenizer file is missing.

## Embedded Llama Assets

Embedded offline chat will use local llama.cpp assets. The default asset
configuration points to external files outside the application JAR:

```yaml
aisme:
  embedded-llama:
    asset-directory: ./models/llama
    server-executable-path: ./models/llama/bin/llama-server
    models:
      - id: embedded-llama-example
        enabled: false
        display-name: Embedded Llama
        gguf-file: models/llama.gguf
        context-size: 4096
        runtime-arguments: []
```

Set a model's `enabled` to `true` only when its local llama.cpp runtime assets
are installed. `asset-directory` is the base directory for local GGUF model
files and related metadata. `server-executable-path` points to the local
`llama-server` binary. For enabled embedded models, the application starts one
managed llama-server process per model on an ephemeral loopback port and sends
chat requests to its OpenAI-compatible `/v1/chat/completions` endpoint. Model
metadata describes GGUF files relative to `asset-directory`, runtime
arguments, and optional checksums.

Embedded offline asset availability is checked when the application starts.
After changing local GGUF files or the configured `llama-server` binary, restart
the application to refresh embedded offline availability. If model metadata
includes `sha256`, the GGUF checksum is verified during that startup check.
The managed local `llama-server` health endpoint must become ready before an
embedded model is reported as available.
Managed `llama-server` lifecycle events and process stdout/stderr are written
to the application logs.

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
`/chat/completions` with bearer-token authentication. If `OPENAI_API_KEY` is
not set, the application still starts, but the model is reported as
`MISCONFIGURED` and cannot be used for chat.

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
`api-key` is optional for unsecured local TGI servers. If `HF_API_KEY` is not
set, no bearer token is sent.

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
- [ADR-007: Embedded Llama](docs/ADR-007-embedded-llama.md)
