export interface ChatMessage {
  id: number
  roomId: string
  userId: number | null
  displayName: string
  avatarUrl: string | null
  content: string
  status: string
  createdAt: string
  guest: boolean
  replyTo?: number | null
  replyToDisplayName?: string | null
  replyToContentPreview?: string | null
  edited?: boolean
  deletedType?: string | null
  reactions?: Record<string, number>
  myReactions?: string[]
}

export interface PresencePayload {
  type: 'PRESENCE'
  online: number
}

export interface DeleteEvent {
  type: 'DELETE'
  id: number
  deletedType?: string
}

export interface RecallEvent {
  type: 'RECALL'
  id: number
  deletedType?: string
}

export interface ReactionUpdateEvent {
  type: 'REACTION'
  messageId: number
  reactions: Record<string, number>
}

export interface EditEvent {
  // 完整 ChatMessage，无 type 字段，由 /topic/chat.edit.<room> 携带
  id: number
  content: string
  edited: boolean
  [key: string]: unknown
}

export interface TypingEvent {
  type?: 'TYPING'
  roomId?: string
  userId?: number | null
  displayName?: string | null
  isTyping: boolean
}

export type ChatEvent =
  | PresencePayload
  | DeleteEvent
  | RecallEvent
  | ReactionUpdateEvent
  | ChatMessage
  | TypingEvent

export interface SendPayload {
  roomId: string
  content: string
  displayName?: string
  replyTo?: number | null
}

export interface ReactionActionPayload {
  messageId: number
  emoji: string
}

export interface EditPayload {
  id: number
  content: string
}

export interface RecallPayload {
  id: number
}

export interface TypingPayload {
  roomId: string
  isTyping: boolean
}
