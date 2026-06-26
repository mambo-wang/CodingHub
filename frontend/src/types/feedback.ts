export type FeedbackCategory = 'SUGGESTION' | 'BUG_REPORT' | 'PRAISE' | 'OTHER'

export interface FeedbackMessage {
  id: number
  content: string
  nickname: string
  contact: string | null
  category: FeedbackCategory
  createdAt: string
  adminReply: string | null
  repliedAt: string | null
}

export interface FeedbackCreateRequest {
  content: string
  nickname?: string
  contact?: string
  category?: FeedbackCategory
}

export interface FeedbackReplyRequest {
  adminReply: string
}

export interface FeedbackPageResponse {
  content: FeedbackMessage[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
