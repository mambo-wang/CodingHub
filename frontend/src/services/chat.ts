import api from './api'
import type { ChatMessage } from '@/types/chat'

export const chatService = {
  async getHistory(roomId: string = 'global', limit: number = 50): Promise<ChatMessage[]> {
    const response = await api.get('/chat/messages', { params: { roomId, limit } })
    return response.data.data
  },

  async deleteMessage(id: number): Promise<void> {
    await api.delete(`/chat/messages/${id}`)
  }
}
