export type ChatMessage = {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
}

export type ThemeId =
  'light' | 'forest' | 'ocean' | 'sunrise' | 'clay' | 'rose' | 'night'

export const themes: { id: ThemeId; label: string }[] = [
  { id: 'light', label: 'Light' },
  { id: 'forest', label: 'Forest' },
  { id: 'ocean', label: 'Ocean' },
  { id: 'sunrise', label: 'Sunrise' },
  { id: 'clay', label: 'Clay' },
  { id: 'rose', label: 'Rose' },
  { id: 'night', label: 'Night' },
]
