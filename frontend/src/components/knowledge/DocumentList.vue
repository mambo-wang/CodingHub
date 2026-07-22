<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { FileText, Trash2, Loader2, Download } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import StatusBadge from './StatusBadge.vue'
import StrategyBadge from './StrategyBadge.vue'
import type { RagDocumentStatus } from '@/types/knowledge'

const props = defineProps<{
  documentsUrl: string
  ragBaseUrl: string
  ragCollection: string
  isOwner: boolean
}>()

const emit = defineEmits<{
  (e: 'refreshed', documents: RagDocumentStatus[]): void
}>()

const documents = ref<RagDocumentStatus[]>([])
const loading = ref(true)
const deletingSource = ref<string | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const hasProcessing = computed(() =>
  documents.value.some(d =>
    ['UPLOADING', 'CONVERTING', 'CHUNKING', 'EMBEDDING'].includes(d.status)
  )
)

const loadDocuments = async () => {
  try {
    documents.value = await knowledgeService.getDocumentStatus(props.ragBaseUrl, props.ragCollection)
  } catch (e) {
    console.error('Failed to load documents:', e)
  } finally {
    loading.value = false
  }
}

const startPolling = () => {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const updated = await knowledgeService.getDocumentStatus(props.ragBaseUrl, props.ragCollection)
      documents.value = updated
      // Stop polling if all documents are in terminal state
      if (!updated.some(d => ['UPLOADING', 'CONVERTING', 'CHUNKING', 'EMBEDDING'].includes(d.status))) {
        stopPolling()
        emit('refreshed', updated)
      }
    } catch (e) {
      console.error('Polling failed:', e)
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleDelete = async (filepath: string) => {
  if (!confirm('确定要删除这个文档吗？')) return
  deletingSource.value = filepath
  try {
    await knowledgeService.deleteDocument(props.documentsUrl, filepath)
    documents.value = documents.value.filter(d => d.filepath !== filepath)
    emit('refreshed', documents.value)
  } catch (e) {
    console.error('Failed to delete document:', e)
  } finally {
    deletingSource.value = null
  }
}

const downloadingFile = ref<string | null>(null)

const handleDownload = async (filepath: string, filename: string) => {
  downloadingFile.value = filepath
  try {
    const url = `${props.ragBaseUrl}/api/collections/${encodeURIComponent(props.ragCollection)}/documents/download?filepath=${encodeURIComponent(filepath)}`
    // Use fetch to check if the file exists (handles 404)
    const response = await fetch(url)
    if (!response.ok) {
      if (response.status === 404) {
        alert('源文件不可用，请重新上传')
      } else {
        alert('下载失败，请稍后重试')
      }
      return
    }
    // Trigger browser download via blob
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('Download failed:', e)
    alert('下载失败，请检查网络连接')
  } finally {
    downloadingFile.value = null
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(async () => {
  await loadDocuments()
  if (hasProcessing.value) {
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})

defineExpose({ loadDocuments, startPolling })
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
      <div v-for="doc in documents" :key="doc.id" class="doc-item glass-card" :class="{ processing: ['UPLOADING','CONVERTING','CHUNKING','EMBEDDING'].includes(doc.status) }">
        <div class="doc-icon">
          <FileText :size="20" />
        </div>
        <div class="doc-info">
          <span class="doc-name" :title="doc.filename">{{ doc.filename }}</span>
          <div class="doc-meta">
            <span v-if="doc.file_size">{{ formatFileSize(doc.file_size) }}</span>
            <span v-if="doc.status === 'READY' && doc.chunk_count > 0">{{ doc.chunk_count }} 个分块</span>
            <StrategyBadge v-if="doc.chunk_mode" :strategy="doc.chunk_mode" />
          </div>
        </div>
        <StatusBadge :status="doc.status" :error-message="doc.error_message" />
        <button
          class="btn-download"
          :disabled="doc.status !== 'READY' || downloadingFile === doc.filepath"
          :title="doc.status !== 'READY' ? '文档处理中或失败，无法下载' : '下载源文件'"
          @click="handleDownload(doc.filepath, doc.filename)"
          aria-label="下载文档"
        >
          <Loader2 v-if="downloadingFile === doc.filepath" :size="16" class="spin" />
          <Download v-else :size="16" />
        </button>
        <button
          v-if="isOwner"
          class="btn-delete"
          :disabled="deletingSource === doc.filepath"
          @click="handleDelete(doc.filepath)"
          aria-label="删除文档"
        >
          <Loader2 v-if="deletingSource === doc.filepath" :size="16" class="spin" />
          <Trash2 v-else :size="16" />
        </button>
      </div>
    </div>

    <div v-if="hasProcessing && !loading" class="polling-hint">
      <Loader2 :size="14" class="spin" />
      <span>处理中，自动刷新状态...</span>
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

.doc-item.processing {
  border-left: 3px solid var(--accent-1);
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

.btn-download {
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

.btn-download:hover:not(:disabled) {
  color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
}

.btn-download:disabled {
  opacity: 0.4;
  cursor: not-allowed;
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

.polling-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  font-size: 12px;
  color: var(--text-muted);
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
