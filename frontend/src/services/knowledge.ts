import api from './api'
import type {
  KnowledgeBase,
  KbDocument,
  KbConfig,
  KbSearchResult,
  KbCreateRequest,
  KbUpdateRequest,
  KbConfigRequest,
  KbSearchRequest,
  KbPageResponse
} from '@/types/knowledge'
import type { AxiosProgressEvent } from 'axios'

export const knowledgeService = {
  /**
   * Get knowledge base list (paginated)
   */
  async getList(page: number = 0, size: number = 20, sortBy: string = 'latest'): Promise<KbPageResponse> {
    const response = await api.get('/knowledge', { params: { page, size, sortBy } })
    return response.data.data
  },

  /**
   * Get knowledge base detail
   */
  async getDetail(id: number): Promise<KnowledgeBase> {
    const response = await api.get(`/knowledge/${id}`)
    return response.data.data
  },

  /**
   * Create a new knowledge base
   */
  async create(data: KbCreateRequest): Promise<KnowledgeBase> {
    const response = await api.post('/knowledge', data)
    return response.data.data
  },

  /**
   * Update knowledge base info
   */
  async update(id: number, data: KbUpdateRequest): Promise<KnowledgeBase> {
    const response = await api.put(`/knowledge/${id}`, data)
    return response.data.data
  },

  /**
   * Delete knowledge base (soft delete)
   */
  async delete(id: number): Promise<void> {
    await api.delete(`/knowledge/${id}`)
  },

  /**
   * Get documents in a knowledge base
   */
  async getDocuments(kbId: number): Promise<KbDocument[]> {
    const response = await api.get(`/knowledge/${kbId}/documents`)
    return response.data.data
  },

  /**
   * Upload a document to a knowledge base
   */
  async uploadDocument(
    kbId: number,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<KbDocument> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await api.post(`/knowledge/${kbId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000,
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
    return response.data.data
  },

  /**
   * Delete a document from a knowledge base
   */
  async deleteDocument(kbId: number, docId: number): Promise<void> {
    await api.delete(`/knowledge/${kbId}/documents/${docId}`)
  },

  /**
   * Search within a knowledge base
   */
  async search(kbId: number, data: KbSearchRequest): Promise<KbSearchResult[]> {
    const response = await api.post(`/knowledge/${kbId}/search`, data)
    return response.data.data
  },

  /**
   * Get knowledge base RAG config
   */
  async getConfig(kbId: number): Promise<KbConfig> {
    const response = await api.get(`/knowledge/${kbId}/config`)
    return response.data.data
  },

  /**
   * Update knowledge base RAG config
   */
  async updateConfig(kbId: number, data: KbConfigRequest): Promise<void> {
    await api.put(`/knowledge/${kbId}/config`, data)
  }
}
