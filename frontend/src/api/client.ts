import type {
  ApiErrorResponse,
  ChatRequest,
  ChatResponse,
  ChatModelsResponse,
  EmbeddingModelsResponse,
  SubjectsResponse,
} from './types'
import { apiUrl } from '../config'

export class ApiError extends Error {
  readonly status: number
  readonly response?: ApiErrorResponse

  constructor(message: string, status: number, response?: ApiErrorResponse) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.response = response
  }
}

export async function getChatModels(): Promise<ChatModelsResponse> {
  return requestJson<ChatModelsResponse>(apiUrl('/chat-models'))
}

export async function getSubjects(): Promise<SubjectsResponse> {
  return requestJson<SubjectsResponse>(apiUrl('/subjects'))
}

export async function getEmbeddingModels(): Promise<EmbeddingModelsResponse> {
  return requestJson<EmbeddingModelsResponse>(apiUrl('/embedding-models'))
}

export async function postChat(request: ChatRequest): Promise<ChatResponse> {
  return requestJson<ChatResponse>(apiUrl('/chat'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
}

async function requestJson<T>(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(input, init)

  if (!response.ok) {
    throw await apiError(response)
  }

  return (await response.json()) as T
}

async function apiError(response: Response): Promise<ApiError> {
  const errorResponse = await parseErrorResponse(response)
  return new ApiError(
    errorResponse?.message ?? `Request failed with HTTP ${response.status}`,
    response.status,
    errorResponse,
  )
}

async function parseErrorResponse(
  response: Response,
): Promise<ApiErrorResponse | undefined> {
  try {
    return (await response.json()) as ApiErrorResponse
  } catch {
    return undefined
  }
}
