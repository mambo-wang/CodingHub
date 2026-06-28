<script setup lang="ts">
import { ref, computed } from 'vue'
import { Upload, Loader2, File, X } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { ElMessage } from 'element-plus'
import StatusBadge from './StatusBadge.vue'
import type { DocumentStatus } from '@/types/knowledge'

const props = defineProps<{
  documentsUrl: string
}>()

const emit = defineEmits<{
  (e: 'uploaded'): void
}>()

interface FileCard {
  id: number
  filename: string
  status: DocumentStatus | 'SELECTED'
  errorMessage?: string | null
}

const uploading = ref(false)
const progress = ref(0)
const fileCards = ref<FileCard[]>([])
const selectedFiles = ref<File[]>([])
let nextFileId = 0

const hasSelectedFiles = computed(() => selectedFiles.value.length > 0)
const hasProcessingFiles = computed(() =>
  fileCards.value.some(c => ['UPLOADING', 'CONVERTING', 'CHUNKING', 'EMBEDDING', 'SELECTED'].includes(c.status))
)

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  selectedFiles.value = Array.from(input.files)
  input.value = ''
}

const handleDrop = (event: DragEvent) => {
  event.preventDefault()
  event.stopPropagation()
  const files = event.dataTransfer?.files
  if (!files?.length) return
  selectedFiles.value = Array.from(files)
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  event.stopPropagation()
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const doUpload = async () => {
  if (selectedFiles.value.length === 0) return

  if (selectedFiles.value.length > 20) {
    ElMessage.warning('单次最多上传 20 个文件')
    return
  }

  uploading.value = true
  progress.value = 0

  // Create initial file cards
  fileCards.value = selectedFiles.value.map(f => ({
    id: nextFileId++,
    filename: f.name,
    status: 'SELECTED' as const,
  }))

  try {
    const results = await knowledgeService.batchUpload(
      props.documentsUrl,
      selectedFiles.value,
      (p) => { progress.value = p }
    )

    selectedFiles.value = []
    fileCards.value = []
    emit('uploaded')
    ElMessage.success(`${results.length} 个文件已提交，正在后台处理`)
  } catch (e: any) {
    const msg = e.response?.data?.error || e.message || '上传失败'
    ElMessage.error(msg)
    // Mark all as failed
    for (const card of fileCards.value) {
      if (card.status === 'SELECTED') {
        card.status = 'FAILED'
        card.errorMessage = msg
      }
    }
  } finally {
    uploading.value = false
    progress.value = 0
  }
}

/** Update file card statuses from external polling (called by parent) */
const updateStatuses = (statuses: { id: number; status: DocumentStatus; error_message?: string | null }[]) => {
  for (const s of statuses) {
    const card = fileCards.value.find(c => c.id === s.id)
    if (card) {
      card.status = s.status
      card.errorMessage = s.error_message || null
    }
  }
}

defineExpose({ updateStatuses })
</script>

<template>
  <div class="document-upload">
    <!-- Upload area -->
    <label v-if="!uploading && !hasProcessingFiles" class="upload-area" @drop="handleDrop" @dragover="handleDragOver">
      <input
        type="file"
        class="file-input"
        @change="handleFileSelect"
        :disabled="uploading"
        multiple
        accept=".pdf,.docx,.doc,.txt,.md,.pptx,.xlsx"
      />
      <div class="upload-prompt">
        <Upload :size="28" />
        <span class="upload-text">拖拽文件到此处，或点击选择文件</span>
        <span class="upload-hint">支持批量上传（最多 20 个），格式：PDF、DOCX、TXT、MD、PPTX、XLSX</span>
      </div>
    </label>

    <!-- Upload progress -->
    <div v-if="uploading" class="upload-progress-area">
      <Loader2 :size="24" class="spin" />
      <span class="progress-text">{{ progress >= 90 ? '提交中...' : `上传中 ${progress}%` }}</span>
    </div>

    <!-- Selected files preview -->
    <div v-if="hasSelectedFiles && !uploading" class="selected-files">
      <div class="selected-header">
        <span class="selected-count">已选择 {{ selectedFiles.length }} 个文件</span>
        <button class="btn-upload" @click="doUpload">
          <Upload :size="14" />
          <span>开始上传</span>
        </button>
      </div>
      <div class="file-list">
        <div v-for="(file, idx) in selectedFiles" :key="idx" class="file-card">
          <File :size="14" aria-hidden="true" />
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ (file.size / 1024).toFixed(1) }} KB</span>
          <button class="btn-remove" @click="removeFile(idx)" aria-label="移除文件">
            <X :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- Processing file cards (after upload) -->
    <div v-if="fileCards.length > 0" class="processing-cards">
      <div v-for="card in fileCards" :key="card.id" class="processing-card" :class="card.status.toLowerCase()">
        <File :size="14" aria-hidden="true" />
        <span class="card-name">{{ card.filename }}</span>
        <StatusBadge v-if="card.status !== 'SELECTED'" :status="card.status as DocumentStatus" :error-message="card.errorMessage" />
        <span v-else class="status-pending">待上传</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.document-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-area {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 20px;
}

.upload-area:hover {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.05);
}

.file-input {
  display: none;
}

.upload-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
}

.upload-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}

/* Upload progress */
.upload-progress-area {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
  color: var(--accent-1);
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-mono);
}

/* Selected files */
.selected-files {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
}

.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.selected-count {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.btn-upload {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-upload:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--bg-secondary);
  font-size: 13px;
  color: var(--text-primary);
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  flex-shrink: 0;
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--text-muted);
}

.btn-remove {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-remove:hover {
  color: #EF4444;
  background: rgba(239, 68, 68, 0.1);
}

/* Processing cards */
.processing-cards {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.processing-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--bg-secondary);
  font-size: 13px;
  color: var(--text-primary);
}

.card-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pending {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
  padding: 2px 8px;
  border-radius: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme */
[data-theme="light"] .upload-area {
  background: var(--bg-card);
}

[data-theme="light"] .selected-files,
[data-theme="light"] .upload-progress-area {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .file-card,
[data-theme="light"] .processing-card {
  background: var(--bg-secondary);
}
</style>
