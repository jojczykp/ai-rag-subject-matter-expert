import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, postChat } from '../api/client'
import type { ChatModel, EmbeddingModel, Subject } from '../api/types'
import type { ChatMessage } from '../types/chat'

type ChatSelection = {
  subject: Subject
  chatModel: ChatModel
  embeddingModel: EmbeddingModel
}

export function useChatSession() {
  const [draft, setDraft] = useState('')
  const [transcript, setTranscript] = useState<ChatMessage[]>([])
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [requestElapsedSeconds, setRequestElapsedSeconds] = useState(0)
  const [draftFocusRevision, setDraftFocusRevision] = useState(0)
  const nextMessageIdRef = useRef(0)

  useEffect(() => {
    if (!isSubmitting) {
      return undefined
    }

    const intervalId = window.setInterval(() => {
      setRequestElapsedSeconds((current) => current + 1)
    }, 1000)

    return () => window.clearInterval(intervalId)
  }, [isSubmitting])

  const initializeDraft = useCallback((defaultQuestion: string) => {
    setDraft(defaultQuestion)
    requestDraftFocus()
  }, [])

  function nextMessageId() {
    nextMessageIdRef.current += 1
    return nextMessageIdRef.current
  }

  function requestDraftFocus() {
    setDraftFocusRevision((current) => current + 1)
  }

  function appendSelectionChange(label: string, name = '', id = '') {
    if (!name && !id) {
      return
    }

    setTranscript((current) => [
      ...current,
      {
        id: nextMessageId(),
        role: 'system',
        content: `${label} changed to ${name || id}.`,
      },
    ])
    setError(null)
  }

  function updateDraftForSubject(
    previousDefaultQuestion: string,
    nextDefaultQuestion: string,
  ) {
    if (draft === previousDefaultQuestion) {
      setDraft(nextDefaultQuestion)
      requestDraftFocus()
    }
  }

  function useDefaultQuestion(defaultQuestion: string) {
    setDraft(defaultQuestion)
    requestDraftFocus()
  }

  async function submit(selection: ChatSelection) {
    const trimmedDraft = draft.trim()
    if (!trimmedDraft || isSubmitting) {
      return
    }

    setTranscript((current) => [
      ...current,
      { id: nextMessageId(), role: 'user', content: trimmedDraft },
    ])
    setDraft('')
    setError(null)
    setRequestElapsedSeconds(0)
    setIsSubmitting(true)

    try {
      const response = await postChat({
        subjectId: selection.subject.id,
        modelId: selection.chatModel.id,
        embeddingModelId: selection.embeddingModel.id,
        message: trimmedDraft,
      })
      setTranscript((current) => [
        ...current,
        {
          id: nextMessageId(),
          role: 'assistant',
          content: response.answer,
        },
      ])
    } catch (requestError: unknown) {
      setError(
        requestError instanceof ApiError
          ? requestError.message
          : 'The selected model could not answer.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  function clear() {
    setTranscript([])
    setError(null)
  }

  return {
    draft,
    setDraft,
    transcript,
    error,
    isSubmitting,
    requestElapsedSeconds,
    draftFocusRevision,
    initializeDraft,
    appendSelectionChange,
    updateDraftForSubject,
    useDefaultQuestion,
    submit,
    clear,
  }
}
