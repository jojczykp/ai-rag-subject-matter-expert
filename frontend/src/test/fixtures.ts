import type { ChatModel, Subject } from '../api/types'

export const culinarySubject: Subject = {
  id: 'culinary-expert',
  enabled: true,
  displayOrder: 10,
  displayName: 'Culinary Expert',
  defaultQuestion: 'How should I cook rice?',
}

export const passiveHouseSubject: Subject = {
  id: 'passive-house',
  enabled: true,
  displayOrder: 20,
  displayName: 'Passive House Architecture Expert',
  defaultQuestion:
    'I am designing a 160 m² house in southern Germany. I want to achieve Passive House certification while keeping construction costs reasonable. Recommend wall, roof, floor, window, ventilation and heating specifications, explain why each choice matters, and identify the biggest design risks',
}

export const availableOllamaModel: ChatModel = {
  id: 'local-ollama-llama',
  displayName: 'Local Ollama Llama',
  description: 'Local Ollama model for development.',
  runtime: 'OLLAMA',
  mode: 'LOCAL_SERVER',
  availability: 'AVAILABLE',
  availableOffline: false,
  promptsMayLeaveLocalMachine: false,
  capabilities: ['CHAT'],
  runtimeRequirements: ['REQUIRES_OLLAMA_SERVER'],
}

export const availableEmbeddedQwenModel: ChatModel = {
  id: 'embedded-qwen-1-5b',
  displayName: 'Embedded Qwen 1.5B',
  description:
    'Smarter fully offline embedded model backed by a local GGUF asset.',
  runtime: 'EMBEDDED_LLAMA',
  mode: 'EMBEDDED_OFFLINE',
  availability: 'AVAILABLE',
  availableOffline: true,
  promptsMayLeaveLocalMachine: false,
  capabilities: ['CHAT'],
  runtimeRequirements: [
    'REQUIRES_LOCAL_GGUF_MODEL',
    'REQUIRES_LLAMA_SERVER_EXECUTABLE',
  ],
}
