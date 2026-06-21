<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutGrid, Video, Bookmark, Loader2 } from '@lucide/vue'
import { interactionApi } from '@/services/interaction'
import { useAuthStore } from '@/stores/auth'
import VideoCard from '@/components/video/VideoCard.vue'
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue'
import type { VideoListItem } from '@/types/video'

const authStore = useAuthStore()
const router = useRouter()

const sidebarItems: SidebarNavItem[] = [
  { label: '微课列表', icon: LayoutGrid, to: '/videos' },
  { label: '我的微课', icon: Video, to: '/videos/my-videos', requiresAuth: true },
  { label: '我的收藏', icon: Bookmark, to: '/videos/my-favorites' }
]

const videos = ref<VideoListItem[]>([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const pageSize = 12
const hasMore = computed(() => page.value < totalPages.value - 1)
const loadingMore = ref(false)

const loadFavorites = async () => {
  loading.value = true
  try {
    const data = await interactionApi.getMyFavorites('VIDEO', 0, pageSize)
    videos.value = data.content
    totalPages.value = data.totalPages
    page.value = data.page
  } catch (e) {
    console.error('Failed to load favorites:', e)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = page.value + 1
    const data = await interactionApi.getMyFavorites('VIDEO', nextPage, pageSize)
    videos.value = [...videos.value, ...data.content]
    page.value = data.page
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more:', e)
  } finally {
    loadingMore.value = false
  }
}

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  loadFavorites()
})
</script>

<template>
  <div class="my-video-favorites-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <GeneralizedSidebar :items="sidebarItems" />

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <div>
          <h1 class="page-title">
            <span class="title-icon">
              <Bookmark :size="32" />
            </span>
            我的收藏
          </h1>
          <p class="page-subtitle">收藏的所有微课视频</p>
        </div>
      </div>

      <div v-if="loading" class="video-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card" aria-hidden="true">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-title"></div>
            <div class="skeleton-stats"></div>
          </div>
        </div>
      </div>

      <div v-else-if="videos.length === 0" class="empty-state">
        <Bookmark :size="48" aria-hidden="true" />
        <p class="empty-title">还没有收藏任何视频</p>
        <p class="empty-subtitle">浏览微课列表，收藏你喜欢的视频吧</p>
      </div>

      <template v-else>
        <div class="video-grid stagger-children">
          <VideoCard
            v-for="video in videos"
            :key="video.id"
            :video="video"
          />
        </div>
        <div v-if="hasMore" class="load-more-wrap">
          <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
            <Loader2 v-if="loadingMore" :size="16" class="spin" aria-hidden="true" />
            <span>{{ loadingMore ? '加载中...' : '加载更多' }}</span>
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.my-video-favorites-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 24px 80px;
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}
.page-bg { position: fixed; inset: 0; z-index: -1; pointer-events: none; overflow: hidden; }
.bg-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.3; }
.bg-orb-1 { top: -100px; right: -100px; width: 400px; height: 400px; background: radial-gradient(circle, var(--accent-1), transparent 70%); }
.bg-orb-2 { bottom: -100px; left: -100px; width: 400px; height: 400px; background: radial-gradient(circle, var(--accent-2), transparent 70%); }
.app-container { flex: 1; min-width: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }
.page-title { display: flex; align-items: center; gap: 12px; font-size: 32px; font-weight: 700; color: var(--text-primary); margin: 0 0 8px; }
.title-icon { filter: drop-shadow(0 0 12px rgba(245, 158, 11, 0.5)); color: #f59e0b; }
.page-subtitle { font-size: 14px; color: var(--text-secondary); margin: 0; }
.video-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 24px; }
.skeleton-card { background: var(--bg-glass); border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; }
.skeleton-cover { height: 160px; background: rgba(255, 255, 255, 0.03); animation: pulse 1.5s ease-in-out infinite; }
.skeleton-info { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.skeleton-title { height: 20px; background: rgba(255, 255, 255, 0.05); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-stats { height: 14px; width: 60%; background: rgba(255, 255, 255, 0.03); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 80px 40px; background: var(--bg-glass); border: 1px solid var(--border-color); border-radius: 16px; color: var(--text-muted); }
.empty-title { font-size: 20px; font-weight: 600; color: var(--text-primary); margin: 16px 0 8px; }
.empty-subtitle { font-size: 14px; color: var(--text-secondary); }
.load-more-wrap { display: flex; justify-content: center; margin-top: 32px; }
.load-more-btn { display: flex; align-items: center; gap: 8px; padding: 12px 24px; background: var(--bg-glass); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-secondary); font-size: 14px; cursor: pointer; transition: all 0.2s; }
.load-more-btn:hover:not(:disabled) { border-color: var(--accent-1); color: var(--accent-1); }
.load-more-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
</style>
