export interface VideoListItem {
  id: number
  title: string
  coverUrl: string | null
  duration: number
  viewCount: number
  likeCount: number
  commentCount: number
  uploaderId: number
  uploaderName: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
  createdAt: string
}

export interface VideoDetail {
  id: number
  title: string
  description: string | null
  coverUrl: string | null
  duration: number
  fileSize: number
  viewCount: number
  likeCount: number
  commentCount: number
  uploaderId: number
  uploaderName: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
  userLiked: boolean
  userFavorited: boolean
  createdAt: string
  updatedAt: string
}

export interface VideoComment {
  id: number
  content: string
  userId: number
  userNickname: string
  userAvatarUrl: string | null
  createdAt: string
}

export interface VideoUploadRequest {
  title: string
  description?: string
}

export interface VideoUpdateRequest {
  title: string
  description?: string
}

export interface VideoInteractionResponse {
  liked: boolean
  favorited: boolean
  likeCount: number
  commentCount: number
}

export interface VideoPageResponse {
  content: VideoListItem[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
