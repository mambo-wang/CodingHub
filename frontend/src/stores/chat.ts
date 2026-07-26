import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import type { ChatMessage, SendPayload } from '@/types/chat'
import { chatService } from '@/services/chat'
import { useAuthStore } from './auth'

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const onlineCount = ref(0)
  const connected = ref(false)
  const connecting = ref(false)
  const error = ref<string | null>(null)
  const loading = ref(false)
  const unreadCount = ref(0)
  const drawerOpen = ref(false)

  let stompClient: Client | null = null

  const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const WS_URL = (() => {
    const base = API_BASE as string
    if (base.startsWith('http')) {
      return base.replace(/^http/, 'ws').replace(/\/api\/v1$/, '/ws')
    }
    const loc = window.location
    const proto = loc.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${proto}//${loc.host}/ws`
  })()

  async function loadHistory() {
    loading.value = true
    error.value = null
    try {
      messages.value = await chatService.getHistory('global', 50)
    } catch (e: any) {
      error.value = '加载历史消息失败'
    } finally {
      loading.value = false
    }
  }

  function connect() {
    if (stompClient?.active) return
    connecting.value = true

    const authStore = useAuthStore()
    const token = authStore.accessToken || ''
    const url = token ? `${WS_URL}?token=${encodeURIComponent(token)}` : WS_URL

    stompClient = new Client({
      brokerURL: url,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
        connecting.value = false
        error.value = null

        stompClient!.subscribe('/topic/chat.global', (msg) => {
          try {
            const data = JSON.parse(msg.body)
            if (data.type === 'DELETE') {
              messages.value = messages.value.filter(m => m.id !== data.id)
            } else if (data.type === 'PRESENCE') {
              onlineCount.value = data.online
            } else if (data.type === 'ERROR') {
              error.value = data.message
            } else if (data.id) {
              messages.value.push(data)
              if (!drawerOpen.value) {
                unreadCount.value++
              }
            }
          } catch (e) {
            console.error('Failed to parse chat message', e)
          }
        })

        stompClient!.subscribe('/topic/chat.presence', (msg) => {
          try {
            const data = JSON.parse(msg.body)
            if (data.online !== undefined) {
              onlineCount.value = data.online
            }
          } catch (e) { /* ignore */ }
        })

        stompClient!.subscribe('/user/queue/errors', (msg) => {
          try {
            const data = JSON.parse(msg.body)
            error.value = data.message
          } catch (e) { /* ignore */ }
        })

        loadHistory()
      },
      onDisconnect: () => {
        connected.value = false
        connecting.value = false
      },
      onStompError: (frame) => {
        error.value = '连接错误: ' + (frame.headers['message'] || '未知')
        connecting.value = false
      },
      onWebSocketError: () => {
        connecting.value = false
      }
    })

    stompClient.activate()
  }

  function disconnect() {
    if (stompClient) {
      stompClient.deactivate()
      stompClient = null
    }
    connected.value = false
    connecting.value = false
  }

  function send(payload: SendPayload) {
    if (!stompClient?.active) return
    stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(payload)
    })
  }

  async function deleteMessage(id: number) {
    try {
      await chatService.deleteMessage(id)
    } catch (e: any) {
      error.value = '删除失败'
    }
  }

  function clearError() {
    error.value = null
  }

  function openDrawer() {
    drawerOpen.value = true
    unreadCount.value = 0
  }

  function closeDrawer() {
    drawerOpen.value = false
  }

  return {
    messages,
    onlineCount,
    connected,
    connecting,
    error,
    loading,
    unreadCount,
    drawerOpen,
    connect,
    disconnect,
    send,
    deleteMessage,
    loadHistory,
    clearError,
    openDrawer,
    closeDrawer
  }
})
