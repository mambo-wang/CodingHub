<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, FileText, Settings, Search, Pencil, Trash2 } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { useAuthStore } from '@/stores/auth'
import KnowledgeSearch from '@/components/knowledge/KnowledgeSearch.vue'
import DocumentList from '@/components/knowledge/DocumentList.vue'
import DocumentUpload from '@/components/knowledge/DocumentUpload.vue'
import ConfigPanel from '@/components/knowledge/ConfigPanel.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { ElMessage } from 'element-plus'
import type { KnowledgeBase } from '@/types/knowledge'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const kb = ref<KnowledgeBase | null>(null)
const loading = ref(true)
const error = ref('')
const activeTab = ref<'search' | 'documents' | 'config'>('search')
const deleteDialogVisible = ref(false)
const deleting = ref(false)
const docListRef = ref<InstanceType<typeof DocumentList> | null>(null)
const uploadRef = ref<InstanceType<typeof DocumentUpload> | null>(null)

const isOwner = computed(() => {
  if (!authStore.isLoggedIn || !kb.value) return false
  return authStore.user?.id === kb.value.ownerId || authStore.isAdmin
})

const loadKb = async () => {
  const id = Number(route.params.id)
  if (!id) {
    error.value = '无效的知识库 ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    kb.value = await knowledgeService.getDetail(id)
  } catch (e: any) {
    error.value = e.response?.data?.message || '加载知识库失败'
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  if (!kb.value) return
  deleting.value = true
  try {
    await knowledgeService.delete(kb.value.id)
    ElMessage.success('知识库已删除')
    router.push('/knowledge')
  } catch (e) {
    ElMessage.error('删除失败')
  } finally {
    deleting.value = false
  }
}

const handleDocumentUploaded = async () => {
  if (docListRef.value) {
    await docListRef.value.loadDocuments()
    docListRef.value.startPolling()
  }
}

/** Forward polled document statuses to DocumentUpload so file cards update */
const handleDocumentsRefreshed = (documents: any[]) => {
  if (uploadRef.value && documents.length > 0) {
    uploadRef.value.updateStatuses(documents)
  }
}

const formatRelativeTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days < 1) return '今天'
  if (days < 30) return `${days}天前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

onMounted(loadKb)
</script>

<template>
  <div class="knowledge-detail-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <!-- Back button -->
      <div class="page-header animate-fade-in-up">
        <button class="back-btn" @click="router.push('/knowledge')">
          <ArrowLeft :size="18" />
          <span class="back-label">返回知识库</span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="detail-loading">
        <Loader2 :size="32" class="spin" />
        <span>加载知识库...</span>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="detail-error">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="loadKb">重试</button>
      </div>

      <!-- Content -->
      <template v-else-if="kb">
        <!-- Title card -->
        <div class="title-card glass-card animate-fade-in-up">
          <div class="title-row">
            <h1 class="kb-title">{{ kb.name }}</h1>
            <div v-if="isOwner" class="title-actions">
              <button class="action-btn" @click="router.push(`/knowledge/${kb.id}/edit`)">
                <Pencil :size="15" />
                <span>编辑</span>
              </button>
              <button class="action-btn danger" @click="deleteDialogVisible = true">
                <Trash2 :size="15" />
                <span>删除</span>
              </button>
            </div>
          </div>
          <p v-if="kb.description" class="kb-desc">{{ kb.description }}</p>
          <div class="kb-meta">
            <span class="meta-item">
              创建者: {{ kb.ownerNickname || '未知' }}
            </span>
            <span class="meta-item">
              {{ formatRelativeTime(kb.createdAt) }}
            </span>
          </div>
        </div>

        <!-- Tabs -->
        <div class="tabs-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'search' }"
            @click="activeTab = 'search'"
          >
            <Search :size="16" />
            <span>语义搜索</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'documents' }"
            @click="activeTab = 'documents'"
          >
            <FileText :size="16" />
            <span>文档管理</span>
          </button>
          <button
            v-if="isOwner"
            class="tab-btn"
            :class="{ active: activeTab === 'config' }"
            @click="activeTab = 'config'"
          >
            <Settings :size="16" />
            <span>配置</span>
          </button>
        </div>

        <!-- Tab content -->
        <div class="tab-content glass-card animate-fade-in-up">
          <KnowledgeSearch v-if="activeTab === 'search'" :kb-id="kb.id" />

          <div v-else-if="activeTab === 'documents'" class="documents-tab">
            <DocumentUpload ref="uploadRef" :documents-url="kb.documentsUrl" @uploaded="handleDocumentUploaded" />
            <DocumentList ref="docListRef" :documents-url="kb.documentsUrl" :rag-base-url="kb.ragBaseUrl" :rag-collection="kb.ragCollection" :is-owner="isOwner" @refreshed="handleDocumentsRefreshed" />
          </div>

          <ConfigPanel v-else-if="activeTab === 'config'" :rag-base-url="kb.ragBaseUrl" :rag-collection="kb.ragCollection" :is-owner="isOwner" />
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
      @confirm="handleDelete"
      @cancel="deleteDialogVisible = false"
      @update:visible="deleteDialogVisible = $event"
    />
  </div>
</template>

<style scoped>
.knowledge-detail-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 24px 80px;
  max-width: 960px;
  margin: 0 auto;
}

.page-bg {
  position: fixed;
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
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  align-items: center;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px;
  color: var(--text-muted);
}

.detail-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px;
  color: var(--text-muted);
}

.retry-btn {
  padding: 8px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  border-color: var(--accent-1);
}

/* Title card */
.title-card {
  padding: 24px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.kb-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
}

.title-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.action-btn.danger:hover {
  border-color: #EF4444;
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

.kb-desc {
  margin: 12px 0 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.kb-meta {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

/* Tabs */
.tabs-bar {
  display: flex;
  gap: 4px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 4px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.tab-btn:hover {
  color: var(--text-secondary);
  background: rgba(139, 92, 246, 0.05);
}

.tab-btn.active {
  background: rgba(139, 92, 246, 0.12);
  color: var(--accent-1);
}

/* Tab content */
.tab-content {
  padding: 24px;
}

.documents-tab {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Light theme */
[data-theme="light"] .title-card,
[data-theme="light"] .tab-content,
[data-theme="light"] .tabs-bar {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .back-btn {
  background: var(--bg-card);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .knowledge-detail-page {
    padding: 16px 12px 60px;
  }

  .title-row {
    flex-direction: column;
  }

  .kb-title {
    font-size: 22px;
  }

  .tab-btn {
    padding: 8px 12px;
    font-size: 13px;
  }
}
</style>
