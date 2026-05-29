import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig, type AxiosProgressEvent } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FileUploadResponse, FileListResponse } from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling and token refresh
api.interceptors.response.use(
  (response) => {
    return response
  },
  async (error: AxiosError) => {
    const authStore = useAuthStore()
    const originalRequest = error.config

    if (error.response?.status === 401 && authStore.refreshToken && originalRequest) {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, null, {
          headers: {
            Authorization: `Bearer ${authStore.refreshToken}`
          }
        })

        if (response.data.code === 200) {
          const newAccessToken = response.data.data.accessToken
          authStore.setTokens(newAccessToken, authStore.refreshToken!)
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
          return api(originalRequest)
        }
      } catch {
        authStore.logout()
        window.location.href = '/login'
      }
    }

    if (error.response?.status !== 401) {
      const message = (error.response?.data as any)?.message || '请求失败'
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

// File upload API
export const fileUploadApi = {
  uploadFiles: (
    toolId: number,
    files: File[],
    readme: string | null,
    onProgress?: (percent: number) => void
  ): Promise<FileUploadResponse> => {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    if (readme) {
      formData.append('readme', readme)
    }

    return api.post(`/tools/${toolId}/files`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    }).then(res => res.data as FileUploadResponse)
  },

  getToolFiles: (toolId: number): Promise<FileListResponse> => {
    return api.get(`/tools/${toolId}/files`)
      .then(res => res.data.data as FileListResponse)
  },

  deleteFile: (toolId: number, fileId: number): Promise<void> => {
    return api.delete(`/tools/${toolId}/files/${fileId}`)
      .then(res => res.data as void)
  },

  downloadFile: (toolId: number, fileId: number, fileName: string): void => {
    api.get(`/tools/${toolId}/files/${fileId}/download`, {
      responseType: 'blob'
    }).then(response => {
      const blob = new Blob([response.data], { type: response.headers['content-type'] as string })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }).catch(error => {
      ElMessage.error('文件下载失败')
      console.error('Download error:', error)
    })
  }
}

export default api
