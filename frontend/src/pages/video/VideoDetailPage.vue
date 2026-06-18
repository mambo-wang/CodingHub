<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Eye, Heart, MessageCircle, Bookmark, Loader2, Clock } from '@lucide/vue'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import VideoPlayer from '@/components/video/VideoPlayer.vue'
import VideoCommentList from '@/components/video/VideoCommentList.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import type { VideoDetail } from '@/types/video'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)

const video = ref<VideoDetail | null>(null)
const loading = ref(true)
const error = ref('')
const likeLoading = ref(false)
const favoriteLoading = ref(false)

const streamUrl = computed(() => {
  if (!video.value) return ''
  return videoService.getStreamUrl(video.value.id)
})

const uploaderUser = computed(() => {
  if (!video.value) return { id: 0, username: '', nickname: '', avatarUrl: null }
  return {
    id: video.value.uploaderId,
    username: video.value.uploaderName,
    nickname: video.value.uploaderNickname,
    avatarUrl: video.value.uploaderAvatarUrl
  }
})

const displayName = computed(() => {
  if (!video.value) return ''
  return video.value.uploaderNickname || video.value.uploaderName
})

const formatCount = (count: number): string => {
  if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}

const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const formatRelativeTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const months = Math.floor(days / 30)
  const years = Math.floor(days / 365)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (months < 12) return `${months}个月前`
  return `${years}年前`
}

onMounted(async () => {
  const videoId = Number(route.params.id)
  try {
    video.value = await videoService.getVideoDetail(videoId)
  } catch (e) {
    error.value = '视频加载失败，请稍后重试'
    console.error('Failed to load video:', e)
  } finally {
    loading.value = false
  }
})

const handleToggleLike = async () => {
  if (!video.value || likeLoading.value || !isLoggedIn.value) return
  likeLoading.value = true
  try {
    const result = await videoService.toggleLike(video.value.id)
    video.value.userLiked = result.liked
    video.value.likeCount = result.likeCount
  } catch (e) {
    console.error('Toggle like failed:', e)
  } finally {
    likeLoading.value = false
  }
}

const handleToggleFavorite = async () => {
  if (!video.value || favoriteLoading.value || !isLoggedIn.value) return
  favoriteLoading.value = true
  try {
    const result = await videoService.toggleFavorite(video.value.id)
    video.value.userFavorited = result.favorited
  } catch (e) {
    console.error('Toggle favorite failed:', e)
  } finally {
    favoriteLoading.value = false
  }
}

const handleCommentAdded = () => {
  if (video.value) {
    video.value.commentCount += 1
  }
}

const goBack = () => {
  router.push('/videos')
}
</script>

<template>
  <div class="video-detail-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <button class="back-btn" @click="goBack" aria-label="返回视频列表">
          <ArrowLeft :size="20" aria-hidden="true" />
        </button>
        <span class="back-label">返回微课列表</span>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="detail-loading">
        <Loader2 :size="28" class="spin" aria-hidden="true" />
        <span>加载视频中...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="detail-error" role="alert">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="$router.push('/videos')">返回列表</button>
      </div>

      <!-- Video Detail -->
      <div v-else-if="video" class="detail-content">
        <div class="player-section">
          <VideoPlayer :src="streamUrl" :title="video.title" />
        </div>

        <div class="info-section glass-card">
          <h1 class="video-title">{{ video.title }}</h1>

          <div class="video-meta">
            <div class="meta-author">
              <UserAvatar :user="uploaderUser" size="sm" :display-name="displayName" />
              <span class="author-name">{{ displayName }}</span>
            </div>
            <span class="meta-separator">·</span>
            <span class="meta-time">{{ formatRelativeTime(video.createdAt) }}</span>
            <span class="meta-separator">·</span>
            <span class="meta-duration">
              <Clock :size="14" aria-hidden="true" />
              {{ formatDuration(video.duration) }}
            </span>
          </div>

          <div class="video-stats">
            <span class="stat-item">
              <Eye :size="16" aria-hidden="true" />
              <span>{{ formatCount(video.viewCount) }} 次观看</span>
            </span>
            <span class="stat-item">
              <Heart :size="16" aria-hidden="true" />
              <span>{{ formatCount(video.likeCount) }} 次点赞</span>
            </span>
            <span class="stat-item">
              <MessageCircle :size="16" aria-hidden="true" />
              <span>{{ formatCount(video.commentCount) }} 条评论</span>
            </span>
          </div>

          <div class="video-actions">
            <button
              v-if="isLoggedIn"
              class="action-btn"
              :class="{ 'liked': video.userLiked }"
              :disabled="likeLoading"
              @click="handleToggleLike"
              :aria-label="video.userLiked ? '取消点赞' : '点赞'"
            >
              <Heart :size="18" :fill="video.userLiked ? 'currentColor' : 'none'" aria-hidden="true" />
              <span>{{ video.userLiked ? '已赞' : '点赞' }}</span>
            </button>
            <button
              v-if="isLoggedIn"
              class="action-btn"
              :class="{ 'favorited': video.userFavorited }"
              :disabled="favoriteLoading"
              @click="handleToggleFavorite"
              :aria-label="video.userFavorited ? '取消收藏' : '收藏'"
            >
              <Bookmark :size="18" :fill="video.userFavorited ? 'currentColor' : 'none'" aria-hidden="true" />
              <span>{{ video.userFavorited ? '已收藏' : '收藏' }}</span>
            </button>
          </div>

          <div v-if="video.description" class="video-description">
            <h3 class="desc-title">视频简介</h3>
            <p class="desc-text">{{ video.description }}</p>
          </div>
        </div>

        <div class="comment-section glass-card">
          <VideoCommentList :video-id="video.id" @comment-added="handleCommentAdded" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.video-detail-page {
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
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.back-btn:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--text-primary);
  border-color: var(--accent-1);
  transform: translateY(-1px);
}

.back-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.back-label {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Loading & Error */
.detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 24px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  color: var(--accent-2);
  font-size: 15px;
}

.detail-error {
  padding: 80px 24px;
  text-align: center;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 16px;
  color: #ef4444;
  font-size: 15px;
}

.retry-btn {
  margin-top: 16px;
  padding: 10px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.retry-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Detail Content */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.player-section {
  border-radius: 16px;
  overflow: hidden;
}

.info-section {
  padding: 28px;
  border-radius: 16px;
}

.video-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
  margin: 0 0 16px;
}

.video-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.meta-separator {
  color: var(--text-muted);
}

.meta-time {
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-duration {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-family: var(--font-mono);
  color: var(--text-secondary);
}

.video-stats {
  display: flex;
  gap: 20px;
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.video-actions {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: rgba(255, 255, 255, 0.15);
}

.action-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.liked {
  color: var(--accent-3);
  border-color: rgba(236, 72, 153, 0.3);
  background: rgba(236, 72, 153, 0.05);
}

.action-btn.liked:hover:not(:disabled) {
  background: rgba(236, 72, 153, 0.1);
  border-color: rgba(236, 72, 153, 0.5);
}

.action-btn.favorited {
  color: var(--accent-1);
  border-color: rgba(139, 92, 246, 0.3);
  background: rgba(139, 92, 246, 0.05);
}

.action-btn.favorited:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.5);
}

/* Description */
.video-description {
  margin-top: 8px;
}

.desc-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.desc-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Comment Section */
.comment-section {
  padding: 28px;
  border-radius: 16px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme action buttons */
[data-theme="light"] .action-btn:hover:not(:disabled) {
  border-color: rgba(0, 0, 0, 0.15);
}

[data-theme="light"] .action-btn.liked {
  color: var(--accent-3);
  border-color: rgba(219, 39, 119, 0.3);
}

[data-theme="light"] .action-btn.favorited {
  color: var(--accent-1);
  border-color: rgba(124, 58, 237, 0.3);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .back-btn,
  .action-btn,
  .retry-btn {
    transition: none;
  }
  .back-btn:hover {
    transform: none;
  }
  .spin {
    animation: none;
  }
}
</style>
