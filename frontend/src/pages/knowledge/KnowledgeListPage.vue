<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, BookOpen, Loader2, LayoutGrid, BookOpenText } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { useAuthStore } from '@/stores/auth'
import KnowledgeCard from '@/components/knowledge/KnowledgeCard.vue'
import SortTab from '@/components/common/SortTab.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue'
import { ElMessage } from 'element-plus'
import type { KnowledgeBase } from '@/types/knowledge'

const authStore = useAuthStore()
const router = useRouter()
const route = useRoute()
const isLoggedIn = computed(() => authStore.isLoggedIn)

const isMyPage = computed(() => route.path === '/knowledge/my')

const sidebarItems: SidebarNavItem[] = [
  { label: '全部知识库', icon: LayoutGrid, to: '/knowledge' },
  { label: '我的知识库', icon: BookOpenText, to: '/knowledge/my', requiresAuth: true },
]

const pageTitle = computed(() => isMyPage.value ? '我的知识库' : '知识库')
const pageSubtitle = computed(() => isMyPage.value
  ? '你创建的知识库都在这里'
  : '探索与管理知识库，语义搜索文档内容')

const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const pageSize = 12
const sortBy = ref('latest')

const hasMore = computed(() => page.value < totalPages.value - 1)
const loadingMore = ref(false)
const deleteDialogVisible = ref(false)
const deleteTargetId = ref<number | null>(null)
const deleting = ref(false)

onMounted(async () => {
  await loadList()
})

// Reload when switching between /knowledge and /knowledge/my
watch(isMyPage, () => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    const ownerId = isMyPage.value ? authStore.user?.id : undefined
    const data = await knowledgeService.getList(0, pageSize, sortBy.value, ownerId)
    knowledgeBases.value = data.content
    totalPages.value = data.totalPages
    page.value = data.page
  } catch (e) {
    console.error('Failed to load knowledge bases:', e)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  try {
    const nextPage = page.value + 1
    const ownerId = isMyPage.value ? authStore.user?.id : undefined
    const data = await knowledgeService.getList(nextPage, pageSize, sortBy.value, ownerId)
    knowledgeBases.value = [...knowledgeBases.value, ...data.content]
    page.value = data.page
    totalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more:', e)
  } finally {
    loadingMore.value = false
  }
}

const handleSortChange = async (value: string) => {
  sortBy.value = value
  knowledgeBases.value = []
  await loadList()
}

const canEditKb = (ownerId: number) => {
  if (!authStore.isLoggedIn) return false
  return authStore.user?.id === ownerId || authStore.isAdmin
}

const handleEdit = (kbId: number) => {
  router.push(`/knowledge/${kbId}/edit`)
}

const handleDelete = (kbId: number) => {
  deleteTargetId.value = kbId
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (deleteTargetId.value === null) return
  deleting.value = true
  try {
    await knowledgeService.delete(deleteTargetId.value)
    knowledgeBases.value = knowledgeBases.value.filter(k => k.id !== deleteTargetId.value)
    ElMessage.success('知识库已删除')
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
  <div class="knowledge-list-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <GeneralizedSidebar :items="sidebarItems" />

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <div>
          <h1 class="page-title">
            <span class="title-icon">📚</span>
            {{ pageTitle }}
          </h1>
          <p class="page-subtitle">{{ pageSubtitle }}</p>
        </div>
        <router-link v-if="isLoggedIn" to="/knowledge/create" class="create-btn" aria-label="创建知识库">
          <Plus :size="16" aria-hidden="true" />
          <span>创建知识库</span>
        </router-link>
      </div>

      <div class="sort-row">
        <SortTab v-model="sortBy" @update:modelValue="handleSortChange" />
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="kb-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card" aria-hidden="true">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-title"></div>
            <div class="skeleton-desc"></div>
            <div class="skeleton-stats"></div>
            <div class="skeleton-uploader"></div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="knowledgeBases.length === 0" class="empty-state">
        <BookOpen :size="48" aria-hidden="true" />
        <p class="empty-title">{{ isMyPage ? '你还没有创建任何知识库' : '暂无知识库' }}</p>
        <p class="empty-subtitle">{{ isMyPage ? '点击右上角按钮创建你的第一个知识库吧' : '还没有任何知识库，成为第一个创建者吧' }}</p>
      </div>

      <!-- Knowledge Grid -->
      <template v-else>
        <div class="kb-grid stagger-children">
          <KnowledgeCard
            v-for="kb in knowledgeBases"
            :key="kb.id"
            :kb="kb"
            :editable="canEditKb(kb.ownerId)"
            :deletable="canEditKb(kb.ownerId)"
            @edit="handleEdit"
            @delete="handleDelete"
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
      title="删除知识库"
      description="确定要删除这个知识库吗？所有文档和配置都将被清除，此操作不可撤销。"
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
.knowledge-list-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 24px 80px;
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
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
  flex: 1;
  min-width: 0;
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

.create-btn {
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

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.create-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.sort-row {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

/* KB Grid */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
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
  aspect-ratio: 16 / 7;
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
  width: 70%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-desc {
  height: 12px;
  width: 90%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.1s;
}

.skeleton-stats {
  height: 12px;
  width: 40%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.15s;
}

.skeleton-uploader {
  height: 12px;
  width: 50%;
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
[data-theme="light"] .skeleton-desc,
[data-theme="light"] .skeleton-stats,
[data-theme="light"] .skeleton-uploader {
  background: linear-gradient(90deg, rgba(124, 58, 237, 0.05) 25%, rgba(124, 58, 237, 0.08) 50%, rgba(124, 58, 237, 0.05) 75%);
  background-size: 200% 100%;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .skeleton-cover,
  .skeleton-title,
  .skeleton-desc,
  .skeleton-stats,
  .skeleton-uploader {
    animation: none;
  }
  .create-btn:hover {
    transform: none;
  }
}
</style>
