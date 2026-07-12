import { expect, test } from '@playwright/test'

test('user can select a model and receive a chat response', async ({
  page,
}) => {
  await page.route('**/models', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        models: [
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
            runtimeRequirements: ['REQUIRES_LOCAL_OLLAMA'],
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
  await page.getByLabel('Model').selectOption('local-ollama-llama')

  await expect(page.getByText('AVAILABLE')).toBeVisible()
  await expect(page.getByText('Prompts stay local')).toBeVisible()

  await page.getByLabel('Message').fill('How should I cook rice?')
  await page.getByRole('button', { name: 'Send' }).click()

  await expect(
    page.getByText('How should I cook rice?', { exact: true }),
  ).toBeVisible()
  await expect(
    page.getByText('E2E answer for: How should I cook rice?'),
  ).toBeVisible()
})
