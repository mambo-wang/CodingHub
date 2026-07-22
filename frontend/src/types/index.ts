export interface User {
  id: number
  username: string
  nickname?: string
  avatarUrl?: string | null
  role?: string
  status?: string
  createdAt?: string
  lastLoginAt?: string
}

export interface Category {
  id: number
  name: string
  icon: string
  logoUrl?: string | null
  sortOrder: number
}

export interface Tag {
  id: number
  name: string
  tagType: string
  usageCount: number
}

export interface ToolSummary {
  id: number
  name: string
  version?: string
  description?: string
  categoryName: string
  categoryIcon: string
  logoUrl?: string | null
  uploaderId: number
  uploaderUsername: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
  score?: number
  pinned?: boolean
  viewCount?: number
  likeCount?: number
  commentCount?: number
  favoriteCount?: number
  downloadCount?: number
  tags?: Tag[]
  createdAt: string
}

export interface ToolDetail {
  id: number
  name: string
  version?: string
  description?: string
  categoryName: string
  categoryIcon: string
  logoUrl?: string | null
  content: string
  uploaderId: number
  uploaderUsername: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
  favoriteCount?: number
  downloadCount?: number
  tags?: Tag[]
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  nickname: string
  password: string
  role?: string
}

export interface CreateToolRequest {
  name: string
  categoryId: number
  content: string
  version: string
  description?: string
  tagIds?: number[]
}

export interface UpdateToolRequest {
  name: string
  categoryId: number
  content: string
  version?: string
  description?: string
  tagIds?: number[]
}

export interface ToolFile {
  id: number
  toolId: number
  originalName: string
  storedPath: string
  fileSize: number
  contentType: string
  createdAt: string
}

export interface FileUploadResponse {
  toolId: number
  files: ToolFile[]
  readmeSaved: boolean
}

export interface FileListResponse {
  toolId: number
  folderPath: string
  files: ToolFile[]
  readmeExists: boolean
}

export interface PendingUser {
  id: number
  username: string
  nickname?: string
  role: string
  createdAt: string
}

export interface AdminUser {
  id: number
  username: string
  nickname?: string
  role: string
  status: string
  createdAt: string
  lastLoginAt?: string
}

export interface ApprovalResponse {
  userId: number
  status: string
  message: string
}
