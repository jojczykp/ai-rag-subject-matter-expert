# AGENTS.md

## Repository Agents

This repository defines five project-scoped Codex custom agents in `.codex/agents/`:

- `orchestrator`: coordinates end-to-end feature development across the other agents.
- `architect`: proposes system design changes and implementation plans.
- `developer`: implements production code and fixes production defects.
- `tester`: creates tests, runs verification, and triages test failures.
- `documenter`: keeps Markdown documentation and project guidance in sync with the codebase.

## Collaboration Flow

- Use `orchestrator` as the default coordinator for end-to-end feature work, especially when a request includes refactoring, extra tests, design changes, or multiple implementation steps.
- Use `architect` for non-trivial feature design, architecture changes, API boundaries, data flow, or migration planning.
- Use `developer` to implement production code according to the accepted design or requested behavior.
- Use `tester` to create or update tests and run the relevant verification command.
- Use `documenter` when project setup, commands, dependencies, agents, architecture, behavior, or verification requirements change.
- `orchestrator` should preserve detailed user constraints and route work to `architect`, `developer`, `tester`, and `documenter` as needed.
- If tests fail, `tester` should determine whether the failure is caused by production code or test code.
- If production code is defective, `tester` should hand the failure details to `developer`, and `developer` should fix production code.
- If the test is incorrect, stale, or brittle, `tester` should fix the test.
- Repeat developer/tester handoff until the relevant test suite passes or a clear external blocker is identified.
- Before final handoff, check whether README.md, AGENTS.md, or other Markdown files need updates for the completed change.

## Project Verification

- Use `./gradlew test` for the full test suite when the Gradle wrapper is available.
- Use narrower Gradle test filters when working on a small behavior change.
- Maintain at least 80% unit test coverage for production code.
- Use `./gradlew koverVerify` to enforce the 80% coverage threshold.
- Use `./gradlew koverHtmlReport` when a human-readable coverage report is useful.
- Use `./gradlew check` before final handoff when practical; it includes coverage verification.

## Code Style

All agents that design, implement, refactor, or test code must follow this code
style.

- Prefer simple, direct solutions that are easy to read, understand, and change.
- Match the existing Kotlin and Spring Boot style before introducing a new pattern.
- Keep classes, functions, and modules focused on one clear responsibility.
- Use descriptive names that explain intent without excessive wording.
- Favor explicit domain language over generic names such as `data`, `item`, `thing`, or `manager`.
- Keep functions small enough to understand at a glance, but do not split code into tiny helpers that hide straightforward logic.
- Avoid cleverness, hidden control flow, premature abstraction, and unnecessary indirection.
- Add abstractions only when they remove real duplication, clarify ownership, or isolate a meaningful change boundary.
- Prefer immutable values and constructor injection where practical.
- Keep null handling explicit and easy to follow.
- Validate inputs and edge cases close to the boundary where they enter the system.
- Keep error handling clear; do not swallow exceptions silently.
- Write comments sparingly, only when they explain why something non-obvious is done.
- Do not leave dead code, unused configuration, commented-out code, or temporary debugging output.
- Keep tests readable and behavior-focused; tests should make the expected behavior obvious.
- Prefer focused tests over broad tests unless the behavior crosses application boundaries.
- Keep unit test coverage at or above 80% while avoiding low-value tests that only assert implementation details.
- Refactor opportunistically only where it supports the requested change; avoid unrelated cleanup.
- Optimize for maintainability first, then performance when there is a demonstrated need.

## Git Commit Messages

All agents that create commits must follow this commit message style.

- Use the imperative mood in the subject line, for example `Add health check endpoint`, not `Added health check endpoint`.
- Keep the subject line concise and professional, ideally 50 characters or less.
- Capitalize the subject line and do not end it with a period.
- Use a body when the reason, tradeoff, migration note, or verification detail is not obvious from the diff.
- Wrap body lines at roughly 72 characters.
- Explain what changed and why; avoid restating every edited file.
- Mention relevant verification in the body when it matters.
- Do not use vague subjects such as `Fix stuff`, `Update code`, or `WIP`.

Preferred format:

```text
Short imperative subject

Optional body explaining why the change was made, any notable tradeoffs,
and verification performed.
```

Examples:

```text
Add project-scoped Codex agents

Defines architect, developer, tester, and orchestrator agents so feature
work can be split across design, implementation, and verification roles.
```

```text
Fix controller test expectation

Aligns the test with the documented root endpoint response.

Verification: ./gradlew test
```
