# ADR-009: Frontend UI Architecture

## Status

Accepted.

## Context

The application currently exposes REST APIs for model discovery and chat:

- `GET /models`
- `POST /chat`

The first user interface should make those backend capabilities usable without
changing the backend ownership model. The UI should let users choose a model,
understand whether prompts may leave the local machine, and send chat messages
against the single configured subject.

The application does not need server-side rendered pages, frontend-managed
authentication, document management, subject management, or persisted chat
history in the first UI scope.

## Decision

Build the frontend as a separate React application using TypeScript and Vite.

The frontend source will live under a separate top-level `frontend/` directory.
During development, the frontend may run through the Vite dev server and call
the Spring Boot backend API. For production packaging, the built frontend assets
can be copied into Spring Boot static resources so the application can still be
distributed as a single backend artifact.

The initial UI scope is:

- [ ] Load configured models from `GET /models`.
- [ ] Show model display name, availability, runtime mode, runtime
      requirements, and whether prompts may leave the local machine.
- [ ] Require the user to select a model before sending a message.
- [ ] Send non-streaming chat requests to `POST /chat`.
- [ ] Keep chat history in browser memory only.
- [ ] Display loading, validation, unavailable-model, and provider-error
      states.
- [ ] Avoid document-management, subject-management, authentication, citations,
      streaming, and persisted history features in the first UI scope.

Frontend testing will use:

- [ ] Vitest for unit and component tests.
- [ ] React Testing Library for behavior-focused component testing.
- [ ] MSW for backend API mocking in frontend tests.
- [ ] V8 coverage through Vitest for frontend coverage reports.
- [ ] Playwright for end-to-end browser tests once the UI has a stable first
      screen.

The initial frontend coverage target is 70%. The backend remains covered by the
existing Kover target. Frontend tests should prefer user-visible behavior over
snapshot-heavy assertions.

## Rationale

React with TypeScript and Vite fits the current architecture because the
backend is already a REST service and the UI needs a focused, interactive chat
experience. Vite keeps the frontend build small and fast without introducing an
additional application server.

Next.js and other full-stack React frameworks are not selected for the first UI
iteration because the application does not currently need server-side rendering,
server actions, route-level data loading on a Node runtime, or a second backend
process.

Keeping the frontend source separate from the Spring Boot source makes the
frontend easier to develop, test, and eventually package into the backend JAR
when needed.

Vitest matches the Vite-based frontend toolchain and keeps test execution fast.
React Testing Library keeps tests focused on user-observable behavior. MSW lets
frontend tests exercise API success and failure flows without depending on a
running Spring Boot process. Playwright is deferred until the UI can be tested
as a meaningful browser workflow.

## Consequences

- [ ] The repository will gain a `frontend/` project with its own package
      metadata and build tooling.
- [ ] Backend and frontend API contracts should stay explicit and tested.
- [ ] The first UI will consume existing REST endpoints rather than adding
      UI-specific backend endpoints.
- [ ] Production packaging will need a Gradle integration step if the frontend
      should be served by Spring Boot.
- [ ] Frontend tests should cover model loading, model selection, chat request
      flow, and API error states.
- [ ] Frontend verification should fail when the configured frontend coverage
      threshold is not met.
