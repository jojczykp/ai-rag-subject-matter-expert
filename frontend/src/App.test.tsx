import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from './App'
import type { ChatModelsResponse, ChatRequest } from './api/types'
import { apiUrl } from './config'
import { availableOllamaModel } from './test/fixtures'
import { server } from './test/server'

describe('App', () => {
  it('loads models and shows selected model details', async () => {
    const user = userEvent.setup()

    const { container } = render(<App />)

    expect(screen.getByText('Loading configured models...')).toBeVisible()

    await user.selectOptions(
      await screen.findByLabelText('Embedding Model'),
      'ollama-nomic-embed',
    )
    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )

    expect(screen.getByLabelText('Embedding Model')).toHaveValue(
      'ollama-nomic-embed',
    )
    expect(
      screen.getByRole('option', { name: 'Ollama Nomic Embed (v1.5)' }),
    ).toBeVisible()
    expect(screen.getByText('768')).toBeVisible()
    expect(screen.getAllByText('Local server')).toHaveLength(2)
    expect(screen.getAllByText('Prompts stay local')).toHaveLength(2)
    expect(screen.getAllByText('Available')).toHaveLength(2)
    expect(
      screen.getByText(
        'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed',
      ),
    ).toBeVisible()
    expect(container.querySelectorAll('.availability-dot-green')).toHaveLength(
      2,
    )
    expect(screen.queryByText('Runtime requirements')).not.toBeInTheDocument()
  })

  it('shows misconfigured availability with a gray status dot', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          chatModels: [
            {
              ...availableOllamaModel,
              availability: 'MISCONFIGURED',
            },
          ],
        }),
      ),
    )

    const { container } = render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )

    expect(screen.getByText('Misconfigured')).toBeVisible()
    expect(container.querySelectorAll('.availability-dot-gray')).toHaveLength(1)
  })

  it('requires a selected model and message before sending', async () => {
    const user = userEvent.setup()

    render(<App />)

    const sendButton = screen.getByRole('button', { name: 'Send' })

    expect(sendButton).toBeDisabled()

    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')

    expect(sendButton).toBeDisabled()

    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )

    expect(sendButton).toBeDisabled()

    await user.selectOptions(
      await screen.findByLabelText('Embedding Model'),
      'ollama-nomic-embed',
    )

    expect(sendButton).toBeEnabled()
  })

  it('sends chat messages with the selected model', async () => {
    const user = userEvent.setup()
    const chatRequests: ChatRequest[] = []
    server.use(
      http.post(apiUrl('/chat'), async ({ request }) => {
        const body = (await request.json()) as ChatRequest
        chatRequests.push(body)

        return HttpResponse.json({
          modelId: body.modelId,
          answer: `Mock answer for: ${body.message}`,
        })
      }),
    )

    render(<App />)

    await selectDefaultModels(user)
    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(screen.getByText('How should I cook rice?')).toBeVisible()
    expect(
      await screen.findByText('Mock answer for: How should I cook rice?'),
    ).toBeVisible()
    expect(chatRequests).toEqual([
      {
        modelId: 'local-ollama-llama',
        embeddingModelId: 'ollama-nomic-embed',
        message: 'How should I cook rice?',
      },
    ])
  })

  it('renders double star phrases in chat messages as bold text', async () => {
    const user = userEvent.setup()
    server.use(
      http.post(apiUrl('/chat'), () =>
        HttpResponse.json({
          modelId: 'local-ollama-llama',
          answer: 'Use **one cup** of rice.',
        }),
      ),
    )

    render(<App />)

    await selectDefaultModels(user)
    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    const boldText = await screen.findByText('one cup')

    expect(boldText.tagName).toBe('STRONG')
    expect(screen.getByText('Use', { exact: false })).toHaveTextContent(
      'Use one cup of rice.',
    )
  })

  it('sends chat messages when pressing enter in the message field', async () => {
    const user = userEvent.setup()

    render(<App />)

    await selectDefaultModels(user)
    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')
    await user.keyboard('{Enter}')

    expect(screen.getByText('How should I cook rice?')).toBeVisible()
    expect(
      await screen.findByText('Mock answer for: How should I cook rice?'),
    ).toBeVisible()
  })

  it('adds a newline without sending when pressing shift enter', async () => {
    const user = userEvent.setup()

    render(<App />)

    await selectDefaultModels(user)

    const messageField = screen.getByLabelText('Message')
    await user.type(messageField, 'Line one')
    await user.keyboard('{Shift>}{Enter}{/Shift}')
    await user.type(messageField, 'Line two')

    expect(messageField).toHaveValue('Line one\nLine two')
    expect(screen.queryByText(/Mock answer for:/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send' })).toBeEnabled()
  })

  it('displays provider errors returned by chat API', async () => {
    const user = userEvent.setup()
    server.use(
      http.post(apiUrl('/chat'), () =>
        HttpResponse.json(
          {
            code: 'PROVIDER_ERROR',
            message: 'Provider failed.',
          },
          { status: 502 },
        ),
      ),
    )

    render(<App />)

    await selectDefaultModels(user)
    await user.type(screen.getByLabelText('Message'), 'Will this fail?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Provider failed.')).toBeVisible()
  })

  it('prevents chat when selected chat model is unavailable', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          chatModels: [
            {
              ...availableOllamaModel,
              availability: 'UNAVAILABLE',
            },
          ],
        }),
      ),
    )

    render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Embedding Model'),
      'ollama-nomic-embed',
    )
    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Can I use it?')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected chat model is unavailable and cannot be used.',
      ),
    ).toBeVisible()
  })

  it('prevents chat when selected embedding model is not available', async () => {
    const user = userEvent.setup()

    render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Embedding Model'),
      'local-bge-small',
    )
    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Can I use it?')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected embedding model is configured and cannot be used.',
      ),
    ).toBeVisible()
  })

  it('prevents chat when selected chat and embedding models are not available', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          chatModels: [
            {
              ...availableOllamaModel,
              availability: 'MISCONFIGURED',
            },
          ],
        }),
      ),
    )

    render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Embedding Model'),
      'local-bge-small',
    )
    await user.selectOptions(
      await screen.findByLabelText('Chat Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Can I use it?')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText('Selected chat and embedding models are not available.'),
    ).toBeVisible()
  })
})

async function selectDefaultModels(user: ReturnType<typeof userEvent.setup>) {
  await user.selectOptions(
    await screen.findByLabelText('Embedding Model'),
    'ollama-nomic-embed',
  )
  await user.selectOptions(
    await screen.findByLabelText('Chat Model'),
    'local-ollama-llama',
  )
}
