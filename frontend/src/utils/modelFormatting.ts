import type { ChatModel, EmbeddingModel, Subject } from '../api/types'

export function resolveDefaultId<T extends { id: string }>(
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

export function formatList(items: string[]): string {
  if (items.length <= 1) {
    return items.join('')
  }

  return `${items.slice(0, -1).join(', ')} or ${items[items.length - 1]}`
}

export function formatModelMode(mode: string): string {
  if (mode === 'EMBEDDED_OFFLINE') {
    return 'Offline'
  }

  return titleCaseIdentifier(mode)
}

export function formatModelAvailability(availability: string): string {
  return titleCaseIdentifier(availability)
}

export function availabilityTone(
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

export function embeddingQueryMayLeaveLocalMachine(
  model: EmbeddingModel,
): boolean {
  return model.mode === 'ONLINE'
}

export function modelAvailabilityBlockingMessage(
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

export function sendDisabledReason({
  configurationLoading,
  selectedSubject,
  selectedChatModel,
  selectedEmbeddingModel,
  trimmedMessage,
  modelBlockingMessage,
}: {
  configurationLoading: boolean
  selectedSubject: Subject | undefined
  selectedChatModel: ChatModel | undefined
  selectedEmbeddingModel: EmbeddingModel | undefined
  trimmedMessage: string
  modelBlockingMessage: string | null
}): string | null {
  if (configurationLoading) {
    return 'Models are still loading.'
  }

  if (!selectedSubject) {
    return 'Select a subject.'
  }

  if (!selectedEmbeddingModel) {
    return 'Select an embedding model.'
  }

  if (!selectedChatModel) {
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

export function remainingSeconds(
  timeoutSeconds: number,
  elapsedSeconds: number,
): number {
  return Math.max(timeoutSeconds - elapsedSeconds, 0)
}

function titleCaseIdentifier(value: string): string {
  const label = value.toLowerCase().replaceAll('_', ' ')
  return `${label.charAt(0).toUpperCase()}${label.slice(1)}`
}
