import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type {
  ChatModelsResponse,
  ChatRequest,
  EmbeddingModelsResponse,
} from './api/types'
import { apiUrl } from './config'
import {
  availableEmbeddedQwenModel,
  culinarySubject,
  passiveHouseSubject,
} from './test/fixtures'
import { server } from './test/server'

const defaultMessage = culinarySubject.defaultQuestion
const defaultSelectionSummary = `${culinarySubject.displayName} · Local BGE Small (1.5, 384d) · Embedded Qwen 1.5B`
const originalScrollIntoView = window.HTMLElement.prototype.scrollIntoView

afterEach(() => {
  window.HTMLElement.prototype.scrollIntoView = originalScrollIntoView
})

describe('App', () => {
  it('loads models and shows selected model details', async () => {
    const { container } = render(<App />)

    expect(screen.getByText('Loading configured models...')).toBeVisible()

    expect(await screen.findByLabelText('Embedding Model')).toHaveValue(
      'local-bge-small',
    )
    expect(screen.getByLabelText('Chat Model')).toHaveValue(
      'embedded-qwen-1-5b',
    )
    expect(
      screen.getByRole('option', { name: 'Local BGE Small (1.5, 384d)' }),
    ).toBeVisible()
    expect(screen.getByText('384')).toBeVisible()
    expect(screen.getAllByText('Offline')).toHaveLength(2)
    expect(screen.getAllByText('Prompts stay local')).toHaveLength(2)
    expect(screen.getAllByText('Available')).toHaveLength(2)
    expect(screen.getByText(defaultSelectionSummary)).toBeVisible()
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
    expect(
      screen.getByText(
        `Ask about ${culinarySubject.displayName}. Answers use the indexed bundled documents for this subject.`,
      ),
    ).toBeVisible()
  })

  it('shows misconfigured availability with a gray status dot', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'embedded-qwen-1-5b',
          chatApiTimeoutSeconds: 60,
          chatModels: [
            {
              ...availableEmbeddedQwenModel,
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

  it('identifies which configuration area failed to load', async () => {
    server.use(
      http.get(apiUrl('/embedding-models'), () =>
        HttpResponse.json({ message: 'Failed' }, { status: 500 }),
      ),
    )

    render(<App />)

    expect(
      await screen.findByText('Could not load embedding models.'),
    ).toHaveAttribute('role', 'alert')
  })

  it('focuses the message field on load', async () => {
    render(<App />)

    const messageField = screen.getByLabelText('Message')

    expect(messageField).toHaveFocus()
    expect(messageField).toHaveValue('')

    await screen.findByText(defaultSelectionSummary)

    expect(messageField).toHaveValue(defaultMessage)
    expect(messageField).toHaveProperty('selectionStart', defaultMessage.length)
    expect(messageField).toHaveProperty('selectionEnd', defaultMessage.length)
  })

  it('updates the message field when changing subject', async () => {
    const user = userEvent.setup()

    render(<App />)

    const messageField = screen.getByLabelText('Message')
    await screen.findByText(defaultSelectionSummary)

    expect(messageField).toHaveValue(defaultMessage)

    await user.selectOptions(
      screen.getByLabelText('Subject'),
      passiveHouseSubject.id,
    )

    expect(messageField).toHaveValue(passiveHouseSubject.defaultQuestion)
    expect(messageField).toHaveProperty(
      'selectionStart',
      passiveHouseSubject.defaultQuestion.length,
    )
    expect(messageField).toHaveProperty(
      'selectionEnd',
      passiveHouseSubject.defaultQuestion.length,
    )
  })

  it('preserves edited message when changing subject', async () => {
    const user = userEvent.setup()

    render(<App />)

    const messageField = screen.getByLabelText('Message')
    await screen.findByText(defaultSelectionSummary)
    await user.clear(messageField)
    await user.type(messageField, 'Custom question')
    await user.selectOptions(
      screen.getByLabelText('Subject'),
      passiveHouseSubject.id,
    )

    expect(messageField).toHaveValue('Custom question')
  })

  it('records selection changes in the chat transcript', async () => {
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

    const { container } = render(<App />)

    await screen.findByText(defaultSelectionSummary)
    expect(screen.queryByText('Selection changed')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Send' }))
    await screen.findByText(`Mock answer for: ${defaultMessage}`)

    await user.selectOptions(
      screen.getByLabelText('Subject'),
      passiveHouseSubject.id,
    )
    await user.selectOptions(
      screen.getByLabelText('Embedding Model'),
      'ollama-nomic-embed',
    )
    await user.selectOptions(
      screen.getByLabelText('Chat Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Second question')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(
      await screen.findByText('Mock answer for: Second question'),
    ).toBeVisible()
    expect(
      screen.getByText(
        `Subject changed to ${passiveHouseSubject.displayName}.`,
      ),
    ).toBeVisible()
    expect(
      screen.getByText(
        'Embedding model changed to Ollama Nomic Embed (v1.5, 768d).',
      ),
    ).toBeVisible()
    expect(
      screen.getByText('Chat model changed to Local Ollama Llama.'),
    ).toBeVisible()
    expect(container.querySelectorAll('.message-system')).toHaveLength(3)
    expect(
      Array.from(container.querySelectorAll('.message')).map((element) =>
        element.textContent?.trim(),
      ),
    ).toEqual([
      `You${defaultMessage}`,
      `AssistantMock answer for: ${defaultMessage}`,
      `Selection changedSubject changed to ${passiveHouseSubject.displayName}.`,
      'Selection changedEmbedding model changed to Ollama Nomic Embed (v1.5, 768d).',
      'Selection changedChat model changed to Local Ollama Llama.',
      'YouSecond question',
      'AssistantMock answer for: Second question',
    ])
    expect(chatRequests).toEqual([
      {
        subjectId: culinarySubject.id,
        modelId: 'embedded-qwen-1-5b',
        embeddingModelId: 'local-bge-small',
        message: defaultMessage,
      },
      {
        subjectId: passiveHouseSubject.id,
        modelId: 'local-ollama-llama',
        embeddingModelId: 'ollama-nomic-embed',
        message: 'Second question',
      },
    ])
  })

  it('clears current chat content without changing selections', async () => {
    const user = userEvent.setup()

    render(<App />)

    const clearButton = screen.getByRole('button', { name: 'Clear chat' })

    expect(clearButton).toBeDisabled()

    await screen.findByText(defaultSelectionSummary)
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(
      await screen.findByText(`Mock answer for: ${defaultMessage}`),
    ).toBeVisible()
    expect(clearButton).toBeEnabled()

    await user.click(clearButton)

    expect(screen.queryByText(defaultMessage)).not.toBeInTheDocument()
    expect(
      screen.queryByText(`Mock answer for: ${defaultMessage}`),
    ).not.toBeInTheDocument()
    expect(clearButton).toBeDisabled()
    expect(screen.getByLabelText('Subject')).toHaveValue(culinarySubject.id)
    expect(screen.getByLabelText('Embedding Model')).toHaveValue(
      'local-bge-small',
    )
    expect(screen.getByLabelText('Chat Model')).toHaveValue(
      'embedded-qwen-1-5b',
    )
    expect(
      screen.getByText(
        `Ask about ${culinarySubject.displayName}. Answers use the indexed bundled documents for this subject.`,
      ),
    ).toBeVisible()
  })

  it('restores selected subject question on request', async () => {
    const user = userEvent.setup()

    render(<App />)

    const messageField = screen.getByLabelText('Message')
    await screen.findByText(defaultSelectionSummary)
    await user.clear(messageField)
    await user.type(messageField, 'Custom question')
    await user.click(
      screen.getByRole('button', { name: 'Use default question' }),
    )

    expect(messageField).toHaveValue(defaultMessage)
    expect(messageField).toHaveProperty('selectionStart', defaultMessage.length)
    expect(messageField).toHaveProperty('selectionEnd', defaultMessage.length)
  })

  it('prefills a message and still requires non-blank content', async () => {
    const user = userEvent.setup()

    render(<App />)

    const sendButton = screen.getByRole('button', { name: 'Send' })

    expect(sendButton).toBeDisabled()
    expect(screen.getByText('Models are still loading.')).toHaveAttribute(
      'role',
      'status',
    )

    await screen.findByText(defaultSelectionSummary)

    expect(sendButton).toBeEnabled()

    await user.clear(screen.getByLabelText('Message'))

    expect(sendButton).toBeDisabled()
    expect(screen.getByText('Enter a message.')).toHaveAttribute(
      'role',
      'status',
    )
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

    await screen.findByText(defaultSelectionSummary)
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(screen.getByText(defaultMessage)).toBeVisible()
    expect(
      await screen.findByText(`Mock answer for: ${defaultMessage}`),
    ).toBeVisible()
    expect(chatRequests).toEqual([
      {
        subjectId: culinarySubject.id,
        modelId: 'embedded-qwen-1-5b',
        embeddingModelId: 'local-bge-small',
        message: defaultMessage,
      },
    ])
  })

  it('scrolls to new chat messages', async () => {
    const scrollIntoView = vi.fn()
    window.HTMLElement.prototype.scrollIntoView = scrollIntoView
    const user = userEvent.setup()

    render(<App />)

    await screen.findByText(defaultSelectionSummary)
    scrollIntoView.mockClear()
    await user.click(screen.getByRole('button', { name: 'Send' }))

    await screen.findByText(`Mock answer for: ${defaultMessage}`)

    expect(scrollIntoView).toHaveBeenCalled()
  })

  it('shows request countdown while waiting for a response', async () => {
    server.use(
      http.post(
        apiUrl('/chat'),
        () =>
          new Promise((resolve) => {
            setTimeout(() => {
              resolve(
                HttpResponse.json({
                  modelId: 'embedded-qwen-1-5b',
                  answer: 'Done',
                }),
              )
            }, 1500)
          }),
      ),
    )

    const user = userEvent.setup()

    render(<App />)

    await screen.findByText(defaultSelectionSummary)
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(screen.getByLabelText('Subject')).toBeDisabled()
    expect(screen.getByLabelText('Embedding Model')).toBeDisabled()
    expect(screen.getByLabelText('Chat Model')).toBeDisabled()
    expect(screen.getByText('Processing request: 60s remaining')).toBeVisible()
    expect(
      screen.getByText(
        'This includes embedding-based retrieval and chat model generation.',
      ),
    ).toBeVisible()

    expect(
      await screen.findByText('Processing request: 59s remaining'),
    ).toBeVisible()
  })

  it('renders double star phrases in chat messages as bold text', async () => {
    const user = userEvent.setup()
    server.use(
      http.post(apiUrl('/chat'), () =>
        HttpResponse.json({
          modelId: 'embedded-qwen-1-5b',
          answer: 'Use **one cup** of rice.',
        }),
      ),
    )

    render(<App />)

    await screen.findByText(defaultSelectionSummary)
    await user.click(screen.getByRole('button', { name: 'Send' }))

    const boldText = await screen.findByText('one cup')
    const assistantResponse = boldText.closest('p')

    expect(boldText.tagName).toBe('STRONG')
    expect(assistantResponse).toHaveTextContent('Use one cup of rice.')
  })

  it('sends chat messages when pressing enter in the message field', async () => {
    const user = userEvent.setup()

    render(<App />)

    await screen.findByText(defaultSelectionSummary)
    await user.keyboard('{Enter}')

    expect(screen.getByText(defaultMessage)).toBeVisible()
    expect(
      await screen.findByText(`Mock answer for: ${defaultMessage}`),
    ).toBeVisible()
  })

  it('adds a newline without sending when pressing shift enter', async () => {
    const user = userEvent.setup()

    render(<App />)

    await screen.findByText(defaultSelectionSummary)

    const messageField = screen.getByLabelText('Message')
    await user.clear(messageField)
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

    await screen.findByText(defaultSelectionSummary)
    await user.clear(screen.getByLabelText('Message'))
    await user.type(screen.getByLabelText('Message'), 'Will this fail?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Provider failed.')).toBeVisible()
  })

  it('clears chat errors from the transcript area', async () => {
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

    await screen.findByText(defaultSelectionSummary)
    await user.click(screen.getByRole('button', { name: 'Send' }))
    expect(await screen.findByText('Provider failed.')).toBeVisible()

    await user.click(screen.getByRole('button', { name: 'Clear chat' }))

    expect(screen.queryByText(defaultMessage)).not.toBeInTheDocument()
    expect(screen.queryByText('Provider failed.')).not.toBeInTheDocument()
  })

  it('prevents chat when selected chat model is unavailable', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'embedded-qwen-1-5b',
          chatApiTimeoutSeconds: 60,
          chatModels: [
            {
              ...availableEmbeddedQwenModel,
              availability: 'UNAVAILABLE',
            },
          ],
        }),
      ),
    )

    render(<App />)

    await screen.findByLabelText('Embedding Model')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected chat model is unavailable and cannot be used.',
      ),
    ).toHaveAttribute('role', 'alert')
  })

  it('prevents chat when selected embedding model is not available', async () => {
    server.use(
      http.get(apiUrl('/embedding-models'), () =>
        HttpResponse.json<EmbeddingModelsResponse>({
          defaultEmbeddingModelId: 'local-bge-small',
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
          ],
        }),
      ),
    )

    render(<App />)

    await screen.findByLabelText('Embedding Model')
    await screen.findByLabelText('Chat Model')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected embedding model is configured and cannot be used.',
      ),
    ).toHaveAttribute('role', 'alert')
  })

  it('prevents chat when selected chat and embedding models are not available', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'embedded-qwen-1-5b',
          chatApiTimeoutSeconds: 60,
          chatModels: [
            {
              ...availableEmbeddedQwenModel,
              availability: 'MISCONFIGURED',
            },
          ],
        }),
      ),
      http.get(apiUrl('/embedding-models'), () =>
        HttpResponse.json<EmbeddingModelsResponse>({
          defaultEmbeddingModelId: 'local-bge-small',
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
          ],
        }),
      ),
    )

    render(<App />)

    await screen.findByLabelText('Embedding Model')
    await screen.findByLabelText('Chat Model')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText('Selected chat and embedding models are not available.'),
    ).toHaveAttribute('role', 'alert')
  })
})
