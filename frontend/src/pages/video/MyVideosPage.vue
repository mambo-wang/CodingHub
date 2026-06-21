<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { LayoutGrid, Video, Bookmark, Upload, Loader2, VideoOff } from '@lucide/vue'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import VideoCard from '@/components/video/VideoCard.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue'
import { ElMessage } from 'element-plus'
import type { VideoListItem } from '@/types/video'

const authStore = useAuthStore()
const router = useRouter()

const sidebarItems: SidebarNavItem[] = [
  { label: '微课列表', icon: LayoutGrid, to: '/videos' },
  { label: '我的微课', icon: Video, to: '/videos/my-videos' },
  { label: '我的收藏', icon: Bookmark, to: '/videos/my-favorites' }
]

const videos = ref<VideoListItem[]>([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const pageSize = 12
const hasMore = computed(() => page.value < totalPages.value - 1)
const loadingMore = ref(false)
const deleteDialogVisible = ref(false)
const deleteTargetId = ref<number | null>(null)
const deleting = ref(false)

const loadVideos = async () => {
  loading.value = true
  try {
    const data = await videoService.getMyVideos(0, pageSize)
    videos.value = data.content
    totalPages.value = data.totalPages
    page.value = data.page
  } catch (e) {
    console.error('Failed to load videos:', e)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = page.value + 1
    const data = await videoService.getMyVideos(nextPage, pageSize)
    videos.value = [...videos.value, ...data.content]
    page.value = data.page
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more:', e)
  } finally {
    loadingMore.value = false
  }
}

const handleVideoEdit = (videoId: number) => {
  router.push(`/videos/${videoId}/edit`)
}

const handleVideoDelete = (videoId: number) => {
  deleteTargetId.value = videoId
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (deleteTargetId.value === null) return
  deleting.value = true
  try {
    await videoService.deleteVideo(deleteTargetId.value)
    videos.value = videos.value.filter(v => v.id !== deleteTargetId.value)
    ElMessage.success('视频已删除')
    deleteDialogVisible.value = false
  } catch (e: any) {
    if (e.response?.status === 403) {
      ElMessage.warning('无权操作此内容')
    } else {
      ElMessage.error('删除失败，请稍后重试')
    }
  } finally {
    deleting.value = false
  }
}

const handleDialogCancel = () => {
  deleteDialogVisible.value = false
}

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  loadVideos()
})
</script>

<template>
  <div class="my-videos-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <GeneralizedSidebar :items="sidebarItems" />

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <div>
          <h1 class="page-title">
            <span class="title-icon">🎬</span>
            我的微课
          </h1>
          <p class="page-subtitle">管理您上传的所有微课视频</p>
        </div>
        <router-link to="/videos/upload" class="upload-btn" aria-label="上传视频">
          <Upload :size="16" aria-hidden="true" />
          <span>上传视频</span>
        </router-link>
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
        <VideoOff :size="48" aria-hidden="true" />
        <p class="empty-title">还没有上传任何视频</p>
        <p class="empty-subtitle">开始分享你的第一个微课吧</p>
      </div>

      <template v-else>
        <div class="video-grid stagger-children">
          <VideoCard
            v-for="video in videos"
            :key="video.id"
            :video="video"
            :editable="true"
            :deletable="true"
            @edit="handleVideoEdit"
            @delete="handleVideoDelete"
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

    <ConfirmDialog
      :visible="deleteDialogVisible"
      title="删除微课"
      description="确定要删除这个微课视频吗？此操作不可撤销。"
      confirm-text="确认删除"
      cancel-text="取消"
      :danger="true"
      :loading="deleting"
      @confirm="handleConfirmDelete"
      @cancel="handleDialogCancel"
      @update:visible="deleteDialogVisible = $event"
    />
  </div>
</template>

<style scoped>
.my-videos-page {
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
.title-icon { filter: drop-shadow(0 0 12px rgba(139, 92, 246, 0.5)); }
.page-subtitle { font-size: 14px; color: var(--text-secondary); margin: 0; }
.upload-btn { display: flex; align-items: center; gap: 8px; padding: 12px 20px; background: linear-gradient(135deg, var(--accent-1), var(--accent-2)); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; text-decoration: none; transition: all 0.2s; }
.upload-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4); }
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
