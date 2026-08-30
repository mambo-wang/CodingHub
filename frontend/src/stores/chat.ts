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
  const drawerOpen = ref(false)
  const unreadCount = ref(0)

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

  function openDrawer() {
    drawerOpen.value = true
    unreadCount.value = 0
  }

  function closeDrawer() {
    drawerOpen.value = false
  }

  function toggleDrawer() {
    if (drawerOpen.value) closeDrawer()
    else openDrawer()
  }

  function onConnect() {
    connected.value = true
    client?.subscribe('/topic/chat.global', (msg) => {
      const data = JSON.parse(msg.body) as ChatEvent
      // 普通聊天消息（ChatMessage）无 type 字段，其余事件均有
      if (!('type' in data)) {
        const chatMsg = data as ChatMessage
        // 按 id 去重：防止多连接/重复订阅导致同一条消息重复显示
        const existing = messages.value.find((m) => m.id === chatMsg.id)
        if (existing) {
          Object.assign(existing, chatMsg)
        } else {
          messages.value.push(chatMsg)
          if (!drawerOpen.value) unreadCount.value += 1
        }
        return
      }
      const type = data.type as string
      if (type === 'DELETE') {
        const idx = messages.value.findIndex((m) => m.id === (data as { id: number }).id)
        if (idx !== -1) messages.value.splice(idx, 1)
      } else if (type === 'PRESENCE') {
        onlineCount.value = (data as PresencePayload).online
      } else if (type === 'ERROR') {
        error.value = (data as unknown as { message: string }).message
      }
    })

    client?.subscribe('/user/queue/errors', (msg) => {
      const data = JSON.parse(msg.body)
      error.value = data.message
    })

    client?.subscribe('/topic/chat.reactions.global', (msg) => {
      const data = JSON.parse(msg.body) as ReactionUpdateEvent
      const target = messages.value.find((m) => m.id === data.messageId)
      if (target) {
        target.reactions = data.reactions
        // 防漂移：广播只带全房间计数，本地 myReactions 与计数矛盾时（如服务端校验失败）以计数为准清理
        if (target.myReactions) {
          target.myReactions = target.myReactions.filter((e) => (data.reactions?.[e] ?? 0) > 0)
        }
      }
    })

    client?.subscribe('/topic/chat.edit.global', (msg) => {
      const data = JSON.parse(msg.body) as ChatMessage
      const idx = messages.value.findIndex((m) => m.id === data.id)
      if (idx !== -1) {
        // 编辑广播不带个人视角：保留本地的 reactions/myReactions，仅更新内容相关字段
        const { reactions: _r, myReactions: _m, ...rest } = data
        messages.value[idx] = { ...messages.value[idx], ...rest }
      }
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

    // 幂等：已有活动连接直接复用，避免重复建连导致重复订阅、消息重复显示
    if (client) {
      if (client.active) return
      client.activate()
      return
    }

    client = new Client({
      brokerURL: url,
      reconnectDelay: 5000,
      onConnect,
    })
    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
    connected.value = false
  }

  function send(payload: SendPayload) {
    if (!client?.connected) return
    client.publish({ destination: '/app/chat.send', body: JSON.stringify(payload) })
  }

  function react(messageId: number, emoji: string) {
    if (!client?.connected) return
    // 乐观更新本地 myReactions：reaction 广播只带全房间计数（不含个人视角），
    // 不本地 toggle 会导致徽章无高亮、二次点击被误判为取消
    const target = messages.value.find((m) => m.id === messageId)
    if (target) {
      const mine = new Set(target.myReactions || [])
      if (mine.has(emoji)) mine.delete(emoji)
      else mine.add(emoji)
      target.myReactions = Array.from(mine)
    }
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
    drawerOpen,
    unreadCount,
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
    openDrawer,
    closeDrawer,
    toggleDrawer,
    ROOM,
  }
})
