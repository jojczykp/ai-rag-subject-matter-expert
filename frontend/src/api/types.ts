export type ChatModelRuntime =
  | 'SPRING_AI'
  | 'OPENAI_COMPATIBLE'
  | 'OLLAMA'
  | 'HUGGING_FACE_TGI'
  | 'EMBEDDED_LLAMA'

export type ChatModelMode = 'ONLINE' | 'LOCAL_SERVER' | 'EMBEDDED_OFFLINE'

export type ChatModelAvailability =
  'CONFIGURED' | 'AVAILABLE' | 'UNAVAILABLE' | 'MISCONFIGURED'

export type ChatModelCapability = 'CHAT'

export type ChatModelRuntimeRequirement =
  | 'REQUIRES_NETWORK'
  | 'REQUIRES_API_KEY'
  | 'REQUIRES_OLLAMA_SERVER'
  | 'REQUIRES_LOCAL_GGUF_MODEL'
  | 'REQUIRES_LLAMA_SERVER_EXECUTABLE'

export type ChatModel = {
  id: string
  displayName: string
  description: string | null
  runtime: ChatModelRuntime
  mode: ChatModelMode
  availability: ChatModelAvailability
  availableOffline: boolean
  promptsMayLeaveLocalMachine: boolean
  capabilities: ChatModelCapability[]
  runtimeRequirements: ChatModelRuntimeRequirement[]
}

export type ChatModelsResponse = {
  defaultChatModelId: string | null
  chatApiTimeoutSeconds: number
  chatModels: ChatModel[]
}

export type EmbeddingModelRuntime = 'ONNX' | 'OLLAMA'

export type EmbeddingModelMode = 'ONLINE' | 'LOCAL_SERVER' | 'EMBEDDED_OFFLINE'

export type EmbeddingModelAvailability =
  'CONFIGURED' | 'AVAILABLE' | 'UNAVAILABLE' | 'MISCONFIGURED'

export type EmbeddingModel = {
  id: string
  enabled: boolean
  displayName: string
  runtime: EmbeddingModelRuntime
  mode: EmbeddingModelMode
  availability: EmbeddingModelAvailability
  version: string | null
  dimensions: number | null
  availableOffline: boolean
}

export type EmbeddingModelsResponse = {
  defaultEmbeddingModelId: string | null
  embeddingApiTimeoutSeconds: number
  embeddingModels: EmbeddingModel[]
}

export type Subject = {
  id: string
  enabled: boolean
  displayOrder: number
  displayName: string
}

export type SubjectsResponse = {
  defaultSubjectId: string | null
  subjects: Subject[]
}

export type ChatRequest = {
  subjectId: string
  modelId: string
  embeddingModelId?: string
  message: string
}

export type ChatResponse = {
  modelId: string
  answer: string
}

export type ApiErrorCode =
  | 'INVALID_REQUEST'
  | 'SUBJECT_NOT_FOUND'
  | 'MODEL_NOT_FOUND'
  | 'MODEL_UNAVAILABLE'
  | 'MODEL_CLIENT_NOT_FOUND'
  | 'PROVIDER_TIMEOUT'
  | 'PROVIDER_ERROR'
  | 'INTERNAL_ERROR'

export type ApiErrorResponse = {
  code: ApiErrorCode
  message: string
  details?: Record<string, string>
}
