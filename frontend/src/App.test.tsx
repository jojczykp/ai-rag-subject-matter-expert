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
    const { container } = render(<App />)

    expect(screen.getByText('Loading configured models...')).toBeVisible()

    expect(await screen.findByLabelText('Embedding Model')).toHaveValue(
      'ollama-nomic-embed',
    )
    expect(screen.getByLabelText('Chat Model')).toHaveValue(
      'local-ollama-llama',
    )
    expect(
      screen.getByRole('option', { name: 'Ollama Nomic Embed (v1.5, 768d)' }),
    ).toBeVisible()
    expect(screen.getByText('768')).toBeVisible()
    expect(screen.getAllByText('Local server')).toHaveLength(2)
    expect(screen.getAllByText('Prompts stay local')).toHaveLength(2)
    expect(screen.getAllByText('Available')).toHaveLength(2)
    expect(
      screen.getByText(
        'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
      ),
    ).toBeVisible()
    expect(container.querySelectorAll('.availability-dot-green')).toHaveLength(
      2,
    )
    expect(screen.queryByText('Choose models to start')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('option', { name: 'Select an embedding model' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('option', { name: 'Select a chat model' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByText('Runtime requirements')).not.toBeInTheDocument()
  })

  it('shows misconfigured availability with a gray status dot', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'local-ollama-llama',
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

    await screen.findByLabelText('Chat Model')

    expect(screen.getByText('Misconfigured')).toBeVisible()
    expect(container.querySelectorAll('.availability-dot-gray')).toHaveLength(1)
  })

  it('requires a message before sending with default models', async () => {
    const user = userEvent.setup()

    render(<App />)

    const sendButton = screen.getByRole('button', { name: 'Send' })

    expect(sendButton).toBeDisabled()

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )

    expect(sendButton).toBeDisabled()

    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')

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

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )
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

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )
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

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )
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

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )

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

    await screen.findByText(
      'Chat: Local Ollama Llama · Embedding: Ollama Nomic Embed (v1.5, 768d)',
    )
    await user.type(screen.getByLabelText('Message'), 'Will this fail?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Provider failed.')).toBeVisible()
  })

  it('prevents chat when selected chat model is unavailable', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'local-ollama-llama',
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

    await screen.findByLabelText('Embedding Model')
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
          defaultChatModelId: 'local-ollama-llama',
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
    await screen.findByLabelText('Chat Model')
    await user.type(screen.getByLabelText('Message'), 'Can I use it?')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText('Selected chat and embedding models are not available.'),
    ).toBeVisible()
  })
})
