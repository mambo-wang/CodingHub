import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import type {
  ChatEvent,
  ChatMessage,
  PresencePayload,
  ReactionUpdateEvent,
  RecallEvent,
  SendPayload,
  ReactionActionPayload,
  EditPayload,
  RecallPayload,
  TypingPayload,
} from '@/types/chat'
import { useAuthStore } from './auth'

const ROOM = 'global'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const onlineCount = ref(0)
  const error = ref('')
  const typingUsers = ref<{ userId: number | null; displayName: string | null }[]>([])
  const connected = ref(false)
  const loading = ref(false)

  let client: Client | null = null
  let typingTimer: number | null = null
  let lastTypingSent = 0

  function isSelf(msg: ChatMessage): boolean {
    const auth = useAuthStore()
    if (msg.userId != null && auth.user?.id != null) {
      return msg.userId === auth.user.id
    }
    return false
  }

  function onConnect() {
    connected.value = true
    client?.subscribe('/topic/chat.global', (msg) => {
      const data = JSON.parse(msg.body) as ChatEvent
      if (data.type === 'DELETE') {
        const idx = messages.value.findIndex((m) => m.id === (data as { id: number }).id)
        if (idx !== -1) messages.value.splice(idx, 1)
      } else if (data.type === 'PRESENCE') {
        onlineCount.value = (data as PresencePayload).online
      } else if (data.type === 'ERROR') {
        error.value = (data as { message: string }).message
      } else if ((data as ChatMessage).id != null && !('type' in data)) {
        messages.value.push(data as ChatMessage)
      }
    })

    client?.subscribe('/user/queue/errors', (msg) => {
      const data = JSON.parse(msg.body)
      error.value = data.message
    })

    client?.subscribe('/topic/chat.reactions.global', (msg) => {
      const data = JSON.parse(msg.body) as ReactionUpdateEvent
      const target = messages.value.find((m) => m.id === data.messageId)
      if (target) target.reactions = data.reactions
    })

    client?.subscribe('/topic/chat.edit.global', (msg) => {
      const data = JSON.parse(msg.body) as ChatMessage
      const idx = messages.value.findIndex((m) => m.id === data.id)
      if (idx !== -1) messages.value[idx] = { ...messages.value[idx], ...data }
    })

    client?.subscribe('/topic/chat.recall.global', (msg) => {
      const data = JSON.parse(msg.body) as RecallEvent
      const target = messages.value.find((m) => m.id === data.id)
      if (target) {
        target.status = 'DELETED'
        target.deletedType = data.deletedType || 'SELF'
      }
    })

    client?.subscribe('/topic/chat.typing.global', (msg) => {
      const data = JSON.parse(msg.body) as TypingPayload & { userId?: number | null; displayName?: string | null }
      if (!data.isTyping) {
        typingUsers.value = typingUsers.value.filter(
          (u) => !(u.userId === data.userId && u.displayName === data.displayName)
        )
      } else {
        const exists = typingUsers.value.some(
          (u) => u.userId === data.userId && u.displayName === data.displayName
        )
        if (!exists) {
          typingUsers.value.push({ userId: data.userId ?? null, displayName: data.displayName ?? null })
        }
      }
    })
  }

  function connect() {
    const auth = useAuthStore()
    const token = auth.accessToken || ''
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const host = `${location.hostname}:${location.port}`
    const base = `${protocol}://${host}/ws`
    const url = token ? `${base}?token=${encodeURIComponent(token)}` : base
    client = new Client({
      brokerURL: url,
      reconnectDelay: 5000,
      onConnect,
    })
    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    connected.value = false
  }

  function send(payload: SendPayload) {
    if (!client?.connected) return
    client.publish({ destination: '/app/chat.send', body: JSON.stringify(payload) })
  }

  function react(messageId: number, emoji: string) {
    if (!client?.connected) return
    const payload: ReactionActionPayload = { messageId, emoji }
    client.publish({ destination: '/app/chat.react', body: JSON.stringify(payload) })
  }

  function editMessage(id: number, content: string) {
    if (!client?.connected) return
    const payload: EditPayload = { id, content }
    client.publish({ destination: '/app/chat.edit', body: JSON.stringify(payload) })
  }

  function recallMessage(id: number) {
    if (!client?.connected) return
    const payload: RecallPayload = { id }
    client.publish({ destination: '/app/chat.recall', body: JSON.stringify(payload) })
  }

  function sendTyping(isTyping: boolean) {
    if (!client?.connected) return
    const now = Date.now()
    if (typingTimer) window.clearTimeout(typingTimer)
    if (isTyping) {
      typingTimer = window.setTimeout(() => sendTyping(false), 3000)
      if (now - lastTypingSent > 1000) {
        lastTypingSent = now
        const payload: TypingPayload = { roomId: ROOM, isTyping: true }
        client.publish({ destination: '/app/chat.typing', body: JSON.stringify(payload) })
      }
    } else {
      const payload: TypingPayload = { roomId: ROOM, isTyping: false }
      client.publish({ destination: '/app/chat.typing', body: JSON.stringify(payload) })
    }
  }

  async function loadHistory(roomId = ROOM, limit = 50) {
    const auth = useAuthStore()
    loading.value = true
    const params = new URLSearchParams({ roomId, limit: String(limit) })
    const headers: Record<string, string> = {}
    if (auth.accessToken) headers['Authorization'] = `Bearer ${auth.accessToken}`
    try {
      const res = await fetch(`/api/v1/chat/messages?${params.toString()}`, { headers })
      const json = await res.json()
      messages.value = json.data || []
    } finally {
      loading.value = false
    }
  }

  function deleteMessage(id: number) {
    const auth = useAuthStore()
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (auth.accessToken) headers['Authorization'] = `Bearer ${auth.accessToken}`
    return fetch(`/api/v1/chat/messages/${id}`, {
      method: 'DELETE',
      headers,
    }).then((r) => r.json())
  }

  function clearError() {
    error.value = ''
  }

  return {
    messages,
    onlineCount,
    error,
    typingUsers,
    connected,
    loading,
    connect,
    disconnect,
    send,
    react,
    editMessage,
    recallMessage,
    sendTyping,
    loadHistory,
    deleteMessage,
    clearError,
    isSelf,
    ROOM,
  }
})
