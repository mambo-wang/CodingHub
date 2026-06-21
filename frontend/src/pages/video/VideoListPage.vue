<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, VideoOff, Loader2 } from '@lucide/vue'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import VideoCard from '@/components/video/VideoCard.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { ElMessage } from 'element-plus'
import type { VideoListItem } from '@/types/video'

const authStore = useAuthStore()
const router = useRouter()
const isLoggedIn = computed(() => authStore.isLoggedIn)

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

onMounted(async () => {
  await loadVideos()
})

const loadVideos = async () => {
  loading.value = true
  try {
    const data = await videoService.getVideoList(0, pageSize)
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
    const data = await videoService.getVideoList(nextPage, pageSize)
    videos.value = [...videos.value, ...data.content]
    page.value = data.page
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more videos:', e)
  } finally {
    loadingMore.value = false
  }
}

const canEditVideo = (uploaderId: number) => {
  if (!authStore.isLoggedIn) return false
  return authStore.user?.id === uploaderId || authStore.isAdmin
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
</script>

<template>
  <div class="video-list-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <div>
          <h1 class="page-title">
            <span class="title-icon">🎬</span>
            微课
          </h1>
          <p class="page-subtitle">探索编程微课程，随时随地学习技术</p>
        </div>
        <router-link v-if="isLoggedIn" to="/videos/upload" class="upload-btn" aria-label="上传视频">
          <Upload :size="16" aria-hidden="true" />
          <span>上传视频</span>
        </router-link>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="video-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card" aria-hidden="true">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-title"></div>
            <div class="skeleton-stats"></div>
            <div class="skeleton-uploader"></div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="videos.length === 0" class="empty-state">
        <VideoOff :size="48" aria-hidden="true" />
        <p class="empty-title">暂无视频</p>
        <p class="empty-subtitle">还没有任何微课视频，成为第一个上传者吧</p>
      </div>

      <!-- Video Grid -->
      <template v-else>
        <div class="video-grid stagger-children">
          <VideoCard
            v-for="video in videos"
            :key="video.id"
            :video="video"
            :editable="canEditVideo(video.uploaderId)"
            :deletable="canEditVideo(video.uploaderId)"
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
.video-list-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 0 80px;
}

.page-bg {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.bg-orb-1 {
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-1), transparent 70%);
}

.bg-orb-2 {
  bottom: -100px;
  left: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-2), transparent 70%);
}

.app-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 32px;
  gap: 16px;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  -webkit-text-fill-color: initial;
  font-size: 28px;
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 6px;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
  text-decoration: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.upload-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Video Grid */
.video-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 640px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .video-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1280px) {
  .video-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* Skeleton */
.skeleton-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
}

.skeleton-cover {
  aspect-ratio: 16 / 9;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-title {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-stats {
  height: 12px;
  width: 60%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.1s;
}

.skeleton-uploader {
  height: 12px;
  width: 40%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.2s;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--text-muted);
  gap: 12px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
}

.empty-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

/* Load More */
.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.load-more-btn:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.load-more-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme skeleton */
[data-theme="light"] .skeleton-cover,
[data-theme="light"] .skeleton-title,
[data-theme="light"] .skeleton-stats,
[data-theme="light"] .skeleton-uploader {
  background: linear-gradient(90deg, rgba(124, 58, 237, 0.05) 25%, rgba(124, 58, 237, 0.08) 50%, rgba(124, 58, 237, 0.05) 75%);
  background-size: 200% 100%;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .skeleton-cover,
  .skeleton-title,
  .skeleton-stats,
  .skeleton-uploader {
    animation: none;
  }
  .upload-btn:hover {
    transform: none;
  }
}
</style>
