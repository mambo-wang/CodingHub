import api from './api'
import axios from 'axios'
import type {
  KnowledgeBase,
  RagDocument,
  KbConfig,
  KbSearchResult,
  KbCreateRequest,
  KbUpdateRequest,
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

  // ── 文档操作：直连 RAG 服务 ─────────────────────────────────

  /**
   * 获取文档列表（直连 RAG）
   * @param documentsUrl KB 详情中的 documentsUrl
   */
  async getDocuments(documentsUrl: string): Promise<RagDocument[]> {
    const response = await axios.get(documentsUrl)
    return response.data
  },

  /**
   * 上传文档（直连 RAG，无需认证）
   * @param documentsUrl KB 详情中的 documentsUrl
   */
  async uploadDocument(
    documentsUrl: string,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<{ status: string; filename: string; chunks: number }> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await axios.post(documentsUrl, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 900000,
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
    return response.data
  },

  /**
   * 删除文档（直连 RAG）
   * @param documentsUrl KB 详情中的 documentsUrl
   * @param filepath 文档路径（RAG 返回的 source 字段）
   */
  async deleteDocument(documentsUrl: string, filepath: string): Promise<void> {
    await axios.delete(documentsUrl, {
      data: { filepath },
      timeout: 30000
    })
  },

  // ── 搜索：经 Java 代理 ─────────────────────────────────────

  /**
   * Search within a knowledge base (via Java proxy)
   */
  async search(kbId: number, data: KbSearchRequest): Promise<KbSearchResult[]> {
    const response = await api.post(`/knowledge/${kbId}/search`, data)
    return response.data.data
  },

  // ── 配置操作：直连 RAG 服务 ─────────────────────────────────

  /**
   * 获取 RAG 配置（直连 RAG）
   * @param ragBaseUrl KB 详情中的 ragBaseUrl
   * @param ragCollection KB 详情中的 ragCollection
   */
  async getConfig(ragBaseUrl: string, ragCollection: string): Promise<KbConfig> {
    const response = await axios.get(`${ragBaseUrl}/api/collections/${encodeURIComponent(ragCollection)}/config`)
    return response.data
  },

  /**
   * 更新 RAG 配置（直连 RAG）
   * @param ragBaseUrl KB 详情中的 ragBaseUrl
   * @param ragCollection KB 详情中的 ragCollection
   */
  async updateConfig(ragBaseUrl: string, ragCollection: string, data: Partial<KbConfig>): Promise<void> {
    await axios.put(`${ragBaseUrl}/api/collections/${encodeURIComponent(ragCollection)}/config`, data)
  }
}
