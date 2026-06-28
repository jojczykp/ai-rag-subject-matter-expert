# ADR-004: Integration Testing Strategy

## Status

Accepted.

## Context

The application integrates with PostgreSQL, pgvector, local model servers, and
cloud-hosted AI providers. Tests should verify real integration behavior where
it matters, without making every local or CI test run depend on real cloud
credentials, network access, or heavyweight model downloads.

## Decision

Use layered integration tests:

- [ ] Testcontainers PostgreSQL with pgvector for database and retrieval tests.
- [ ] Testcontainers with an Ollama Docker container for Ollama integration
      tests.
- [ ] Testcontainers MockServer for cloud and hosted provider protocol tests.
- [x] Fake model clients for application-flow integration tests where provider
      protocol behavior is not under test.

Ollama container tests should be optional or tagged when model download/runtime
cost makes them unsuitable for every local or CI run.

## Default And Optional Test Split

Default verification should include deterministic tests that are suitable for
normal local development and CI:

- [ ] Unit tests.
- [ ] Spring Boot application-flow integration tests with fake model clients.
- [ ] PostgreSQL and pgvector integration tests using Testcontainers.
- [ ] Cloud and hosted provider adapter tests using Testcontainers MockServer.
- [ ] Kover coverage verification for production code.

Optional verification should include expensive or runtime-heavy tests:

- [ ] Ollama container tests that require model pull or model runtime startup.
- [ ] Embedded runtime tests that require local model files or heavy native
      runtime setup.
- [ ] Manual smoke tests against real cloud providers.

Keep the Gradle setup simple while the project is small:

```bash
./gradlew test
./gradlew check
```

`test` runs the project test suite. Integration-style tests stay under
`src/test` and use `*IntegrationTest` in the file and class name so their scope
is visible without a separate source set. A dedicated `integrationTest` source
set can be introduced later if the test suite becomes large enough to justify
the extra Gradle configuration.

Use JUnit tags for optional runtime-heavy tests when they are introduced:

```kotlin
@Tag("optional")
@Tag("ollama")
@Tag("embedded-runtime")
@Tag("cloud-smoke")
```

## Test Categories

### Database And Retrieval

- [x] Use Testcontainers for integration tests that need PostgreSQL.
- [x] Use the pgvector image selected in ADR-002.
- [x] Verify Flyway migrations.
- [x] Verify document chunk persistence.
- [x] Verify embedding metadata persistence.
- [x] Verify vector similarity search behavior.

### Application Flow

- [ ] Add integration tests for REST endpoints.
- [ ] Add integration tests for static document indexing and chat flow.
- [x] Use fake model clients to keep app-flow tests deterministic.
- [ ] Avoid real cloud, Ollama, or embedded model calls when the test is not
      verifying provider protocol behavior.

### Ollama

- [ ] Use Testcontainers with an Ollama Docker container.
- [ ] Verify request mapping to Ollama.
- [ ] Verify response mapping from Ollama.
- [ ] Verify local-server availability and failure behavior.
- [ ] Keep expensive Ollama tests optional or tagged.

### Cloud And Hosted Providers

- [ ] Use Testcontainers MockServer.
- [ ] Verify request and response mapping.
- [ ] Verify authentication headers.
- [ ] Verify timeout handling.
- [ ] Verify provider error translation.
- [ ] Avoid calling real external APIs in automated tests by default.
- [ ] Keep real cloud provider tests as explicitly enabled manual smoke tests
      only.

### Embedded Offline

- [ ] Add integration tests for offline model registry behavior.
- [ ] Use lightweight fakes for embedded inference until a practical local
      runtime test fixture exists.
- [ ] Keep model-file and runtime-heavy tests optional or tagged.

## Consequences

- [ ] Default verification remains reliable without cloud credentials.
- [ ] Provider adapters can be tested at HTTP protocol boundaries.
- [ ] Expensive runtime tests can still exist without slowing every build.
- [ ] Kover coverage remains at or above 80% for production code in the default
      verification path.
- [ ] Optional expensive tests are not required to satisfy coverage.
- [ ] Unit tests should run without Docker.
- [ ] CI and default integration-test verification require Docker once
      Testcontainers-based persistence tests must run on every build.

## Naming Conventions

- [ ] Use `*Test` for unit tests.
- [x] Use `*IntegrationTest` for Spring Boot, Testcontainers, and MockServer
      integration tests included in default verification.
- [ ] Use `*OptionalIT` for expensive optional runtime tests.
