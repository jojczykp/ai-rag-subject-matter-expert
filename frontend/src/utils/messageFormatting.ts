import type { ChatMessage } from '../types/chat'

export type MessageContentPart = {
  text: string
  bold: boolean
  key: number
}

export function messageContentParts(content: string): MessageContentPart[] {
  const parts: MessageContentPart[] = []
  const boldPattern = /\*\*(.+?)\*\*/g
  let lastIndex = 0

  for (const match of content.matchAll(boldPattern)) {
    const [matchedText, boldText] = match
    const matchIndex = match.index

    if (matchIndex > lastIndex) {
      parts.push({
        text: content.slice(lastIndex, matchIndex),
        bold: false,
        key: lastIndex,
      })
    }

    parts.push({ text: boldText, bold: true, key: matchIndex })
    lastIndex = matchIndex + matchedText.length
  }

  if (lastIndex < content.length) {
    parts.push({ text: content.slice(lastIndex), bold: false, key: lastIndex })
  }

  return parts
}

export function messageRoleLabel(role: ChatMessage['role']): string {
  if (role === 'user') {
    return 'You'
  }

  if (role === 'assistant') {
    return 'Assistant'
  }

  return 'Selection changed'
}
