export interface KnowledgeBase {
  id: number
  name: string
  description: string | null
  ownerId: number
  ownerNickname: string | null
  ragCollection: string
  ragBaseUrl: string
  documentsUrl: string
  createdAt: string
}

/** Document processing status enum */
export type DocumentStatus = 'UPLOADING' | 'CONVERTING' | 'CHUNKING' | 'EMBEDDING' | 'READY' | 'FAILED'

/** RAG SQLite document metadata with processing status */
export interface RagDocumentStatus {
  id: number
  collection: string
  filename: string
  filepath: string
  file_size: number
  uploader: string | null
  status: DocumentStatus
  chunk_count: number
  chunk_mode: string | null
  error_message: string | null
  created_at: string
  updated_at: string
}

/** RAG 服务返回的文档格式（直连 RAG 时使用） */
export interface RagDocument {
  source: string
  chunk_count: number
}

export interface KbConfig {
  chunk_mode: string
  chunk_size: number
  chunk_overlap: number
  rerank: boolean
  description: string | null
  strategy: string
  context_header: boolean
}

export interface KbSearchResult {
  text: string
  source: string
  score: number
  chunkIndex: number | null
}

export interface KbCreateRequest {
  name: string
  description?: string
  chunkMode?: string
  chunkSize?: number
  chunkOverlap?: number
  rerank?: boolean
}

export interface KbUpdateRequest {
  name?: string
  description?: string
}

export interface KbConfigRequest {
  chunkMode?: string
  chunkSize?: number
  chunkOverlap?: number
  rerank?: boolean
  description?: string
}

export interface KbSearchRequest {
  query: string
  topK?: number
  rerank?: boolean
  expandContext?: number
}

export interface KbPageResponse {
  content: KnowledgeBase[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}
