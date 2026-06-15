<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import api, { fileUploadApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Category, CreateToolRequest } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const categories = ref<Category[]>([])
const previewContent = ref('')

// File upload state
const selectedFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)

// 工具附件已放开格式限制：不再维护 allowedExtensions 白名单，仅按大小预检
const maxFileSize = 50 * 1024 * 1024 // 50MB
const maxTotalSize = 200 * 1024 * 1024 // 200MB

const form = ref<CreateToolRequest>({
  name: '',
  categoryId: 0,
  content: '',
  version: '1.0.0'
})

const md = new MarkdownIt()

const totalFileSize = computed(() => {
  return selectedFiles.value.reduce((sum, file) => sum + file.size, 0)
})

const formattedFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const renderedPreview = () => {
  previewContent.value = md.render(form.value.content || '')
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
    if (categories.value.length > 0) {
      form.value.categoryId = categories.value[0].id
    }
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  const newFiles: File[] = []
  for (let i = 0; i < input.files.length; i++) {
    const file = input.files[i]

    if (file.size > maxFileSize) {
      ElMessage.warning(`文件 ${file.name} 超过50MB限制`)
      continue
    }

    newFiles.push(file)
  }

  // Check total size
  const totalSize = totalFileSize.value + newFiles.reduce((sum, f) => sum + f.size, 0)
  if (totalSize > maxTotalSize) {
    ElMessage.warning('总上传大小超过200MB限制')
    return
  }

  selectedFiles.value = [...selectedFiles.value, ...newFiles]
  input.value = ''
}

const removeFile = (index: number) => {
  selectedFiles.value.splice(index, 1)
}

const clearFiles = () => {
  selectedFiles.value = []
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.categoryId || !form.value.content || !form.value.version) {
    ElMessage.warning('请填写完整的工具信息')
    return
  }
  // Version format validation (SemVer)
  const versionPattern = /^\d+\.\d+\.\d+(-[a-zA-Z0-9]+)?$/
  if (!versionPattern.test(form.value.version)) {
    ElMessage.warning('版本号格式不正确，请使用标准格式（如 1.0.0）')
    return
  }

  loading.value = true
  try {
    // Create tool first
    const response = await api.post('/tools', form.value)
    const toolId = response.data.data.id

    // Upload files if selected
    if (selectedFiles.value.length > 0) {
      uploading.value = true
      uploadProgress.value = 0

      await fileUploadApi.uploadFiles(
        toolId,
        selectedFiles.value,
        form.value.content,
        (percent) => {
          uploadProgress.value = percent
        }
      )
    }

    ElMessage.success('工具上传成功')
    router.push('/')
  } catch (error: any) {
    console.error('Upload failed:', error)
  } finally {
    loading.value = false
    uploading.value = false
  }
}

const handleReset = () => {
  form.value.name = ''
  form.value.categoryId = categories.value[0]?.id || 0
  form.value.content = ''
  form.value.version = '1.0.0'
  previewContent.value = ''
  selectedFiles.value = []
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  fetchCategories()
})
</script>

<template>
  <div class="upload-page">
    <!-- Background effects -->
    <div class="upload-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <!-- Header -->
      <div class="page-header animate-fade-in-up">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="url(#uploadGrad)" stroke-width="2">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
            </svg>
          </span>
          上传新工具
        </h1>
        <p class="page-subtitle">分享您的 AI 工具到工具广场</p>
      </div>

      <!-- Form -->
      <div class="form-container glass-card animate-fade-in-up" style="animation-delay: 0.1s">
        <form class="upload-form" @submit.prevent="handleSubmit">
          <!-- Tool Name -->
          <div class="form-group">
            <label class="form-label">
              <span class="label-icon">✨</span>
              工具名称
            </label>
            <div class="input-wrapper">
              <input
                v-model="form.name"
                type="text"
                class="form-input"
                placeholder="给工具起个好听的名字"
                maxlength="100"
              />
              <span class="char-count">{{ form.name.length }}/100</span>
            </div>
          </div>

          <!-- Category -->
          <div class="form-group">
            <label class="form-label">
              <span class="label-icon">📁</span>
              分类
            </label>
            <div class="select-wrapper">
              <select v-model="form.categoryId" class="form-select">
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                  {{ cat.icon }} {{ cat.name }}
                </option>
              </select>
              <svg class="select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 9l6 6 6-6"/>
              </svg>
            </div>
          </div>

          <!-- Version -->
          <div class="form-group">
            <label class="form-label">
              <span class="label-icon">🏷️</span>
              版本号
            </label>
            <div class="input-wrapper">
              <input
                v-model="form.version"
                type="text"
                class="form-input"
                placeholder="如 1.0.0"
                maxlength="50"
              />
              <span class="char-count">{{ form.version.length }}/50</span>
            </div>
            <div class="input-hint">使用语义化版本号格式，如 1.0.0、2.1.3-alpha</div>
          </div>

          <!-- Content -->
          <div class="form-group">
            <label class="form-label">
              <span class="label-icon">📝</span>
              工具介绍（Markdown）
            </label>
            <textarea
              v-model="form.content"
              class="form-textarea"
              placeholder="详细介绍工具的功能、使用方法、特点等...&#10;&#10;支持 Markdown 格式：&#10;- 标题、列表、代码块、图片"
              maxlength="5000"
              @input="renderedPreview"
            ></textarea>
            <div class="textarea-footer">
              <span class="char-count">{{ form.content.length }}/5000</span>
            </div>
          </div>

          <!-- Preview -->
          <div v-if="previewContent" class="preview-section">
            <div class="preview-header">
              <span class="preview-icon">👁️</span>
              <span>实时预览</span>
            </div>
            <div class="preview-content">
              <div class="markdown-body" v-html="previewContent"></div>
            </div>
          </div>

          <!-- File Upload -->
          <div class="form-group">
            <label class="form-label">
              <span class="label-icon">📦</span>
              上传文件
            </label>
            <div class="file-upload-area" @click="triggerFileInput">
              <input
                ref="fileInputRef"
                type="file"
                multiple
                accept=".*"
                class="file-input-hidden"
                @change="handleFileSelect"
              />
              <div class="upload-hint">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
                </svg>
                <span>点击选择文件或将文件拖拽到此处</span>
                <span class="upload-hint-ext">支持任意格式文件（单文件 ≤ 50MB，单次请求 ≤ 200MB）</span>
              </div>
            </div>

            <!-- File List -->
            <div v-if="selectedFiles.length > 0" class="file-list">
              <div class="file-list-header">
                <span>已选择 {{ selectedFiles.length }} 个文件</span>
                <span class="total-size">总计: {{ formattedFileSize(totalFileSize) }}</span>
                <button type="button" class="clear-files-btn" @click.stop="clearFiles">清除全部</button>
              </div>
              <div v-for="(file, index) in selectedFiles" :key="index" class="file-item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <path d="M14 2v6h6"/>
                </svg>
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ formattedFileSize(file.size) }}</span>
                <button type="button" class="remove-file-btn" @click.stop="removeFile(index)">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M18 6L6 18M6 6l12 12"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Upload Progress -->
            <div v-if="uploading" class="upload-progress">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
              </div>
              <span class="progress-text">上传中... {{ uploadProgress }}%</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="form-actions">
            <button type="button" class="reset-btn" @click="handleReset">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 12a9 9 0 109-9 9.75 9.75 0 00-6.74 2.74L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
              重置
            </button>
            <button type="submit" class="submit-btn" :disabled="loading || uploading || !form.name || !form.content || !form.version">
              <span v-if="loading || uploading" class="loading-spinner"></span>
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
              </svg>
              {{ uploading ? '上传中...' : '提交工具' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Deerflow Branding -->
    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">
      ✦ Created By Deerflow
    </a>
  </div>
</template>

<style scoped>
.upload-page {
  min-height: calc(100vh - 60px);
  padding: 40px 20px 80px;
  position: relative;
}

.upload-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(139, 92, 246, 0.3);
  top: -150px;
  left: -200px;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: rgba(6, 182, 212, 0.2);
  bottom: -100px;
  right: -150px;
}

.app-container {
  max-width: 800px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Header */
.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.title-icon {
  filter: drop-shadow(0 0 12px rgba(139, 92, 246, 0.5));
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
}

/* Form */
.form-container {
  padding: 40px;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.label-icon {
  font-size: 16px;
}

.input-wrapper {
  position: relative;
}

.input-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
}

.form-input {
  width: 100%;
  padding: 14px 60px 14px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  outline: none;
  transition: all 0.25s ease;
}

.form-input:focus {
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.char-count {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.select-wrapper {
  position: relative;
}

.form-select {
  width: 100%;
  padding: 14px 40px 14px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  appearance: none;
  transition: all 0.25s ease;
}

.form-select:focus {
  border-color: rgba(139, 92, 246, 0.5);
}

.select-arrow {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.form-textarea {
  width: 100%;
  min-height: 200px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  line-height: 1.6;
  outline: none;
  resize: vertical;
  transition: all 0.25s ease;
}

.form-textarea:focus {
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.form-textarea::placeholder {
  color: var(--text-muted);
}

.textarea-footer {
  display: flex;
  justify-content: flex-end;
}

/* Preview */
.preview-section {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(139, 92, 246, 0.05);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.preview-icon {
  font-size: 14px;
}

.preview-content {
  padding: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.markdown-body {
  line-height: 1.7;
  color: var(--text-secondary);
}

.markdown-body :deep(h1) {
  font-size: 22px;
  margin: 20px 0 12px;
  color: var(--text-primary);
  font-weight: 600;
}

.markdown-body :deep(h2) {
  font-size: 18px;
  margin: 18px 0 10px;
  color: var(--text-primary);
  font-weight: 600;
}

.markdown-body :deep(p) {
  margin: 0 0 12px;
}

.markdown-body :deep(code) {
  background: rgba(139, 92, 246, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--accent-2);
}

.markdown-body :deep(pre) {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 12px;
  padding-left: 20px;
}

/* Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.reset-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.reset-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border: none;
  border-radius: 10px;
  color: white;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* File Upload */
.file-input-hidden {
  display: none;
}

.file-upload-area {
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.file-upload-area:hover {
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.05);
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--text-secondary);
}

.upload-hint svg {
  color: var(--accent-1);
}

.upload-hint-ext {
  font-size: 12px;
  color: var(--text-muted);
}

/* File List */
.file-list {
  margin-top: 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}

.file-list-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(139, 92, 246, 0.05);
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
  color: var(--text-secondary);
}

.total-size {
  margin-left: auto;
  color: var(--accent-1);
}

.clear-files-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.clear-files-btn:hover {
  color: var(--accent-2);
  background: rgba(255, 255, 255, 0.05);
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
}

.file-item:last-child {
  border-bottom: none;
}

.file-item svg {
  color: var(--text-muted);
  flex-shrink: 0;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
}

.file-size {
  color: var(--text-muted);
  font-size: 12px;
  flex-shrink: 0;
}

.remove-file-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.remove-file-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* Upload Progress */
.upload-progress {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar {
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-1), var(--accent-2));
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}
</style>