# ADR-005: Subject Document Scope

## Status

Accepted.

## Context

The initial product should stay intentionally small. The application currently
targets one predefined subject and uses documents maintained by the application
owner. Users should be able to chat against that subject, but they should not
manage subjects or documents through the API in the first implementation.

## Decision

Start with one implicit predefined subject backed by static documents bundled
with the application.

Initial scope:

- [ ] Support exactly one predefined subject.
- [ ] Do not require a subject id in API requests or responses.
- [ ] Load all reasoning documents from static application resources.
- [ ] Store bundled documents under a resource folder such as
      `src/main/resources/subject-documents/`.
- [ ] Support plain text `.txt` documents as the first input format.
- [ ] Provide `POST /chat` for asking questions.
- [ ] Provide `GET /models` for listing configured models.
- [ ] Do not provide `/subjects` endpoints.
- [ ] Do not provide `/documents` endpoints.
- [ ] Do not expose original bundled files for download.
- [ ] Do not persist chat prompts, responses, or conversation history in the
      initial scope.
- [ ] Do not require subject display name or description metadata initially.

## Static Document Loading

- [ ] Load `.txt` files recursively from `src/main/resources/subject-documents/`.
- [ ] Use the classpath resource path relative to `subject-documents/` as the
      stable document identity.
- [ ] Sort resource paths lexicographically before indexing for deterministic
      behavior.
- [ ] Fail application startup when the configured document folder is missing.
- [ ] Fail application startup when no supported documents are found.
- [ ] Fail application startup when a bundled document cannot be read.
- [ ] Fail application startup when a bundled document is empty.
- [ ] Fail application startup when chunking or indexing fails.
- [ ] Use statically configurable chunk size and chunk overlap.
- [ ] Use character-count based chunking initially.
- [ ] Use `1000` characters as the initial default chunk size.
- [ ] Use `150` characters as the initial default chunk overlap.
- [ ] Keep chunk size and overlap values in application configuration.

## Rationale

This keeps the first backend implementation focused on the core reasoning flow:
load bundled content, index it, retrieve relevant chunks, route a chat request
to the selected model, and return a provider-neutral response.

Avoiding dynamic subjects and document management reduces API surface, security
requirements, persistence complexity, and test scope while the model-routing
and retrieval foundation is still being built.

## Future Scope

- [ ] Support multiple subjects.
- [ ] Let users create, update, and delete subjects.
- [ ] Let users upload content to subjects at runtime.
- [ ] Support dynamic document ingestion pipelines.
- [ ] Support Markdown documents.
- [ ] Support structured CSV documents.
- [ ] Support PDF documents.
- [ ] Support Word documents.
- [ ] Expose citations and source references in chat answers.
- [ ] Support persisted chat history when there is a clear product need and
      retention policy.

## Future Considerations

- [ ] Evaluate token-estimate based chunking if character-count chunking is not
      accurate enough for selected models.
- [ ] Evaluate paragraph-aware or hybrid chunking if retrieval quality requires
      it.

## Consequences

- [ ] API consumers do not need to know about subject ids.
- [ ] The first API remains small and easier to test.
- [ ] Bundled document changes are deployed with the application.
- [ ] Invalid bundled documents fail fast during startup.
- [ ] Repeated indexing is deterministic for the same bundled resources and
      chunking configuration.
- [ ] Future dynamic document ingestion can be added without changing the first
      chat contract if the implicit subject remains the default.
