<script setup lang="ts">
import { ref, computed } from 'vue'
import { Eye, Heart, MessageCircle, Play, Pencil, Trash2, ArrowUp, Flame, Pin, PinOff } from '@lucide/vue'
import UserAvatar from '@/components/UserAvatar.vue'
import type { VideoListItem } from '@/types/video'
import { useAuthStore } from '@/stores/auth'
import { videoService } from '@/services/video'

const props = withDefaults(defineProps<{
  video: VideoListItem
  editable?: boolean
  deletable?: boolean
  isHot?: boolean
}>(), {
  editable: false,
  deletable: false,
  isHot: false,
})
const emit = defineEmits<{
  (e: 'edit', videoId: number): void
  (e: 'delete', videoId: number): void
  (e: 'pin-changed'): void
}>()

const authStore = useAuthStore()
const pinLoading = ref(false)

const canPin = computed(() => authStore.isAdmin)

const handlePin = async () => {
  if (pinLoading.value) return
  pinLoading.value = true
  try {
    if (props.video.pinned) {
      await videoService.unpinVideo(props.video.id)
    } else {
      await videoService.pinVideo(props.video.id)
    }
    props.video.pinned = !props.video.pinned
    emit('pin-changed')
  } catch (error) {
    console.error('Pin/unpin failed:', error)
  } finally {
    pinLoading.value = false
  }
}

const formatDuration = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const formatCount = (count: number): string => {
  if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
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

const uploaderUser = computed(() => ({
  id: props.video.uploaderId,
  username: props.video.uploaderName,
  nickname: props.video.uploaderNickname,
  avatarUrl: props.video.uploaderAvatarUrl
}))

const displayName = computed(() => props.video.uploaderNickname || props.video.uploaderName)
</script>

<template>
  <router-link :to="`/videos/${video.id}`" class="video-card-link" :aria-label="`观看视频: ${video.title}`">
    <div class="video-card">
      <div class="video-cover">
        <img
          v-if="video.coverUrl"
          :src="video.coverUrl"
          :alt="video.title"
          class="cover-img"
          loading="lazy"
        />
        <div v-else class="cover-placeholder">
          <Play :size="40" aria-hidden="true" />
        </div>
        <span class="duration-badge">{{ formatDuration(video.duration) }}</span>
        <div class="card-badges">
          <span v-if="video.pinned" class="badge-pill badge-pinned">
            <ArrowUp :size="12" aria-hidden="true" />
            <span>置顶</span>
          </span>
          <span v-if="isHot" class="badge-pill badge-hot">
            <Flame :size="12" aria-hidden="true" />
            <span>热门</span>
          </span>
        </div>
      </div>

      <div class="card-actions" v-if="editable || deletable || canPin">
        <button
          v-if="editable"
          class="btn-icon-edit"
          aria-label="编辑视频"
          @click.stop.prevent="emit('edit', video.id)"
          @pointerdown.stop
        >
          <Pencil :size="16" />
        </button>
        <button
          v-if="deletable"
          class="btn-icon-delete"
          aria-label="删除视频"
          @click.stop.prevent="emit('delete', video.id)"
          @pointerdown.stop
        >
          <Trash2 :size="16" />
        </button>
        <button
          v-if="canPin"
          class="btn-icon-pin"
          :aria-label="video.pinned ? '取消置顶' : '置顶'"
          :disabled="pinLoading"
          @click.stop.prevent="handlePin"
          @pointerdown.stop
        >
          <PinOff v-if="video.pinned" :size="14" />
          <Pin v-else :size="14" />
        </button>
      </div>

      <div class="video-info">
        <h3 class="video-title">{{ video.title }}</h3>

        <div class="video-stats">
          <span class="stat-item">
            <Eye :size="14" aria-hidden="true" />
            <span>{{ formatCount(video.viewCount) }}</span>
          </span>
          <span class="stat-item">
            <Heart :size="14" aria-hidden="true" />
            <span>{{ formatCount(video.likeCount) }}</span>
          </span>
          <span class="stat-item">
            <MessageCircle :size="14" aria-hidden="true" />
            <span>{{ formatCount(video.commentCount) }}</span>
          </span>
        </div>

        <div class="video-uploader">
          <UserAvatar :user="uploaderUser" size="sm" :display-name="displayName" />
          <span class="uploader-name">{{ displayName }}</span>
          <span class="upload-time">{{ formatRelativeTime(video.createdAt) }}</span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.video-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.video-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.2s ease;
  cursor: pointer;
  position: relative;
}

.video-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.video-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--bg-secondary);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.2s ease;
}

.video-card:hover .cover-img {
  transform: scale(1.03);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  color: var(--text-muted);
}

.duration-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 4px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: #fff;
  letter-spacing: 0.5px;
}

.video-info {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.video-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-stats {
  display: flex;
  gap: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--text-muted);
}

.stat-item svg {
  opacity: 0.7;
}

.video-uploader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-name {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.upload-time {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
  z-index: 2;
  opacity: 0.35;
  transition: opacity 200ms ease;
}

.video-card:hover .card-actions {
  opacity: 1;
}

.btn-icon-edit,
.btn-icon-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1.5px solid rgba(255,255,255,0.3);
  background: rgba(0,0,0,0.5);
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  transition: all 200ms ease;
}

.btn-icon-edit:hover {
  color: #8B5CF6;
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.15);
}

.btn-icon-delete:hover {
  color: #EF4444;
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.15);
}

/* Light theme */
[data-theme="light"] .video-card {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .video-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .video-card,
  .cover-img {
    transition: none;
  }
  .video-card:hover {
    transform: none;
  }
  .video-card:hover .cover-img {
    transform: none;
  }
  .badge-pill:hover {
    transform: none;
  }
}

.card-badges {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  gap: 6px;
  z-index: 2;
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px 3px 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;
}

.badge-pill:hover {
  transform: translateY(-1px);
}

.badge-pinned {
  background: rgba(139, 92, 246, 0.25);
  color: #c4b5fd;
  border: 1px solid rgba(139, 92, 246, 0.3);
}

.badge-pinned:hover {
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.35);
}

.badge-hot {
  background: rgba(245, 158, 11, 0.25);
  color: #fde68a;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.badge-hot:hover {
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35);
}

[data-theme="light"] .badge-pinned {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
  border-color: rgba(124, 58, 237, 0.18);
}

[data-theme="light"] .badge-hot {
  background: rgba(217, 119, 6, 0.1);
  color: #b45309;
  border-color: rgba(217, 119, 6, 0.18);
}

.btn-icon-pin {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1.5px solid rgba(255,255,255,0.3);
  background: rgba(0,0,0,0.5);
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  transition: all 200ms ease;
}

.btn-icon-pin:hover {
  color: #8B5CF6;
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.15);
}

.btn-icon-pin:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon-pin:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}
</style>
