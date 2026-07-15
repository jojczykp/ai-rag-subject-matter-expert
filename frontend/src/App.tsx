import { useEffect, useMemo, useRef, useState } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import './App.css'
import {
  ApiError,
  getChatModels,
  getEmbeddingModels,
  postChat,
} from './api/client'
import type { ChatModel, EmbeddingModel } from './api/types'

type ChatMessage = {
  id: number
  role: 'user' | 'assistant'
  content: string
}

const defaultMessage = 'How should I cook rice?'

function App() {
  const [models, setModels] = useState<ChatModel[]>([])
  const [embeddingModels, setEmbeddingModels] = useState<EmbeddingModel[]>([])
  const [modelsLoading, setModelsLoading] = useState(true)
  const [modelsError, setModelsError] = useState<string | null>(null)
  const [chatApiTimeoutSeconds, setChatApiTimeoutSeconds] = useState(60)
  const [embeddingApiTimeoutSeconds, setEmbeddingApiTimeoutSeconds] =
    useState(60)
  const [selectedModelId, setSelectedModelId] = useState('')
  const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState('')
  const [message, setMessage] = useState(defaultMessage)
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatError, setChatError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [requestElapsedSeconds, setRequestElapsedSeconds] = useState(0)
  const messageInputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    let active = true

    Promise.all([getChatModels(), getEmbeddingModels()])
      .then(([chatModelsResponse, embeddingModelsResponse]) => {
        if (!active) {
          return
        }
        const chatModels = chatModelsResponse.chatModels
        const embeddingModels = embeddingModelsResponse.embeddingModels
        const enabledEmbeddingModels = embeddingModels.filter(
          (model) => model.enabled,
        )

        setModels(chatModels)
        setEmbeddingModels(embeddingModels)
        setChatApiTimeoutSeconds(chatModelsResponse.chatApiTimeoutSeconds)
        setEmbeddingApiTimeoutSeconds(
          embeddingModelsResponse.embeddingApiTimeoutSeconds,
        )
        setSelectedModelId(
          defaultModelId(chatModels, chatModelsResponse.defaultChatModelId),
        )
        setSelectedEmbeddingModelId(
          defaultModelId(
            enabledEmbeddingModels,
            embeddingModelsResponse.defaultEmbeddingModelId,
          ),
        )
        setModelsError(null)
      })
      .catch((error: unknown) => {
        if (!active) {
          return
        }
        setModelsError(errorMessage(error, 'Could not load configured models.'))
      })
      .finally(() => {
        if (active) {
          setModelsLoading(false)
        }
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const messageInput = messageInputRef.current
    messageInput?.focus()
    messageInput?.setSelectionRange(messageInput.value.length, messageInput.value.length)
  }, [])

  useEffect(() => {
    if (!sending) {
      return undefined
    }

    const intervalId = window.setInterval(() => {
      setRequestElapsedSeconds((current) => current + 1)
    }, 1000)

    return () => window.clearInterval(intervalId)
  }, [sending])

  const selectedModel = useMemo(
    () => models.find((model) => model.id === selectedModelId),
    [models, selectedModelId],
  )
  const enabledEmbeddingModels = useMemo(
    () => embeddingModels.filter((model) => model.enabled),
    [embeddingModels],
  )
  const selectedEmbeddingModel = useMemo(
    () =>
      enabledEmbeddingModels.find(
        (model) => model.id === selectedEmbeddingModelId,
      ),
    [enabledEmbeddingModels, selectedEmbeddingModelId],
  )
  const trimmedMessage = message.trim()
  const chatModelCanChat = selectedModel?.availability === 'AVAILABLE'
  const embeddingModelCanChat =
    selectedEmbeddingModel?.availability === 'AVAILABLE'
  const modelCanChat = chatModelCanChat && embeddingModelCanChat
  const modelBlockingMessage = modelAvailabilityBlockingMessage(
    selectedModel,
    selectedEmbeddingModel,
  )
  const sendDisabled =
    sending ||
    !selectedModel ||
    !selectedEmbeddingModel ||
    !trimmedMessage ||
    !modelCanChat
  const selectionSummary =
    selectedModel && selectedEmbeddingModel
      ? `Chat: ${selectedModel.displayName} · Embedding: ${selectedEmbeddingModel.displayName}`
      : null

  async function submitChat() {
    if (sendDisabled || !selectedModel || !selectedEmbeddingModel) {
      return
    }

    const userMessage: ChatMessage = {
      id: Date.now(),
      role: 'user',
      content: trimmedMessage,
    }
    setChatMessages((current) => [...current, userMessage])
    setMessage('')
    setChatError(null)
    setRequestElapsedSeconds(0)
    setSending(true)

    try {
      const response = await postChat({
        modelId: selectedModel.id,
        embeddingModelId: selectedEmbeddingModel.id,
        message: trimmedMessage,
      })
      setChatMessages((current) => [
        ...current,
        {
          id: Date.now() + 1,
          role: 'assistant',
          content: response.answer,
        },
      ])
    } catch (error: unknown) {
      setChatError(errorMessage(error, 'The selected model could not answer.'))
    } finally {
      setSending(false)
    }
  }

  function handleMessageKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      void submitChat()
    }
  }

  return (
    <main className="app-shell">
      <section className="model-panel" aria-labelledby="model-panel-heading">
        <div className="panel-heading">
          <p className="eyebrow">AI Subject Matter Expert</p>
          <h1 id="model-panel-heading">Chat workspace</h1>
        </div>

        <label className="field" htmlFor="embedding-model">
          <span>Embedding Model</span>
          <select
            id="embedding-model"
            value={selectedEmbeddingModelId}
            onChange={(event) =>
              setSelectedEmbeddingModelId(event.target.value)
            }
            disabled={modelsLoading || enabledEmbeddingModels.length === 0}
          >
            {enabledEmbeddingModels.map((model) => (
              <option key={model.id} value={model.id}>
                {embeddingModelOptionLabel(model)}
              </option>
            ))}
          </select>
        </label>

        <EmbeddingModelDetails model={selectedEmbeddingModel} />

        <label className="field" htmlFor="chat-model">
          <span>Chat Model</span>
          <select
            id="chat-model"
            value={selectedModelId}
            onChange={(event) => setSelectedModelId(event.target.value)}
            disabled={modelsLoading || models.length === 0}
          >
            {models.map((model) => (
              <option key={model.id} value={model.id}>
                {model.displayName}
              </option>
            ))}
          </select>
        </label>

        {modelsLoading && (
          <p className="status">Loading configured models...</p>
        )}
        {modelsError && <p className="status status-error">{modelsError}</p>}
        {!modelsLoading && !modelsError && models.length === 0 && (
          <p className="status status-error">No chat models are configured.</p>
        )}
        {!modelsLoading &&
          !modelsError &&
          enabledEmbeddingModels.length === 0 && (
            <p className="status status-error">
              No enabled embedding models are configured.
            </p>
          )}

        <ModelDetails model={selectedModel} />
      </section>

      <section className="chat-panel" aria-labelledby="chat-heading">
        <div className="chat-header">
          <div>
            <p className="eyebrow">Single subject</p>
            <h2 id="chat-heading">Ask a question</h2>
          </div>
          {selectionSummary && (
            <p className="chat-summary">{selectionSummary}</p>
          )}
        </div>

        <div className="messages" aria-live="polite">
          {chatMessages.length === 0 ? (
            <div className="empty-state">
              Ask a question and the answer will use the indexed bundled
              documents as context.
            </div>
          ) : (
            chatMessages.map((chatMessage) => (
              <article
                key={chatMessage.id}
                className={`message message-${chatMessage.role}`}
              >
                <span>{chatMessage.role === 'user' ? 'You' : 'Assistant'}</span>
                <p>{renderMessageContent(chatMessage.content)}</p>
              </article>
            ))
          )}
          {sending && (
            <RequestProgress
              elapsedSeconds={requestElapsedSeconds}
              embeddingApiTimeoutSeconds={embeddingApiTimeoutSeconds}
              chatApiTimeoutSeconds={chatApiTimeoutSeconds}
            />
          )}
        </div>

        {chatError && <p className="status status-error">{chatError}</p>}
        {modelBlockingMessage && (
          <p className="status status-error">{modelBlockingMessage}</p>
        )}

        <form
          className="chat-form"
          onSubmit={(event) => {
            event.preventDefault()
            void submitChat()
          }}
        >
          <label className="field" htmlFor="message">
            <span>Message</span>
            <textarea
              id="message"
              ref={messageInputRef}
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleMessageKeyDown}
              placeholder="Ask about the bundled subject documents..."
              rows={4}
            />
          </label>
          <button type="submit" disabled={sendDisabled}>
            {sending ? 'Sending...' : 'Send'}
          </button>
        </form>
      </section>
    </main>
  )
}

function renderMessageContent(content: string): ReactNode[] {
  const nodes: ReactNode[] = []
  const boldPattern = /\*\*(.+?)\*\*/g
  let lastIndex = 0

  for (const match of content.matchAll(boldPattern)) {
    const [matchedText, boldText] = match
    const matchIndex = match.index

    if (matchIndex > lastIndex) {
      nodes.push(content.slice(lastIndex, matchIndex))
    }

    nodes.push(<strong key={matchIndex}>{boldText}</strong>)
    lastIndex = matchIndex + matchedText.length
  }

  if (lastIndex < content.length) {
    nodes.push(content.slice(lastIndex))
  }

  return nodes
}

function defaultModelId<T extends { id: string }>(
  models: T[],
  configuredDefaultModelId: string | null,
): string {
  if (
    configuredDefaultModelId &&
    models.some((model) => model.id === configuredDefaultModelId)
  ) {
    return configuredDefaultModelId
  }

  return models[0]?.id ?? ''
}

function EmbeddingModelDetails({
  model,
}: {
  model: EmbeddingModel | undefined
}) {
  if (!model) {
    return (
      <div className="model-details embedding-model-details model-details-empty">
        Embedding model availability, dimensions, mode, and privacy appear here
        after selection.
      </div>
    )
  }

  return (
    <div className="model-details embedding-model-details">
      <div>
        <span>Availability</span>
        <AvailabilityValue availability={model.availability} />
      </div>
      <div>
        <span>Dimensions</span>
        <strong>{model.dimensions ?? 'Unknown'}</strong>
      </div>
      <div>
        <span>Mode</span>
        <strong>{formatModelMode(model.mode)}</strong>
      </div>
      <div>
        <span>Privacy</span>
        <strong>
          {embeddingQueryMayLeaveLocalMachine(model)
            ? 'Prompts may leave this machine'
            : 'Prompts stay local'}
        </strong>
      </div>
    </div>
  )
}

function embeddingModelOptionLabel(model: EmbeddingModel): string {
  return model.displayName
}

function embeddingQueryMayLeaveLocalMachine(model: EmbeddingModel): boolean {
  return model.mode === 'ONLINE'
}

function RequestProgress({
  elapsedSeconds,
  embeddingApiTimeoutSeconds,
  chatApiTimeoutSeconds,
}: {
  elapsedSeconds: number
  embeddingApiTimeoutSeconds: number
  chatApiTimeoutSeconds: number
}) {
  const requestTimeoutSeconds = Math.max(
    embeddingApiTimeoutSeconds,
    chatApiTimeoutSeconds,
  )
  const requestRemainingSeconds = remainingSeconds(
    requestTimeoutSeconds,
    elapsedSeconds,
  )

  return (
    <div className="status request-progress" role="status">
      <p>{`Processing request: ${requestRemainingSeconds}s remaining`}</p>
      <p>This includes embedding-based retrieval and chat model generation.</p>
    </div>
  )
}

function remainingSeconds(timeoutSeconds: number, elapsedSeconds: number) {
  return Math.max(timeoutSeconds - elapsedSeconds, 0)
}

function AvailabilityValue({ availability }: { availability: string }) {
  return (
    <strong className="availability-value">
      <span
        aria-hidden="true"
        className={`availability-dot availability-dot-${availabilityTone(availability)}`}
      />
      {formatModelAvailability(availability)}
    </strong>
  )
}

function ModelDetails({ model }: { model: ChatModel | undefined }) {
  if (!model) {
    return (
      <div className="model-details model-details-empty">
        Model availability, privacy, and runtime requirements appear here after
        selection.
      </div>
    )
  }

  return (
    <div className="model-details">
      <div>
        <span>Availability</span>
        <AvailabilityValue availability={model.availability} />
      </div>
      <div>
        <span>Mode</span>
        <strong>{formatModelMode(model.mode)}</strong>
      </div>
      <div>
        <span>Privacy</span>
        <strong>
          {model.promptsMayLeaveLocalMachine
            ? 'Prompts may leave this machine'
            : 'Prompts stay local'}
        </strong>
      </div>
      {model.description && <p>{model.description}</p>}
    </div>
  )
}

function formatModelMode(mode: string): string {
  if (mode === 'EMBEDDED_OFFLINE') {
    return 'Offline'
  }

  const label = mode.toLowerCase().replaceAll('_', ' ')
  return `${label.charAt(0).toUpperCase()}${label.slice(1)}`
}

function formatModelAvailability(availability: string): string {
  const label = availability.toLowerCase().replaceAll('_', ' ')
  return `${label.charAt(0).toUpperCase()}${label.slice(1)}`
}

function availabilityTone(
  availability: string,
): 'green' | 'amber' | 'red' | 'gray' {
  if (availability === 'AVAILABLE') {
    return 'green'
  }

  if (availability === 'CONFIGURED') {
    return 'amber'
  }

  if (availability === 'MISCONFIGURED') {
    return 'gray'
  }

  return 'red'
}

function modelAvailabilityBlockingMessage(
  chatModel: ChatModel | undefined,
  embeddingModel: EmbeddingModel | undefined,
): string | null {
  const chatBlocked = chatModel && chatModel.availability !== 'AVAILABLE'
  const embeddingBlocked =
    embeddingModel && embeddingModel.availability !== 'AVAILABLE'

  if (chatBlocked && embeddingBlocked) {
    return 'Selected chat and embedding models are not available.'
  }

  if (chatBlocked && chatModel) {
    return `Selected chat model is ${formatModelAvailability(chatModel.availability).toLowerCase()} and cannot be used.`
  }

  if (embeddingBlocked && embeddingModel) {
    return `Selected embedding model is ${formatModelAvailability(embeddingModel.availability).toLowerCase()} and cannot be used.`
  }

  return null
}

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return fallback
}

export default App
