<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { FileText, Trash2, Loader2 } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import type { KbDocument } from '@/types/knowledge'

const props = defineProps<{
  kbId: number
  isOwner: boolean
}>()

const emit = defineEmits<{
  (e: 'refreshed'): void
}>()

const documents = ref<KbDocument[]>([])
const loading = ref(true)
const deletingId = ref<number | null>(null)

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatRelativeTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  return `${days}天前`
}

const loadDocuments = async () => {
  loading.value = true
  try {
    documents.value = await knowledgeService.getDocuments(props.kbId)
  } catch (e) {
    console.error('Failed to load documents:', e)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (docId: number) => {
  if (!confirm('确定要删除这个文档吗？')) return
  deletingId.value = docId
  try {
    await knowledgeService.deleteDocument(props.kbId, docId)
    documents.value = documents.value.filter(d => d.id !== docId)
    emit('refreshed')
  } catch (e) {
    console.error('Failed to delete document:', e)
  } finally {
    deletingId.value = null
  }
}

onMounted(loadDocuments)

defineExpose({ loadDocuments })
</script>

<template>
  <div class="document-list">
    <div v-if="loading" class="doc-loading">
      <Loader2 :size="24" class="spin" />
      <span>加载文档列表...</span>
    </div>

    <div v-else-if="documents.length === 0" class="doc-empty">
      <FileText :size="36" />
      <p>暂无文档，上传文件开始构建知识库</p>
    </div>

    <div v-else class="doc-items">
      <div v-for="doc in documents" :key="doc.id" class="doc-item glass-card">
        <div class="doc-icon">
          <FileText :size="20" />
        </div>
        <div class="doc-info">
          <span class="doc-name">{{ doc.originalName }}</span>
          <div class="doc-meta">
            <span>{{ formatFileSize(doc.fileSize) }}</span>
            <span v-if="doc.chunkCount != null">{{ doc.chunkCount }} 个分块</span>
            <span v-if="doc.uploaderNickname">{{ doc.uploaderNickname }}</span>
            <span>{{ formatRelativeTime(doc.createdAt) }}</span>
          </div>
        </div>
        <button
          v-if="isOwner"
          class="btn-delete"
          :disabled="deletingId === doc.id"
          @click="handleDelete(doc.id)"
          aria-label="删除文档"
        >
          <Loader2 v-if="deletingId === doc.id" :size="16" class="spin" />
          <Trash2 v-else :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.document-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.doc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.doc-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}

.doc-icon {
  flex-shrink: 0;
  color: var(--accent-1);
  display: flex;
  align-items: center;
}

.doc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.btn-delete {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-delete:hover {
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

[data-theme="light"] .doc-item {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}
</style>
