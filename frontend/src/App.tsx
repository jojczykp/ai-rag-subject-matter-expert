import { useEffect, useMemo, useRef, useState } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import './App.css'
import {
  ApiError,
  getChatModels,
  getEmbeddingModels,
  getSubjects,
  postChat,
} from './api/client'
import type { ChatModel, EmbeddingModel, Subject } from './api/types'

type ChatMessage = {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
}

type ThemeId = 'light' | 'forest' | 'sunrise' | 'clay' | 'rose' | 'night'

const themes: { id: ThemeId; label: string }[] = [
  { id: 'light', label: 'Light' },
  { id: 'forest', label: 'Forest' },
  { id: 'sunrise', label: 'Sunrise' },
  { id: 'clay', label: 'Clay' },
  { id: 'rose', label: 'Rose' },
  { id: 'night', label: 'Night' },
]

function App() {
  const [models, setModels] = useState<ChatModel[]>([])
  const [embeddingModels, setEmbeddingModels] = useState<EmbeddingModel[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [modelsLoading, setModelsLoading] = useState(true)
  const [modelsError, setModelsError] = useState<string | null>(null)
  const [chatApiTimeoutSeconds, setChatApiTimeoutSeconds] = useState(60)
  const [embeddingApiTimeoutSeconds, setEmbeddingApiTimeoutSeconds] =
    useState(60)
  const [selectedModelId, setSelectedModelId] = useState('')
  const [selectedEmbeddingModelId, setSelectedEmbeddingModelId] = useState('')
  const [selectedSubjectId, setSelectedSubjectId] = useState('')
  const [selectedThemeId, setSelectedThemeId] = useState<ThemeId>('light')
  const [message, setMessage] = useState('')
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([])
  const [chatError, setChatError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)
  const [requestElapsedSeconds, setRequestElapsedSeconds] = useState(0)
  const [focusMessageAtEndRequest, setFocusMessageAtEndRequest] = useState(0)
  const messageInputRef = useRef<HTMLTextAreaElement>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const nextMessageIdRef = useRef(0)

  useEffect(() => {
    let active = true

    async function loadConfiguration() {
      const [subjectsResult, embeddingModelsResult, chatModelsResult] =
        await Promise.allSettled([
          getSubjects(),
          getEmbeddingModels(),
          getChatModels(),
        ])

      if (!active) {
        return
      }

      const failedConfigurationAreas: string[] = []

      if (subjectsResult.status === 'fulfilled') {
        const subjects = subjectsResult.value.subjects
        const enabledSubjects = subjects.filter((subject) => subject.enabled)
        const defaultSubjectId = defaultModelId(
          enabledSubjects,
          subjectsResult.value.defaultSubjectId,
        )
        const defaultSubject = enabledSubjects.find(
          (subject) => subject.id === defaultSubjectId,
        )

        setSubjects(subjects)
        setSelectedSubjectId(defaultSubjectId)
        setMessage(defaultSubject?.defaultQuestion ?? '')
        setFocusMessageAtEndRequest((current) => current + 1)
      } else {
        failedConfigurationAreas.push('subjects')
      }

      if (embeddingModelsResult.status === 'fulfilled') {
        const embeddingModels = embeddingModelsResult.value.embeddingModels
        const enabledEmbeddingModels = embeddingModels.filter(
          (model) => model.enabled,
        )

        setEmbeddingModels(embeddingModels)
        setEmbeddingApiTimeoutSeconds(
          embeddingModelsResult.value.embeddingApiTimeoutSeconds,
        )
        setSelectedEmbeddingModelId(
          defaultModelId(
            enabledEmbeddingModels,
            embeddingModelsResult.value.defaultEmbeddingModelId,
          ),
        )
      } else {
        failedConfigurationAreas.push('embedding models')
      }

      if (chatModelsResult.status === 'fulfilled') {
        const chatModels = chatModelsResult.value.chatModels

        setModels(chatModels)
        setChatApiTimeoutSeconds(chatModelsResult.value.chatApiTimeoutSeconds)
        setSelectedModelId(
          defaultModelId(chatModels, chatModelsResult.value.defaultChatModelId),
        )
      } else {
        failedConfigurationAreas.push('chat models')
      }

      setModelsError(
        failedConfigurationAreas.length > 0
          ? `Could not load ${formatList(failedConfigurationAreas)}.`
          : null,
      )
      setModelsLoading(false)
    }

    void loadConfiguration()

    return () => {
      active = false
    }
  }, [])
  useEffect(() => {
    const messageInput = messageInputRef.current
    messageInput?.focus()
    messageInput?.setSelectionRange(
      messageInput.value.length,
      messageInput.value.length,
    )
  }, [focusMessageAtEndRequest])

  useEffect(() => {
    if (!sending) {
      return undefined
    }

    const intervalId = window.setInterval(() => {
      setRequestElapsedSeconds((current) => current + 1)
    }, 1000)

    return () => window.clearInterval(intervalId)
  }, [sending])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView?.({ block: 'end' })
  }, [chatMessages, sending])

  const selectedModel = useMemo(
    () => models.find((model) => model.id === selectedModelId),
    [models, selectedModelId],
  )
  const enabledSubjects = useMemo(
    () => subjects.filter((subject) => subject.enabled),
    [subjects],
  )
  const selectedSubject = useMemo(
    () => enabledSubjects.find((subject) => subject.id === selectedSubjectId),
    [enabledSubjects, selectedSubjectId],
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
  const sendBlockingMessage = sendDisabledReason({
    modelsLoading,
    selectedSubject,
    selectedModel,
    selectedEmbeddingModel,
    trimmedMessage,
    modelBlockingMessage,
  })
  const sendDisabled =
    sending ||
    !selectedSubject ||
    !selectedModel ||
    !selectedEmbeddingModel ||
    !trimmedMessage ||
    !modelCanChat
  const selectionSummary =
    selectedSubject && selectedModel && selectedEmbeddingModel
      ? `${selectedSubject.displayName} · ${selectedEmbeddingModel.displayName} · ${selectedModel.displayName}`
      : null

  async function submitChat() {
    if (
      sendDisabled ||
      !selectedSubject ||
      !selectedModel ||
      !selectedEmbeddingModel
    ) {
      return
    }

    const userMessage: ChatMessage = {
      id: nextMessageId(),
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
        subjectId: selectedSubject.id,
        modelId: selectedModel.id,
        embeddingModelId: selectedEmbeddingModel.id,
        message: trimmedMessage,
      })
      setChatMessages((current) => [
        ...current,
        {
          id: nextMessageId(),
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

  function handleSubjectChange(subjectId: string) {
    const previousDefaultQuestion = selectedSubject?.defaultQuestion ?? ''
    const subject = enabledSubjects.find((subject) => subject.id === subjectId)

    appendSelectionChangeMessage('Subject', subject?.displayName, subjectId)
    setSelectedSubjectId(subjectId)
    if (message === previousDefaultQuestion) {
      setMessage(subject?.defaultQuestion ?? '')
      setFocusMessageAtEndRequest((current) => current + 1)
    }
  }

  function handleEmbeddingModelChange(modelId: string) {
    const model = enabledEmbeddingModels.find((model) => model.id === modelId)

    appendSelectionChangeMessage('Embedding model', model?.displayName, modelId)
    setSelectedEmbeddingModelId(modelId)
  }

  function handleChatModelChange(modelId: string) {
    const model = models.find((model) => model.id === modelId)

    appendSelectionChangeMessage('Chat model', model?.displayName, modelId)
    setSelectedModelId(modelId)
  }

  function appendSelectionChangeMessage(label: string, name = '', id = '') {
    if (!name && !id) {
      return
    }

    setChatMessages((current) => [
      ...current,
      {
        id: nextMessageId(),
        role: 'system',
        content: `${label} changed to ${name || id}.`,
      },
    ])
    setChatError(null)
  }

  function nextMessageId() {
    nextMessageIdRef.current += 1
    return nextMessageIdRef.current
  }

  function useSubjectQuestion() {
    setMessage(selectedSubject?.defaultQuestion ?? '')
    setFocusMessageAtEndRequest((current) => current + 1)
  }

  function clearChat() {
    setChatMessages([])
    setChatError(null)
  }

  return (
    <main className="app-shell" data-theme={selectedThemeId}>
      <header className="theme-bar">
        <div className="theme-heading">
          <p className="eyebrow">AI RAG Subject Matter Expert</p>
          <h1 id="model-panel-heading">Chat workspace</h1>
        </div>
        <label className="field theme-field" htmlFor="theme">
          <span>Theme</span>
          <select
            id="theme"
            value={selectedThemeId}
            onChange={(event) =>
              setSelectedThemeId(event.target.value as ThemeId)
            }
          >
            {themes.map((theme) => (
              <option key={theme.id} value={theme.id}>
                {theme.label}
              </option>
            ))}
          </select>
        </label>
      </header>

      <div className="workspace">
        <section className="model-panel" aria-labelledby="model-panel-heading">
          <label className="field" htmlFor="subject">
            <span>Subject</span>
            <select
              id="subject"
              value={selectedSubjectId}
              onChange={(event) => handleSubjectChange(event.target.value)}
              disabled={
                modelsLoading || sending || enabledSubjects.length === 0
              }
            >
              {enabledSubjects.map((subject) => (
                <option key={subject.id} value={subject.id}>
                  {subject.displayName}
                </option>
              ))}
            </select>
          </label>

          <label className="field" htmlFor="embedding-model">
            <span>Embedding Model</span>
            <select
              id="embedding-model"
              value={selectedEmbeddingModelId}
              onChange={(event) =>
                handleEmbeddingModelChange(event.target.value)
              }
              disabled={
                modelsLoading || sending || enabledEmbeddingModels.length === 0
              }
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
              onChange={(event) => handleChatModelChange(event.target.value)}
              disabled={modelsLoading || sending || models.length === 0}
            >
              {models.map((model) => (
                <option key={model.id} value={model.id}>
                  {model.displayName}
                </option>
              ))}
            </select>
          </label>

          {modelsLoading && (
            <p className="status" role="status">
              Loading configured models...
            </p>
          )}
          {modelsError && (
            <p className="status status-error" role="alert">
              {modelsError}
            </p>
          )}
          {!modelsLoading && !modelsError && models.length === 0 && (
            <p className="status status-error" role="alert">
              No chat models are configured.
            </p>
          )}
          {!modelsLoading && !modelsError && enabledSubjects.length === 0 && (
            <p className="status status-error" role="alert">
              No enabled subjects are configured.
            </p>
          )}
          {!modelsLoading &&
            !modelsError &&
            enabledEmbeddingModels.length === 0 && (
              <p className="status status-error" role="alert">
                No enabled embedding models are configured.
              </p>
            )}

          <ModelDetails model={selectedModel} />
        </section>

        <section className="chat-panel" aria-labelledby="chat-heading">
          <div className="chat-header">
            <div>
              <p className="eyebrow">
                {selectedSubject?.displayName ?? 'Subject'}
              </p>
              <h2 id="chat-heading">Ask a question</h2>
            </div>
            <div className="chat-header-actions">
              {selectionSummary && (
                <p className="chat-summary">{selectionSummary}</p>
              )}
              <button
                className="secondary-button"
                type="button"
                onClick={clearChat}
                disabled={sending || (!chatError && chatMessages.length === 0)}
              >
                Clear chat
              </button>
            </div>
          </div>

          <div className="messages" aria-live="polite">
            {chatMessages.length === 0 ? (
              <div className="empty-state">
                {selectedSubject
                  ? `Ask about ${selectedSubject.displayName}. Answers use the indexed bundled documents for this subject.`
                  : 'Ask a question and the answer will use the indexed bundled documents as context.'}
              </div>
            ) : (
              chatMessages.map((chatMessage) => (
                <article
                  key={chatMessage.id}
                  className={`message message-${chatMessage.role}`}
                >
                  <span>{messageRoleLabel(chatMessage.role)}</span>
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
            <div ref={messagesEndRef} />
          </div>

          {chatError && (
            <p className="status status-error" role="alert">
              {chatError}
            </p>
          )}
          <form
            className="chat-form"
            onSubmit={(event) => {
              event.preventDefault()
              void submitChat()
            }}
          >
            <div className="field">
              <span className="field-heading">
                <label htmlFor="message">Message</label>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={useSubjectQuestion}
                  disabled={!selectedSubject || sending}
                >
                  Use default question
                </button>
              </span>
              <textarea
                id="message"
                ref={messageInputRef}
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                onKeyDown={handleMessageKeyDown}
                placeholder="Ask about the bundled subject documents..."
                rows={4}
              />
            </div>
            <div className="send-actions">
              <button type="submit" disabled={sendDisabled}>
                {sending ? 'Sending...' : 'Send'}
              </button>
              {sendBlockingMessage && !sending && (
                <p
                  className={`send-guidance ${modelBlockingMessage ? 'status-error' : ''}`}
                  role={modelBlockingMessage ? 'alert' : 'status'}
                >
                  {sendBlockingMessage}
                </p>
              )}
            </div>
          </form>
        </section>
      </div>
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

function messageRoleLabel(role: ChatMessage['role']) {
  if (role === 'user') {
    return 'You'
  }

  if (role === 'assistant') {
    return 'Assistant'
  }

  return 'Selection changed'
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

function formatList(items: string[]): string {
  if (items.length <= 1) {
    return items.join('')
  }

  return `${items.slice(0, -1).join(', ')} or ${items[items.length - 1]}`
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

function sendDisabledReason({
  modelsLoading,
  selectedSubject,
  selectedModel,
  selectedEmbeddingModel,
  trimmedMessage,
  modelBlockingMessage,
}: {
  modelsLoading: boolean
  selectedSubject: Subject | undefined
  selectedModel: ChatModel | undefined
  selectedEmbeddingModel: EmbeddingModel | undefined
  trimmedMessage: string
  modelBlockingMessage: string | null
}): string | null {
  if (modelsLoading) {
    return 'Models are still loading.'
  }

  if (!selectedSubject) {
    return 'Select a subject.'
  }

  if (!selectedEmbeddingModel) {
    return 'Select an embedding model.'
  }

  if (!selectedModel) {
    return 'Select a chat model.'
  }

  if (modelBlockingMessage) {
    return modelBlockingMessage
  }

  if (!trimmedMessage) {
    return 'Enter a message.'
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
