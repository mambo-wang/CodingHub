import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig, type AxiosProgressEvent } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { FileUploadResponse, FileListResponse } from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

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

// Redirect to login page on auth errors
const redirectToLogin = () => {
  const authStore = useAuthStore()
  authStore.logout()
  const currentPath = router.currentRoute.value.fullPath
  router.push({
    name: 'Login',
    query: currentPath !== '/login' ? { redirect: currentPath } : undefined
  })
}

// Token refresh state — prevents concurrent refresh requests
let isRefreshing = false
let refreshSubscribers: Array<(success: boolean) => void> = []

const subscribeTokenRefresh = (callback: (success: boolean) => void) => {
  refreshSubscribers.push(callback)
}

const onTokenRefreshed = (success: boolean) => {
  refreshSubscribers.forEach(cb => cb(success))
  refreshSubscribers = []
}

// Response interceptor for error handling and token refresh
api.interceptors.response.use(
  (response) => {
    return response
  },
  async (error: AxiosError) => {
    const status = error.response?.status
    const authStore = useAuthStore()
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    // 401: token expired or invalid — try refresh, then redirect
    if (status === 401 && originalRequest && !originalRequest._retry) {
      // If another request is already refreshing, queue this one
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          subscribeTokenRefresh((success) => {
            if (success && originalRequest) {
              originalRequest.headers.Authorization = `Bearer ${authStore.accessToken}`
              resolve(api(originalRequest))
            } else {
              reject(error)
            }
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      if (authStore.refreshToken) {
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
            isRefreshing = false
            onTokenRefreshed(true)
            return api(originalRequest)
          }
        } catch {
          // Refresh token also invalid — fall through to logout
        }
      }

      // Refresh failed — notify all queued requests and redirect
      isRefreshing = false
      onTokenRefreshed(false)
      redirectToLogin()
      return Promise.reject(error)
    }

    // 403: genuine permission denied (user lacks required role)
    if (status === 403) {
      const requestUrl = originalRequest?.url || ''
      if (!requestUrl.includes('/auth/')) {
        ElMessage.warning('没有权限执行此操作')
      }
      return Promise.reject(error)
    }

    // Other errors
    const message = (error.response?.data as any)?.message || '请求失败'
    ElMessage.error(message)
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
      timeout: 600000,
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

// Tag API
export const tagApi = {
  getTags: (type: string) => {
    return api.get('/tags', { params: { type } }).then(res => res.data.data)
  },
  getHotTags: (type: string, limit: number = 10) => {
    return api.get('/tags/hot', { params: { type, limit } }).then(res => res.data.data)
  },
  createTag: (name: string, type: string) => {
    return api.post('/tags', { name, type }).then(res => res.data.data)
  }
}

export default api
