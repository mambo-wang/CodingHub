<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Loader2, MessageCircle } from '@lucide/vue'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import type { VideoComment } from '@/types/video'

const props = defineProps<{
  videoId: number
}>()

const emit = defineEmits<{
  (e: 'commentAdded'): void
}>()

const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const comments = ref<VideoComment[]>([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const pageSize = 20
const hasMore = computed(() => page.value < totalPages.value - 1)
const loadingMore = ref(false)

const commentText = ref('')
const submitting = ref(false)
const error = ref('')

onMounted(async () => {
  await loadComments()
})

const loadComments = async () => {
  loading.value = true
  try {
    const data = await videoService.getComments(props.videoId, 0, pageSize)
    comments.value = data.content
    totalPages.value = data.totalPages
    page.value = data.page
  } catch (e) {
    console.error('Failed to load comments:', e)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = page.value + 1
    const data = await videoService.getComments(props.videoId, nextPage, pageSize)
    comments.value = [...comments.value, ...data.content]
    page.value = data.page
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more comments:', e)
  } finally {
    loadingMore.value = false
  }
}

const handleSubmit = async () => {
  if (!commentText.value.trim() || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    const newComment = await videoService.addComment(props.videoId, commentText.value.trim())
    comments.value = [newComment, ...comments.value]
    commentText.value = ''
    emit('commentAdded')
  } catch (e: any) {
    error.value = e?.response?.data?.message || '评论发送失败，请重试'
  } finally {
    submitting.value = false
  }
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

const commenterUser = (comment: VideoComment) => ({
  id: comment.userId,
  username: `user-${comment.userId}`,
  nickname: comment.userNickname,
  avatarUrl: comment.userAvatarUrl
})
</script>

<template>
  <div class="comment-list">
    <h3 class="comment-section-title">
      <MessageCircle :size="18" aria-hidden="true" />
      <span>评论</span>
    </h3>

    <!-- Comment Input (only if logged in) -->
    <div v-if="isLoggedIn" class="comment-form">
      <textarea
        v-model="commentText"
        class="comment-input"
        placeholder="写下你的评论..."
        rows="3"
        maxlength="1000"
        aria-label="评论内容"
        @keydown.ctrl.enter="handleSubmit"
        @keydown.meta.enter="handleSubmit"
      ></textarea>
      <div class="comment-form-footer">
        <span v-if="error" class="form-error" role="alert">{{ error }}</span>
        <span class="char-count">{{ commentText.length }} / 1000</span>
        <button
          class="submit-btn"
          :disabled="!commentText.trim() || submitting"
          @click="handleSubmit"
        >
          <Loader2 v-if="submitting" :size="14" class="spin" aria-hidden="true" />
          <span>{{ submitting ? '发送中...' : '发送' }}</span>
        </button>
      </div>
    </div>
    <div v-else class="login-hint">
      请先<span class="login-link" @click="$router.push(`/login?redirect=${$route.fullPath}`)">登录</span>后发表评论
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="comment-loading">
      <Loader2 :size="20" class="spin" aria-hidden="true" />
      <span>加载评论中...</span>
    </div>

    <!-- Empty State -->
    <div v-else-if="comments.length === 0" class="comment-empty">
      <p>暂无评论，来发表第一条评论吧</p>
    </div>

    <!-- Comment List -->
    <template v-else>
      <div class="comments">
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <UserAvatar :user="commenterUser(comment)" size="sm" :display-name="comment.userNickname" />
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-author">{{ comment.userNickname }}</span>
              <span class="comment-time">{{ formatRelativeTime(comment.createdAt) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="load-more-wrap">
        <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
          <Loader2 v-if="loadingMore" :size="14" class="spin" aria-hidden="true" />
          <span>{{ loadingMore ? '加载中...' : '加载更多' }}</span>
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.comment-list {
  margin-top: 32px;
}

.comment-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

/* Comment Form */
.comment-form {
  margin-bottom: 24px;
}

.comment-input {
  width: 100%;
  padding: 12px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: all 0.2s ease;
}

.comment-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

.comment-input::placeholder {
  color: var(--text-muted);
}

.comment-form-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  justify-content: flex-end;
}

.form-error {
  font-size: 13px;
  color: #ef4444;
  margin-right: auto;
}

.char-count {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.login-hint {
  padding: 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  margin-bottom: 24px;
}

.login-link {
  color: var(--accent-1);
  cursor: pointer;
  font-weight: 500;
  text-decoration: underline;
}

.login-link:hover {
  color: var(--accent-2);
}

/* Loading & Empty */
.comment-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px;
  color: var(--text-muted);
  font-size: 14px;
}

.comment-empty {
  padding: 32px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

/* Comment Items */
.comments {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.comment-item:hover {
  border-color: var(--border-glow);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-author {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-time {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-content {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-word;
}

/* Load More */
.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
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

/* Light theme */
[data-theme="light"] .comment-input {
  background: var(--bg-card);
}

[data-theme="light"] .comment-input:focus {
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .comment-input,
  .comment-item,
  .submit-btn,
  .load-more-btn {
    transition: none;
  }
  .spin {
    animation: none;
  }
  .submit-btn:hover {
    transform: none;
  }
}
</style>
