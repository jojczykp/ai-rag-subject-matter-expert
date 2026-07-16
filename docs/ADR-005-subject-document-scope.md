# ADR-005: Subject Document Scope

## Status

Accepted.

## Context

The initial product should stay intentionally small. The application uses
predefined subjects and static documents maintained by the application owner.
Users should be able to chat against a selected subject, but they should not
manage subjects or documents through the API in the first implementation.

## Decision

Start with configured predefined subjects backed by static documents bundled
with the application.

Initial scope:

- [x] Support multiple predefined static subjects.
- [x] Require a subject id in chat requests.
- [ ] Load all reasoning documents from static application resources.
- [x] Configure each subject under `aisme.subjects.<subject-id>`.
- [x] Configure each subject's document location under
      `aisme.subjects.<subject-id>.documents.location`.
- [x] Support subject `enabled`, `display-order`, and `display-name`
      configuration.
- [ ] Support plain text `.txt` documents as the first input format.
- [x] Provide `POST /chat` for asking questions.
- [x] Provide `GET /chat-models` for listing configured models.
- [x] Provide `GET /subjects` for listing configured enabled subjects.
- [ ] Do not provide `/documents` endpoints.
- [ ] Do not expose original bundled files for download.
- [ ] Do not persist chat prompts, responses, or conversation history in the
      initial scope.

## Static Document Loading

- [x] Load `.txt` files recursively from each configured subject document
      location.
- [x] Use the classpath resource path relative to the configured subject
      document location as the stable document identity.
- [ ] Sort resource paths lexicographically before indexing for deterministic
      behavior.
- [ ] Fail application startup when the configured document folder is missing.
- [ ] Fail application startup when no supported documents are found.
- [ ] Fail application startup when a bundled document cannot be read.
- [ ] Fail application startup when a bundled document is empty.
- [ ] Fail application startup when chunking or indexing fails.
- [x] Use statically configurable chunk size and chunk overlap per subject.
- [ ] Use character-count based chunking initially.
- [ ] Use `700` characters as the initial default chunk size.
- [ ] Use `100` characters as the initial default chunk overlap.
- [ ] Keep chunk size and overlap values in application configuration.

## Rationale

This keeps the first backend implementation focused on the core reasoning flow:
load bundled content, index it, retrieve relevant chunks, route a chat request
to the selected model, and return a provider-neutral response.

Avoiding dynamic subjects and document management reduces API surface, security
requirements, persistence complexity, and test scope while the model-routing
and retrieval foundation is still being built.

## Future Scope

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

- [x] API consumers must provide a configured subject id for chat.
- [x] The API remains small because subject management is read-only.
- [ ] Bundled document changes are deployed with the application.
- [ ] Invalid bundled documents fail fast during startup.
- [ ] Repeated indexing is deterministic for the same bundled resources and
      chunking configuration.
- [ ] Future dynamic document ingestion can be added under the existing subject
      concept without exposing bundled source files.
