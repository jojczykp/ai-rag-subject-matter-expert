import { useState } from 'react'
import './styles/themes.css'
import './App.css'
import { AppHeader } from './components/AppHeader'
import { ChatPanel } from './components/ChatPanel'
import { ConfigurationPanel } from './components/ConfigurationPanel'
import { useChatSession } from './hooks/useChatSession'
import { useConfiguration } from './hooks/useConfiguration'
import type { ThemeId } from './types/chat'
import {
  modelAvailabilityBlockingMessage,
  sendDisabledReason,
} from './utils/modelFormatting'

function App() {
  const [selectedThemeId, setSelectedThemeId] = useState<ThemeId>('light')
  const chat = useChatSession()
  const configuration = useConfiguration(chat.initializeDraft)
  const trimmedDraft = chat.draft.trim()
  const modelBlockingMessage = modelAvailabilityBlockingMessage(
    configuration.selectedChatModel,
    configuration.selectedEmbeddingModel,
  )
  const sendBlockingMessage = sendDisabledReason({
    configurationLoading: configuration.isLoading,
    selectedSubject: configuration.selectedSubject,
    selectedChatModel: configuration.selectedChatModel,
    selectedEmbeddingModel: configuration.selectedEmbeddingModel,
    trimmedMessage: trimmedDraft,
    modelBlockingMessage,
  })
  const sendDisabled = chat.isSubmitting || Boolean(sendBlockingMessage)
  const selectionSummary =
    configuration.selectedSubject &&
    configuration.selectedChatModel &&
    configuration.selectedEmbeddingModel
      ? `${configuration.selectedSubject.displayName} · ${configuration.selectedEmbeddingModel.displayName} · ${configuration.selectedChatModel.displayName}`
      : null

  function handleSubjectChange(subjectId: string) {
    const previousDefaultQuestion =
      configuration.selectedSubject?.defaultQuestion ?? ''
    const subject = configuration.enabledSubjects.find(
      (candidate) => candidate.id === subjectId,
    )

    chat.appendSelectionChange('Subject', subject?.displayName, subjectId)
    configuration.selectSubject(subjectId)
    chat.updateDraftForSubject(
      previousDefaultQuestion,
      subject?.defaultQuestion ?? '',
    )
  }

  function handleEmbeddingModelChange(modelId: string) {
    const model = configuration.enabledEmbeddingModels.find(
      (candidate) => candidate.id === modelId,
    )
    chat.appendSelectionChange('Embedding model', model?.displayName, modelId)
    configuration.selectEmbeddingModel(modelId)
  }

  function handleChatModelChange(modelId: string) {
    const model = configuration.chatModels.find(
      (candidate) => candidate.id === modelId,
    )
    chat.appendSelectionChange('Chat model', model?.displayName, modelId)
    configuration.selectChatModel(modelId)
  }

  function submitChat() {
    if (
      sendDisabled ||
      !configuration.selectedSubject ||
      !configuration.selectedChatModel ||
      !configuration.selectedEmbeddingModel
    ) {
      return
    }

    void chat.submit({
      subject: configuration.selectedSubject,
      chatModel: configuration.selectedChatModel,
      embeddingModel: configuration.selectedEmbeddingModel,
    })
  }

  return (
    <main className="app-shell" data-theme={selectedThemeId}>
      <AppHeader
        selectedThemeId={selectedThemeId}
        onThemeChange={setSelectedThemeId}
      />
      <div className="workspace">
        <ConfigurationPanel
          chatModels={configuration.chatModels}
          enabledEmbeddingModels={configuration.enabledEmbeddingModels}
          enabledSubjects={configuration.enabledSubjects}
          selectedChatModelId={configuration.selectedChatModelId}
          selectedEmbeddingModelId={configuration.selectedEmbeddingModelId}
          selectedSubjectId={configuration.selectedSubjectId}
          selectedChatModel={configuration.selectedChatModel}
          selectedEmbeddingModel={configuration.selectedEmbeddingModel}
          configurationLoading={configuration.isLoading}
          configurationError={configuration.error}
          isSubmitting={chat.isSubmitting}
          onSubjectChange={handleSubjectChange}
          onEmbeddingModelChange={handleEmbeddingModelChange}
          onChatModelChange={handleChatModelChange}
        />
        <ChatPanel
          selectedSubject={configuration.selectedSubject}
          selectionSummary={selectionSummary}
          transcript={chat.transcript}
          error={chat.error}
          draft={chat.draft}
          isSubmitting={chat.isSubmitting}
          sendDisabled={sendDisabled}
          sendBlockingMessage={sendBlockingMessage}
          modelBlockingMessage={modelBlockingMessage}
          requestElapsedSeconds={chat.requestElapsedSeconds}
          embeddingApiTimeoutSeconds={configuration.embeddingApiTimeoutSeconds}
          chatApiTimeoutSeconds={configuration.chatApiTimeoutSeconds}
          draftFocusRevision={chat.draftFocusRevision}
          onDraftChange={chat.setDraft}
          onSubmit={submitChat}
          onClear={chat.clear}
          onUseDefaultQuestion={() =>
            chat.useDefaultQuestion(
              configuration.selectedSubject?.defaultQuestion ?? '',
            )
          }
        />
      </div>
    </main>
  )
}

export default App
