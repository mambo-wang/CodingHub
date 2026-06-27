<script setup lang="ts">
import { ref } from 'vue'
import { Upload, Loader2, CheckCircle, XCircle } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  documentsUrl: string
}>()

const emit = defineEmits<{
  (e: 'uploaded'): void
}>()

const uploading = ref(false)
const progress = ref(0)
const uploadSuccess = ref(false)
const uploadError = ref('')

const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files?.length) return
  const file = input.files[0]
  await doUpload(file)
  input.value = ''
}

const handleDrop = async (event: DragEvent) => {
  event.preventDefault()
  event.stopPropagation()
  const file = event.dataTransfer?.files[0]
  if (!file) return
  await doUpload(file)
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  event.stopPropagation()
}

const doUpload = async (file: File) => {
  uploading.value = true
  progress.value = 0
  uploadSuccess.value = false
  uploadError.value = ''

  try {
    await knowledgeService.uploadDocument(props.documentsUrl, file, (p) => {
      progress.value = p
    })
    uploadSuccess.value = true
    ElMessage.success('文档上传成功')
    emit('uploaded')
    setTimeout(() => {
      uploadSuccess.value = false
    }, 3000)
  } catch (e: any) {
    const msg = e.response?.data?.message || '上传失败'
    uploadError.value = msg
    ElMessage.error(msg)
    setTimeout(() => {
      uploadError.value = ''
    }, 5000)
  } finally {
    uploading.value = false
    progress.value = 0
  }
}
</script>

<template>
  <div class="document-upload">
    <label class="upload-area" :class="{ uploading }" @drop="handleDrop" @dragover="handleDragOver">
      <input
        type="file"
        class="file-input"
        @change="handleFileSelect"
        :disabled="uploading"
        accept=".pdf,.docx,.doc,.txt,.md,.pptx,.xlsx"
      />
      <div v-if="uploading" class="upload-progress">
        <Loader2 :size="28" class="spin" />
        <span class="progress-text">{{ progress >= 90 ? '处理中...' : progress + '%' }}</span>
      </div>
      <div v-else-if="uploadSuccess" class="upload-status success">
        <CheckCircle :size="28" />
        <span>上传成功</span>
      </div>
      <div v-else-if="uploadError" class="upload-status error">
        <XCircle :size="28" />
        <span>{{ uploadError }}</span>
      </div>
      <div v-else class="upload-prompt">
        <Upload :size="28" />
        <span class="upload-text">拖拽文件到此处，或点击选择文件</span>
        <span class="upload-hint">支持 PDF、DOCX、TXT、MD、PPTX、XLSX 格式</span>
      </div>
    </label>
  </div>
</template>

<style scoped>
.document-upload {
  width: 100%;
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

.upload-area:hover:not(.uploading) {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.05);
}

.upload-area.uploading {
  cursor: default;
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
}

.upload-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--accent-1);
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-mono);
}

.upload-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
}

.upload-status.success {
  color: #10B981;
}

.upload-status.error {
  color: #EF4444;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

[data-theme="light"] .upload-area {
  background: var(--bg-card);
}
</style>
