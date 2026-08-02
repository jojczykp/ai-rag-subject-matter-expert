# Configuration Reference

Application properties are configured under the `aisme` prefix in
`backend/src/main/resources/application.yml`.

## Common Changes

### Change Default Subject

```yaml
aisme:
  subjects:
    default-subject-id: passive-house
```

### Change Default Models

```yaml
aisme:
  embedding:
    default-model-id: ollama-nomic-embed
  chat:
    default-model-id: embedded-mistral-7b
```

### Disable A Model

```yaml
aisme:
  chat:
    models:
      openai-compatible-example:
        enabled: false
```

### Add A Static Subject

Place `.txt` files under:

```text
backend/src/main/resources/subject_documents/<folder_name>/
```

Then configure the subject:

```yaml
aisme:
  subjects:
    definitions:
      my-subject:
        enabled: true
        display-order: 30
        display-name: My Subject
        default-question: What should I know first?
        documents:
          location: classpath:/subject_documents/my_subject/
          chunk-size: 700
          chunk-overlap: 100
```

Use dash-separated subject ids in configuration and API requests. Resource
folder names can use underscores when that keeps paths readable.

### Configure OpenAI-Compatible Provider

```yaml
aisme:
  chat:
    runtimes:
      openai-compatible:
        type: OPENAI_COMPATIBLE
        base-url: https://api.openai.com/v1
        api-key: ${OPENAI_API_KEY:}
    models:
      openai-compatible-example:
        enabled: true
        display-name: OpenAI-Compatible Cloud Example
        runtime:
          id: openai-compatible
          model-name: gpt-4.1-mini
```

When the API key is missing, the application starts and reports the model as
`MISCONFIGURED`.

### Configure Hugging Face TGI Endpoint

```yaml
aisme:
  chat:
    runtimes:
      hugging-face-tgi:
        type: HUGGING_FACE_TGI
        base-url: https://example.endpoints.huggingface.cloud
        api-key: ${HF_API_KEY:}
    models:
      hugging-face-tgi-example:
        enabled: true
        runtime:
          id: hugging-face-tgi
```

## Property Reference

| Property | Default | Description |
| --- | --- | --- |
| `aisme.api.cors.allowed-origins` | `http://localhost:5173` | Browser origins allowed to call the backend API. |
| `aisme.subjects.default-subject-id` | `passive-house` | Subject preselected by API clients and the UI. |
| `aisme.subjects.definitions.<subject-id>.enabled` | `true` in example config | Whether this subject is indexed and selectable. |
| `aisme.subjects.definitions.<subject-id>.display-order` | optional | Sort order for subject selectors and catalog responses. |
| `aisme.subjects.definitions.<subject-id>.display-name` | derived from id when omitted | Human-readable subject name for API clients and the UI. |
| `aisme.subjects.definitions.<subject-id>.default-question` | empty string | Message prefilled in the UI when the subject is selected. |
| `aisme.subjects.definitions.<subject-id>.documents.location` | required | Bundled document resource folder for this subject. |
| `aisme.subjects.definitions.<subject-id>.documents.chunk-size` | subject-specific | Maximum character count per indexed document chunk for this subject. |
| `aisme.subjects.definitions.<subject-id>.documents.chunk-overlap` | subject-specific | Character overlap between adjacent chunks. Must be smaller than `chunk-size`. |
| `aisme.embedding.api-timeout` | `60s` | Timeout for embedding generation provider calls. |
| `aisme.embedding.default-model-id` | `ollama-nomic-embed` | Embedding model preselected by API clients and the UI. |
| `aisme.embedding.model-availability.timeout` | `5s` | Timeout for embedding runtime availability checks. |
| `aisme.embedding.model-availability.cache-ttl` | `5s` | Time to cache embedding availability check results. |
| `aisme.embedding.runtimes.<runtime-id>.type` | required | Embedding runtime adapter: `ONNX` or `OLLAMA`. |
| `aisme.embedding.runtimes.<runtime-id>.base-url` | Ollama only | Ollama server base URL for embedding generation. |
| `aisme.embedding.models.<model-id>.enabled` | `true` in example config | Whether this embedding model is indexed and selectable for retrieval. |
| `aisme.embedding.models.<model-id>.download-missing-assets-on-startup` | `true` in local asset examples | Whether missing local files are downloaded during application startup. |
| `aisme.embedding.models.<model-id>.display-order` | optional | Sort order for embedding model selectors and catalog responses. |
| `aisme.embedding.models.<model-id>.display-name` | optional | Human-readable embedding model name for API clients. |
| `aisme.embedding.models.<model-id>.version` | required when enabled | Embedding model version stored with embeddings. |
| `aisme.embedding.models.<model-id>.dimensions` | required when enabled | Embedding vector dimension. |
| `aisme.embedding.models.<model-id>.assets[].label` | local assets only | Human-readable asset label used in startup download logs. |
| `aisme.embedding.models.<model-id>.assets[].path` | local assets only | Local file path for a downloadable model asset. |
| `aisme.embedding.models.<model-id>.assets[].url` | local assets only | Source URL used when startup download is enabled and the asset file is missing. |
| `aisme.embedding.models.<model-id>.runtime.id` | required when enabled | Runtime id from `aisme.embedding.runtimes`. |
| `aisme.embedding.models.<model-id>.runtime.model-path` | ONNX only | ONNX model file path. |
| `aisme.embedding.models.<model-id>.runtime.tokenizer-path` | ONNX only | tokenizer file path. |
| `aisme.embedding.models.<model-id>.runtime.model-name` | Ollama only | Provider model name for Ollama embedding models. |
| `aisme.chat.api-timeout` | `60s` | Timeout for model chat generation. |
| `aisme.chat.retrieved-chunk-limit` | `5` | Maximum number of retrieved chunks sent as chat context. |
| `aisme.chat.default-model-id` | `embedded-mistral-7b` | Chat model preselected by API clients and the UI. |
| `aisme.chat.model-availability.timeout` | `5s` | Timeout for runtime availability checks. |
| `aisme.chat.model-availability.cache-ttl` | `5s` | Time to cache availability check results. |
| `aisme.chat.runtimes.<runtime-id>.type` | required | Runtime adapter: `OLLAMA`, `OPENAI_COMPATIBLE`, `HUGGING_FACE_TGI`, `EMBEDDED_LLAMA`, or `SPRING_AI`. |
| `aisme.chat.runtimes.<runtime-id>.base-url` | runtime-specific | Provider base URL for Ollama, OpenAI-compatible, and Hugging Face endpoint runtimes. |
| `aisme.chat.runtimes.<runtime-id>.api-key` | runtime-specific | Provider API key. |
| `aisme.chat.runtimes.<runtime-id>.asset-directory` | embedded only | Base directory for local embedded llama assets. |
| `aisme.chat.runtimes.<runtime-id>.server-executable-path` | embedded only | Path to the managed `llama-server` executable. |
| `aisme.chat.runtimes.<runtime-id>.download-missing-assets-on-startup` | `true` in embedded example | Whether missing local runtime files are downloaded during application startup. |
| `aisme.chat.runtimes.<runtime-id>.assets[].label` | local runtime assets only | Human-readable runtime asset label used in startup download logs. |
| `aisme.chat.runtimes.<runtime-id>.assets[].path` | local runtime assets only | Local file path expected after download or archive installation. |
| `aisme.chat.runtimes.<runtime-id>.assets[].url` | local runtime assets only | Source URL used when startup download is enabled and the asset file is missing. |
| `aisme.chat.runtimes.<runtime-id>.assets[].os` | optional | Normalized OS selector: `macos`, `linux`, or `windows`. |
| `aisme.chat.runtimes.<runtime-id>.assets[].arch` | optional | Normalized CPU selector: `aarch64` or `x86_64`. |
| `aisme.chat.runtimes.<runtime-id>.assets[].archive.format` | archive assets only | Archive type: `TAR_GZ` or `ZIP`. |
| `aisme.chat.runtimes.<runtime-id>.assets[].archive.executable-name` | archive assets only | Executable file name inside the downloaded archive. |
| `aisme.chat.models.<model-id>.enabled` | `true` in example config | Whether the chat model is visible and selectable. |
| `aisme.chat.models.<model-id>.download-missing-assets-on-startup` | `true` in embedded examples | Whether missing local files for this chat model are downloaded during application startup. |
| `aisme.chat.models.<model-id>.display-order` | optional | Sort order for API and UI display. Lower values appear first. |
| `aisme.chat.models.<model-id>.display-name` | required when enabled | Human-readable model name. |
| `aisme.chat.models.<model-id>.description` | optional | Short model description for clients and selection UIs. |
| `aisme.chat.models.<model-id>.assets[].label` | local assets only | Human-readable asset label used in startup download logs. |
| `aisme.chat.models.<model-id>.assets[].path` | local assets only | Local file path for a downloadable model asset. |
| `aisme.chat.models.<model-id>.assets[].url` | local assets only | Source URL used when startup download is enabled and the asset file is missing. |
| `aisme.chat.models.<model-id>.runtime.id` | required when enabled | Runtime id from `aisme.chat.runtimes`. |
| `aisme.chat.models.<model-id>.runtime.model-name` | runtime-specific | Provider model name for Ollama, OpenAI-compatible, and embedded llama models. |
| `aisme.chat.models.<model-id>.runtime.gguf-file` | embedded only | GGUF file path relative to the embedded runtime `asset-directory`. |
| `aisme.chat.models.<model-id>.runtime.context-size` | embedded only | Context size passed to `llama-server`. |
| `aisme.chat.models.<model-id>.runtime.runtime-arguments` | `[]` | Extra arguments passed to `llama-server` for embedded models. |

Runtime and mode combinations are intentionally narrow in the current scope:
`OLLAMA` uses `LOCAL_SERVER`, `OPENAI_COMPATIBLE` and `HUGGING_FACE_TGI` use
`ONLINE`, `EMBEDDED_LLAMA` uses `EMBEDDED_OFFLINE`, and `SPRING_AI` uses
`ONLINE`.
