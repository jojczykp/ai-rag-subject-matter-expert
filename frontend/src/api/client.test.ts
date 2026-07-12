import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { getModels, postChat } from './client'
import { apiUrl, backendApiBaseUrl } from '../config'
import { server } from '../test/server'

describe('API client', () => {
  it('defaults to the local backend API', () => {
    expect(backendApiBaseUrl).toBe('http://localhost:8080')
  })

  it('loads configured models', async () => {
    const response = await getModels()

    expect(response.models).toHaveLength(1)
    expect(response.models[0]?.id).toBe('local-ollama-llama')
    expect(response.models[0]?.availability).toBe('AVAILABLE')
  })

  it('posts chat requests', async () => {
    const response = await postChat({
      modelId: 'local-ollama-llama',
      message: 'How should I cook rice?',
    })

    expect(response.modelId).toBe('local-ollama-llama')
    expect(response.answer).toBe('Mock answer for: How should I cook rice?')
  })

  it('throws typed API errors for backend error responses', async () => {
    server.use(
      http.post(apiUrl('/chat'), () =>
        HttpResponse.json(
          {
            code: 'MODEL_UNAVAILABLE',
            message: 'Selected model is unavailable.',
            details: {
              modelId: 'local-ollama-llama',
            },
          },
          { status: 503 },
        ),
      ),
    )

    await expect(
      postChat({
        modelId: 'local-ollama-llama',
        message: 'How should I cook rice?',
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      status: 503,
      response: {
        code: 'MODEL_UNAVAILABLE',
        message: 'Selected model is unavailable.',
        details: {
          modelId: 'local-ollama-llama',
        },
      },
    })
  })
})
