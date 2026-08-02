import { expect, test } from '@playwright/test'

test('user can select a model and receive a chat response', async ({
  page,
}) => {
  await page.route('**/subjects', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        defaultSubjectId: 'passive-house',
        subjects: [
          {
            id: 'passive-house',
            enabled: true,
            displayOrder: 10,
            displayName: 'Passive House Architecture Expert',
            defaultQuestion:
              'I am designing a 160 m² house in southern Germany.',
          },
        ],
      }),
    })
  })
  await page.route('**/embedding-models', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        defaultEmbeddingModelId: 'ollama-nomic-embed',
        embeddingApiTimeoutSeconds: 60,
        embeddingModels: [
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
    })
  })
  await page.route('**/chat-models', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        defaultChatModelId: 'local-ollama-llama',
        chatApiTimeoutSeconds: 60,
        chatModels: [
          {
            id: 'local-ollama-llama',
            displayName: 'Local Ollama Llama',
            description: 'Local Ollama model for development.',
            runtime: 'OLLAMA',
            mode: 'LOCAL_SERVER',
            availability: 'AVAILABLE',
            availableOffline: false,
            promptsMayLeaveLocalMachine: false,
            capabilities: ['CHAT'],
            runtimeRequirements: ['REQUIRES_OLLAMA_SERVER'],
          },
        ],
      }),
    })
  })
  await page.route('**/chat', async (route) => {
    const request = route.request().postDataJSON() as {
      modelId: string
      message: string
    }

    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        modelId: request.modelId,
        answer: `E2E answer for: ${request.message}`,
      }),
    })
  })

  await page.goto('/')

  await expect(
    page.getByRole('heading', { name: 'Chat workspace' }),
  ).toBeVisible()
  await expect(page.getByLabel('Subject')).toHaveValue('passive-house')
  await expect(page.getByLabel('Embedding Model')).toHaveValue(
    'ollama-nomic-embed',
  )
  await expect(page.getByLabel('Chat Model')).toHaveValue('local-ollama-llama')

  await expect(page.getByText('Available')).toHaveCount(2)
  await expect(page.getByText('Prompts stay local')).toHaveCount(2)

  await page.getByLabel('Message').fill('How should I reduce heat loss?')
  await page.getByRole('button', { name: 'Send' }).click()

  await expect(
    page.getByText('How should I reduce heat loss?', { exact: true }),
  ).toBeVisible()
  await expect(
    page.getByText('E2E answer for: How should I reduce heat loss?'),
  ).toBeVisible()
})
