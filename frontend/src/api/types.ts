export type ChatModelRuntime =
  | 'SPRING_AI'
  | 'OPENAI_COMPATIBLE'
  | 'OLLAMA'
  | 'HUGGING_FACE_ENDPOINT'
  | 'EMBEDDED_OFFLINE'

export type ChatModelMode = 'ONLINE' | 'LOCAL_SERVER' | 'EMBEDDED_OFFLINE'

export type ChatModelAvailability =
  'CONFIGURED' | 'AVAILABLE' | 'UNAVAILABLE' | 'MISCONFIGURED'

export type ChatModelCapability = 'CHAT'

export type ChatModelRuntimeRequirement =
  | 'REQUIRES_NETWORK'
  | 'REQUIRES_API_KEY'
  | 'REQUIRES_LOCAL_OLLAMA'
  | 'REQUIRES_LOCAL_GGUF_MODEL'
  | 'REQUIRES_MANAGED_LLAMA_SERVER'

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

export type ModelsResponse = {
  models: ChatModel[]
}

export type ChatRequest = {
  modelId: string
  message: string
}

export type ChatResponse = {
  modelId: string
  answer: string
}

export type ApiErrorCode =
  | 'INVALID_REQUEST'
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
