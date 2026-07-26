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
}

export interface PresencePayload {
  type: 'PRESENCE'
  online: number
}

export interface DeleteEvent {
  type: 'DELETE'
  id: number
}

export interface ErrorEvent {
  type: 'ERROR'
  message: string
}

export type ChatEvent = PresencePayload | DeleteEvent | ErrorEvent

export interface SendPayload {
  roomId: string
  content: string
  displayName?: string
}
