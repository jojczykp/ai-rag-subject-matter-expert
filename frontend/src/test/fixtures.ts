import type { ChatModel } from '../api/types'

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
  runtimeRequirements: ['REQUIRES_LOCAL_OLLAMA'],
}
