import { useEffect, useRef } from 'react'
import type { KeyboardEvent } from 'react'
import type { Subject } from '../api/types'
import type { ChatMessage } from '../types/chat'
import {
  messageContentParts,
  messageRoleLabel,
} from '../utils/messageFormatting'
import { remainingSeconds } from '../utils/modelFormatting'

export function ChatPanel({
  selectedSubject,
  selectionSummary,
  transcript,
  error,
  draft,
  isSubmitting,
  sendDisabled,
  sendBlockingMessage,
  modelBlockingMessage,
  requestElapsedSeconds,
  embeddingApiTimeoutSeconds,
  chatApiTimeoutSeconds,
  draftFocusRevision,
  onDraftChange,
  onSubmit,
  onClear,
  onUseDefaultQuestion,
}: {
  selectedSubject: Subject | undefined
  selectionSummary: string | null
  transcript: ChatMessage[]
  error: string | null
  draft: string
  isSubmitting: boolean
  sendDisabled: boolean
  sendBlockingMessage: string | null
  modelBlockingMessage: string | null
  requestElapsedSeconds: number
  embeddingApiTimeoutSeconds: number
  chatApiTimeoutSeconds: number
  draftFocusRevision: number
  onDraftChange: (draft: string) => void
  onSubmit: () => void
  onClear: () => void
  onUseDefaultQuestion: () => void
}) {
  const draftInputRef = useRef<HTMLTextAreaElement>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const draftInput = draftInputRef.current
    draftInput?.focus()
    draftInput?.setSelectionRange(
      draftInput.value.length,
      draftInput.value.length,
    )
  }, [draftFocusRevision])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView?.({ block: 'end' })
  }, [transcript, isSubmitting])

  function handleDraftKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      onSubmit()
    }
  }

  return (
    <section className="chat-panel" aria-labelledby="chat-heading">
      <div className="chat-header">
        <div>
          <p className="eyebrow">{selectedSubject?.displayName ?? 'Subject'}</p>
          <h2 id="chat-heading">Ask a question</h2>
        </div>
        <div className="chat-header-actions">
          {selectionSummary && (
            <p className="chat-summary">{selectionSummary}</p>
          )}
          <button
            className="secondary-button"
            type="button"
            onClick={onClear}
            disabled={isSubmitting || (!error && transcript.length === 0)}
          >
            Clear chat
          </button>
        </div>
      </div>

      <div className="messages" aria-live="polite">
        {transcript.length === 0 ? (
          <div className="empty-state">
            {selectedSubject
              ? `Ask about ${selectedSubject.displayName}. Answers use the indexed bundled documents for this subject.`
              : 'Ask a question and the answer will use the indexed bundled documents as context.'}
          </div>
        ) : (
          transcript.map((message) => (
            <article
              key={message.id}
              className={`message message-${message.role}`}
            >
              <span>{messageRoleLabel(message.role)}</span>
              <p>
                {messageContentParts(message.content).map((part) =>
                  part.bold ? (
                    <strong key={part.key}>{part.text}</strong>
                  ) : (
                    part.text
                  ),
                )}
              </p>
            </article>
          ))
        )}
        {isSubmitting && (
          <RequestProgress
            elapsedSeconds={requestElapsedSeconds}
            embeddingApiTimeoutSeconds={embeddingApiTimeoutSeconds}
            chatApiTimeoutSeconds={chatApiTimeoutSeconds}
          />
        )}
        <div ref={messagesEndRef} />
      </div>

      {error && (
        <p className="status status-error" role="alert">
          {error}
        </p>
      )}
      <form
        className="chat-form"
        onSubmit={(event) => {
          event.preventDefault()
          onSubmit()
        }}
      >
        <div className="field">
          <span className="field-heading">
            <label htmlFor="message">Message</label>
            <button
              className="secondary-button"
              type="button"
              onClick={onUseDefaultQuestion}
              disabled={!selectedSubject || isSubmitting}
            >
              Use default question
            </button>
          </span>
          <textarea
            id="message"
            ref={draftInputRef}
            value={draft}
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={handleDraftKeyDown}
            placeholder="Ask about the bundled subject documents..."
            rows={4}
          />
        </div>
        <div className="send-actions">
          <button type="submit" disabled={sendDisabled}>
            {isSubmitting ? 'Sending...' : 'Send'}
          </button>
          {sendBlockingMessage && !isSubmitting && (
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
  )
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
