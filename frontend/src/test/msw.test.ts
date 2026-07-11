import { describe, expect, it } from 'vitest'
import type { ChatResponse, ModelsResponse } from '../api/types'

describe('MSW backend API mocks', () => {
  it('returns configured models', async () => {
    const response = await fetch('/models')
    const body = (await response.json()) as ModelsResponse

    expect(response.ok).toBe(true)
    expect(body.models).toHaveLength(1)
    expect(body.models[0]?.id).toBe('local-ollama-llama')
    expect(body.models[0]?.availability).toBe('AVAILABLE')
  })

  it('returns a deterministic chat response', async () => {
    const response = await fetch('/chat', {
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
