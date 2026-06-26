<script setup lang="ts">
import { ref, computed } from 'vue'
import { Send, Loader2 } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { feedbackService } from '@/services/feedback'
import type { FeedbackCategory } from '@/types/feedback'

const authStore = useAuthStore()
const emit = defineEmits<{ submitted: [] }>()

const content = ref('')
const nickname = ref('')
const contact = ref('')
const category = ref<FeedbackCategory>('SUGGESTION')
const submitting = ref(false)
const error = ref('')
const success = ref(false)

const categories: { value: FeedbackCategory; label: string }[] = [
  { value: 'SUGGESTION', label: '建议' },
  { value: 'BUG_REPORT', label: 'Bug 反馈' },
  { value: 'PRAISE', label: '表扬' },
  { value: 'OTHER', label: '其他' }
]

const isAnonymous = computed(() => !authStore.isLoggedIn)

const resetForm = () => {
  content.value = ''
  nickname.value = ''
  contact.value = ''
  category.value = 'SUGGESTION'
  error.value = ''
}

const handleSubmit = async () => {
  if (!content.value.trim()) {
    error.value = '请输入留言内容'
    return
  }
  submitting.value = true
  error.value = ''
  success.value = false

  try {
    await feedbackService.createFeedback({
      content: content.value.trim(),
      nickname: nickname.value.trim() || undefined,
      contact: contact.value.trim() || undefined,
      category: category.value
    })
    success.value = true
    resetForm()
    setTimeout(() => { success.value = false }, 3000)
    emit('submitted')
  } catch (e: any) {
    error.value = e.response?.data?.message || '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="feedback-form" @submit.prevent="handleSubmit">
    <div v-if="success" class="form-toast" role="alert">
      留言提交成功，感谢你的反馈！
    </div>

    <div class="form-group">
      <label for="feedback-content">留言内容 *</label>
      <textarea
        id="feedback-content"
        v-model="content"
        placeholder="写下你的建议、Bug 反馈或想法..."
        rows="4"
        :class="{ 'has-error': error }"
        :disabled="submitting"
        required
      />
    </div>

    <div class="form-row">
      <div v-if="isAnonymous" class="form-group">
        <label for="feedback-nickname">昵称</label>
        <input
          id="feedback-nickname"
          v-model="nickname"
          type="text"
          placeholder="匿名路人"
          maxlength="50"
          :disabled="submitting"
        />
      </div>
      <div class="form-group">
        <label for="feedback-contact">联系方式</label>
        <input
          id="feedback-contact"
          v-model="contact"
          type="text"
          placeholder="邮箱或微信（可选）"
          maxlength="100"
          :disabled="submitting"
        />
      </div>
      <div class="form-group form-group--category">
        <label for="feedback-category">分类</label>
        <select
          id="feedback-category"
          v-model="category"
          :disabled="submitting"
        >
          <option v-for="cat in categories" :key="cat.value" :value="cat.value">
            {{ cat.label }}
          </option>
        </select>
      </div>
    </div>

    <div v-if="error" class="form-error" role="alert">{{ error }}</div>

    <button
      type="submit"
      class="btn btn-primary submit-btn"
      :disabled="submitting || !content.trim()"
    >
      <Loader2 v-if="submitting" :size="16" class="spin" aria-label="加载中" />
      <Send v-else :size="16" aria-hidden="true" />
      <span>{{ submitting ? '提交中...' : '提交留言' }}</span>
    </button>
  </form>
</template>

<style scoped>
.feedback-form {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  backdrop-filter: blur(20px);
  transition: all 0.25s ease;
}

.form-toast {
  background: rgba(6, 182, 212, 0.1);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: var(--accent-2);
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  animation: fadeIn 0.3s ease;
}

.form-group {
  margin-bottom: 16px;
  flex: 1;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.form-group--category {
  max-width: 160px;
}

textarea,
input,
select {
  width: 100%;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 14px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  transition: all 0.2s ease;
  outline: none;
}

textarea {
  resize: vertical;
  min-height: 100px;
}

textarea:hover,
input:hover,
select:hover {
  border-color: rgba(255, 255, 255, 0.15);
}

textarea:focus,
input:focus,
select:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

textarea.has-error {
  border-color: #EF4444;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%23a1a1aa' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.form-error {
  color: #EF4444;
  font-size: 13px;
  margin-bottom: 12px;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.submit-btn:disabled {
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

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Light theme */
[data-theme="light"] textarea:hover,
[data-theme="light"] input:hover,
[data-theme="light"] select:hover {
  border-color: rgba(0, 0, 0, 0.15);
}

[data-theme="light"] textarea:focus,
[data-theme="light"] input:focus,
[data-theme="light"] select:focus {
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.15);
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
  .form-group--category {
    max-width: 100%;
  }
}
</style>
