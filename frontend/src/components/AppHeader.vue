<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { Sun, Moon } from '@lucide/vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const username = computed(() => authStore.user?.username)
const isDark = computed(() => themeStore.theme === 'dark')

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')
const goToMyTools = () => router.push('/me/tools')
const goToForum = () => router.push('/forum')
const goToOverview = () => router.push('/overview')
const goHome = () => router.push('/')
const toggleTheme = () => themeStore.toggleTheme()
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <!-- Logo -->
      <div class="logo" @click="goHome">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <path d="M14 2L4 8v12l10 6 10-6V8L14 2z" stroke="url(#logoGrad)" stroke-width="2" fill="none"/>
            <path d="M14 10l-4 2.4v4.8L14 20l4-2.4v-4.8L14 10z" fill="url(#logoGrad)"/>
            <defs>
              <linearGradient id="logoGrad" x1="4" y1="2" x2="24" y2="26" gradientUnits="userSpaceOnUse">
                <stop stop-color="#8b5cf6"/>
                <stop offset="1" stop-color="#06b6d4"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="logo-text">AI <span class="logo-accent">工具广场</span></span>
      </div>

      <!-- Nav Links -->
      <nav class="nav-links">
        <button class="nav-btn" @click="goHome">工具广场</button>
        <button class="nav-btn" @click="goToForum">论坛</button>
        <button class="nav-btn" @click="goToOverview">热榜</button>
        <template v-if="isLoggedIn">
          <button class="nav-btn" @click="goToMyTools">我的工具</button>
          <button class="theme-toggle-btn" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
            <Moon v-if="isDark" :size="18" />
            <Sun v-else :size="18" />
          </button>
          <div class="user-menu">
            <div class="user-avatar">
              <span>{{ username?.charAt(0).toUpperCase() }}</span>
            </div>
            <span class="user-name">{{ username }}</span>
            <button class="logout-btn" @click="handleLogout">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9"/>
              </svg>
            </button>
          </div>
        </template>
        <template v-else>
          <button class="theme-toggle-btn" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
            <Moon v-if="isDark" :size="18" />
            <Sun v-else :size="18" />
          </button>
          <button class="nav-btn" @click="goToLogin">登录</button>
          <button class="nav-btn nav-btn-primary" @click="goToRegister">注册</button>
        </template>
      </nav>
    </div>

    <!-- Decorative line -->
    <div class="header-line"></div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: rgba(9, 9, 11, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-color);
}

.header-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  filter: drop-shadow(0 0 10px rgba(139, 92, 246, 0.5));
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
}

.logo-accent {
  background: linear-gradient(135deg, #8b5cf6, #06b6d4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* Nav */
.nav-links {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-btn {
  padding: 10px 18px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-1px);
}

.nav-btn-primary {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
}

.nav-btn-primary:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.35), rgba(6, 182, 212, 0.35));
  border-color: rgba(139, 92, 246, 0.6);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.25);
}

/* User menu */
.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 24px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: white;
  box-shadow: 0 0 16px rgba(139, 92, 246, 0.4);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.logout-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 6px;
  color: #ef4444;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
  transform: scale(1.05);
}

/* Theme toggle button */
.theme-toggle-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.25s ease;
}

.theme-toggle-btn:hover {
  color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  transform: translateY(-1px);
}

/* Header decorative line */
.header-line {
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(139, 92, 246, 0.3) 20%,
    rgba(6, 182, 212, 0.3) 50%,
    rgba(236, 72, 153, 0.3) 80%,
    transparent 100%);
}

/* Light theme header */
[data-theme="light"] .app-header {
  background: rgba(248, 250, 252, 0.9);
}

[data-theme="light"] .nav-btn:hover {
  background: rgba(124, 58, 237, 0.08);
  border-color: rgba(124, 58, 237, 0.2);
  color: var(--accent-1);
}

[data-theme="light"] .user-menu {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 0, 0, 0.08);
}

[data-theme="light"] .logout-btn {
  background: rgba(239, 68, 68, 0.08);
  border-color: rgba(239, 68, 68, 0.15);
}

[data-theme="light"] .logout-btn:hover {
  background: rgba(239, 68, 68, 0.15);
}
</style>