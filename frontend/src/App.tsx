import { useEffect, useMemo, useState } from 'react'
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

function App() {
  const [models, setModels] = useState<ChatModel[]>([])
  const [embeddingModels, setEmbeddingModels] = useState<EmbeddingModel[]>([])
  const [modelsLoading, setModelsLoading] = useState(true)
  const [modelsError, setModelsError] = useState<string | null>(null)
  const [selectedModelId, setSelectedModelId] = useState('')
  const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState('')
  const [message, setMessage] = useState('')
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatError, setChatError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  useEffect(() => {
    let active = true

    Promise.all([getChatModels(), getEmbeddingModels()])
      .then(([chatModelsResponse, embeddingModelsResponse]) => {
        if (!active) {
          return
        }
        setModels(chatModelsResponse.chatModels)
        setEmbeddingModels(embeddingModelsResponse.embeddingModels)
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
  const modelCanChat =
    selectedModel?.availability === 'AVAILABLE' ||
    selectedModel?.availability === 'CONFIGURED'
  const sendDisabled =
    sending ||
    !selectedModel ||
    !selectedEmbeddingModel ||
    !trimmedMessage ||
    !modelCanChat

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
            <option value="">Select an embedding model</option>
            {enabledEmbeddingModels.map((model) => (
              <option key={model.id} value={model.id}>
                {model.displayName}
              </option>
            ))}
          </select>
        </label>

        <label className="field" htmlFor="chat-model">
          <span>Chat Model</span>
          <select
            id="chat-model"
            value={selectedModelId}
            onChange={(event) => setSelectedModelId(event.target.value)}
            disabled={modelsLoading || models.length === 0}
          >
            <option value="">Select a chat model</option>
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
          <p className="chat-summary">
            {selectedModel
              ? `Using ${selectedModel.displayName}`
              : 'Choose models to start'}
          </p>
        </div>

        <div className="messages" aria-live="polite">
          {chatMessages.length === 0 ? (
            <div className="empty-state">
              Select embedding and chat models, ask a question, and the answer
              will use the indexed bundled documents as context.
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
          {sending && <p className="status">Waiting for model response...</p>}
        </div>

        {chatError && <p className="status status-error">{chatError}</p>}
        {selectedModel && !modelCanChat && (
          <p className="status status-error">
            Selected model is {selectedModel.availability.toLowerCase()} and
            cannot be used for chat.
          </p>
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
        <strong>{model.availability}</strong>
      </div>
      <div>
        <span>Mode</span>
        <strong>{model.mode.replaceAll('_', ' ')}</strong>
      </div>
      <div>
        <span>Privacy</span>
        <strong>
          {model.promptsMayLeaveLocalMachine
            ? 'Prompts may leave this machine'
            : 'Prompts stay local'}
        </strong>
      </div>
      <div>
        <span>Runtime requirements</span>
        <strong>{runtimeRequirements(model)}</strong>
      </div>
      {model.description && <p>{model.description}</p>}
    </div>
  )
}

function runtimeRequirements(model: ChatModel): string {
  if (model.runtimeRequirements.length === 0) {
    return 'None'
  }

  return model.runtimeRequirements
    .map((requirement) => requirement.replaceAll('_', ' ').toLowerCase())
    .join(', ')
}

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message
  }

  return fallback
}

export default App
