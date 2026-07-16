import { http, HttpResponse } from 'msw'
import type {
  ChatModelsResponse,
  ChatRequest,
  ChatResponse,
  EmbeddingModelsResponse,
  SubjectsResponse,
} from '../api/types'
import { apiUrl } from '../config'
import { availableOllamaModel, culinarySubject } from './fixtures'

export const handlers = [
  http.get(apiUrl('/subjects'), () =>
    HttpResponse.json<SubjectsResponse>({
      defaultSubjectId: culinarySubject.id,
      subjects: [culinarySubject],
    }),
  ),
  http.get(apiUrl('/chat-models'), () =>
    HttpResponse.json<ChatModelsResponse>({
      defaultChatModelId: 'local-ollama-llama',
      chatApiTimeoutSeconds: 60,
      chatModels: [availableOllamaModel],
    }),
  ),
  http.get(apiUrl('/embedding-models'), () =>
    HttpResponse.json<EmbeddingModelsResponse>({
      defaultEmbeddingModelId: 'ollama-nomic-embed',
      embeddingApiTimeoutSeconds: 60,
      embeddingModels: [
        {
          id: 'local-bge-small',
          enabled: true,
          displayName: 'Local BGE Small (1.5, 384d)',
          runtime: 'ONNX',
          mode: 'EMBEDDED_OFFLINE',
          availability: 'CONFIGURED',
          version: '1.5',
          dimensions: 384,
          availableOffline: true,
        },
        {
          id: 'ollama-nomic-embed',
          enabled: true,
          displayName: 'Ollama Nomic Embed (v1.5, 768d)',
          runtime: 'OLLAMA',
          mode: 'LOCAL_SERVER',
          availability: 'AVAILABLE',
          version: 'v1.5',
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
