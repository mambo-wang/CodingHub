import api from './api'
import axios from 'axios'
import type {
  KnowledgeBase,
  RagDocument,
  RagDocumentStatus,
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
   * @param ownerId Optional owner ID to filter by
   */
  async getList(page: number = 0, size: number = 20, sortBy: string = 'latest', ownerId?: number): Promise<KbPageResponse> {
    const params: Record<string, any> = { page, size, sortBy }
    if (ownerId !== undefined) {
      params.ownerId = ownerId
    }
    const response = await api.get('/knowledge', { params })
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
   * 批量上传文档（直连 RAG，异步处理）
   * @param documentsUrl KB 详情中的 documentsUrl（会自动加 /batch 后缀）
   * @param files 多个文件
   * @param onProgress 整体上传进度回调（0-90为HTTP传输，90+为处理中）
   */
  async batchUpload(
    documentsUrl: string,
    files: File[],
    onProgress?: (percent: number) => void
  ): Promise<RagDocumentStatus[]> {
    const batchUrl = documentsUrl + '/batch'
    const formData = new FormData()
    for (const file of files) {
      formData.append('files', file)
    }

    const response = await axios.post(batchUrl, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (progressEvent.total && onProgress) {
          const rawPercent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(Math.min(rawPercent, 90))
        }
      }
    })
    return response.data
  },

  /**
   * 上传单个文档（兼容方法，内部调用 batchUpload）
   * @param documentsUrl KB 详情中的 documentsUrl
   */
  async uploadDocument(
    documentsUrl: string,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<{ status: string; filename: string; chunks: number }> {
    const results = await this.batchUpload(documentsUrl, [file], onProgress)
    if (results.length > 0) {
      const r = results[0]
      return { status: r.status, filename: r.filename, chunks: r.chunk_count }
    }
    throw new Error('上传失败')
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

  // ── 文档状态查询：直连 RAG 服务 ─────────────────────────────

  /**
   * 获取集合内所有文档的处理状态（直连 RAG）
   * @param ragBaseUrl KB 详情中的 ragBaseUrl
   * @param ragCollection KB 详情中的 ragCollection
   */
  async getDocumentStatus(ragBaseUrl: string, ragCollection: string): Promise<RagDocumentStatus[]> {
    const url = `${ragBaseUrl}/api/collections/${encodeURIComponent(ragCollection)}/documents/status`
    const response = await axios.get(url, { timeout: 10000 })
    return response.data
  },

  /**
   * 获取单个文档的处理状态（直连 RAG）
   * @param ragBaseUrl KB 详情中的 ragBaseUrl
   * @param ragCollection KB 详情中的 ragCollection
   * @param docId 文档 ID
   */
  async getSingleDocumentStatus(ragBaseUrl: string, ragCollection: string, docId: number): Promise<RagDocumentStatus> {
    const url = `${ragBaseUrl}/api/collections/${encodeURIComponent(ragCollection)}/documents/${docId}/status`
    const response = await axios.get(url, { timeout: 10000 })
    return response.data
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
