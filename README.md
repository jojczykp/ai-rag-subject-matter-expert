# AI Subject Matter Expert

AI Subject Matter Expert is a Kotlin Spring Boot backend with a separate React
and Vite frontend. The backend exposes REST endpoints for model discovery and
chat, plus Spring Boot Actuator health and info endpoints.

This project is vibe-coded with Codex using GPT-5, project agents, and
iteratively improved skills. The workflow starts by documenting requirements,
architecture, and decisions, then implements code according to that documented
plan.

## Requirements

Backend:

- JDK 26
- Kotlin 2.4.0
- Spring Boot 4.1.0
- Gradle 9.5.1 through the wrapper
- Gradle Wrapper included in this repository
- Kover for coverage verification
- Docker for PostgreSQL, Docker Compose, and Testcontainers integration tests

Frontend:

- Node.js 25.2.1
- npm 11.12.1
- React, TypeScript, and Vite

## Build and Run

Run commands from the repository root.

### Models (optional)

If not already present.

#### Local embedding model

```bash
mkdir -p backend/models/bge-small-en-v1.5
curl -L \
  https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/onnx/model.onnx \
  -o backend/models/bge-small-en-v1.5/model.onnx
curl -L \
  https://huggingface.co/BAAI/bge-small-en-v1.5/resolve/main/tokenizer.json \
  -o backend/models/bge-small-en-v1.5/tokenizer.json
```

#### Offline Llama server (optional)

If you want to use the bundled `embedded-llama-example` chat model:

```bash
mkdir -p backend/models/llama/models
curl -L \
  https://huggingface.co/QuantFactory/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/TinyLlama-1.1B-Chat-v1.0.Q4_K_M.gguf \
  -o backend/models/llama/models/llama.gguf
```

##### macOS Apple Silicon (optional)

```bash
mkdir -p backend/models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-macos-arm64.tar.gz \
  -o /tmp/llama-bin-macos-arm64.tar.gz
tar -xzf /tmp/llama-bin-macos-arm64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server backend/models/llama/bin/llama-server
chmod +x backend/models/llama/bin/llama-server
```

##### macOS Intel x64 (optional)

```bash
mkdir -p backend/models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-macos-x64.tar.gz \
  -o /tmp/llama-bin-macos-x64.tar.gz
tar -xzf /tmp/llama-bin-macos-x64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server backend/models/llama/bin/llama-server
chmod +x backend/models/llama/bin/llama-server
```

##### Linux Ubuntu x64 (optional)

```bash
mkdir -p backend/models/llama/bin
curl -L \
  https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-ubuntu-x64.tar.gz \
  -o /tmp/llama-bin-ubuntu-x64.tar.gz
tar -xzf /tmp/llama-bin-ubuntu-x64.tar.gz -C /tmp
cp /tmp/llama-b9892/llama-server backend/models/llama/bin/llama-server
chmod +x backend/models/llama/bin/llama-server
```

##### Windows x64 (optional)

```powershell
New-Item -ItemType Directory -Force backend\models\llama\bin | Out-Null
Invoke-WebRequest `
  -Uri https://github.com/ggml-org/llama.cpp/releases/download/b9892/llama-b9892-bin-win-cpu-x64.zip `
  -OutFile $env:TEMP\llama-bin-win-cpu-x64.zip
Expand-Archive -Force $env:TEMP\llama-bin-win-cpu-x64.zip $env:TEMP\llama-bin-win-cpu-x64
Copy-Item $env:TEMP\llama-bin-win-cpu-x64\llama-server.exe backend\models\llama\bin\llama-server.exe
```

When running on Windows, set
`aisme.embedded-llama.server-executable-path` to
`./models/llama/bin/llama-server.exe`.

#### Calculate the model checksum (optional)

Calculate and copy it into `aisme.embedded-llama.models[0].sha256`:

##### macOS

```bash
shasum -a 256 backend/models/llama/models/llama.gguf
```

##### Linux

```bash
sha256sum backend/models/llama/models/llama.gguf
```

##### Windows (PowerShell)

```powershell
Get-FileHash backend\models\llama\models\llama.gguf -Algorithm SHA256
```

The example embedded llama model is enabled in `application.yml`. It is
selectable when the configured GGUF file and `llama-server` executable are
present and pass startup availability checks.

### Ollama (optional)

If you want to use the local Ollama model, start Ollama and pull the configured
chat model if it is not already available:

```bash
ollama serve
```

In another terminal:

```bash
ollama pull llama3.2
ollama list
```

### Database

Start the database:

```bash
docker compose up -d db
```

The default local database connection is:

```text
jdbc:postgresql://localhost:5432/aisme
```

Override it with `AISME_DATASOURCE_URL`, `AISME_DATASOURCE_USERNAME`, and
`AISME_DATASOURCE_PASSWORD` when needed.

### Backend

#### Build

Docker must be running for the backend integration tests executed by the
backend build.

```bash
./gradlew :backend:build
```

#### Run

```bash
./gradlew :backend:run
```

The backend API and actuator endpoints are served on port `8080`.

#### Check API

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

The response includes each model's availability, capabilities, runtime
requirements, and whether prompts may leave the local machine.

Send a sample chat request:

```bash
curl http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "modelId": "local-ollama-llama",
    "message": "How should I cook rice?"
  }'
```

### Frontend

#### Build

Install frontend dependencies and build the production assets:

```bash
cd frontend
npm install
npm run build
```

Or build the frontend from the repository root through Gradle:

```bash
./gradlew :frontend:build
```

#### Run

```bash
npm run dev
```

Or run the frontend from the repository root through Gradle:

```bash
./gradlew :frontend:run
```

### Both

#### Build

Alternatively, having models downloaded, build both backend and frontend from the repository root:

```bash
./gradlew build
```

The root `build` task depends on `:backend:build` and `:frontend:build`.

#### Run

Run backend and frontend together from the repository root:

```bash
./gradlew --parallel run
```

This starts `:backend:run` and `:frontend:run` in parallel. The frontend Vite
server usually serves the UI at `http://localhost:5173` and calls the backend
API at `http://localhost:8080`.

## Development

### Backend

Run normal backend verification:

```bash
./gradlew :backend:check
```

`:backend:check` runs the backend test suite and Kover coverage verification.
Some tests named `*IntegrationTest` use Testcontainers and require Docker.

Docker is required to execute PostgreSQL/pgvector Testcontainers tests.

When debugging, run focused backend verification tasks directly:

```bash
./gradlew :backend:test
./gradlew :backend:koverVerify
```

Generate the backend HTML coverage report when a local report is useful:

```bash
./gradlew :backend:koverHtmlReport
```

The generated backend coverage report is available at
[backend/build/reports/kover/html/index.html](backend/build/reports/kover/html/index.html):

```text
backend/build/reports/kover/html/index.html
```

Optional Ollama container tests are tagged separately because they pull and
start the Ollama Docker image and may pull a small model for the model-backed
chat-flow test. Run them explicitly:

```bash
./gradlew :backend:ollamaTest
```

Optional OpenAI-compatible adapter flow tests are also tagged separately. They
use a local mock HTTP server, not a real cloud provider:

```bash
./gradlew :backend:openAiCompatibleTest
```

The default test image is `ollama/ollama:latest`, and the default model-backed
test model is `tinyllama:latest`. Override them when needed:

```bash
./gradlew :backend:ollamaTest \
  -Daisme.ollama.test.image=ollama/ollama:latest \
  -Daisme.ollama.test.model=tinyllama:latest
```

Run full project verification before final handoff when practical. The Gradle
root `check` task depends on `:backend:check` and `:frontend:check`:

```bash
./gradlew check
```

Playwright browser end-to-end tests are not part of the default Gradle `check`
task yet. Run them explicitly from `frontend/`:

```bash
cd frontend
npm run e2e
```

### Frontend

The frontend is a React, TypeScript, and Vite application in `frontend/`.

Run the development UI from the repository root:

```bash
./gradlew :frontend:run
```

The Gradle task installs dependencies with `npm ci` when needed and starts the
Vite development server.

Alternatively, run frontend npm commands directly from `frontend/`:

```bash
cd frontend
npm install
npm run dev
```

The Vite development server serves the UI on the printed local URL, usually
`http://localhost:5173`. The frontend calls the Spring Boot backend through
`VITE_BACKEND_API_BASE_URL`, which defaults to `http://localhost:8080`.

Override the backend URL when needed:

```bash
VITE_BACKEND_API_BASE_URL=http://localhost:8081 ./gradlew :frontend:run
```

Run frontend-only verification:

```bash
cd frontend
npm run format:check
npm run lint
npm run test
npm run test:coverage
npm run typecheck
npm run build
```

From the project root, run the frontend verification aggregate:

```bash
./gradlew :frontend:check
```

`npm run build` writes production frontend assets to `frontend/dist`. Generated
frontend assets remain build output and are not committed to source control.

Run browser end-to-end tests explicitly:

```bash
cd frontend
npm run e2e
```

After `npm run test:coverage`, the frontend coverage report is generated at
[frontend/coverage/index.html](frontend/coverage/index.html):

```text
frontend/coverage/index.html
```

The initial frontend coverage threshold is 70%.

## Configuration

### Reference

Application properties are configured under the `aisme` prefix.

| Property | Default | Description |
| --- | --- | --- |
| `aisme.documents.location` | `classpath:/subject-documents/` | Bundled document resource folder. |
| `aisme.api.cors.allowed-origins` | `http://localhost:5173` | Browser origins allowed to call the backend API. |
| `aisme.documents.chunk-size` | `700` | Maximum character count per indexed document chunk. |
| `aisme.documents.chunk-overlap` | `100` | Character overlap between adjacent chunks. Must be smaller than `chunk-size`. |
| `aisme.embedding-model.id` | `local-bge-small` | Stable embedding model identifier stored with embeddings. |
| `aisme.embedding-model.version` | `1.5` | Embedding model version stored with embeddings. |
| `aisme.embedding-model.dimensions` | `384` | Embedding vector dimension. |
| `aisme.embedding-model.runtime` | `ONNX` | Local embedding runtime. |
| `aisme.embedding-model.model-path` | `./models/bge-small-en-v1.5/model.onnx` | ONNX model file path. |
| `aisme.embedding-model.tokenizer-path` | `./models/bge-small-en-v1.5/tokenizer.json` | tokenizer file path. |
| `aisme.chat.timeout` | `60s` | Timeout for model chat generation. |
| `aisme.chat.relevant-chunk-limit` | `5` | Maximum number of retrieved chunks sent as chat context. |
| `aisme.chat-model-availability.timeout` | `5s` | Timeout for runtime availability checks. |
| `aisme.chat-model-availability.cache-ttl` | `5s` | Time to cache availability check results. |
| `aisme.embedded-llama.asset-directory` | `./models/llama` | Base directory for local embedded llama assets. |
| `aisme.embedded-llama.server-executable-path` | `./models/llama/bin/llama-server` | Path to the managed `llama-server` executable. |
| `aisme.embedded-llama.models[*].id` | required | Embedded llama model id. Should match the related `chat-models[*].id`. |
| `aisme.embedded-llama.models[*].enabled` | `true` in example config | Whether the embedded runtime should manage this model. |
| `aisme.embedded-llama.models[*].display-name` | required | Human-readable embedded model name. |
| `aisme.embedded-llama.models[*].gguf-file` | required | GGUF file path relative to `asset-directory`. |
| `aisme.embedded-llama.models[*].context-size` | required | Context size passed to `llama-server`. |
| `aisme.embedded-llama.models[*].runtime-arguments` | `[]` | Extra arguments passed to `llama-server`. |
| `aisme.embedded-llama.models[*].sha256` | optional | Lowercase SHA-256 checksum for the GGUF file. |
| `aisme.chat-models[*].id` | required | User-selected chat model id used in `/chat` requests. |
| `aisme.chat-models[*].enabled` | `true` in example config | Whether the chat model is visible and selectable. |
| `aisme.chat-models[*].config.display-name` | required when enabled | Human-readable model name. |
| `aisme.chat-models[*].config.description` | optional | Short model description for clients and selection UIs. |
| `aisme.chat-models[*].config.runtime` | required when enabled | Runtime adapter: `OLLAMA`, `OPENAI_COMPATIBLE`, `HUGGING_FACE_ENDPOINT`, `EMBEDDED_OFFLINE`, or `SPRING_AI`. |
| `aisme.chat-models[*].config.mode` | required when enabled | Runtime mode: `ONLINE`, `LOCAL_SERVER`, or `EMBEDDED_OFFLINE`. |
| `aisme.chat-models[*].config.available-offline` | required when enabled | Whether the model can answer without network access. |
| `aisme.chat-models[*].config.base-url` | runtime-specific | Provider base URL for Ollama, OpenAI-compatible, and Hugging Face endpoint models. |
| `aisme.chat-models[*].config.model-name` | runtime-specific | Provider model name for Ollama and OpenAI-compatible models. |
| `aisme.chat-models[*].config.api-key` | runtime-specific | Provider API key. OpenAI-compatible online models are `MISCONFIGURED` when this is missing. |

Runtime and mode combinations are intentionally narrow in the initial scope:
`OLLAMA` uses `LOCAL_SERVER`, `OPENAI_COMPATIBLE` and `HUGGING_FACE_ENDPOINT`
use `ONLINE`, `EMBEDDED_OFFLINE` uses `EMBEDDED_OFFLINE`, and `SPRING_AI` uses
`ONLINE`.

### Local Embedding Model

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

### Embedded Llama Assets

Embedded offline chat will use local llama.cpp assets. The default asset
configuration points to external files outside the application JAR:

```yaml
aisme:
  embedded-llama:
    asset-directory: ./models/llama
    server-executable-path: ./models/llama/bin/llama-server
    models:
      - id: embedded-llama-example
        enabled: true
        display-name: Embedded Llama
        gguf-file: models/llama.gguf
        context-size: 4096
        runtime-arguments: []
```

The example embedded llama model is enabled by default. It becomes selectable
only when its local llama.cpp runtime assets are installed and pass startup
availability checks. `asset-directory` is the base directory for local GGUF
model files and related metadata. `server-executable-path` points to the local
`llama-server` binary. For enabled embedded models, the application starts one
managed llama-server process per model on an ephemeral loopback port and sends
chat requests to its OpenAI-compatible `/v1/chat/completions` endpoint. Model
metadata describes GGUF files relative to `asset-directory`, runtime arguments,
and optional checksums.

Embedded offline asset availability is checked when the application starts.
After changing local GGUF files or the configured `llama-server` binary, restart
the application to refresh embedded offline availability. If model metadata
includes `sha256`, the GGUF checksum is verified during that startup check.
The managed local `llama-server` health endpoint must become ready before an
embedded model is reported as available.
Managed `llama-server` lifecycle events and process stdout/stderr are written
to the application logs.

### Local Ollama Model

The default chat model entry points to Ollama at `http://localhost:11434`:

```yaml
aisme:
  chat-models:
    - id: local-ollama-llama
      enabled: true
      config:
        display-name: Local Ollama Llama
        description: Local Ollama model for chat requests when Ollama is running on this machine.
        runtime: OLLAMA
        mode: LOCAL_SERVER
        available-offline: false
        base-url: http://localhost:11434
        model-name: llama3.2
```

The application-owned `aisme.chat-models` configuration is the source of truth
for selectable chat models. Spring AI Ollama settings are handled by the Ollama
adapter instead of duplicated in a separate Spring profile.

### OpenAI-Compatible Cloud Model

OpenAI-compatible chat providers can be configured as selectable online models:

```yaml
aisme:
  chat-models:
    - id: cloud-gpt
      enabled: true
      config:
        display-name: Cloud GPT
        description: Online OpenAI-compatible model for cloud-hosted chat requests.
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

### Hugging Face Inference Endpoint / TGI Model

Hugging Face Inference Endpoints can be configured as selectable online models:

```yaml
aisme:
  chat-models:
    - id: hf-mistral
      enabled: true
      config:
        display-name: Hugging Face Mistral
        description: Online Hugging Face endpoint using the TGI-compatible generate API.
        runtime: HUGGING_FACE_ENDPOINT
        mode: ONLINE
        available-offline: false
        base-url: https://example.endpoints.huggingface.cloud
        api-key: ${HF_API_KEY}
```

The adapter sends non-streaming TGI-compatible requests to `/generate`.
If `HF_API_KEY` is not set, no bearer token is sent.

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
- [ADR-008: Operational Logging](docs/ADR-008-operational-logging.md)
- [ADR-009: Frontend UI Architecture](docs/ADR-009-frontend-ui-architecture.md)
- [Database Schema](docs/DATABASE_SCHEMA.md)
