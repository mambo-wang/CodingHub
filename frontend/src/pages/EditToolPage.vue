<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import MarkdownIt from 'markdown-it'
import { ElMessage, ElMessageBox } from 'element-plus'
import api, { fileUploadApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Category, ToolDetail, ToolFile, UpdateToolRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const categories = ref<Category[]>([])
const toolId = ref<number>(0)
const previewContent = ref('')
const existingFiles = ref<ToolFile[]>([])
const newFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const allowedExtensions = ['zip', 'tar', 'gz', 'py', 'js', 'ts', 'md', 'txt', 'json', 'yaml', 'yml', 'toml', 'xml', 'html', 'css']
const maxFileSize = 50 * 1024 * 1024

const form = ref<UpdateToolRequest>({ name: '', categoryId: 0, content: '', version: '' })

const md = new MarkdownIt()

const totalNewFileSize = computed(() => {
  return newFiles.value.reduce((sum, file) => sum + file.size, 0)
})

const formattedFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
  } catch (error) { console.error('Failed to fetch categories:', error) }
}

const fetchTool = async () => {
  loading.value = true
  try {
    const response = await api.get(`/tools/${toolId.value}`)
    const tool: ToolDetail = response.data.data

    if (tool.uploaderId !== authStore.user?.id) {
      router.push('/me/tools')
      return
    }

    form.value.name = tool.name
    form.value.content = tool.content
    form.value.version = tool.version || '1.0.0'

    const category = categories.value.find(c => c.name === tool.categoryName)
    if (category) form.value.categoryId = category.id

    previewContent.value = md.render(form.value.content || '')
  } catch {
    router.push('/me/tools')
  } finally { loading.value = false }
}

const fetchFiles = async () => {
  try {
    const response = await fileUploadApi.getToolFiles(toolId.value)
    existingFiles.value = response.files || []
  } catch (error) { console.error('Failed to fetch files:', error) }
}

const handleDeleteFile = async (file: ToolFile) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件 "${file.originalName}" 吗？此操作不可撤销。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await fileUploadApi.deleteFile(toolId.value, file.id)
    existingFiles.value = existingFiles.value.filter(f => f.id !== file.id)
    ElMessage.success('文件已删除')
  } catch (error) {
    // User cancelled or error
  }
}

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  for (let i = 0; i < input.files.length; i++) {
    const file = input.files[i]
    const ext = file.name.split('.').pop()?.toLowerCase() || ''

    if (!allowedExtensions.includes(ext)) {
      ElMessage.warning(`不支持的文件类型: .${ext}`)
      continue
    }

    if (file.size > maxFileSize) {
      ElMessage.warning(`文件 ${file.name} 超过50MB限制`)
      continue
    }

    const existsInExisting = existingFiles.value.some(f => f.originalName === file.name)
    const existsInNew = newFiles.value.some(f => f.name === file.name)
    if (existsInExisting || existsInNew) {
      ElMessage.info(`文件 "${file.name}" 已存在，上传后将替换原文件`)
    }

    newFiles.value = newFiles.value.filter(f => f.name !== file.name)
    newFiles.value.push(file)
  }

  input.value = ''
}

const removeNewFile = (index: number) => {
  newFiles.value.splice(index, 1)
}

const triggerFileInput = () => {
  fileInputRef.value?.click()
}

const handleSubmit = async () => {
  if (!form.value.name || !form.value.categoryId || !form.value.content) return

  submitting.value = true
  try {
    await api.put(`/tools/${toolId.value}`, form.value)

    if (newFiles.value.length > 0) {
      uploading.value = true
      uploadProgress.value = 0
      await fileUploadApi.uploadFiles(
        toolId.value,
        newFiles.value,
        null,
        (percent) => { uploadProgress.value = percent }
      )
    }

    ElMessage.success('工具更新成功')
    router.push('/me/tools')
  } catch (error) { console.error('Update failed:', error) }
  finally { submitting.value = false; uploading.value = false }
}

onMounted(async () => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }

  toolId.value = Number(route.params.id)
  if (!toolId.value) { router.push('/me/tools'); return }

  await fetchCategories()
  await fetchTool()
  await fetchFiles()
})
</script>

<template>
  <div class="edit-page">
    <div class="edit-bg"><div class="bg-orb bg-orb-1"></div><div class="bg-orb bg-orb-2"></div></div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="url(#editGrad)" stroke-width="2">
              <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </span>
          编辑工具
        </h1>
        <p class="page-subtitle">修改工具信息、版本号和管理文件</p>
      </div>

      <div class="form-container glass-card animate-fade-in-up" style="animation-delay: 0.1s">
        <div v-if="loading" class="loading-state">
          <div class="skeleton-line"></div>
          <div class="skeleton-line short"></div>
          <div class="skeleton-line"></div>
        </div>

        <form v-else class="edit-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">工具名称</label>
            <input v-model="form.name" type="text" class="form-input" placeholder="输入工具名称" maxlength="100" />
          </div>

          <div class="form-group">
            <label class="form-label">分类</label>
            <div class="select-wrapper">
              <select v-model="form.categoryId" class="form-select">
                <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.icon }} {{ cat.name }}</option>
              </select>
              <svg class="select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">🏷️ 版本号</label>
            <input v-model="form.version" type="text" class="form-input" placeholder="如 1.0.0" maxlength="50" />
            <div class="input-hint">使用语义化版本号格式，如 1.0.0、2.1.3-beta</div>
          </div>

          <div class="form-group">
            <label class="form-label">工具介绍（Markdown）</label>
            <textarea v-model="form.content" class="form-textarea" placeholder="详细介绍工具..." maxlength="5000" @input="previewContent = md.render(form.content || '')"></textarea>
          </div>

          <div v-if="previewContent" class="preview-section">
            <div class="preview-header"><span>👁️</span> 实时预览</div>
            <div class="preview-content"><div class="markdown-body" v-html="previewContent"></div></div>
          </div>

          <!-- File Management Section -->
          <div class="form-group">
            <label class="form-label">📦 文件管理</label>

            <!-- Existing Files -->
            <div v-if="existingFiles.length > 0" class="file-list">
              <div class="file-list-header">
                <span>已有文件 ({{ existingFiles.length }})</span>
              </div>
              <div v-for="file in existingFiles" :key="file.id" class="file-item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <path d="M14 2v6h6"/>
                </svg>
                <span class="file-name">{{ file.originalName }}</span>
                <span class="file-size">{{ formattedFileSize(file.fileSize) }}</span>
                <button type="button" class="delete-file-btn" @click="handleDeleteFile(file)" title="删除文件">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
                  </svg>
                </button>
              </div>
            </div>

            <div v-if="existingFiles.length === 0 && newFiles.length === 0" class="no-files-hint">
              暂无文件，可以上传新文件
            </div>

            <!-- New File Upload -->
            <div class="file-upload-area" @click="triggerFileInput">
              <input
                ref="fileInputRef"
                type="file"
                multiple
                class="file-input-hidden"
                @change="handleFileSelect"
              />
              <div class="upload-hint">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
                </svg>
                <span>点击上传新文件（同名文件将替换已有文件）</span>
              </div>
            </div>

            <!-- New Files List -->
            <div v-if="newFiles.length > 0" class="file-list">
              <div class="file-list-header">
                <span>待上传文件 ({{ newFiles.length }})</span>
                <span class="total-size">总计: {{ formattedFileSize(totalNewFileSize) }}</span>
              </div>
              <div v-for="(file, index) in newFiles" :key="index" class="file-item new-file-item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <path d="M14 2v6h6"/>
                </svg>
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ formattedFileSize(file.size) }}</span>
                <span v-if="existingFiles.some(f => f.originalName === file.name)" class="replace-badge">替换</span>
                <button type="button" class="remove-file-btn" @click.stop="removeNewFile(index)">
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

          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="router.push('/me/tools')">取消</button>
            <button type="submit" class="submit-btn" :disabled="submitting || uploading || !form.name || !form.content">
              <span v-if="submitting || uploading" class="loading-spinner"></span>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
              保存修改
            </button>
          </div>
        </form>
      </div>
    </div>

    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">✦ Created By Deerflow</a>
  </div>
</template>

<style scoped>
.edit-page { min-height: calc(100vh - 60px); padding: 40px 20px 80px; position: relative; }
.edit-bg { position: fixed; inset: 0; pointer-events: none; overflow: hidden; }
.bg-orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.3; }
.bg-orb-1 { width: 400px; height: 400px; background: rgba(139, 92, 246, 0.3); top: -100px; left: -100px; }
.bg-orb-2 { width: 300px; height: 300px; background: rgba(6, 182, 212, 0.2); bottom: 100px; right: -100px; }
.app-container { max-width: 800px; margin: 0 auto; position: relative; z-index: 1; }
.page-header { text-align: center; margin-bottom: 40px; }
.page-title { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 36px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.title-icon { filter: drop-shadow(0 0 12px rgba(139, 92, 246, 0.5)); }
.page-subtitle { font-size: 16px; color: var(--text-secondary); }
.form-container { padding: 40px; }
.loading-state { display: flex; flex-direction: column; gap: 16px; }
.skeleton-line { width: 100%; height: 48px; background: rgba(255, 255, 255, 0.05); border-radius: 10px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-line.short { width: 60%; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.edit-form { display: flex; flex-direction: column; gap: 24px; }
.form-group { display: flex; flex-direction: column; gap: 10px; }
.form-label { font-size: 14px; font-weight: 500; color: var(--text-primary); }
.input-hint { font-size: 12px; color: var(--text-muted); margin-top: -4px; }
.form-input, .form-textarea { width: 100%; padding: 14px 16px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; outline: none; transition: all 0.25s ease; }
.form-input:focus, .form-textarea:focus { border-color: rgba(139, 92, 246, 0.5); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1); }
.form-textarea { min-height: 180px; resize: vertical; line-height: 1.6; }
.select-wrapper { position: relative; }
.form-select { width: 100%; padding: 14px 40px 14px 16px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; cursor: pointer; outline: none; appearance: none; transition: all 0.25s ease; }
.select-arrow { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }
.preview-section { background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); border-radius: 12px; overflow: hidden; }
.preview-header { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: rgba(139, 92, 246, 0.05); border-bottom: 1px solid var(--border-color); font-size: 13px; font-weight: 500; color: var(--text-secondary); }
.preview-content { padding: 20px; max-height: 300px; overflow-y: auto; }
.markdown-body { line-height: 1.7; color: var(--text-secondary); }
.markdown-body :deep(h1) { font-size: 20px; margin: 16px 0 10px; color: var(--text-primary); font-weight: 600; }
.markdown-body :deep(h2) { font-size: 17px; margin: 14px 0 8px; color: var(--text-primary); font-weight: 600; }
.markdown-body :deep(p) { margin: 0 0 12px; }
.markdown-body :deep(code) { background: rgba(139, 92, 246, 0.1); padding: 2px 6px; border-radius: 4px; font-family: var(--font-mono); font-size: 13px; color: var(--accent-2); }
.markdown-body :deep(pre) { background: var(--bg-secondary); padding: 16px; border-radius: 8px; overflow-x: auto; }
.markdown-body :deep(pre code) { background: transparent; padding: 0; color: var(--text-primary); }

/* File Management Styles */
.file-input-hidden { display: none; }
.no-files-hint { font-size: 13px; color: var(--text-muted); text-align: center; padding: 16px; background: rgba(255, 255, 255, 0.02); border: 1px dashed var(--border-color); border-radius: 8px; }
.file-upload-area { border: 2px dashed var(--border-color); border-radius: 10px; padding: 20px; text-align: center; cursor: pointer; transition: all 0.25s ease; }
.file-upload-area:hover { border-color: rgba(139, 92, 246, 0.5); background: rgba(139, 92, 246, 0.05); }
.upload-hint { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--text-secondary); font-size: 13px; }
.upload-hint svg { color: var(--accent-1); }
.file-list { background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); border-radius: 10px; overflow: hidden; }
.file-list-header { display: flex; align-items: center; gap: 12px; padding: 10px 16px; background: rgba(139, 92, 246, 0.05); border-bottom: 1px solid var(--border-color); font-size: 13px; color: var(--text-secondary); }
.total-size { margin-left: auto; color: var(--accent-1); font-size: 12px; }
.file-item { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid var(--border-color); font-size: 13px; }
.file-item:last-child { border-bottom: none; }
.file-item svg { color: var(--text-muted); flex-shrink: 0; }
.file-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.file-size { color: var(--text-muted); font-size: 12px; flex-shrink: 0; }
.delete-file-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all 0.2s ease; }
.delete-file-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); }
.replace-badge { background: rgba(234, 179, 8, 0.15); color: #eab308; font-size: 11px; padding: 2px 6px; border-radius: 4px; flex-shrink: 0; }
.remove-file-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all 0.2s ease; }
.remove-file-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); }
.upload-progress { margin-top: 8px; display: flex; flex-direction: column; gap: 8px; }
.progress-bar { height: 6px; background: rgba(255, 255, 255, 0.1); border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, var(--accent-1), var(--accent-2)); border-radius: 3px; transition: width 0.3s ease; }
.progress-text { font-size: 12px; color: var(--text-secondary); text-align: center; }

.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
.cancel-btn { padding: 12px 20px; background: transparent; border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-secondary); font-family: var(--font-display); font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.25s ease; }
.cancel-btn:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.submit-btn { display: flex; align-items: center; gap: 8px; padding: 12px 24px; background: linear-gradient(135deg, var(--accent-1), var(--accent-2)); border: none; border-radius: 10px; color: white; font-family: var(--font-display); font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.25s ease; }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.loading-spinner { width: 16px; height: 16px; border: 2px solid rgba(255, 255, 255, 0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
