import type { ChatModel, Subject } from '../api/types'

export const culinarySubject: Subject = {
  id: 'culinary-expert',
  enabled: true,
  displayOrder: 10,
  displayName: 'Culinary Expert',
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
