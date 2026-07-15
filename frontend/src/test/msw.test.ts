import { describe, expect, it } from 'vitest'
import type { ChatResponse, ChatModelsResponse } from '../api/types'
import { apiUrl } from '../config'

describe('MSW backend API mocks', () => {
  it('returns configured models', async () => {
    const response = await fetch(apiUrl('/chat-models'))
    const body = (await response.json()) as ChatModelsResponse

    expect(response.ok).toBe(true)
    expect(body.chatModels).toHaveLength(1)
    expect(body.chatModels[0]?.id).toBe('local-ollama-llama')
    expect(body.chatModels[0]?.availability).toBe('AVAILABLE')
  })

  it('returns a deterministic chat response', async () => {
    const response = await fetch(apiUrl('/chat'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        modelId: 'local-ollama-llama',
        message: 'How should I cook rice?',
      }),
    })
    const body = (await response.json()) as ChatResponse

    expect(response.ok).toBe(true)
    expect(body.modelId).toBe('local-ollama-llama')
    expect(body.answer).toBe('Mock answer for: How should I cook rice?')
  })
})
