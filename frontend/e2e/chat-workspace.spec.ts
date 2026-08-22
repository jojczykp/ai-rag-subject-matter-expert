import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

test('user can select a model and receive a chat response', async ({
  page,
}) => {
  await routeWorkspaceApi(page)

  await page.goto('/')

  await expect(
    page.getByRole('heading', { name: 'Ask a question' }),
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

test('chat transcript keeps a fixed height and scrolls internally', async ({
  page,
}) => {
  await routeWorkspaceApi(page, {
    answerPrefix:
      'E2E answer with enough detail to overflow the transcript. '.repeat(40),
  })

  await page.goto('/')

  const transcript = page.locator('.messages')
  const initialHeight = await transcript.evaluate(
    (element) => element.clientHeight,
  )

  for (const prompt of ['Question one?', 'Question two?', 'Question three?']) {
    await page.getByLabel('Message').fill(prompt)
    await page.getByRole('button', { name: 'Send' }).click()
    await expect(page.getByText(`E2E answer for: ${prompt}`)).toBeVisible()
  }

  await expect(transcript).toHaveCSS('overflow-y', 'auto')
  await expect
    .poll(() => transcript.evaluate((element) => element.clientHeight))
    .toBe(initialHeight)
  await expect
    .poll(() =>
      transcript.evaluate(
        (element) => element.scrollHeight > element.clientHeight,
      ),
    )
    .toBe(true)
})

async function routeWorkspaceApi(
  page: Page,
  options: { answerPrefix?: string } = {},
) {
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
        embeddingApiTimeoutSeconds: 180,
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
        chatApiTimeoutSeconds: 180,
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
        answer: `${options.answerPrefix ?? ''}E2E answer for: ${request.message}`,
      }),
    })
  })
}
