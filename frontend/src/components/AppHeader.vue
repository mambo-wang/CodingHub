<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { Sun, Moon, User, LogOut } from '@lucide/vue'
import UserAvatar from './UserAvatar.vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const username = computed(() => authStore.user?.username)
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username)
const isAdmin = computed(() => authStore.isAdmin)
const isSuperAdmin = computed(() => authStore.isSuperAdmin)
const isDark = computed(() => themeStore.theme === 'dark')

const menuOpen = ref(false)

const toggleMenu = () => {
  menuOpen.value = !menuOpen.value
}

const closeMenu = () => {
  menuOpen.value = false
}

const handleDocClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement | null
  if (target && target.closest('.user-menu-wrapper')) return
  menuOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', handleDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocClick)
})

const handleLogout = () => {
  authStore.logout()
  router.push('/')
  closeMenu()
}

const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')
const goToMyTools = () => router.push('/me/tools')
const goToProfile = () => { router.push('/me/profile'); closeMenu() }
const goToForum = () => router.push('/forum')
const goToOverview = () => router.push('/overview')
const goHome = () => router.push('/')
const toggleTheme = () => themeStore.toggleTheme()
const goToQuickStart = () => router.push('/quickstart')
const goToAbout = () => router.push('/about')
const goToVideos = () => router.push('/videos')
const goToApprovals = () => router.push('/admin/approvals')
const goToUserList = () => router.push('/admin/users')
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
        <span class="logo-text">Coding<span class="logo-accent">Hub</span></span>
      </div>

      <!-- Nav Links -->
      <nav class="nav-links">
        <button class="nav-btn" @click="goHome">工具广场</button>
        <button class="nav-btn" @click="goToForum">论坛</button>
        <button class="nav-btn" @click="goToVideos">微课</button>
        <button class="nav-btn" @click="goToOverview">热榜</button>
        <button class="nav-btn" @click="goToQuickStart">快速开始</button>
        <button class="nav-btn" @click="goToAbout">关于</button>

        <template v-if="isLoggedIn">
          <button class="nav-btn" @click="goToMyTools">我的工具</button>
          <button v-if="isSuperAdmin" class="nav-btn" @click="goToApprovals">审批管理</button>
          <button v-if="isAdmin" class="nav-btn" @click="goToUserList">用户管理</button>
          <button class="theme-toggle-btn" @click="toggleTheme" :title="isDark ? '切换到浅色模式' : '切换到深色模式'">
            <Moon v-if="isDark" :size="18" />
            <Sun v-else :size="18" />
          </button>
          <div class="user-menu-wrapper" @click.stop>
            <button class="user-menu-trigger" @click="toggleMenu" :aria-expanded="menuOpen" aria-haspopup="true">
              <UserAvatar v-if="authStore.user" :user="authStore.user" size="md" />
              <span class="user-name">{{ displayName }}</span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" :class="{ 'caret-open': menuOpen }">
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </button>
            <Transition name="dropdown">
              <div v-if="menuOpen" class="user-dropdown" role="menu">
                <div class="user-dropdown-header">
                  <div class="user-dropdown-name">{{ displayName }}</div>
                  <div class="user-dropdown-username">@{{ username }}</div>
                </div>
                <hr class="user-dropdown-divider" />
                <button class="user-dropdown-item" role="menuitem" @click="goToProfile">
                  <User :size="16" aria-hidden="true" />
                  <span>个人资料</span>
                </button>
                <button class="user-dropdown-item" role="menuitem" @click="goToMyTools">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M20 7h-9M14 17H5"/><circle cx="17" cy="17" r="3"/><circle cx="7" cy="7" r="3"/>
                  </svg>
                  <span>我的工具</span>
                </button>
                <button class="user-dropdown-item" role="menuitem" @click="goToForum">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
                  </svg>
                  <span>论坛</span>
                </button>
                <hr class="user-dropdown-divider" />
                <button class="user-dropdown-item user-dropdown-item--danger" role="menuitem" @click="handleLogout">
                  <LogOut :size="16" aria-hidden="true" />
                  <span>退出登录</span>
                </button>
              </div>
            </Transition>
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

/* Dropdown */
.dropdown {
  position: relative;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 160px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  z-index: 100;
  animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.dropdown-item:hover {
  background: rgba(139, 92, 246, 0.15);
  color: var(--text-primary);
}

.dropdown-icon {
  font-size: 16px;
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
.user-menu-wrapper {
  position: relative;
}
.user-menu-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px 6px 6px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
  font-size: 14px;
}
.user-menu-trigger:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: var(--border-glow);
}
.user-menu-trigger:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.caret-open { transform: rotate(180deg); transition: transform 0.2s ease; }
.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 220px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  z-index: 100;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}
.user-dropdown-header { padding: 10px 12px 8px; }
.user-dropdown-name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.user-dropdown-username { font-size: 12px; color: var(--text-muted); margin-top: 2px; font-family: var(--font-mono); }
.user-dropdown-divider { border: none; height: 1px; background: var(--border-color); margin: 6px 0; }
.user-dropdown-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 10px 12px;
  background: transparent; border: none; border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px; font-family: var(--font-display);
  cursor: pointer; transition: all 0.15s ease; text-align: left;
}
.user-dropdown-item:hover { background: rgba(139, 92, 246, 0.15); color: var(--text-primary); }
.user-dropdown-item--danger { color: #ef4444; }
.user-dropdown-item--danger:hover { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.15s ease; }
.dropdown-enter-from { opacity: 0; transform: translateY(-4px); }
.dropdown-leave-to { opacity: 0; transform: translateY(-4px); }

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

[data-theme="light"] .user-menu-wrapper .user-menu-trigger {
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