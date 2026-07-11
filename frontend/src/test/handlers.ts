import { http, HttpResponse } from 'msw'
import type { ChatRequest, ChatResponse, ModelsResponse } from '../api/types'
import { availableOllamaModel } from './fixtures'

export const handlers = [
  http.get('/models', () =>
    HttpResponse.json<ModelsResponse>({
      models: [availableOllamaModel],
    }),
  ),
  http.post('/chat', async ({ request }) => {
    const body = (await request.json()) as ChatRequest

    return HttpResponse.json<ChatResponse>({
      modelId: body.modelId,
      answer: `Mock answer for: ${body.message}`,
    })
  }),
]
