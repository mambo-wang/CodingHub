import api from './api'

export type TargetType = 'TOOL' | 'FORUM_POST' | 'VIDEO' | 'PLUGIN'

export interface InteractionRequest {
  targetType: TargetType
  targetId: number
  content?: string
  parentId?: number
  userName?: string
}

export interface LikeResponse {
  liked: boolean
  likeCount: number
}

export interface FavoriteResponse {
  favorited: boolean
}

export interface CommentResponse {
  id: number
  targetType: string
  targetId: number
  userId: number | null
  userName: string | null
  userNickname: string | null
  userAvatarUrl: string | null
  parentId: number | null
  rootId: number | null
  content: string
  commentLikeCount: number
  createdAt: string
}

export interface InteractionPageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface MyCommentItem {
  id: number
  targetType: TargetType
  targetId: number
  targetTitle: string
  content: string
  createdAt: string
}

export const interactionApi = {
  // Likes
  toggleLike: (targetType: TargetType, targetId: number): Promise<LikeResponse> => {
    return api.post('/interactions/likes', { targetType, targetId })
      .then(res => res.data.data)
  },

  getLikeStatus: (targetType: TargetType, targetId: number): Promise<LikeResponse> => {
    return api.get('/interactions/likes/status', { params: { targetType, targetId } })
      .then(res => res.data.data)
  },

  // Comments
  getComments: (targetType: TargetType, targetId: number, page: number = 0, size: number = 20): Promise<InteractionPageResponse<CommentResponse>> => {
    return api.get('/interactions/comments', { params: { targetType, targetId, page, size } })
      .then(res => res.data.data)
  },

  addComment: (targetType: TargetType, targetId: number, content: string, parentId?: number, userName?: string): Promise<CommentResponse> => {
    return api.post('/interactions/comments', { targetType, targetId, content, parentId, userName })
      .then(res => res.data.data)
  },

  deleteComment: (commentId: number): Promise<void> => {
    return api.delete(`/interactions/comments/${commentId}`)
      .then(res => res.data)
  },

  // Favorites
  toggleFavorite: (targetType: TargetType, targetId: number): Promise<FavoriteResponse> => {
    return api.post('/interactions/favorites', { targetType, targetId })
      .then(res => res.data.data)
  },

  getMyFavorites: (targetType: TargetType, page: number = 0, size: number = 10): Promise<InteractionPageResponse<any>> => {
    return api.get('/interactions/favorites', { params: { targetType, page, size } })
      .then(res => res.data.data)
  },

  getMyLikes: (targetType: TargetType, page: number = 0, size: number = 10): Promise<InteractionPageResponse<any>> => {
    return api.get('/interactions/likes/mine', { params: { targetType, page, size } })
      .then(res => res.data.data)
  },

  getMyComments: (page: number = 0, size: number = 10): Promise<InteractionPageResponse<MyCommentItem>> => {
    return api.get('/interactions/comments/mine', { params: { page, size } })
      .then(res => res.data.data)
  },

  getFavoriteStatus: (targetType: TargetType, targetId: number): Promise<FavoriteResponse> => {
    return api.get('/interactions/favorites/status', { params: { targetType, targetId } })
      .then(res => res.data.data)
  }
}
