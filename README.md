# AI Subject Matter Expert

AI Subject Matter Expert is a Kotlin Spring Boot service. The current
application exposes Spring Boot Actuator health and info endpoints.

This project is vibe-coded with Codex using GPT-5.

## Requirements

- JDK 26
- Gradle Wrapper included in this repository

The Gradle build uses the Java 26 toolchain, Kotlin 2.4.0, Spring Boot 4.1.0,
and Kover for coverage verification. The wrapper currently uses Gradle 9.5.1.

## Build And Run

Run commands from the repository root.

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

## Verification

Run the unit tests:

```bash
./gradlew test
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
