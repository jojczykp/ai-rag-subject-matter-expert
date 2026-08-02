# AI RAG Subject Matter Expert

AI RAG Subject Matter Expert is a RAG application with a Kotlin Spring Boot
backend and a React, TypeScript, and Vite frontend. It lets a user choose a
subject, an embedding model, and a chat model, then asks questions against
static document knowledge bundled with the application.

The codebase demonstrates:

- pragmatic Kotlin and Spring Boot backend development;
- PostgreSQL + pgvector persistence and vector retrieval;
- provider-neutral chat model routing;
- local Ollama integration;
- embedded local GGUF models through a managed `llama-server` process;
- OpenAI-compatible and Hugging Face TGI-style cloud adapter foundations;
- a usable React frontend with tests and coverage;
- Codex-assisted development using GPT-5.5, agents, skills, ADRs, and an
  implementation plan kept in sync with the code.

## Requirements

Backend:

- JDK 26
- Gradle 9.5.1 through the included Gradle Wrapper
- Docker for PostgreSQL, Docker Compose, and Testcontainers integration tests
- Optional: Ollama for the configured local Ollama chat and embedding models

Frontend:

- Node.js 25.2.1
- npm 11.12.1

Main technologies:

- Kotlin 2.4.0
- Spring Boot 4.1.0
- PostgreSQL + pgvector
- Flyway
- Spring Data JDBC and `JdbcClient`
- Kover backend coverage
- React, TypeScript, Vite, Vitest, React Testing Library, MSW, Playwright

## Quick Start

Clone this repository:
```bash
git clone <repository-url>
```
Enter cloned repository folder:
```bash
cd ai-subject-matter-expert
```

Terminal 1 - Run database:
```bash
docker compose up
```

Terminal 2 - Run local ollama server (optional):
```bash
ollama serve
```

Terminal 3 - Download ollama assets (optional):
```bash
ollama pull llama3.2
ollama pull nomic-embed-text:v1.5
```

Terminal 4 - Run application:
```bash
./gradlew --parallel run
```

Open the UI:
```bash
open http://localhost:5173
```

Useful backend URLs:
```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
http://localhost:8080/subjects
http://localhost:8080/embedding-models
http://localhost:8080/chat-models
```

The first backend startup can take noticeably longer because enabled local
model assets and the platform-matching `llama-server` archive may be downloaded
before Spring finishes startup. Static subject documents are also indexed into
PostgreSQL before the application becomes ready.

## First Run Notes

By default, missing file-backed model assets are downloaded on startup when
their `download-missing-assets-on-startup` flag is enabled in
`backend/src/main/resources/application.yml`.

The default local setup uses:

| Area | Default |
| --- | --- |
| Subject | `passive-house` |
| Embedding model | `ollama-nomic-embed` |
| Chat model | `embedded-mistral-7b` |
| Database | PostgreSQL + pgvector on `localhost:5432` |
| Backend | `http://localhost:8080` |
| Frontend | `http://localhost:5173` |

During startup, `/actuator/health` may temporarily report `OUT_OF_SERVICE`.
That usually means startup indexing or runtime initialization is still in
progress. When the application is ready, health should become `UP`.

Downloaded model assets are ignored by git. To force the application to
download them again on the next startup:

```bash
./gradlew :backend:cleanDownloadedModelAssets
```

## What To Try

In the UI:

- Switch between `Passive House Architecture Expert` and `Culinary Expert`.
- Compare `Ollama Nomic Embed` and `Local BGE Small` retrieval behavior.
- Compare `Local Ollama Llama` with embedded `Qwen` or `Mistral` chat models.
- Inspect model availability, mode, privacy, and runtime details.
- Use the default subject question, then edit it and send your own.

The application keeps chat history in browser memory only. It does not persist
chat prompts, responses, or conversations.

## Architecture at a glance

- React UI.
- Spring Boot REST API.
- Configured static subjects.
- Bundled `.txt` documents knowledge base.
- Deterministic chunks.
- PostgreSQL + pgvector embeddings.
- Selected embedding model for retrieval.
- Selected chat model for answer generation.

Supported model styles:

- Embedded offline chat models via local `GGUF` files and managed `llama-server`.
- Local server models through Ollama on `localhost`.
- OpenAI-compatible online providers.
- Hugging Face TGI-compatible online endpoints.
- Local ONNX and Ollama embedding models.

See [Architecture](docs/ARCHITECTURE.md) and
[Model Runtime Integration](docs/ADR-003-model-runtime-integration.md) for the
full design.

## Build And Run

### Database

Start PostgreSQL + pgvector:

```bash
docker compose up -d db
```

Check database container status:

```bash
docker compose ps
```

Follow database logs:

```bash
docker compose logs -f db
```

Stop the database:

```bash
docker compose stop db
```

The default local database connection is:

```text
jdbc:postgresql://localhost:5432/aisme
```

Override it with `AISME_DATASOURCE_URL`, `AISME_DATASOURCE_USERNAME`, and
`AISME_DATASOURCE_PASSWORD` when needed.

### Backend

Build and verify the backend:

```bash
./gradlew :backend:clean :backend:build
```

Run the backend:

```bash
./gradlew :backend:run
```

The backend listens on `http://localhost:8080`.

### Frontend

Build the frontend from the repository root:

```bash
./gradlew :frontend:clean :frontend:build
```

Run the frontend:

```bash
./gradlew :frontend:run
```

The Vite development server usually serves the UI at
`http://localhost:5173`. The frontend calls the backend through
`VITE_BACKEND_API_BASE_URL`, which defaults to `http://localhost:8080`.

Override the backend URL when needed:

```bash
VITE_BACKEND_API_BASE_URL=http://localhost:8081 ./gradlew :frontend:run
```

### Both

Build backend and frontend:

```bash
./gradlew build
```

Run backend and frontend together:

```bash
./gradlew --parallel run
```

The root `run` task must use `--parallel` because backend and frontend dev
servers are both long-running processes.

## API Checks

Check health:

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

View application info:

```bash
curl -s http://localhost:8080/actuator/info | jq .
```

List subjects:

```bash
curl -s http://localhost:8080/subjects | jq .
```

List embedding models:

```bash
curl -s http://localhost:8080/embedding-models | jq .
```

List chat models:

```bash
curl -s http://localhost:8080/chat-models | jq .
```

Send a sample chat request:

```bash
curl -s http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{
    "subjectId": "passive-house",
    "modelId": "local-ollama-llama",
    "embeddingModelId": "ollama-nomic-embed",
    "message": "What are the biggest Passive House design risks?"
  }' | jq .
```

## Common Configuration Changes

Most local behavior is configured in
`backend/src/main/resources/application.yml`.

Useful first changes:

- `aisme.subjects.default-subject-id`: choose the subject preselected by the UI.
- `aisme.embedding.default-model-id`: choose the default embedding model.
- `aisme.chat.default-model-id`: choose the default chat model.
- `aisme.subjects.definitions.<subject-id>.documents.chunk-size`: tune chunk
  size per subject.
- `aisme.subjects.definitions.<subject-id>.documents.chunk-overlap`: tune chunk
  overlap per subject.
- `aisme.chat.retrieved-chunk-limit`: tune how many chunks are sent to the
  selected chat model.
- `aisme.chat.models.<model-id>.enabled`: show or hide a chat model.
- `aisme.embedding.models.<model-id>.enabled`: enable or disable an embedding
  model for indexing and retrieval.
- `download-missing-assets-on-startup`: control startup downloads for local
  file-backed runtime and model assets.

Full configuration reference:

- [Configuration Reference](docs/CONFIGURATION.md)

## Development

Run backend verification:

```bash
./gradlew :backend:check
```

`:backend:check` runs the backend test suite and Kover coverage verification.
Some `*IntegrationTest` tests use Testcontainers and require Docker.

Generate the backend HTML coverage report:

```bash
./gradlew :backend:koverHtmlReport
```

Backend coverage report:

```text
backend/build/reports/kover/html/index.html
```

Run frontend verification:

```bash
./gradlew :frontend:check
```

Generate frontend coverage through the frontend check or directly:

```bash
cd frontend
npm run test:coverage
```

Frontend coverage report:

```text
frontend/coverage/index.html
```

Run full project verification:

```bash
./gradlew check
```

Optional backend integration tests:

```bash
./gradlew :backend:extendedIntegrationTest
```

Run individual optional suites when debugging a specific integration:

```bash
./gradlew :backend:ollamaTest
./gradlew :backend:openAiCompatibleTest
./gradlew :backend:huggingFaceTgiTest
./gradlew :backend:onnxModelTest
```

Playwright browser end-to-end tests are run explicitly:

```bash
cd frontend
npm run e2e
```

## Troubleshooting

### First startup is slow

The backend may download local ONNX files, GGUF model files, and a
platform-specific `llama-server` archive, then index bundled subject documents.
Watch backend logs until startup completes.

### Health is OUT_OF_SERVICE

`OUT_OF_SERVICE` during startup usually means the application is still indexing
documents or initializing managed local runtimes. Retry `/actuator/health`
after startup logs report readiness.

### Docker or PostgreSQL fails

Check Docker is running and port `5432` is free:

```bash
docker compose ps
docker compose logs --tail=100 db
```

If needed, restart the local database:

```bash
docker compose down
docker compose up -d db
```

### Ollama model is unavailable

Make sure Ollama is running and the configured models are pulled:

```bash
ollama serve
ollama pull llama3.2
ollama pull nomic-embed-text:v1.5
ollama list
```

### Embedded model is unavailable

Check backend logs. The model may still be downloading, the local GGUF file may
be missing, or managed `llama-server` may have failed to start.

To force fresh local asset downloads:

```bash
./gradlew :backend:cleanDownloadedModelAssets
./gradlew :backend:run
```

### Frontend cannot call backend

Make sure the backend is running on `http://localhost:8080`. If using another
port, run the frontend with:

```bash
VITE_BACKEND_API_BASE_URL=http://localhost:8081 ./gradlew :frontend:run
```

## Project Agents

Project-scoped Codex agents live in `.codex/agents/` and are described in
`AGENTS.md`.

- `orchestrator` coordinates end-to-end feature work across the other agents.
- `architect` proposes system design changes and implementation plans.
- `ux` proposes user flows, frontend interaction design, accessibility checks,
  and user-facing copy.
- `developer` implements production code and fixes production defects.
- `tester` creates tests, runs verification, and triages failures.
- `documenter` keeps Markdown documentation and project guidance in sync.

## Documentation

Start with [Documentation Index](docs/README.md).

Key documents:

- [Product Requirements Document](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Configuration Reference](docs/CONFIGURATION.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [Database Schema](docs/DATABASE_SCHEMA.md)
