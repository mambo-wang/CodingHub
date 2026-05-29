import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
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

export default api
