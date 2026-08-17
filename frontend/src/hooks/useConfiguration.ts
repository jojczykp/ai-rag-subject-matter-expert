import { useEffect, useMemo, useState } from 'react'
import { getChatModels, getEmbeddingModels, getSubjects } from '../api/client'
import type { ChatModel, EmbeddingModel, Subject } from '../api/types'
import { formatList, resolveDefaultId } from '../utils/modelFormatting'

type ConfigurationState = {
  chatModels: ChatModel[]
  embeddingModels: EmbeddingModel[]
  subjects: Subject[]
  isLoading: boolean
  error: string | null
  chatApiTimeoutSeconds: number
  embeddingApiTimeoutSeconds: number
  selectedChatModelId: string
  selectedEmbeddingModelId: string
  selectedSubjectId: string
}

const initialState: ConfigurationState = {
  chatModels: [],
  embeddingModels: [],
  subjects: [],
  isLoading: true,
  error: null,
  chatApiTimeoutSeconds: 60,
  embeddingApiTimeoutSeconds: 60,
  selectedChatModelId: '',
  selectedEmbeddingModelId: '',
  selectedSubjectId: '',
}

export function useConfiguration(
  initializeDraft: (defaultQuestion: string) => void,
) {
  const [configuration, setConfiguration] =
    useState<ConfigurationState>(initialState)

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

      const failedAreas: string[] = []
      let nextState = { ...initialState }

      if (subjectsResult.status === 'fulfilled') {
        const subjects = subjectsResult.value.subjects
        const enabledSubjects = subjects.filter((subject) => subject.enabled)
        const selectedSubjectId = resolveDefaultId(
          enabledSubjects,
          subjectsResult.value.defaultSubjectId,
        )
        const selectedSubject = enabledSubjects.find(
          (subject) => subject.id === selectedSubjectId,
        )

        nextState = { ...nextState, subjects, selectedSubjectId }
        initializeDraft(selectedSubject?.defaultQuestion ?? '')
      } else {
        failedAreas.push('subjects')
      }

      if (embeddingModelsResult.status === 'fulfilled') {
        const embeddingModels = embeddingModelsResult.value.embeddingModels
        const enabledEmbeddingModels = embeddingModels.filter(
          (model) => model.enabled,
        )

        nextState = {
          ...nextState,
          embeddingModels,
          embeddingApiTimeoutSeconds:
            embeddingModelsResult.value.embeddingApiTimeoutSeconds,
          selectedEmbeddingModelId: resolveDefaultId(
            enabledEmbeddingModels,
            embeddingModelsResult.value.defaultEmbeddingModelId,
          ),
        }
      } else {
        failedAreas.push('embedding models')
      }

      if (chatModelsResult.status === 'fulfilled') {
        const chatModels = chatModelsResult.value.chatModels
        nextState = {
          ...nextState,
          chatModels,
          chatApiTimeoutSeconds: chatModelsResult.value.chatApiTimeoutSeconds,
          selectedChatModelId: resolveDefaultId(
            chatModels,
            chatModelsResult.value.defaultChatModelId,
          ),
        }
      } else {
        failedAreas.push('chat models')
      }

      setConfiguration({
        ...nextState,
        isLoading: false,
        error:
          failedAreas.length > 0
            ? `Could not load ${formatList(failedAreas)}.`
            : null,
      })
    }

    void loadConfiguration()

    return () => {
      active = false
    }
  }, [initializeDraft])

  const enabledSubjects = useMemo(
    () => configuration.subjects.filter((subject) => subject.enabled),
    [configuration.subjects],
  )
  const enabledEmbeddingModels = useMemo(
    () => configuration.embeddingModels.filter((model) => model.enabled),
    [configuration.embeddingModels],
  )
  const selectedSubject = enabledSubjects.find(
    (subject) => subject.id === configuration.selectedSubjectId,
  )
  const selectedEmbeddingModel = enabledEmbeddingModels.find(
    (model) => model.id === configuration.selectedEmbeddingModelId,
  )
  const selectedChatModel = configuration.chatModels.find(
    (model) => model.id === configuration.selectedChatModelId,
  )

  function updateSelection(
    key:
      'selectedSubjectId' | 'selectedEmbeddingModelId' | 'selectedChatModelId',
    value: string,
  ) {
    setConfiguration((current) => ({ ...current, [key]: value }))
  }

  return {
    ...configuration,
    enabledSubjects,
    enabledEmbeddingModels,
    selectedSubject,
    selectedEmbeddingModel,
    selectedChatModel,
    selectSubject: (id: string) => updateSelection('selectedSubjectId', id),
    selectEmbeddingModel: (id: string) =>
      updateSelection('selectedEmbeddingModelId', id),
    selectChatModel: (id: string) => updateSelection('selectedChatModelId', id),
  }
}
