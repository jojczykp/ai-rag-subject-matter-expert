import { describe, expect, it } from 'vitest'
import type {
  ChatResponse,
  ChatModelsResponse,
  EmbeddingModelsResponse,
  SubjectsResponse,
} from '../api/types'
import { apiUrl } from '../config'

describe('MSW backend API mocks', () => {
  it('returns configured models', async () => {
    const response = await fetch(apiUrl('/chat-models'))
    const body = (await response.json()) as ChatModelsResponse

    expect(response.ok).toBe(true)
    expect(body.defaultChatModelId).toBe('local-ollama-llama')
    expect(body.chatApiTimeoutSeconds).toBe(60)
    expect(body.chatModels).toHaveLength(1)
    expect(body.chatModels[0]?.id).toBe('local-ollama-llama')
    expect(body.chatModels[0]?.availability).toBe('AVAILABLE')
  })

  it('returns indexed subjects', async () => {
    const response = await fetch(apiUrl('/subjects'))
    const body = (await response.json()) as SubjectsResponse

    expect(response.ok).toBe(true)
    expect(body.defaultSubjectId).toBe('passive-house')
    expect(body.subjects).toEqual([
      {
        id: 'culinary-expert',
        enabled: true,
        displayOrder: 10,
        displayName: 'Culinary Expert',
        defaultQuestion: 'How should I cook rice?',
      },
      {
        id: 'passive-house',
        enabled: true,
        displayOrder: 20,
        displayName: 'Passive House Architecture Expert',
        defaultQuestion:
          'I am designing a 160 m² house in southern Germany. I want to achieve Passive House certification while keeping construction costs reasonable. Recommend wall, roof, floor, window, ventilation and heating specifications, explain why each choice matters, and identify the biggest design risks',
      },
    ])
  })

  it('returns configured embedding models', async () => {
    const response = await fetch(apiUrl('/embedding-models'))
    const body = (await response.json()) as EmbeddingModelsResponse

    expect(response.ok).toBe(true)
    expect(body.defaultEmbeddingModelId).toBe('ollama-nomic-embed')
    expect(body.embeddingApiTimeoutSeconds).toBe(60)
    expect(body.embeddingModels).toHaveLength(2)
    expect(body.embeddingModels[0]?.id).toBe('local-bge-small')
    expect(body.embeddingModels[0]?.availability).toBe('CONFIGURED')
    expect(body.embeddingModels[1]?.id).toBe('ollama-nomic-embed')
    expect(body.embeddingModels[1]?.availability).toBe('AVAILABLE')
  })

  it('returns a deterministic chat response', async () => {
    const response = await fetch(apiUrl('/chat'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        subjectId: 'culinary-expert',
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
