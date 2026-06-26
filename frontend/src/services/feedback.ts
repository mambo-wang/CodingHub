import api from './api'
import type { FeedbackCreateRequest, FeedbackPageResponse, FeedbackMessage } from '@/types/feedback'

export const feedbackService = {
  async getFeedbacks(params: {
    category?: string
    page?: number
    size?: number
  }): Promise<FeedbackPageResponse> {
    const response = await api.get('/feedback', { params })
    return response.data.data
  },

  async createFeedback(data: FeedbackCreateRequest): Promise<FeedbackMessage> {
    const response = await api.post('/feedback', data)
    return response.data.data
  },

  async replyFeedback(id: number, adminReply: string): Promise<FeedbackMessage> {
    const response = await api.put(`/feedback/${id}/reply`, { adminReply })
    return response.data.data
  },

  async deleteFeedback(id: number): Promise<void> {
    await api.delete(`/feedback/${id}`)
  }
}
