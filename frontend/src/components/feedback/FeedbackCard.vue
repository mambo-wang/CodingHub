<script setup lang="ts">
import { ref } from 'vue'
import { Reply, Trash2, CheckCircle, Loader2 } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { feedbackService } from '@/services/feedback'
import type { FeedbackMessage, FeedbackCategory } from '@/types/feedback'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const props = defineProps<{
  feedback: FeedbackMessage
}>()

const emit = defineEmits<{ deleted: [id: number]; replied: [id: number] }>()

const authStore = useAuthStore()
const isAdmin = authStore.isAdmin

const showReplyInput = ref(false)
const replyText = ref('')
const replying = ref(false)
const showDeleteDialog = ref(false)
const deleting = ref(false)

const categoryLabels: Record<FeedbackCategory, string> = {
  SUGGESTION: '建议',
  BUG_REPORT: 'Bug',
  PRAISE: '表扬',
  OTHER: '其他'
}

const categoryClass = (cat: FeedbackCategory) => `category-badge category-${cat.toLowerCase()}`

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}

const handleReply = async () => {
  if (!replyText.value.trim()) return
  replying.value = true
  try {
    await feedbackService.replyFeedback(props.feedback.id, replyText.value.trim())
    showReplyInput.value = false
    replyText.value = ''
    emit('replied', props.feedback.id)
  } catch {
    // handled by api interceptor
  } finally {
    replying.value = false
  }
}

const handleDelete = async () => {
  deleting.value = true
  try {
    await feedbackService.deleteFeedback(props.feedback.id)
    emit('deleted', props.feedback.id)
  } catch {
    // handled by api interceptor
  } finally {
    deleting.value = false
    showDeleteDialog.value = false
  }
}
</script>

<template>
  <div class="feedback-card glass-card">
    <div class="card-header">
      <span :class="categoryClass(feedback.category)" class="category-badge">
        {{ categoryLabels[feedback.category] }}
      </span>
      <div class="card-meta">
        <span class="nickname">{{ feedback.nickname || '匿名用户' }}</span>
        <span class="time">{{ formatTime(feedback.createdAt) }}</span>
      </div>
      <div v-if="isAdmin" class="admin-actions">
        <button
          class="icon-btn"
          title="回复"
          @click="showReplyInput = !showReplyInput"
        >
          <Reply :size="15" aria-hidden="true" />
        </button>
        <button
          class="icon-btn icon-btn--danger"
          title="删除留言"
          aria-label="删除留言"
          @click="showDeleteDialog = true"
        >
          <Trash2 :size="15" aria-hidden="true" />
        </button>
      </div>
    </div>

    <div class="card-content">
      {{ feedback.content }}
    </div>

    <!-- Admin reply area -->
    <div v-if="feedback.adminReply" class="admin-reply-area">
      <div class="reply-header">
        <CheckCircle :size="14" class="reply-icon" aria-hidden="true" />
        <span>管理员回复</span>
      </div>
      <p class="reply-text">{{ feedback.adminReply }}</p>
    </div>

    <!-- Inline reply input -->
    <div v-if="showReplyInput" class="reply-input-area">
      <textarea
        v-model="replyText"
        placeholder="输入回复内容..."
        rows="2"
        :disabled="replying"
      />
      <div class="reply-actions">
        <button class="btn btn-sm btn-cancel" @click="showReplyInput = false">
          取消
        </button>
        <button
          class="btn btn-sm btn-primary"
          :disabled="!replyText.trim() || replying"
          @click="handleReply"
        >
          <Loader2 v-if="replying" :size="14" class="spin" aria-label="加载中" />
          {{ replying ? '发送中...' : '发送回复' }}
        </button>
      </div>
    </div>

    <Teleport to="body">
      <ConfirmDialog
        v-if="showDeleteDialog"
        v-model:visible="showDeleteDialog"
        title="确认删除"
        message="确定要删除这条留言吗？此操作不可撤销。"
        confirm-text="删除"
        :loading="deleting"
        @confirm="handleDelete"
      />
    </Teleport>
  </div>
</template>

<style scoped>
.feedback-card {
  padding: 20px 24px;
  border-radius: 16px;
  transition: all 0.25s ease;
}

.feedback-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.category-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.category-suggestion {
  background: rgba(139, 92, 246, 0.12);
  color: var(--accent-1);
  border: 1px solid rgba(139, 92, 246, 0.2);
}

.category-bug_report {
  background: rgba(239, 68, 68, 0.1);
  color: #EF4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
}

.category-praise {
  background: rgba(6, 182, 212, 0.12);
  color: var(--accent-2);
  border: 1px solid rgba(6, 182, 212, 0.2);
}

.category-other {
  background: var(--bg-glass);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.card-meta {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nickname {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.time {
  font-size: 12px;
  color: var(--text-muted);
}

.admin-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.feedback-card:hover .admin-actions {
  opacity: 1;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  background: var(--bg-glass);
  color: var(--text-primary);
}

.icon-btn--danger:hover {
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

.card-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.admin-reply-area {
  margin-top: 16px;
  background: rgba(139, 92, 246, 0.06);
  border-left: 3px solid var(--accent-1);
  padding: 12px 16px;
  border-radius: 0 8px 8px 0;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--accent-1);
  margin-bottom: 6px;
}

.reply-icon {
  color: var(--accent-1);
}

.reply-text {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  margin: 0;
}

.reply-input-area {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.reply-input-area textarea {
  width: 100%;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 14px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 13px;
  resize: vertical;
  outline: none;
  transition: all 0.2s ease;
}

.reply-input-area textarea:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-sm {
  padding: 6px 14px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-cancel {
  background: transparent;
  color: var(--text-secondary);
}

.btn-cancel:hover {
  background: var(--bg-glass);
  color: var(--text-primary);
}

.btn-primary {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border: none;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme */
[data-theme="light"] .admin-reply-area {
  background: rgba(124, 58, 237, 0.05);
}

[data-theme="light"] .reply-input-area textarea:focus {
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.15);
}

@media (max-width: 768px) {
  .feedback-card {
    padding: 16px;
  }
  .admin-actions {
    opacity: 1;
  }
}
</style>
