import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from './App'
import type { ModelsResponse } from './api/types'
import { apiUrl } from './config'
import { availableOllamaModel } from './test/fixtures'
import { server } from './test/server'

describe('App', () => {
  it('loads models and shows selected model details', async () => {
    const user = userEvent.setup()

    render(<App />)

    expect(screen.getByText('Loading configured models...')).toBeVisible()

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
    )

    expect(screen.getByText('AVAILABLE')).toBeVisible()
    expect(screen.getByText('LOCAL SERVER')).toBeVisible()
    expect(screen.getByText('Prompts stay local')).toBeVisible()
    expect(screen.getByText('requires local ollama')).toBeVisible()
  })

  it('requires a selected model and message before sending', async () => {
    const user = userEvent.setup()

    render(<App />)

    const sendButton = screen.getByRole('button', { name: 'Send' })

    expect(sendButton).toBeDisabled()

    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')

    expect(sendButton).toBeDisabled()

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
    )

    expect(sendButton).toBeEnabled()
  })

  it('sends chat messages with the selected model', async () => {
    const user = userEvent.setup()

    render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'How should I cook rice?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(screen.getByText('How should I cook rice?')).toBeVisible()
    expect(
      await screen.findByText('Mock answer for: How should I cook rice?'),
    ).toBeVisible()
  })

  it('sends chat messages when pressing enter in the message field', async () => {
    const user = userEvent.setup()

    render(<App />)

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
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

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
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

    await user.selectOptions(
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Will this fail?')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(await screen.findByText('Provider failed.')).toBeVisible()
  })

  it('prevents chat when selected model is unavailable', async () => {
    const user = userEvent.setup()
    server.use(
      http.get(apiUrl('/models'), () =>
        HttpResponse.json<ModelsResponse>({
          models: [
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
      await screen.findByLabelText('Model'),
      'local-ollama-llama',
    )
    await user.type(screen.getByLabelText('Message'), 'Can I use it?')

    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled()
    expect(
      screen.getByText(
        'Selected model is unavailable and cannot be used for chat.',
      ),
    ).toBeVisible()
  })
})
