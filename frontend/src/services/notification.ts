import api from './api'
import type { PageResponse } from '@/types'

export interface NotificationItem {
  id: number
  type: string
  targetType: string
  targetId: number
  message: string
  actorId: number | null
  actorName: string | null
  isRead: boolean
  createdAt: string
}

export const notificationService = {
  async getNotifications(page: number = 0, size: number = 20): Promise<PageResponse<NotificationItem>> {
    const response = await api.get('/notifications', { params: { page, size } })
    return response.data.data
  },

  async getUnreadCount(): Promise<number> {
    const response = await api.get('/notifications/unread-count')
    return response.data.data
  },

  async markAsRead(id: number): Promise<void> {
    await api.put(`/notifications/${id}/read`)
  },

  async markAllAsRead(): Promise<void> {
    await api.put('/notifications/read-all')
  }
}
