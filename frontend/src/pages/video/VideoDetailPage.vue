<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Eye, Heart, MessageCircle, Loader2, Clock, Pencil, Trash2, Send } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'
import VideoPlayer from '@/components/video/VideoPlayer.vue'
import DanmakuPlayer from '@/components/video/DanmakuPlayer.vue'
import UnifiedLikeButton from '@/components/common/UnifiedLikeButton.vue'
import UnifiedFavoriteButton from '@/components/common/UnifiedFavoriteButton.vue'
import UnifiedCommentSection from '@/components/common/UnifiedCommentSection.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import type { CommentResponse } from '@/services/interaction'
import type { VideoDetail } from '@/types/video'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const video = ref<VideoDetail | null>(null)
const loading = ref(true)
const error = ref('')

const canModify = computed(() => {
  if (!authStore.isLoggedIn || !video.value) return false
  return authStore.user?.id === video.value.uploaderId || authStore.isAdmin
})

const deleteDialogVisible = ref(false)
const deleting = ref(false)

// === Danmaku ===
const videoPlayerRef = ref<InstanceType<typeof VideoPlayer> | null>(null)
const danmakuInput = ref('')
const danmakuSending = ref(false)

const currentVideoTime = computed(() => {
  return videoPlayerRef.value?.currentTime ?? 0
})

const videoDuration = computed(() => {
  return videoPlayerRef.value?.duration ?? 0
})

const handleSendDanmaku = async () => {
  if (!authStore.isLoggedIn) {
    router.push(`/login?redirect=${route.fullPath}`)
    return
  }
  const content = danmakuInput.value.trim()
  if (!content || !video.value) return

  danmakuSending.value = true
  try {
    await api.post(`/videos/${video.value.id}/danmaku`, {
      content,
      timeSeconds: currentVideoTime.value
    })
    danmakuInput.value = ''
    ElMessage.success('弹幕已发送')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '发送弹幕失败')
  } finally {
    danmakuSending.value = false
  }
}

const handleEdit = () => {
  if (video.value) router.push(`/videos/${video.value.id}/edit`)
}

const handleDeleteClick = () => {
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (!video.value) return
  deleting.value = true
  try {
    await videoService.deleteVideo(video.value.id)
    router.push('/videos')
  } catch (e: any) {
    if (e.response?.status === 403) {
      // show error
    }
    console.error('Delete failed:', e)
  } finally {
    deleting.value = false
  }
}

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

const handleCommentAdded = (_comment: CommentResponse) => {
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
          <div class="player-wrapper">
            <VideoPlayer ref="videoPlayerRef" :src="streamUrl" :title="video.title" />
            <DanmakuPlayer
              v-if="video"
              :video-id="video.id"
              :current-time="currentVideoTime"
              :duration="videoDuration"
            />
          </div>
        </div>

        <!-- Danmaku Input Bar -->
        <div class="danmaku-bar glass-card">
          <template v-if="authStore.isLoggedIn">
            <input
              v-model="danmakuInput"
              type="text"
              class="danmaku-input"
              placeholder="发送弹幕..."
              maxlength="100"
              :disabled="danmakuSending"
              @keydown.enter="handleSendDanmaku"
            />
            <button
              class="danmaku-send-btn"
              :disabled="danmakuSending || !danmakuInput.trim()"
              @click="handleSendDanmaku"
            >
              <Send :size="14" />
              <span>发送弹幕</span>
            </button>
          </template>
          <template v-else>
            <div class="danmaku-login-prompt">
              <span>登录后即可发送弹幕</span>
              <button class="danmaku-login-btn" @click="router.push(`/login?redirect=${route.fullPath}`)">
                去登录
              </button>
            </div>
          </template>
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
            <UnifiedLikeButton
              target-type="VIDEO"
              :target-id="video.id"
              :initial-liked="video.userLiked"
              :initial-count="video.likeCount"
            />
            <UnifiedFavoriteButton
              target-type="VIDEO"
              :target-id="video.id"
              :initial-favorited="video.userFavorited"
            />
            <button v-if="canModify" class="action-btn" @click="handleEdit">
              <Pencil :size="16" />
              编辑
            </button>
            <button v-if="canModify" class="action-btn delete-action-btn" @click="handleDeleteClick">
              <Trash2 :size="16" />
              删除
            </button>
          </div>

          <div v-if="video.description" class="video-description">
            <h3 class="desc-title">视频简介</h3>
            <p class="desc-text">{{ video.description }}</p>
          </div>
        </div>

        <div class="comment-section glass-card">
          <UnifiedCommentSection
            target-type="VIDEO"
            :target-id="video.id"
            @comment-added="handleCommentAdded"
          />
        </div>
      </div>
    </div>

    <ConfirmDialog
      :visible="deleteDialogVisible"
      title="删除视频"
      description="确定要删除此视频吗？此操作不可恢复。"
      confirm-text="确认删除"
      :danger="true"
      :loading="deleting"
      @confirm="handleConfirmDelete"
      @cancel="deleteDialogVisible = false"
      @update:visible="deleteDialogVisible = $event"
    />
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

.player-wrapper {
  position: relative;
}

/* Danmaku Bar */
.danmaku-bar {
  padding: 12px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.danmaku-input {
  flex: 1;
  padding: 8px 14px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
}

.danmaku-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}

.danmaku-input::placeholder {
  color: var(--text-muted);
}

.danmaku-input:disabled {
  opacity: 0.5;
}

.danmaku-send-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border: none;
  border-radius: 8px;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.danmaku-send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-glow);
}

.danmaku-send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.danmaku-login-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  font-size: 14px;
  color: var(--text-secondary);
}

.danmaku-login-btn {
  padding: 6px 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(139, 92, 246, 0.4);
  border-radius: 6px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.danmaku-login-btn:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.35), rgba(6, 182, 212, 0.35));
  border-color: rgba(139, 92, 246, 0.6);
}

/* Light theme danmaku input */
[data-theme="light"] .danmaku-input {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.1);
}

[data-theme="light"] .danmaku-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
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

.delete-action-btn:hover:not(:disabled) {
  color: var(--color-destructive);
  border-color: color-mix(in srgb, var(--color-destructive) 30%, transparent);
  background: rgba(239, 68, 68, 0.05);
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
