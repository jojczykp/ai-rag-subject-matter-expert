import { http, HttpResponse } from 'msw'
import type {
  ChatModelsResponse,
  ChatRequest,
  ChatResponse,
} from '../api/types'
import { apiUrl } from '../config'
import { availableOllamaModel } from './fixtures'

export const handlers = [
  http.get(apiUrl('/chat-models'), () =>
    HttpResponse.json<ChatModelsResponse>({
      chatModels: [availableOllamaModel],
    }),
  ),
  http.post(apiUrl('/chat'), async ({ request }) => {
    const body = (await request.json()) as ChatRequest

    return HttpResponse.json<ChatResponse>({
      modelId: body.modelId,
      answer: `Mock answer for: ${body.message}`,
    })
  }),
]
