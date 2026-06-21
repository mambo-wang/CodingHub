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
  sortOrder: number
}

export interface ToolSummary {
  id: number
  name: string
  version?: string
  categoryName: string
  categoryIcon: string
  uploaderId: number
  uploaderUsername: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
  createdAt: string
}

export interface ToolDetail {
  id: number
  name: string
  version?: string
  categoryName: string
  categoryIcon: string
  content: string
  uploaderId: number
  uploaderUsername: string
  uploaderNickname?: string
  uploaderAvatarUrl?: string | null
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
}

export interface UpdateToolRequest {
  name: string
  categoryId: number
  content: string
  version?: string
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
