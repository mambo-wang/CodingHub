import api from './api'
import type {
  VideoDetail,
  VideoComment,
  VideoInteractionResponse,
  VideoPageResponse,
  VideoUpdateRequest
} from '@/types/video'
import type { PageResponse } from '@/types'
import type { AxiosProgressEvent } from 'axios'

export const videoService = {
  /**
   * Upload video with progress tracking
   */
  async uploadVideo(
    file: File,
    title: string,
    description?: string,
    onProgress?: (percent: number) => void
  ): Promise<VideoDetail> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', title)
    if (description) formData.append('description', description)

    const response = await api.post('/videos', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000, // 10 min for large files
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
   * Get video list (paginated)
   */
  async getVideoList(page: number = 0, size: number = 20): Promise<VideoPageResponse> {
    const response = await api.get('/videos', { params: { page, size } })
    return response.data.data
  },

  /**
   * Get video detail (auto increments view count)
   */
  async getVideoDetail(id: number): Promise<VideoDetail> {
    const response = await api.get(`/videos/${id}`)
    return response.data.data
  },

  /**
   * Get stream URL for video player
   */
  getStreamUrl(id: number): string {
    return `/api/v1/videos/${id}/stream`
  },

  /**
   * Update video info (title, description)
   */
  async updateVideo(id: number, data: VideoUpdateRequest): Promise<VideoDetail> {
    const response = await api.put(`/videos/${id}`, data)
    return response.data.data
  },

  /**
   * Delete video (soft delete)
   */
  async deleteVideo(id: number): Promise<void> {
    await api.delete(`/videos/${id}`)
  },

  /**
   * Get my uploaded videos
   */
  async getMyVideos(page: number = 0, size: number = 20): Promise<VideoPageResponse> {
    const response = await api.get('/videos/my', { params: { page, size } })
    return response.data.data
  },

  /**
   * Toggle like (like/unlike)
   */
  async toggleLike(id: number): Promise<VideoInteractionResponse> {
    const response = await api.post(`/videos/${id}/like`)
    return response.data.data
  },

  /**
   * Toggle favorite (favorite/unfavorite)
   */
  async toggleFavorite(id: number): Promise<VideoInteractionResponse> {
    const response = await api.post(`/videos/${id}/favorite`)
    return response.data.data
  },

  /**
   * Get comments (paginated)
   */
  async getComments(id: number, page: number = 0, size: number = 20): Promise<PageResponse<VideoComment>> {
    const response = await api.get(`/videos/${id}/comments`, { params: { page, size } })
    return response.data.data
  },

  /**
   * Add comment
   */
  async addComment(id: number, content: string): Promise<VideoComment> {
    const response = await api.post(`/videos/${id}/comments`, { content })
    return response.data.data
  },

  /**
   * Get my favorites (paginated)
   */
  async getMyFavorites(page: number = 0, size: number = 20): Promise<VideoPageResponse> {
    const response = await api.get('/videos/my/favorites', { params: { page, size } })
    return response.data.data
  }
}
