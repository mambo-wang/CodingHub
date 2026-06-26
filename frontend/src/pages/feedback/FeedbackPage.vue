<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { MessageSquareText, MessageSquareOff, Loader2 } from '@lucide/vue'
import { feedbackService } from '@/services/feedback'
import FeedbackForm from '@/components/feedback/FeedbackForm.vue'
import FeedbackCard from '@/components/feedback/FeedbackCard.vue'
import type { FeedbackMessage, FeedbackCategory } from '@/types/feedback'

const feedbacks = ref<FeedbackMessage[]>([])
const loading = ref(false)
const error = ref('')
const selectedCategory = ref<FeedbackCategory | ''>('')
const pagination = ref({ page: 0, size: 20, totalElements: 0, totalPages: 0 })

const categories: { value: FeedbackCategory | ''; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'SUGGESTION', label: '建议' },
  { value: 'BUG_REPORT', label: 'Bug' },
  { value: 'PRAISE', label: '表扬' },
  { value: 'OTHER', label: '其他' }
]

const fetchFeedbacks = async () => {
  loading.value = true
  error.value = ''
  try {
    const params: any = {
      page: pagination.value.page,
      size: pagination.value.size
    }
    if (selectedCategory.value) {
      params.category = selectedCategory.value
    }
    const data = await feedbackService.getFeedbacks(params)
    feedbacks.value = data.content
    pagination.value.totalElements = data.totalElements
    pagination.value.totalPages = data.totalPages
    pagination.value.page = data.page
  } catch (e: any) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const handleCategoryChange = (cat: FeedbackCategory | '') => {
  selectedCategory.value = cat
  pagination.value.page = 0
  fetchFeedbacks()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page
  fetchFeedbacks()
}

const onSubmitted = () => {
  pagination.value.page = 0
  fetchFeedbacks()
}

const onDeleted = () => {
  fetchFeedbacks()
}

const onReplied = () => {
  fetchFeedbacks()
}

onMounted(() => {
  fetchFeedbacks()
})
</script>

<template>
  <div class="feedback-page">
    <div class="page-container">
      <!-- Page header -->
      <div class="page-header">
        <div class="header-title">
          <MessageSquareText :size="28" class="header-icon" aria-hidden="true" />
          <h1>留言板</h1>
        </div>
        <p class="header-desc">有任何建议或想法？欢迎在这里留下你的反馈。</p>
      </div>

      <!-- Feedback form -->
      <FeedbackForm @submitted="onSubmitted" />

      <!-- Category filter chips -->
      <div class="filter-chips">
        <button
          v-for="cat in categories"
          :key="cat.value"
          :class="['chip', { active: selectedCategory === cat.value }]"
          @click="handleCategoryChange(cat.value)"
        >
          {{ cat.label }}
        </button>
      </div>

      <!-- Feedback list -->
      <div v-if="loading" class="loading-state">
        <Loader2 :size="24" class="spin" aria-label="加载中" />
        <span>加载中...</span>
      </div>

      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <button class="btn btn-primary btn-sm" @click="fetchFeedbacks">重试</button>
      </div>

      <div v-else-if="feedbacks.length === 0" class="empty-state">
        <MessageSquareOff :size="48" class="empty-icon" aria-hidden="true" />
        <p>还没有留言，来做第一个留言的人吧！</p>
      </div>

      <div v-else class="feedback-list">
        <TransitionGroup name="list">
          <FeedbackCard
            v-for="fb in feedbacks"
            :key="fb.id"
            :feedback="fb"
            @deleted="onDeleted"
            @replied="onReplied"
          />
        </TransitionGroup>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="pagination">
        <button
          class="page-btn"
          :disabled="pagination.page === 0"
          @click="handlePageChange(pagination.page - 1)"
        >
          上一页
        </button>
        <span class="page-info">
          {{ pagination.page + 1 }} / {{ pagination.totalPages }}
        </span>
        <button
          class="page-btn"
          :disabled="pagination.page >= pagination.totalPages - 1"
          @click="handlePageChange(pagination.page + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feedback-page {
  min-height: 100vh;
  padding: var(--space-2xl) var(--space-md);
}

.page-container {
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.header-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}

.header-icon {
  color: var(--accent-1);
}

.header-title h1 {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

/* Filter chips */
.filter-chips {
  display: flex;
  gap: 8px;
  margin: var(--space-lg) 0;
  flex-wrap: wrap;
}

.chip {
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: var(--bg-glass);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.chip:hover {
  border-color: var(--border-glow);
  color: var(--text-primary);
}

.chip.active {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border-color: transparent;
}

/* States */
.loading-state,
.error-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: var(--space-3xl) 0;
  color: var(--text-muted);
  font-size: 14px;
}

.empty-icon {
  color: var(--text-muted);
  opacity: 0.5;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Feedback list */
.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* List transition */
.list-enter-active {
  transition: all 0.3s ease;
}
.list-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: var(--space-xl);
  padding: var(--space-md) 0;
}

.page-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: var(--bg-glass);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--border-glow);
  color: var(--text-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.btn-sm {
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
}

@media (max-width: 768px) {
  .feedback-page {
    padding: var(--space-lg) var(--space-sm);
  }
  .header-title h1 {
    font-size: 22px;
  }
  .filter-chips {
    overflow-x: auto;
    flex-wrap: nowrap;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }
  .filter-chips::-webkit-scrollbar {
    display: none;
  }
  .chip {
    flex-shrink: 0;
  }
}
</style>
