<template>
  <div class="unified-comment-section">
    <div class="comment-header">
      <MessageCircle :size="20" />
      <h3>评论 ({{ totalElements }})</h3>
    </div>

    <!-- Comment editor -->
    <div class="comment-editor">
      <div class="editor-card">
        <div class="reply-indicator" v-if="replyingTo">
          回复 <strong>{{ replyingTo.userName || replyingTo.userNickname || '用户' }}</strong>
          <button class="cancel-reply" @click="cancelReply">&times;</button>
        </div>
        <textarea
          v-model="newComment"
          :placeholder="replyingTo ? '写下你的回复...' : '发表评论...'"
          class="comment-input"
          rows="3"
        ></textarea>
        <div class="editor-footer">
          <div class="anonymous-input" v-if="!authStore.isLoggedIn">
            <input
              v-model="anonymousName"
              placeholder="昵称（可选，默认匿名）"
              class="name-input"
            />
          </div>
          <button
            @click="submitComment"
            :disabled="!canSubmit || submitting"
            class="submit-btn"
          >
            <Send :size="14" />
            {{ submitting ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Comment list -->
    <div class="comment-list" role="feed" aria-label="评论列表">
      <div v-if="loading && comments.length === 0" class="loading-comments">
        加载中...
      </div>
      <template v-else>
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="comment-item"
          :class="{ 'is-reply': comment.parentId !== null }"
          :style="comment.parentId ? { marginLeft: '40px' } : {}"
        >
          <div class="comment-avatar">
            <img
              v-if="comment.userAvatarUrl"
              :src="comment.userAvatarUrl"
              :alt="comment.userNickname || '用户'"
              class="avatar-img"
            />
            <span v-else class="avatar-fallback">
              {{ (comment.userNickname || comment.userName || '匿名')[0] }}
            </span>
          </div>
          <div class="comment-body">
            <div class="comment-meta">
              <span class="author">{{ comment.userNickname || comment.userName || '匿名' }}</span>
              <span class="date">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-actions">
              <button class="reply-btn" @click="startReply(comment)">
                <Reply :size="14" />
                回复
              </button>
              <button
                v-if="canDelete(comment)"
                class="delete-btn"
                @click="handleDelete(comment.id)"
              >
                删除
              </button>
            </div>
          </div>
        </div>

        <div v-if="comments.length === 0 && !loading" class="empty-comments">
          暂无评论，快来抢沙发吧
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="comment-pagination">
          <button
            :disabled="currentPage === 0"
            @click="loadPage(currentPage - 1)"
            class="page-btn"
          >上一页</button>
          <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            :disabled="currentPage >= totalPages - 1"
            @click="loadPage(currentPage + 1)"
            class="page-btn"
          >下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { MessageCircle, Send, Reply } from '@lucide/vue'
import { useInteraction } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import type { TargetType, CommentResponse } from '@/services/interaction'

const props = defineProps<{
  targetType: TargetType
  targetId: number
}>()

const emit = defineEmits<{
  (e: 'comment-added', comment: CommentResponse): void
  (e: 'comment-deleted', commentId: number): void
}>()

const authStore = useAuthStore()

const {
  comments,
  commentsTotalElements: totalElements,
  commentsTotalPages: totalPages,
  commentsPage: currentPage,
  commentsLoading: loading,
  commentSubmitting: submitting,
  loadComments,
  addComment,
  deleteComment,
} = useInteraction(props.targetType, props.targetId)

const newComment = ref('')
const anonymousName = ref('')
const replyingTo = ref<CommentResponse | null>(null)

const canSubmit = computed(() => newComment.value.trim().length > 0)

onMounted(() => {
  loadComments(0)
})

const startReply = (comment: CommentResponse) => {
  replyingTo.value = comment
}

const cancelReply = () => {
  replyingTo.value = null
}

const submitComment = async () => {
  if (!canSubmit.value || submitting.value) return

  try {
    const parentId = replyingTo.value?.id
    const userName = !authStore.isLoggedIn ? (anonymousName.value.trim() || '匿名') : undefined
    const comment = await addComment(newComment.value.trim(), parentId, userName)
    newComment.value = ''
    replyingTo.value = null
    emit('comment-added', comment)
  } catch (e) {
    console.error('Failed to submit comment:', e)
  }
}

const handleDelete = async (commentId: number) => {
  if (!confirm('确定删除此评论？')) return
  try {
    await deleteComment(commentId)
    emit('comment-deleted', commentId)
  } catch (e) {
    console.error('Failed to delete comment:', e)
  }
}

const canDelete = (comment: CommentResponse) => {
  if (!authStore.user) return false
  const isOwner = comment.userId === authStore.user?.id
  const isAdmin = authStore.isAdmin
  return isOwner || isAdmin
}

const loadPage = (page: number) => {
  loadComments(page)
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.unified-comment-section {
  margin-top: 24px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: var(--accent-1);
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* Editor */
.comment-editor {
  margin-bottom: 24px;
}

.editor-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
}

.reply-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: rgba(139, 92, 246, 0.1);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.cancel-reply {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 18px;
  padding: 0 4px;
  margin-left: auto;
}

.comment-input {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  resize: vertical;
  font-family: inherit;
  min-height: 80px;
  outline: none;
  transition: border-color 0.2s;
}

.comment-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

.comment-input::placeholder {
  color: var(--text-muted);
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.anonymous-input {
  flex: 1;
  margin-right: 12px;
}

.name-input {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
  width: 200px;
}

.name-input:focus {
  border-color: var(--accent-1);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--accent-2), var(--accent-1));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.submit-btn:disabled {
  background: var(--text-muted);
  cursor: not-allowed;
}

/* Comment list */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  transition: all 0.2s;
}

.comment-item.is-reply {
  border-left: 2px solid rgba(139, 92, 246, 0.3);
}

.comment-item:hover {
  border-color: var(--border-glow);
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  font-size: 14px;
  font-weight: 600;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.author {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.date {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.reply-btn, .delete-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
  transition: color 0.2s;
}

.reply-btn:hover {
  color: var(--accent-1);
}

.delete-btn:hover {
  color: var(--color-destructive, #ef4444);
}

.empty-comments, .loading-comments {
  text-align: center;
  padding: 32px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

/* Pagination */
.comment-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 0;
}

.page-btn {
  padding: 8px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent-1);
  color: var(--accent-1);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
}

/* Light theme */
[data-theme="light"] .comment-item.is-reply {
  border-left-color: rgba(124, 58, 237, 0.2);
}

[data-theme="light"] .comment-input:focus {
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.15);
}
</style>
