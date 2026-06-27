<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { FileText, Trash2, Loader2 } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import type { RagDocument } from '@/types/knowledge'

const props = defineProps<{
  documentsUrl: string
  isOwner: boolean
}>()

const emit = defineEmits<{
  (e: 'refreshed'): void
}>()

const documents = ref<RagDocument[]>([])
const loading = ref(true)
const deletingSource = ref<string | null>(null)

const loadDocuments = async () => {
  loading.value = true
  try {
    documents.value = await knowledgeService.getDocuments(props.documentsUrl)
  } catch (e) {
    console.error('Failed to load documents:', e)
  } finally {
    loading.value = false
  }
}

const handleDelete = async (source: string) => {
  if (!confirm('确定要删除这个文档吗？')) return
  deletingSource.value = source
  try {
    await knowledgeService.deleteDocument(props.documentsUrl, source)
    documents.value = documents.value.filter(d => d.source !== source)
    emit('refreshed')
  } catch (e) {
    console.error('Failed to delete document:', e)
  } finally {
    deletingSource.value = null
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
      <div v-for="doc in documents" :key="doc.source" class="doc-item glass-card">
        <div class="doc-icon">
          <FileText :size="20" />
        </div>
        <div class="doc-info">
          <span class="doc-name">{{ doc.source }}</span>
          <div class="doc-meta">
            <span v-if="doc.chunk_count != null">{{ doc.chunk_count }} 个分块</span>
          </div>
        </div>
        <button
          v-if="isOwner"
          class="btn-delete"
          :disabled="deletingSource === doc.source"
          @click="handleDelete(doc.source)"
          aria-label="删除文档"
        >
          <Loader2 v-if="deletingSource === doc.source" :size="16" class="spin" />
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
