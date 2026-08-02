import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import type { ChatModelsResponse, ChatRequest } from './api/types'
import { apiUrl } from './config'
import {
  availableOllamaModel,
  culinarySubject,
  passiveHouseSubject,
} from './test/fixtures'
import { server } from './test/server'

const defaultMessage = passiveHouseSubject.defaultQuestion
const defaultSelectionSummary = `${passiveHouseSubject.displayName} · Ollama Nomic Embed (v1.5, 768d) · Local Ollama Llama`
const originalScrollIntoView = window.HTMLElement.prototype.scrollIntoView

afterEach(() => {
  window.HTMLElement.prototype.scrollIntoView = originalScrollIntoView
})

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
        `Ask about ${passiveHouseSubject.displayName}. Answers use the indexed bundled documents for this subject.`,
      ),
    ).toBeVisible()
  })

  it('shows misconfigured availability with a gray status dot', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'local-ollama-llama',
          chatApiTimeoutSeconds: 60,
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
      culinarySubject.id,
    )

    expect(messageField).toHaveValue(culinarySubject.defaultQuestion)
    expect(messageField).toHaveProperty(
      'selectionStart',
      culinarySubject.defaultQuestion.length,
    )
    expect(messageField).toHaveProperty(
      'selectionEnd',
      culinarySubject.defaultQuestion.length,
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
      culinarySubject.id,
    )

    expect(messageField).toHaveValue('Custom question')
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
        subjectId: passiveHouseSubject.id,
        modelId: 'local-ollama-llama',
        embeddingModelId: 'ollama-nomic-embed',
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
                  modelId: 'local-ollama-llama',
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
          modelId: 'local-ollama-llama',
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

  it('prevents chat when selected chat model is unavailable', async () => {
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'local-ollama-llama',
          chatApiTimeoutSeconds: 60,
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

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected chat model is unavailable and cannot be used.',
      ),
    ).toHaveAttribute('role', 'alert')
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

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected embedding model is configured and cannot be used.',
      ),
    ).toHaveAttribute('role', 'alert')
  })

  it('prevents chat when selected chat and embedding models are not available', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/chat-models'), () =>
        HttpResponse.json<ChatModelsResponse>({
          defaultChatModelId: 'local-ollama-llama',
          chatApiTimeoutSeconds: 60,
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

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText('Selected chat and embedding models are not available.'),
    ).toHaveAttribute('role', 'alert')
  })
})
