import type { ChatModel, EmbeddingModel, Subject } from '../api/types'
import { ChatModelDetails, EmbeddingModelDetails } from './ModelDetails'

export function ConfigurationPanel({
  chatModels,
  enabledEmbeddingModels,
  enabledSubjects,
  selectedChatModelId,
  selectedEmbeddingModelId,
  selectedSubjectId,
  selectedChatModel,
  selectedEmbeddingModel,
  configurationLoading,
  configurationError,
  isSubmitting,
  onSubjectChange,
  onEmbeddingModelChange,
  onChatModelChange,
}: {
  chatModels: ChatModel[]
  enabledEmbeddingModels: EmbeddingModel[]
  enabledSubjects: Subject[]
  selectedChatModelId: string
  selectedEmbeddingModelId: string
  selectedSubjectId: string
  selectedChatModel: ChatModel | undefined
  selectedEmbeddingModel: EmbeddingModel | undefined
  configurationLoading: boolean
  configurationError: string | null
  isSubmitting: boolean
  onSubjectChange: (id: string) => void
  onEmbeddingModelChange: (id: string) => void
  onChatModelChange: (id: string) => void
}) {
  return (
    <section className="model-panel" aria-label="Configuration">
      <label className="field" htmlFor="subject">
        <span>Subject</span>
        <select
          id="subject"
          value={selectedSubjectId}
          onChange={(event) => onSubjectChange(event.target.value)}
          disabled={
            configurationLoading || isSubmitting || enabledSubjects.length === 0
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
          onChange={(event) => onEmbeddingModelChange(event.target.value)}
          disabled={
            configurationLoading ||
            isSubmitting ||
            enabledEmbeddingModels.length === 0
          }
        >
          {enabledEmbeddingModels.map((model) => (
            <option key={model.id} value={model.id}>
              {model.displayName}
            </option>
          ))}
        </select>
      </label>

      <EmbeddingModelDetails model={selectedEmbeddingModel} />

      <label className="field" htmlFor="chat-model">
        <span>Chat Model</span>
        <select
          id="chat-model"
          value={selectedChatModelId}
          onChange={(event) => onChatModelChange(event.target.value)}
          disabled={
            configurationLoading || isSubmitting || chatModels.length === 0
          }
        >
          {chatModels.map((model) => (
            <option key={model.id} value={model.id}>
              {model.displayName}
            </option>
          ))}
        </select>
      </label>

      <ConfigurationStatus
        isLoading={configurationLoading}
        error={configurationError}
        chatModelCount={chatModels.length}
        enabledSubjectCount={enabledSubjects.length}
        enabledEmbeddingModelCount={enabledEmbeddingModels.length}
      />

      <ChatModelDetails model={selectedChatModel} />
    </section>
  )
}

function ConfigurationStatus({
  isLoading,
  error,
  chatModelCount,
  enabledSubjectCount,
  enabledEmbeddingModelCount,
}: {
  isLoading: boolean
  error: string | null
  chatModelCount: number
  enabledSubjectCount: number
  enabledEmbeddingModelCount: number
}) {
  return (
    <>
      {isLoading && (
        <p className="status" role="status">
          Loading configured models...
        </p>
      )}
      {error && (
        <p className="status status-error" role="alert">
          {error}
        </p>
      )}
      {!isLoading && !error && chatModelCount === 0 && (
        <p className="status status-error" role="alert">
          No chat models are configured.
        </p>
      )}
      {!isLoading && !error && enabledSubjectCount === 0 && (
        <p className="status status-error" role="alert">
          No enabled subjects are configured.
        </p>
      )}
      {!isLoading && !error && enabledEmbeddingModelCount === 0 && (
        <p className="status status-error" role="alert">
          No enabled embedding models are configured.
        </p>
      )}
    </>
  )
}
