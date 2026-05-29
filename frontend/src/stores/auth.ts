import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<User | null>(null)

  const isLoggedIn = computed(() => !!accessToken.value)

  const setTokens = (access: string, refresh: string) => {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  const setUser = (userData: User) => {
    user.value = userData
  }

  const logout = () => {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  const initFromStorage = () => {
    const storedToken = localStorage.getItem('accessToken')
    const storedRefresh = localStorage.getItem('refreshToken')
    if (storedToken) accessToken.value = storedToken
    if (storedRefresh) refreshToken.value = storedRefresh
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    setTokens,
    setUser,
    logout,
    initFromStorage
  }
})
