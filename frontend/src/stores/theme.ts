import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type Theme = 'dark' | 'light'

export const useThemeStore = defineStore('theme', () => {
  // 从 localStorage 读取主题，默认为 dark
  const savedTheme = localStorage.getItem('theme') as Theme | null
  const theme = ref<Theme>(savedTheme || 'dark')

  // 应用主题到 document
  const applyTheme = (newTheme: Theme) => {
    if (newTheme === 'light') {
      document.documentElement.setAttribute('data-theme', 'light')
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
  }

  // 初始化主题
  applyTheme(theme.value)

  // 监听主题变化，保存到 localStorage
  watch(theme, (newTheme) => {
    localStorage.setItem('theme', newTheme)
    applyTheme(newTheme)
  })

  // 切换主题
  const toggleTheme = () => {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  // 设置特定主题
  const setTheme = (newTheme: Theme) => {
    theme.value = newTheme
  }

  return {
    theme,
    toggleTheme,
    setTheme
  }
})