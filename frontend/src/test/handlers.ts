import { http, HttpResponse } from 'msw'
import type {
  ChatModelsResponse,
  ChatRequest,
  ChatResponse,
  EmbeddingModelsResponse,
} from '../api/types'
import { apiUrl } from '../config'
import { availableOllamaModel } from './fixtures'

export const handlers = [
  http.get(apiUrl('/chat-models'), () =>
    HttpResponse.json<ChatModelsResponse>({
      chatModels: [availableOllamaModel],
    }),
  ),
  http.get(apiUrl('/embedding-models'), () =>
    HttpResponse.json<EmbeddingModelsResponse>({
      embeddingModels: [
        {
          id: 'local-bge-small',
          enabled: true,
          displayName: 'Local BGE Small',
          runtime: 'ONNX',
          version: '1.5',
          dimensions: 384,
          availableOffline: true,
        },
        {
          id: 'ollama-nomic-embed',
          enabled: true,
          displayName: 'Ollama Nomic Embed',
          runtime: 'OLLAMA',
          version: 'latest',
          dimensions: 768,
          availableOffline: false,
        },
      ],
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
