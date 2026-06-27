# ADR-006: Local Embedding Runtime

## Status

Accepted.

## Context

ADR-001 selects one statically configured embedding model for document chunk
and query embeddings. That model should preferably run locally so document
indexing and query embedding generation do not require a network call and can
support offline operation.

Using Ollama for embeddings is possible, but it requires a running local Ollama
server. The application should also support calculating embeddings internally
from locally available model files.

## Decision

Use a local ONNX embedding model as the preferred initial embedded embedding
runtime. The application should load the configured model and tokenizer from
local files and calculate embeddings during startup indexing and request-time
retrieval.

Initial recommended model direction:

- [ ] Use a Hugging Face sentence-transformer style embedding model exported to
      ONNX.
- [ ] Prefer `BAAI/bge-small-en-v1.5` as the first practical local model.
- [ ] Use `384` embedding dimensions for the initial local ONNX model.
- [ ] Store model files outside the application JAR.
- [ ] Configure model and tokenizer paths through application configuration.
- [ ] Keep Ollama embeddings as an alternative runtime, not the default
      embedded embedding path.

Example configuration:

```yaml
aisme:
  embedding-model:
    id: local-bge-small
    version: "1.5"
    runtime: ONNX
    model-path: ./models/bge-small-en-v1.5/model.onnx
    tokenizer-path: ./models/bge-small-en-v1.5/tokenizer.json
    dimensions: 384
```

## Options Considered

### Option 1: Local ONNX Embedding Model

Benefits:

- [ ] Runs locally without requiring Ollama or a cloud provider.
- [ ] Works offline after model files are present.
- [ ] Fits JVM applications through ONNX Runtime.
- [ ] Keeps retrieval independent from user-selected chat models.

Tradeoffs:

- [ ] Requires model export or compatible ONNX model files.
- [ ] Requires tokenizer handling in the application.
- [ ] Model file installation and licensing need documentation.

This is the selected option.

### Option 2: Local Ollama Embedding Model

Benefits:

- [ ] Simple operational path when Ollama is already running.
- [ ] Can use embedding models such as `nomic-embed-text`.

Tradeoffs:

- [ ] Requires a local Ollama server.
- [ ] Retrieval availability depends on an external local process.
- [ ] Does not satisfy fully embedded startup indexing by itself.

This remains an alternative runtime.

### Option 3: Remote Hugging Face Inference

Benefits:

- [ ] No local model runtime setup.
- [ ] Can use hosted embedding providers.

Tradeoffs:

- [ ] Requires network access.
- [ ] Sends document/query text to a remote provider.
- [ ] Does not satisfy offline operation.

This is not the default embedding runtime.

## Consequences

- [ ] pgvector dimensions must match the configured ONNX embedding model.
- [ ] Changing the ONNX model or dimensions requires re-indexing documents.
- [ ] Startup indexing can calculate embeddings internally without Ollama.
- [ ] Documentation must describe where local embedding model files live.
- [ ] Tests should use a fake or lightweight embedding implementation unless
      they specifically verify ONNX runtime behavior.

## Future Considerations

- [ ] Evaluate `nomic-ai/nomic-embed-text-v1.5` as a stronger local model when
      JVM/ONNX integration is practical.
- [ ] Evaluate ONNX Runtime performance and memory usage with production-sized
      document sets.
- [ ] Add optional tests with real ONNX model files when a small stable fixture
      is available.
