# AI RAG Subject Matter Expert

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

### Models

May skip if not already present and correct.

#### Embedding model

Remove locally downloaded embedding model files first:

```bash
./gradlew :backend:cleanEmbeddingModel
```

Download:

```bash
./gradlew :backend:embeddingModelDownload
```

#### Embedded local models on llama-server

If you want to use the bundled `embedded-qwen-0-5b`,
`embedded-qwen-1-5b`, `embedded-qwen-3b`, and `embedded-mistral-7b`
chat models.

Remove downloaded model first if exists:

```bash
./gradlew :backend:cleanEmbeddedLlamaModel
```

Download models:

```bash
./gradlew :backend:embeddedLlamaDownloadModel
```

To download only one embedded model, use the specific task:

```bash
./gradlew :backend:embeddedLlamaDownloadQwen0p5BModel
./gradlew :backend:embeddedLlamaDownloadQwen1p5BModel
./gradlew :backend:embeddedLlamaDownloadQwen3BModel
./gradlew :backend:embeddedLlamaDownloadMistral7BModel
```

Remove `llama-server` for the current platform first if exists:

```bash
./gradlew :backend:cleanEmbeddedLlamaServer
```

Download and verify the `llama-server` for the current platform:

```bash
./gradlew :backend:embeddedLlamaDownloadServer
```

If platform auto-detection does not match your environment, run the matching
explicit task:

```bash
./gradlew :backend:embeddedLlamaDownloadServerMacAppleSilicon
./gradlew :backend:embeddedLlamaDownloadServerMacIntel
./gradlew :backend:embeddedLlamaDownloadServerLinuxUbuntuX64
./gradlew :backend:embeddedLlamaDownloadServerWindowsX64
```

### Ollama (optional)

If you want to use the local Ollama model, start Ollama and pull the configured
chat model if it is not already available:

```bash
ollama serve
```

In another terminal:

```bash
ollama pull llama3.2
```

```bash
ollama list
```

### Database

Make sure previous instance is no longer running:

```bash
docker compose ps
```

Remove if necessary:

```bash
docker compose down
```

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

To view logs (i.e. last 100 lines):

```bash
docker compose logs --tail=100 db
```

To follow logs:

```bash
docker compose logs -f db
```

To stop:

```bash
docker compose stop db
```

To restart later:

```bash
docker compose start db
```

### Backend

#### Build

Docker must be running for the backend integration tests executed by the
backend build.

```bash
./gradlew :backend:clean :backend:build
```

#### Run

```bash
./gradlew :backend:run
```

#### Check API

The backend API and actuator endpoints are served on port `8080`.

Check application health:

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

Expected response:

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"],
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  },
  ...
}
```

For some period of time you may notice `OUT_OF_SERVICE` status, this means indexing is still in progress.

View application info:

```bash
curl -s http://localhost:8080/actuator/info | jq .
```

Expected response:

```json
{
  "app": {
    "name": "AI RAG Subject Matter Expert",
    "description": "Backend RAG application for subject-matter chat."
  }
}
```

View the configured models:

```bash
curl -s http://localhost:8080/chat-models | jq .
```

The response includes each model's availability, capabilities, runtime
requirements, and whether prompts may leave the local machine.

View the configured embedding models:

```bash
curl -s http://localhost:8080/embedding-models | jq .
```

The response includes configured embedding model ids, runtime type, dimensions,
and whether the model is currently enabled for indexing.

Send a sample chat request:

```bash
curl -s http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "modelId": "local-ollama-llama",
    "message": "How should I cook rice?"
  }' | jq .
```

### Frontend

#### Build

Install frontend dependencies and build the production assets:

```bash
cd frontend
npm run clean
npm ci
npm run format:check
npm run lint
npm run test:coverage
npm run build
```

Or build the frontend from the repository root through Gradle:

```bash
./gradlew :frontend:clean :frontend:build
```

#### Run

```bash
cd frontend
npm ci
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

Optional Hugging Face TGI adapter flow tests are tagged the same way. They use
a local mock HTTP server, not a real Hugging Face endpoint:

```bash
./gradlew :backend:huggingFaceTgiTest
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
npm ci
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
npm run clean
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

`npm run clean` empties `frontend/dist`, and `npm run build` also empties it
before writing production frontend assets. Generated frontend assets remain
build output and are not committed to source control.

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
| `aisme.embedding.runtimes.<runtime-id>.type` | required | Embedding runtime adapter: `ONNX` or `OLLAMA`. |
| `aisme.embedding.runtimes.<runtime-id>.base-url` | Ollama only | Ollama server base URL for embedding generation. |
| `aisme.embedding.models.<model-id>.enabled` | `true` in example config | Whether this embedding model is active. Exactly one model must be enabled. |
| `aisme.embedding.models.<model-id>.display-name` | optional | Human-readable embedding model name for API clients. |
| `aisme.embedding.models.<model-id>.version` | required when enabled | Embedding model version stored with embeddings. |
| `aisme.embedding.models.<model-id>.dimensions` | required when enabled | Embedding vector dimension. |
| `aisme.embedding.models.<model-id>.runtime.id` | required when enabled | Runtime id from `aisme.embedding.runtimes`. |
| `aisme.embedding.models.<model-id>.runtime.model-path` | ONNX only | ONNX model file path. |
| `aisme.embedding.models.<model-id>.runtime.tokenizer-path` | ONNX only | tokenizer file path. |
| `aisme.embedding.models.<model-id>.runtime.model-name` | Ollama only | Provider model name for Ollama embedding models. |
| `aisme.chat.api-timeout` | `60s` | Timeout for model chat generation. |
| `aisme.chat.relevant-chunk-limit` | `5` | Maximum number of retrieved chunks sent as chat context. |
| `aisme.chat.model-availability.timeout` | `5s` | Timeout for runtime availability checks. |
| `aisme.chat.model-availability.cache-ttl` | `5s` | Time to cache availability check results. |
| `aisme.chat.runtimes.<runtime-id>.type` | required | Runtime adapter: `OLLAMA`, `OPENAI_COMPATIBLE`, `HUGGING_FACE_TGI`, `EMBEDDED_LLAMA`, or `SPRING_AI`. |
| `aisme.chat.runtimes.<runtime-id>.base-url` | runtime-specific | Provider base URL for Ollama, OpenAI-compatible, and Hugging Face endpoint runtimes. |
| `aisme.chat.runtimes.<runtime-id>.api-key` | runtime-specific | Provider API key. OpenAI-compatible online models are `MISCONFIGURED` when this is missing. |
| `aisme.chat.runtimes.<runtime-id>.asset-directory` | embedded only | Base directory for local embedded llama assets. |
| `aisme.chat.runtimes.<runtime-id>.server-executable-path` | embedded only | Path to the managed `llama-server` executable. |
| `aisme.chat.models.<model-id>` | required | Map entry whose key is the user-selected chat model id used in `/chat` requests. |
| `aisme.chat.models.<model-id>.enabled` | `true` in example config | Whether the chat model is visible and selectable. |
| `aisme.chat.models.<model-id>.display-order` | optional | Sort order for API and UI display. Lower values appear first. |
| `aisme.chat.models.<model-id>.display-name` | required when enabled | Human-readable model name. |
| `aisme.chat.models.<model-id>.description` | optional | Short model description for clients and selection UIs. |
| `aisme.chat.models.<model-id>.runtime.id` | required when enabled | Runtime id from `aisme.chat.runtimes`. |
| `aisme.chat.models.<model-id>.runtime.model-name` | runtime-specific | Provider model name for Ollama, OpenAI-compatible, and embedded llama models. |
| `aisme.chat.models.<model-id>.runtime.gguf-file` | embedded only | GGUF file path relative to the embedded runtime `asset-directory`. |
| `aisme.chat.models.<model-id>.runtime.context-size` | embedded only | Context size passed to `llama-server`. |
| `aisme.chat.models.<model-id>.runtime.runtime-arguments` | `[]` | Extra arguments passed to `llama-server` for embedded models. |

Runtime and mode combinations are intentionally narrow in the initial scope:
`OLLAMA` uses `LOCAL_SERVER`, `OPENAI_COMPATIBLE` and `HUGGING_FACE_TGI`
use `ONLINE`, `EMBEDDED_LLAMA` uses `EMBEDDED_OFFLINE`, and `SPRING_AI` uses
`ONLINE`.

### Local Embedding Model

The default embedding runtime is local ONNX. Model files are configured outside
the application JAR and are ignored by git:

```yaml
aisme:
  embedding:
    runtimes:
      local-onnx:
        type: ONNX
    models:
      local-bge-small:
        enabled: true
        version: "1.5"
        dimensions: 384
        runtime:
          id: local-onnx
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

Embedded offline chat will use local llama.cpp assets. The default models are
Qwen2.5 Instruct and Mistral 7B Instruct in GGUF format. The default asset
configuration points to external files outside the application JAR:

```yaml
aisme:
  chat:
    runtimes:
      embedded-llama:
        type: EMBEDDED_LLAMA
        asset-directory: ./models/llama
        server-executable-path: ./models/llama/bin/llama-server
    models:
      embedded-qwen-0-5b:
        enabled: true
        display-order: 10
        display-name: Embedded Qwen 0.5B
        description: Fully offline embedded model backed by a local GGUF asset.
        runtime:
          id: embedded-llama
          model-name: qwen2.5-0.5b-instruct-q4_k_m
          gguf-file: models/qwen2.5-0.5b-instruct-q4_k_m.gguf
          context-size: 2048
          runtime-arguments: []
      embedded-qwen-1-5b:
        enabled: true
        display-order: 20
        display-name: Embedded Qwen 1.5B
        description: Smarter fully offline embedded model backed by a local GGUF asset.
        runtime:
          id: embedded-llama
          model-name: qwen2.5-1.5b-instruct-q4_k_m
          gguf-file: models/qwen2.5-1.5b-instruct-q4_k_m.gguf
          context-size: 2048
          runtime-arguments: []
      embedded-qwen-3b:
        enabled: true
        display-order: 30
        display-name: Embedded Qwen 3B
        description: Larger fully offline embedded model backed by a local GGUF asset.
        runtime:
          id: embedded-llama
          model-name: qwen2.5-3b-instruct-q4_k_m
          gguf-file: models/qwen2.5-3b-instruct-q4_k_m.gguf
          context-size: 2048
          runtime-arguments: []
      embedded-mistral-7b:
        enabled: true
        display-order: 40
        display-name: Embedded Mistral 7B
        description: Heavier fully offline embedded model for stronger local answers.
        runtime:
          id: embedded-llama
          model-name: mistral-7b-instruct-v0.3-q4_k_m
          gguf-file: models/mistral-7b-instruct-v0.3-q4_k_m.gguf
          context-size: 2048
          runtime-arguments: []
```

Embedded models are enabled by default. Each becomes selectable
only when its local llama.cpp runtime assets are installed and pass startup
availability checks. `asset-directory` is the base directory for local GGUF
model files and related metadata. `server-executable-path` points to the local
`llama-server` binary. For enabled embedded models, the application starts one
managed llama-server process per model on an ephemeral loopback port and sends
chat requests to its `/completion` endpoint. Model
metadata describes GGUF files relative to `asset-directory` and runtime
arguments.

Embedded offline asset availability is checked when the application starts.
After changing local GGUF files or the configured `llama-server` binary, restart
the application to refresh embedded offline availability. The managed local
`llama-server` health endpoint must become ready before an embedded model is
reported as available.
Managed `llama-server` lifecycle events and process stdout/stderr are written
to the application logs.

### Local Ollama Model

The default chat model entry points to Ollama at `http://localhost:11434`:

```yaml
aisme:
  chat:
    runtimes:
      local-ollama:
        type: OLLAMA
        base-url: http://localhost:11434
    models:
      local-ollama-llama:
        enabled: true
        display-order: 50
        display-name: Local Ollama Llama
        description: Local Ollama model for chat requests when Ollama is running on this machine.
        runtime:
          id: local-ollama
          model-name: llama3.2
```

The application-owned `aisme.chat.models` configuration is the source of truth
for selectable chat models. Spring AI Ollama settings are handled by the Ollama
adapter instead of duplicated in a separate Spring profile.

### OpenAI-Compatible Cloud Model

OpenAI-compatible chat providers can be configured as selectable online models:

```yaml
aisme:
  chat:
    runtimes:
      openai-compatible:
        type: OPENAI_COMPATIBLE
        base-url: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY}
    models:
      cloud-gpt:
        enabled: true
        display-order: 60
        display-name: Cloud GPT
        description: Online OpenAI-compatible model for cloud-hosted chat requests.
        runtime:
          id: openai-compatible
          model-name: gpt-4.1-mini
```

The adapter sends non-streaming chat-completion requests to
`/chat/completions` with bearer-token authentication. If `OPENAI_API_KEY` is
not set, the application still starts, but the model is reported as
`MISCONFIGURED` and cannot be used for chat.

### Hugging Face Inference Endpoint / TGI Model

Hugging Face Inference Endpoints can be configured as selectable online models:

```yaml
aisme:
  chat:
    runtimes:
      hugging-face-tgi:
        type: HUGGING_FACE_TGI
        base-url: https://example.endpoints.huggingface.cloud
        api-key: ${HF_API_KEY}
    models:
      hf-mistral:
        enabled: true
        display-order: 70
        display-name: Hugging Face Mistral
        description: Online Hugging Face endpoint using the TGI-compatible generate API.
        runtime:
          id: hugging-face-tgi
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
