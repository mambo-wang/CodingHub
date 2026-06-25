<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Upload, CheckCircle, XCircle, Loader2, Video } from '@lucide/vue'
import { videoService } from '@/services/video'
import { useAuthStore } from '@/stores/auth'
import type { Tag } from '@/types'
import TagSelector from '@/components/common/TagSelector.vue'
import VideoCoverPicker from '@/components/video/VideoCoverPicker.vue'

const router = useRouter()
const authStore = useAuthStore()

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login?redirect=/videos/upload')
  }
})

const MAX_FILE_SIZE = 1 * 1024 * 1024 * 1024 // 1GB
const ALLOWED_TYPES = ['video/mp4']

const selectedFile = ref<File | null>(null)
const title = ref('')
const description = ref('')
const dragging = ref(false)
const uploading = ref(false)
const progress = ref(0)
const error = ref('')
const uploadSuccess = ref(false)
const uploadedVideoId = ref<number | null>(null)
const selectedTags = ref<Tag[]>([])
const videoSrc = ref<string | null>(null)
const coverBlob = ref<Blob | null>(null)
const coverFile = ref<File | null>(null)

const fileInputRef = ref<HTMLInputElement | null>(null)

const hasFile = computed(() => !!selectedFile.value)
const canSubmit = computed(() => hasFile.value && title.value.trim().length > 0 && !uploading.value)

const formatFileSize = (bytes: number): string => {
  if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${bytes} B`
}

const validateFile = (file: File): string | null => {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return '仅支持 MP4 格式的视频文件'
  }
  if (file.size > MAX_FILE_SIZE) {
    return '视频文件不能超过 1GB'
  }
  return null
}

const handleFile = (file: File) => {
  const validationError = validateFile(file)
  if (validationError) {
    error.value = validationError
    return
  }
  selectedFile.value = file
  error.value = ''
  // Create video object URL for cover picker
  if (videoSrc.value) URL.revokeObjectURL(videoSrc.value)
  videoSrc.value = URL.createObjectURL(file)
  coverBlob.value = null
  coverFile.value = null
  // Auto-fill title from filename if title is empty
  if (!title.value) {
    title.value = file.name.replace(/\.mp4$/i, '')
  }
}

const handleFileInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) handleFile(file)
}

const handleDragEnter = (e: DragEvent) => {
  e.preventDefault()
  dragging.value = true
}

const handleDragLeave = (e: DragEvent) => {
  e.preventDefault()
  dragging.value = false
}

const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  dragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) handleFile(file)
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const clearFile = () => {
  selectedFile.value = null
  error.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

const handleUpload = async () => {
  if (!canSubmit.value || !selectedFile.value) return

  uploading.value = true
  error.value = ''
  progress.value = 0

  try {
    const tagIds = selectedTags.value.map(t => t.id)
    const result = await videoService.uploadVideo(
      selectedFile.value,
      title.value.trim(),
      description.value.trim() || undefined,
      tagIds.length > 0 ? tagIds : undefined,
      (percent) => {
        progress.value = percent
      }
    )
    uploadSuccess.value = true
    uploadedVideoId.value = result.id

    // Upload cover if set
    if (coverBlob.value) {
      await videoService.uploadCover(result.id, coverBlob.value)
    } else if (coverFile.value) {
      await videoService.uploadCover(result.id, coverFile.value)
    }

    // Redirect after a short delay
    setTimeout(() => {
      router.push(`/videos/${result.id}`)
    }, 1500)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '上传失败，请稍后重试'
  } finally {
    uploading.value = false
  }
}

const goBack = () => {
  router.push('/videos')
}
</script>

<template>
  <div class="upload-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <button class="back-btn" @click="goBack" aria-label="返回">
          <ArrowLeft :size="20" aria-hidden="true" />
        </button>
        <div>
          <h1 class="page-title">
            <span class="title-icon">📤</span>
            上传视频
          </h1>
          <p class="page-subtitle">分享你的编程微课程</p>
        </div>
      </div>

      <!-- Success State -->
      <div v-if="uploadSuccess" class="upload-result upload-success" role="status">
        <CheckCircle :size="48" aria-hidden="true" />
        <p class="result-title">上传成功！</p>
        <p class="result-subtitle">正在跳转到视频页面...</p>
      </div>

      <!-- Upload Form -->
      <div v-else class="upload-form glass-card animate-fade-in-up">
        <!-- Drop Zone -->
        <div
          class="drop-zone"
          :class="{ 'drag-over': dragging, 'has-file': hasFile }"
          @dragenter="handleDragEnter"
          @dragleave="handleDragLeave"
          @dragover="handleDragOver"
          @drop="handleDrop"
          @click="triggerFileInput"
          role="button"
          tabindex="0"
          aria-label="点击或拖拽视频文件到此处上传"
          @keydown.enter="triggerFileInput"
          @keydown.space="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept=".mp4,video/mp4"
            class="hidden-input"
            @change="handleFileInput"
            aria-label="选择视频文件"
          />

          <template v-if="!hasFile">
            <Upload :size="48" aria-hidden="true" class="drop-icon" />
            <p class="drop-title">拖拽视频文件到此处</p>
            <p class="drop-subtitle">或点击选择文件</p>
            <p class="drop-hint">仅支持 MP4 格式，最大 1GB</p>
          </template>
          <template v-else>
            <Video :size="40" aria-hidden="true" class="drop-icon file-icon" />
            <p class="file-name">{{ selectedFile!.name }}</p>
            <p class="file-size">{{ formatFileSize(selectedFile!.size) }}</p>
            <button class="clear-file-btn" @click.stop="clearFile" aria-label="清除已选文件">
              重新选择
            </button>
          </template>
        </div>

        <!-- Title Input -->
        <div class="form-group">
          <label class="form-label" for="video-title">
            视频标题 <span class="required">*</span>
          </label>
          <input
            id="video-title"
            v-model="title"
            type="text"
            class="form-input"
            placeholder="输入视频标题（必填）"
            maxlength="200"
            :disabled="uploading"
          />
          <span class="char-count">{{ title.length }} / 200</span>
        </div>

        <!-- Description Textarea -->
        <div class="form-group">
          <label class="form-label" for="video-desc">视频简介</label>
          <textarea
            id="video-desc"
            v-model="description"
            class="form-textarea"
            placeholder="添加视频简介（可选）"
            rows="4"
            maxlength="2000"
            :disabled="uploading"
          ></textarea>
          <span class="char-count">{{ description.length }} / 2000</span>
        </div>

        <!-- Tags -->
        <div class="form-group">
          <label class="form-label">标签</label>
          <TagSelector v-model="selectedTags" tagType="VIDEO" />
        </div>

        <!-- Cover Picker -->
        <VideoCoverPicker
          v-if="videoSrc"
          :videoSrc="videoSrc"
          :coverUrl="null"
          @cover-capture="(blob) => { coverBlob = blob; coverFile = null }"
          @cover-upload="(file) => { coverFile = file; coverBlob = null }"
          @cover-remove="() => { coverBlob = null; coverFile = null }"
        />

        <!-- Progress Bar -->
        <div v-if="uploading" class="progress-section">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
          </div>
          <div class="progress-info">
            <Loader2 :size="16" class="spin" aria-hidden="true" />
            <span>上传中 {{ progress }}%</span>
          </div>
        </div>

        <!-- Error -->
        <div v-if="error" class="alert alert-error" role="alert">
          <XCircle :size="16" aria-hidden="true" />
          <span>{{ error }}</span>
        </div>

        <!-- Submit -->
        <div class="form-actions">
          <button
            class="submit-btn"
            :disabled="!canSubmit"
            @click="handleUpload"
          >
            <Loader2 v-if="uploading" :size="16" class="spin" aria-hidden="true" />
            <Upload v-else :size="16" aria-hidden="true" />
            <span>{{ uploading ? '上传中...' : '开始上传' }}</span>
          </button>
          <button class="cancel-btn" :disabled="uploading" @click="goBack">
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 0 80px;
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
  max-width: 720px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 32px;
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  margin-top: 4px;
}

.back-btn:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--text-primary);
  border-color: var(--accent-1);
  transform: translateY(-1px);
}

.back-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.page-title {
  font-size: 28px;
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
  font-size: 24px;
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}

/* Success State */
.upload-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  gap: 12px;
}

.upload-success {
  color: var(--accent-2);
}

.result-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.result-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Upload Form */
.upload-form {
  padding: 32px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Drop Zone */
.drop-zone {
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  background: var(--bg-glass);
  padding: 48px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.drop-zone:hover {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.03);
}

.drop-zone:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.drop-zone.drag-over {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.05);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.1);
}

.drop-zone.has-file {
  border-style: solid;
  border-color: var(--accent-2);
  background: rgba(6, 182, 212, 0.03);
  cursor: default;
}

.drop-icon {
  color: var(--text-muted);
  margin-bottom: 8px;
}

.file-icon {
  color: var(--accent-2);
}

.drop-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.drop-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.drop-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.file-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  word-break: break-all;
}

.file-size {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.clear-file-btn {
  margin-top: 8px;
  padding: 6px 16px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.clear-file-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.clear-file-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.hidden-input {
  display: none;
}

/* Form Groups */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.required {
  color: #ef4444;
}

.form-input {
  padding: 12px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.form-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-textarea {
  padding: 12px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  transition: all 0.2s ease;
}

.form-textarea:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2);
}

.form-textarea::placeholder {
  color: var(--text-muted);
}

.form-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.char-count {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  text-align: right;
}

/* Progress */
.progress-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-bar {
  height: 8px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-1), var(--accent-2));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--accent-2);
  font-family: var(--font-mono);
}

/* Alert */
.alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
}

.alert-error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
}

[data-theme="light"] .alert-error {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.05);
}

/* Actions */
.form-actions {
  display: flex;
  gap: 12px;
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.cancel-btn {
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

.cancel-btn:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.15);
  color: var(--text-primary);
}

.cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cancel-btn:focus-visible {
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

/* Light theme */
[data-theme="light"] .drop-zone {
  background: var(--bg-card);
}

[data-theme="light"] .drop-zone.drag-over {
  background: rgba(124, 58, 237, 0.05);
}

[data-theme="light"] .form-input,
[data-theme="light"] .form-textarea {
  background: var(--bg-card);
}

[data-theme="light"] .form-input:focus,
[data-theme="light"] .form-textarea:focus {
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2);
}

[data-theme="light"] .cancel-btn:hover:not(:disabled) {
  border-color: rgba(0, 0, 0, 0.15);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .drop-zone,
  .form-input,
  .form-textarea,
  .submit-btn,
  .cancel-btn,
  .back-btn,
  .clear-file-btn,
  .progress-fill {
    transition: none;
  }
  .back-btn:hover,
  .submit-btn:hover {
    transform: none;
  }
  .spin {
    animation: none;
  }
}
</style>
