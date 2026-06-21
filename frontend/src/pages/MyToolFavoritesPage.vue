<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutGrid, Wrench, Bookmark } from '@lucide/vue'
import { interactionApi } from '@/services/interaction'
import { useAuthStore } from '@/stores/auth'
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue'
import type { ToolSummary } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const sidebarItems: SidebarNavItem[] = [
  { label: '工具列表', icon: LayoutGrid, to: '/' },
  { label: '我的工具', icon: Wrench, to: '/me/tools', requiresAuth: true },
  { label: '我的收藏', icon: Bookmark, to: '/me/favorites' }
]

const favorites = ref<ToolSummary[]>([])
const loading = ref(true)
const pagination = ref({ page: 0, size: 12, totalElements: 0, totalPages: 0 })

const fetchFavorites = async () => {
  loading.value = true
  try {
    const data = await interactionApi.getMyFavorites('TOOL', pagination.value.page, pagination.value.size)
    favorites.value = data.content
    pagination.value = {
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages
    }
  } catch (error) {
    console.error('Failed to fetch favorites:', error)
  } finally {
    loading.value = false
  }
}

const goToDetail = (toolId: number) => {
  router.push(`/tools/${toolId}`)
}

const formatDate = (dateStr: string) => new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  fetchFavorites()
})
</script>

<template>
  <div class="my-favorites-page">
    <div class="page-bg"><div class="bg-orb bg-orb-1"></div><div class="bg-orb bg-orb-2"></div></div>

    <GeneralizedSidebar :items="sidebarItems" />

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <h1 class="page-title">
          <span class="title-icon">
            <Bookmark :size="32" />
          </span>
          我的收藏
        </h1>
        <p class="page-subtitle">收藏的所有 AI 工具</p>
      </div>

      <div v-if="loading" class="tools-list glass-card">
        <div v-for="i in 4" :key="i" class="tool-item-skeleton">
          <div class="skeleton-badge"></div><div class="skeleton-title"></div><div class="skeleton-date"></div>
        </div>
      </div>

      <div v-else-if="favorites.length === 0" class="empty-state glass-card animate-fade-in-up">
        <div class="empty-icon">
          <Bookmark :size="48" />
        </div>
        <h3 class="empty-title">还没有收藏任何工具</h3>
        <p class="empty-desc">浏览工具列表，收藏你喜欢的工具吧</p>
        <button class="browse-btn" @click="router.push('/')">
          浏览工具
        </button>
      </div>

      <div v-else class="tools-list glass-card animate-fade-in-up">
        <div
          v-for="tool in favorites"
          :key="tool.id"
          class="tool-item"
          @click="goToDetail(tool.id)"
        >
          <div class="tool-info">
            <span class="category-badge">{{ tool.categoryIcon }} {{ tool.categoryName }}</span>
            <h3 class="tool-name">{{ tool.name }}</h3>
            <span class="tool-date">收藏于 {{ formatDate(tool.createdAt) }}</span>
          </div>
        </div>
      </div>

      <div v-if="pagination.totalPages > 1" class="pagination-wrapper">
        <div class="pagination glass-card">
          <button class="page-btn" :disabled="pagination.page === 0" @click="pagination.page--; fetchFavorites()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <div class="page-numbers">
            <button v-for="p in pagination.totalPages" :key="p" class="page-number" :class="{ active: pagination.page === p - 1 }" @click="pagination.page = p - 1; fetchFavorites()">{{ p }}</button>
          </div>
          <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="pagination.page++; fetchFavorites()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>
    </div>

    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">&#10022; Created By Deerflow</a>
  </div>
</template>

<style scoped>
.my-favorites-page { min-height: calc(100vh - 60px); padding: 40px 20px 80px; position: relative; display: flex; gap: 24px; max-width: 1200px; margin: 0 auto; }
.page-bg { position: fixed; inset: 0; pointer-events: none; overflow: hidden; }
.bg-orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.3; }
.bg-orb-1 { width: 400px; height: 400px; background: rgba(139, 92, 246, 0.3); top: -100px; right: -100px; }
.bg-orb-2 { width: 300px; height: 300px; background: rgba(6, 182, 212, 0.2); bottom: 100px; left: -100px; }
.app-container { flex: 1; min-width: 0; position: relative; z-index: 1; }
.page-header { text-align: center; margin-bottom: 24px; }
.page-title { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 36px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.title-icon { filter: drop-shadow(0 0 12px rgba(245, 158, 11, 0.5)); color: #f59e0b; }
.page-subtitle { font-size: 16px; color: var(--text-secondary); }
.tools-list { overflow: hidden; }
.tool-item-skeleton { display: flex; align-items: center; gap: 16px; padding: 24px; border-bottom: 1px solid var(--border-color); }
.tool-item-skeleton:last-child { border-bottom: none; }
.skeleton-badge { width: 80px; height: 24px; background: rgba(255, 255, 255, 0.05); border-radius: 12px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-title { width: 200px; height: 20px; background: rgba(255, 255, 255, 0.05); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-date { width: 100px; height: 14px; background: rgba(255, 255, 255, 0.03); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 80px 40px; }
.empty-icon { width: 100px; height: 100px; margin: 0 auto 24px; background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(139, 92, 246, 0.1)); border: 1px solid var(--border-color); border-radius: 24px; display: flex; align-items: center; justify-content: center; color: #f59e0b; }
.empty-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: var(--text-secondary); margin-bottom: 32px; }
.browse-btn { display: inline-flex; align-items: center; gap: 8px; padding: 14px 28px; background: linear-gradient(135deg, var(--accent-1), var(--accent-2)); border: none; border-radius: 10px; color: white; font-family: var(--font-display); font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.25s ease; }
.browse-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35); }
.tool-item { display: flex; justify-content: space-between; align-items: center; padding: 24px; border-bottom: 1px solid var(--border-color); transition: background 0.2s ease; cursor: pointer; }
.tool-item:last-child { border-bottom: none; }
.tool-item:hover { background: rgba(255, 255, 255, 0.02); }
.tool-info { display: flex; flex-direction: column; gap: 8px; }
.category-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.2); border-radius: 16px; font-size: 12px; color: var(--accent-1); width: fit-content; }
.tool-name { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.tool-date { font-size: 13px; color: var(--text-muted); font-family: var(--font-mono); }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 32px; }
.pagination { display: flex; align-items: center; gap: 8px; padding: 8px; }
.page-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 8px; color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease; }
.page-btn:hover:not(:disabled) { background: rgba(139, 92, 246, 0.1); border-color: rgba(139, 92, 246, 0.3); color: var(--text-primary); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-numbers { display: flex; gap: 4px; }
.page-number { min-width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: transparent; border: 1px solid transparent; border-radius: 8px; color: var(--text-secondary); font-family: var(--font-mono); font-size: 13px; cursor: pointer; transition: all 0.2s ease; }
.page-number:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.page-number.active { background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2)); border-color: rgba(139, 92, 246, 0.4); color: var(--text-primary); box-shadow: 0 0 16px rgba(139, 92, 246, 0.2); }
</style>
