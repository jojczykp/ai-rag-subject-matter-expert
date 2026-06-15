# AI Subject Matter Expert

AI Subject Matter Expert is a Kotlin Spring Boot service. The current
application exposes a root status endpoint and Spring Boot Actuator health and
info endpoints.

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
curl http://localhost:8080/
```

Expected response:

```json
{"message":"AI Subject Matter Expert service is running"}
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
