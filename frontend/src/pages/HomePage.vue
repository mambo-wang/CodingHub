<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Pencil, Trash2, Upload, Bookmark, Wrench } from '@lucide/vue'
import MarkdownIt from 'markdown-it'
import { ElMessage } from 'element-plus'
import api, { fileUploadApi } from '@/services/api'
import { interactionApi } from '@/services/interaction'
import type { ToolSummary, Category, PageResponse, CreateToolRequest } from '@/types'
import { useAuthStore } from '@/stores/auth'
import AuthorBadge from '@/components/AuthorBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

const tools = ref<ToolSummary[]>([])
const categories = ref<Category[]>([])
const selectedCategory = ref<number | null>(null)
const searchKeyword = ref('')
const sortBy = ref('latest')
const loading = ref(false)
const showMcpModal = ref(false)
const copySuccess = ref(false)
const deleteDialogVisible = ref(false)
const deleteTargetId = ref<number | null>(null)
const deleting = ref(false)
const pagination = ref({
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0
})

// Tab pill navigation
const activeTab = ref<'all' | 'favorites' | 'myTools'>('all')

// Upload modal state
const showUploadModal = ref(false)
const uploadLoading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const previewContent = ref('')
const selectedFiles = ref<File[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const maxFileSize = 50 * 1024 * 1024
const maxTotalSize = 200 * 1024 * 1024

const uploadForm = ref<CreateToolRequest>({
  name: '',
  categoryId: 0,
  content: '',
  version: '1.0.0'
})

const md = new MarkdownIt()

const totalFileSize = computed(() => selectedFiles.value.reduce((sum, f) => sum + f.size, 0))
const formattedFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// MCP config
const mcpBackendPort = (import.meta.env.VITE_BACKEND_PORT as string) || '8082'
const mcpConfig = {
  "CodingHub-mcp": {
    type: "sse",
    url: `http://${window.location.hostname}:${mcpBackendPort}/sse`,
    description: "CodingHub MCP Server",
    disabled: false
  }
}
const mcpConfigJson = JSON.stringify(mcpConfig, null, 2)

const copyMcpConfig = async () => {
  try {
    await navigator.clipboard.writeText(mcpConfigJson)
    copySuccess.value = true
    setTimeout(() => { copySuccess.value = false }, 2000)
  } catch (error) {
    console.error('Failed to copy:', error)
  }
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
    if (categories.value.length > 0 && !uploadForm.value.categoryId) {
      uploadForm.value.categoryId = categories.value[0].id
    }
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

// Unified data fetcher — dispatches by activeTab
const fetchTools = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'favorites') {
      const data = await interactionApi.getMyFavorites('TOOL', pagination.value.page, pagination.value.size)
      tools.value = data.content
      pagination.value = { page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages }
    } else if (activeTab.value === 'myTools') {
      const response = await api.get('/users/me/tools', {
        params: { page: pagination.value.page, size: pagination.value.size }
      })
      const data: PageResponse<ToolSummary> = response.data.data
      tools.value = data.content
      pagination.value = { page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages }
    } else {
      const params: Record<string, any> = {
        page: pagination.value.page,
        size: pagination.value.size,
        sortBy: sortBy.value
      }
      if (selectedCategory.value) params.categoryId = selectedCategory.value
      if (searchKeyword.value) params.keyword = searchKeyword.value

      const response = await api.get('/tools', { params })
      const data: PageResponse<ToolSummary> = response.data.data
      tools.value = data.content
      pagination.value = { page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages }
    }
  } catch (error) {
    console.error('Failed to fetch tools:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab: 'all' | 'favorites' | 'myTools') => {
  activeTab.value = tab
  pagination.value.page = 0
  fetchTools()
}

const handleSearch = () => {
  pagination.value.page = 0
  fetchTools()
}

const handleCategoryChange = (categoryId: number | null) => {
  selectedCategory.value = categoryId
  pagination.value.page = 0
  fetchTools()
}

const handleSortChange = (value: string) => {
  sortBy.value = value
  fetchTools()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page - 1
  fetchTools()
}

const goToDetail = (toolId: number) => router.push(`/tools/${toolId}`)

const canModifyTool = (tool: ToolSummary) => {
  if (!authStore.isLoggedIn) return false
  const userId = authStore.user?.id
  if (userId === undefined) return false
  return userId === tool.uploaderId || authStore.isAdmin
}

const handleToolEdit = (toolId: number) => router.push(`/me/tools/${toolId}/edit`)

const handleToolDeleteClick = (toolId: number) => {
  deleteTargetId.value = toolId
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (deleteTargetId.value === null) return
  deleting.value = true
  try {
    await api.delete(`/tools/${deleteTargetId.value}`)
    tools.value = tools.value.filter(t => t.id !== deleteTargetId.value)
    deleteDialogVisible.value = false
  } catch (e) {
    console.error('Delete failed:', e)
  } finally {
    deleting.value = false
  }
}

// Upload modal helpers
const openUploadModal = () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  showUploadModal.value = true
}

const closeUploadModal = () => {
  showUploadModal.value = false
  resetUploadForm()
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
  const totalSize = totalFileSize.value + newFiles.reduce((s, f) => f.size + s, 0)
  if (totalSize > maxTotalSize) {
    ElMessage.warning('总上传大小超过200MB限制')
    return
  }
  selectedFiles.value = [...selectedFiles.value, ...newFiles]
  input.value = ''
}

const removeFile = (index: number) => selectedFiles.value.splice(index, 1)
const clearFiles = () => { selectedFiles.value = [] }
const triggerFileInput = () => fileInputRef.value?.click()
const renderedPreview = () => { previewContent.value = md.render(uploadForm.value.content || '') }

const handleUploadSubmit = async () => {
  if (!uploadForm.value.name || !uploadForm.value.categoryId || !uploadForm.value.content || !uploadForm.value.version) {
    ElMessage.warning('请填写完整的工具信息')
    return
  }
  const versionPattern = /^\d+\.\d+\.\d+(-[a-zA-Z0-9]+)?$/
  if (!versionPattern.test(uploadForm.value.version)) {
    ElMessage.warning('版本号格式不正确，请使用标准格式（如 1.0.0）')
    return
  }
  uploadLoading.value = true
  try {
    const response = await api.post('/tools', uploadForm.value)
    const toolId = response.data.data.id
    if (selectedFiles.value.length > 0) {
      uploading.value = true
      uploadProgress.value = 0
      await fileUploadApi.uploadFiles(toolId, selectedFiles.value, uploadForm.value.content, (p) => { uploadProgress.value = p })
    }
    ElMessage.success('工具上传成功')
    closeUploadModal()
    fetchTools()
  } catch (error: any) {
    console.error('Upload failed:', error)
  } finally {
    uploadLoading.value = false
    uploading.value = false
  }
}

const resetUploadForm = () => {
  uploadForm.value = { name: '', categoryId: categories.value[0]?.id || 0, content: '', version: '1.0.0' }
  previewContent.value = ''
  selectedFiles.value = []
  uploadProgress.value = 0
}

onMounted(() => {
  fetchCategories()
  fetchTools()
})
</script>

<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-bg">
        <div class="hero-orb hero-orb-1"></div>
        <div class="hero-orb hero-orb-2"></div>
        <div class="hero-orb hero-orb-3"></div>
      </div>
      <div class="hero-content">
        <h1 class="hero-title inline-title">
          <span>发现</span>
          <span class="gradient-text">AI</span>
          <span>的无限可能</span>
        </h1>
        <p class="hero-subtitle">探索、分享、协作 — 找到最适合你的 AI 助手</p>
      </div>
    </section>

    <!-- Filter Section -->
    <section class="filter-section">
      <div class="app-container">
        <div class="filter-bar glass-card">
          <!-- Search -->
          <div class="search-wrapper">
            <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              placeholder="搜索工具名称..."
              @keyup.enter="handleSearch"
            />
          </div>

          <!-- Category Pills + Tab Pills -->
          <div class="category-pills">
            <!-- "全部" pill — only in 'all' tab context makes sense, but always visible as a reset -->
            <button
              class="category-pill"
              :class="{ active: activeTab === 'all' && selectedCategory === null }"
              @click="handleTabChange('all'); selectedCategory = null"
            >
              全部
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              class="category-pill"
              :class="{ active: activeTab === 'all' && selectedCategory === cat.id }"
              @click="handleCategoryChange(cat.id); activeTab = 'all'"
            >
              <span class="cat-icon">{{ cat.icon }}</span>
              {{ cat.name }}
            </button>

            <!-- Right-aligned personal pills -->
            <div class="pills-right">
              <button
                v-if="authStore.isLoggedIn"
                class="category-pill personal-pill"
                :class="{ active: activeTab === 'favorites' }"
                @click="handleTabChange('favorites')"
              >
                <Bookmark :size="14" />
                我的收藏
              </button>
              <button
                v-if="authStore.isLoggedIn"
                class="category-pill personal-pill"
                :class="{ active: activeTab === 'myTools' }"
                @click="handleTabChange('myTools')"
              >
                <Wrench :size="14" />
                我的工具
              </button>

              <!-- Upload icon button -->
              <button class="upload-icon-btn" @click="openUploadModal" title="上传新工具">
                <Upload :size="16" />
              </button>
            </div>
          </div>

          <!-- Sort -->
          <div class="sort-wrapper">
            <select v-model="sortBy" class="sort-select" @change="handleSortChange(sortBy)">
              <option value="latest">最新上传</option>
              <option value="name">按名称</option>
            </select>
            <svg class="select-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </div>
        </div>
      </div>
    </section>

    <!-- Tools Grid (no sidebar) -->
    <section class="tools-section">
      <div class="app-container">
        <div class="tools-content">
          <!-- Loading State -->
          <div v-if="loading" class="tools-grid">
            <div v-for="i in 8" :key="i" class="tool-card-skeleton glass-card">
              <div class="skeleton-header"></div>
              <div class="skeleton-title"></div>
              <div class="skeleton-meta"></div>
            </div>
          </div>

          <!-- Tools Grid -->
          <div v-else-if="tools.length > 0" class="tools-grid stagger-children">
            <div
              v-for="tool in tools"
              :key="tool.id"
              class="tool-card glass-card"
              @click="goToDetail(tool.id)"
            >
              <div class="tool-card-inner">
                <div class="tool-category-tag">
                  <span class="cat-icon">{{ tool.categoryIcon }}</span>
                  <span>{{ tool.categoryName }}</span>
                </div>
                <h3 class="tool-name">{{ tool.name }}</h3>
                <div class="tool-footer">
                  <div class="tool-uploader">
                    <AuthorBadge
                      :username="tool.uploaderUsername"
                      :nickname="tool.uploaderNickname"
                      size="sm"
                    />
                  </div>
                  <span class="tool-date">{{ new Date(tool.createdAt).toLocaleDateString('zh-CN') }}</span>
                </div>
              </div>
              <div class="tool-card-glow"></div>
              <div class="tool-card-actions" v-if="canModifyTool(tool)">
                <button class="btn-icon-edit" aria-label="编辑工具" @click.stop="handleToolEdit(tool.id)">
                  <Pencil :size="16" />
                </button>
                <button class="btn-icon-delete" aria-label="删除工具" @click.stop="handleToolDeleteClick(tool.id)">
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="empty-state glass-card">
            <svg class="empty-icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 15s1.5-2 4-2 4 2 4 2M9 9h.01M15 9h.01"/>
            </svg>
            <h3 class="empty-title">
              {{ activeTab === 'favorites' ? '暂无收藏' : activeTab === 'myTools' ? '暂无工具' : '暂无工具' }}
            </h3>
            <p class="empty-desc">
              {{ activeTab === 'favorites' ? '还没有收藏任何工具，去发现喜欢的工具吧' : activeTab === 'myTools' ? '还没有上传任何工具，快来上传第一个工具吧' : '还没有任何工具，快来成为第一个上传者吧' }}
            </p>
          </div>

          <!-- Pagination -->
          <div v-if="pagination.totalPages > 1" class="pagination-wrapper">
            <div class="pagination glass-card">
              <button class="page-btn" :disabled="pagination.page === 0" @click="handlePageChange(pagination.page)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M15 18l-6-6 6-6"/>
                </svg>
              </button>
              <div class="page-numbers">
                <button
                  v-for="p in pagination.totalPages"
                  :key="p"
                  class="page-number"
                  :class="{ active: pagination.page === p - 1 }"
                  @click="handlePageChange(p)"
                >
                  {{ p }}
                </button>
              </div>
              <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="handlePageChange(pagination.page + 2)">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 18l6-6-6-6"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- MCP Float Button -->
    <button class="mcp-float-btn" @click="showMcpModal = true">
      <span class="mcp-float-icon">MCP</span>
      <span class="mcp-float-text">配置到CodeBuddy</span>
    </button>

    <!-- MCP Modal -->
    <Teleport to="body">
      <div v-if="showMcpModal" class="mcp-modal-overlay" @click.self="showMcpModal = false">
        <div class="mcp-modal glass-card">
          <div class="mcp-modal-header">
            <div class="mcp-modal-title">
              <span class="mcp-badge">SSE</span>
              MCP 配置
            </div>
            <button class="mcp-modal-close" @click="showMcpModal = false">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>
          <div class="mcp-modal-body">
            <p class="mcp-modal-desc">将以下配置添加到 CodeBuddy 的 MCP 配置中：</p>
            <pre class="mcp-code-block">{{ mcpConfigJson }}</pre>
          </div>
          <div class="mcp-modal-footer">
            <button class="mcp-copy-btn" :class="{ success: copySuccess }" @click="copyMcpConfig">
              <svg v-if="!copySuccess" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2"/>
                <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
              </svg>
              <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 6L9 17l-5-5"/>
              </svg>
              {{ copySuccess ? '已复制' : '一键复制' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Upload Tool Modal -->
    <Teleport to="body">
      <div v-if="showUploadModal" class="upload-modal-overlay" @click.self="closeUploadModal">
        <div class="upload-modal glass-card">
          <div class="upload-modal-header">
            <h2 class="upload-modal-title">
              <Upload :size="20" />
              上传新工具
            </h2>
            <button class="upload-modal-close" @click="closeUploadModal">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div class="upload-modal-body">
            <form class="upload-form" @submit.prevent="handleUploadSubmit">
              <!-- Tool Name -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-icon">✨</span>
                  工具名称
                </label>
                <div class="input-wrapper">
                  <input v-model="uploadForm.name" type="text" class="form-input" placeholder="给工具起个好听的名字" maxlength="100" />
                  <span class="char-count">{{ uploadForm.name.length }}/100</span>
                </div>
              </div>

              <!-- Category -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-icon">📁</span>
                  分类
                </label>
                <div class="select-wrapper">
                  <select v-model="uploadForm.categoryId" class="form-select">
                    <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.icon }} {{ cat.name }}</option>
                  </select>
                  <svg class="select-arrow-inner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
                  <input v-model="uploadForm.version" type="text" class="form-input" placeholder="如 1.0.0" maxlength="50" />
                  <span class="char-count">{{ uploadForm.version.length }}/50</span>
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
                  v-model="uploadForm.content"
                  class="form-textarea"
                  placeholder="详细介绍工具的功能、使用方法、特点等..."
                  maxlength="5000"
                  @input="renderedPreview"
                ></textarea>
                <div class="textarea-footer">
                  <span class="char-count static">{{ uploadForm.content.length }}/5000</span>
                </div>
              </div>

              <!-- Preview -->
              <div v-if="previewContent" class="preview-section">
                <div class="preview-header">
                  <span>👁️</span>
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
                  <input ref="fileInputRef" type="file" multiple accept=".*" class="file-input-hidden" @change="handleFileSelect" />
                  <div class="upload-hint">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
                    </svg>
                    <span>点击选择文件或将文件拖拽到此处</span>
                    <span class="upload-hint-ext">支持任意格式文件（单文件 ≤ 50MB，单次请求 ≤ 200MB）</span>
                  </div>
                </div>

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

                <div v-if="uploading" class="upload-progress">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
                  </div>
                  <span class="progress-text">上传中... {{ uploadProgress }}%</span>
                </div>
              </div>

              <!-- Form Actions -->
              <div class="form-actions">
                <button type="button" class="reset-btn" @click="closeUploadModal">取消</button>
                <button
                  type="submit"
                  class="submit-btn"
                  :disabled="uploadLoading || uploading || !uploadForm.name || !uploadForm.content || !uploadForm.version"
                >
                  <span v-if="uploadLoading || uploading" class="loading-spinner"></span>
                  <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                  </svg>
                  {{ uploading ? '上传中...' : '提交工具' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Deerflow Branding -->
    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">
      ✦ Created By Deerflow
    </a>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      :visible="deleteDialogVisible"
      title="删除工具"
      description="确定要删除此工具吗？此操作不可恢复。"
      confirm-text="确认删除"
      :danger="true"
      :loading="deleting"
      @confirm="handleConfirmDelete"
      @cancel="deleteDialogVisible = false"
      @update:visible="deleteDialogVisible = $event"
    />
  </div>
</template>

<style scoped>
.home-page {
  min-height: calc(100vh - 60px);
  padding-bottom: 80px;
}

/* Hero */
.hero {
  position: relative;
  padding: 40px 24px 32px;
  text-align: center;
  overflow: hidden;
}

.hero-bg { position: absolute; inset: 0; pointer-events: none; }
.hero-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.5; }
.hero-orb-1 { width: 300px; height: 300px; background: rgba(139, 92, 246, 0.3); top: -80px; left: 10%; animation: float 8s ease-in-out infinite; }
.hero-orb-2 { width: 200px; height: 200px; background: rgba(6, 182, 212, 0.25); top: 20px; right: 15%; animation: float 10s ease-in-out infinite reverse; }
.hero-orb-3 { width: 180px; height: 180px; background: rgba(236, 72, 153, 0.2); bottom: -40px; left: 50%; transform: translateX(-50%); animation: float 12s ease-in-out infinite; }
.hero-content { position: relative; z-index: 1; max-width: 800px; margin: 0 auto; }
.hero-title { font-size: 48px; font-weight: 800; line-height: 1.1; letter-spacing: -2px; margin-bottom: 16px; display: flex; justify-content: center; align-items: center; gap: 12px; flex-wrap: wrap; }
.inline-title span { display: inline-block; }
.gradient-text { background: linear-gradient(135deg, #8b5cf6, #06b6d4, #ec4899); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; background-size: 200% 200%; animation: gradientShift 4s ease infinite; }
@keyframes gradientShift { 0%, 100% { background-position: 0% 50%; } 50% { background-position: 100% 50%; } }
.hero-subtitle { font-size: 18px; color: var(--text-secondary); font-weight: 400; }

/* Filter Section */
.filter-section { padding: 0 0 40px; }
.filter-bar { display: flex; align-items: center; gap: 20px; padding: 16px 20px; flex-wrap: wrap; }

.search-wrapper { position: relative; flex: 0 0 280px; }
.search-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); }
.search-input { width: 100%; padding: 12px 12px 12px 44px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; outline: none; transition: all 0.25s ease; }
.search-input:focus { border-color: rgba(139, 92, 246, 0.5); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1); }
.search-input::placeholder { color: var(--text-muted); }

.category-pills { display: flex; gap: 8px; flex-wrap: wrap; flex: 1; align-items: center; }

.category-pill {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}
.category-pill:hover { background: rgba(139, 92, 246, 0.1); border-color: rgba(139, 92, 246, 0.3); color: var(--text-primary); }
.category-pill.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.15);
}
.cat-icon { font-size: 14px; }

/* Right-aligned personal pills */
.pills-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
  align-items: center;
}

.personal-pill {
  gap: 5px;
}

.upload-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.15));
  border: 1px solid rgba(139, 92, 246, 0.35);
  border-radius: 50%;
  color: var(--accent-1);
  cursor: pointer;
  transition: all 0.25s ease;
  flex-shrink: 0;
}
.upload-icon-btn:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.3), rgba(6, 182, 212, 0.3));
  border-color: rgba(139, 92, 246, 0.6);
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.25);
}

.sort-wrapper { position: relative; flex: 0 0 140px; }
.sort-select { width: 100%; padding: 10px 36px 10px 14px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 8px; color: var(--text-primary); font-family: var(--font-display); font-size: 13px; cursor: pointer; outline: none; appearance: none; transition: all 0.25s ease; }
.sort-select:focus { border-color: rgba(139, 92, 246, 0.5); }
.select-arrow { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }

/* Tools Grid */
.tools-content { min-width: 0; }
.tools-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 24px; }

.tool-card { position: relative; cursor: pointer; overflow: hidden; }
.tool-card-inner { padding: 24px; position: relative; z-index: 1; }
.tool-card-glow { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(6, 182, 212, 0.05)); opacity: 0; transition: opacity 0.3s ease; }
.tool-card:hover .tool-card-glow { opacity: 1; }
.tool-card-actions { position: absolute; top: 12px; right: 12px; display: flex; gap: 6px; z-index: 2; opacity: 0.35; transition: opacity 200ms ease; }
.tool-card:hover .tool-card-actions { opacity: 1; }

.btn-icon-edit, .btn-icon-delete { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border-radius: 8px; border: 1.5px solid var(--border-color); background: var(--bg-glass); color: var(--text-muted); cursor: pointer; transition: all 200ms ease; }
.btn-icon-edit:hover { color: var(--accent-1); border-color: color-mix(in srgb, var(--accent-1) 30%, transparent); }
.btn-icon-delete:hover { color: var(--color-destructive); border-color: color-mix(in srgb, var(--color-destructive) 30%, transparent); }

.tool-category-tag { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.2); border-radius: 16px; font-size: 12px; color: var(--accent-1); margin-bottom: 16px; }
.tool-name { font-size: 20px; font-weight: 600; color: var(--text-primary); margin-bottom: 20px; line-height: 1.3; }
.tool-footer { display: flex; justify-content: space-between; align-items: center; }
.tool-uploader { display: flex; align-items: center; gap: 8px; }
.tool-date { font-size: 12px; color: var(--text-muted); font-family: var(--font-mono); }

/* Loading Skeleton */
.tool-card-skeleton { padding: 24px; }
.skeleton-header { width: 100px; height: 28px; background: rgba(255, 255, 255, 0.05); border-radius: 14px; margin-bottom: 16px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-title { width: 70%; height: 24px; background: rgba(255, 255, 255, 0.05); border-radius: 6px; margin-bottom: 12px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-meta { width: 50%; height: 16px; background: rgba(255, 255, 255, 0.03); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }

/* Empty State */
.empty-state { text-align: center; padding: 80px 40px; }
.empty-icon { color: var(--text-muted); margin-bottom: 20px; }
.empty-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: var(--text-secondary); }

/* Pagination */
.pagination-wrapper { display: flex; justify-content: center; margin-top: 48px; }
.pagination { display: flex; align-items: center; gap: 8px; padding: 8px; }
.page-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 8px; color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease; }
.page-btn:hover:not(:disabled) { background: rgba(139, 92, 246, 0.1); border-color: rgba(139, 92, 246, 0.3); color: var(--text-primary); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-numbers { display: flex; gap: 4px; }
.page-number { min-width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: transparent; border: 1px solid transparent; border-radius: 8px; color: var(--text-secondary); font-family: var(--font-mono); font-size: 13px; cursor: pointer; transition: all 0.2s ease; }
.page-number:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.page-number.active { background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2)); border-color: rgba(139, 92, 246, 0.4); color: var(--text-primary); box-shadow: 0 0 16px rgba(139, 92, 246, 0.2); }

/* Deerflow Branding */
.deerflow-brand { position: fixed; bottom: 20px; right: 20px; color: var(--text-muted); font-size: 12px; text-decoration: none; transition: color 0.2s ease; z-index: 10; }
.deerflow-brand:hover { color: var(--text-secondary); }

/* MCP Float Button */
.mcp-float-btn { position: fixed; right: 24px; bottom: 120px; display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 12px 16px; background: linear-gradient(135deg, rgba(139, 92, 246, 0.9), rgba(6, 182, 212, 0.9)); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; cursor: pointer; z-index: 100; transition: all 0.25s ease; box-shadow: 0 4px 20px rgba(139, 92, 246, 0.3); }
.mcp-float-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 28px rgba(139, 92, 246, 0.4); }
.mcp-float-icon { font-family: 'Fira Code', monospace; font-size: 14px; font-weight: 700; color: white; letter-spacing: 1px; }
.mcp-float-text { font-size: 10px; color: rgba(255, 255, 255, 0.85); white-space: nowrap; }

/* MCP Modal */
.mcp-modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease; }
.mcp-modal { width: 90%; max-width: 520px; padding: 0; animation: slideUp 0.3s ease; }
.mcp-modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border-color); }
.mcp-modal-title { display: flex; align-items: center; gap: 12px; font-size: 18px; font-weight: 600; color: var(--text-primary); }
.mcp-badge { padding: 4px 10px; background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2)); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: 6px; font-size: 11px; font-weight: 600; color: var(--accent-1); font-family: var(--font-mono); }
.mcp-modal-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; border-radius: 8px; color: var(--text-muted); cursor: pointer; transition: all 0.2s ease; }
.mcp-modal-close:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.mcp-modal-body { padding: 24px; }
.mcp-modal-desc { font-size: 14px; color: var(--text-secondary); margin-bottom: 16px; }
.mcp-code-block { padding: 16px; background: rgba(0, 0, 0, 0.4); border: 1px solid var(--border-color); border-radius: 8px; font-family: 'Fira Code', monospace; font-size: 13px; color: #ffffff; line-height: 1.6; overflow-x: auto; white-space: pre; }
.mcp-modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-color); display: flex; justify-content: flex-end; }
.mcp-copy-btn { display: flex; align-items: center; gap: 8px; padding: 10px 20px; background: linear-gradient(135deg, rgba(139, 92, 246, 0.8), rgba(6, 182, 212, 0.8)); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 8px; color: white; font-family: var(--font-display); font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.25s ease; }
.mcp-copy-btn:hover { opacity: 0.9; transform: translateY(-1px); }
.mcp-copy-btn.success { background: linear-gradient(135deg, rgba(34, 197, 94, 0.8), rgba(6, 182, 212, 0.8)); }

/* Upload Modal */
.upload-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.upload-modal {
  width: 90%;
  max-width: 680px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s ease;
}

.upload-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 28px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.upload-modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.upload-modal-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}
.upload-modal-close:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }

.upload-modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

.upload-form { display: flex; flex-direction: column; gap: 20px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-label { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 500; color: var(--text-primary); }
.label-icon { font-size: 16px; }
.input-wrapper { position: relative; }
.input-hint { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.form-input { width: 100%; padding: 12px 56px 12px 14px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; outline: none; transition: all 0.25s ease; box-sizing: border-box; }
.form-input:focus { border-color: rgba(139, 92, 246, 0.5); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1); }
.form-input::placeholder { color: var(--text-muted); }
.char-count { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); font-size: 12px; color: var(--text-muted); font-family: var(--font-mono); }
.char-count.static { position: static; transform: none; }

.select-wrapper { position: relative; }
.form-select { width: 100%; padding: 12px 36px 12px 14px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; cursor: pointer; outline: none; appearance: none; transition: all 0.25s ease; box-sizing: border-box; }
.form-select:focus { border-color: rgba(139, 92, 246, 0.5); }
.select-arrow-inner { position: absolute; right: 14px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }

.form-textarea { width: 100%; min-height: 120px; padding: 12px 14px; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-primary); font-family: var(--font-display); font-size: 14px; line-height: 1.6; outline: none; resize: vertical; transition: all 0.25s ease; box-sizing: border-box; }
.form-textarea:focus { border-color: rgba(139, 92, 246, 0.5); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1); }
.form-textarea::placeholder { color: var(--text-muted); }
.textarea-footer { display: flex; justify-content: flex-end; }

/* Upload Modal - Preview */
.preview-section { background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); border-radius: 10px; overflow: hidden; }
.preview-header { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: rgba(139, 92, 246, 0.05); border-bottom: 1px solid var(--border-color); font-size: 13px; font-weight: 500; color: var(--text-secondary); }
.preview-content { padding: 16px; max-height: 200px; overflow-y: auto; }
.markdown-body { line-height: 1.6; color: var(--text-secondary); font-size: 13px; }
.markdown-body :deep(h1) { font-size: 18px; margin: 12px 0 8px; color: var(--text-primary); font-weight: 600; }
.markdown-body :deep(h2) { font-size: 16px; margin: 10px 0 6px; color: var(--text-primary); font-weight: 600; }
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(code) { background: rgba(139, 92, 246, 0.1); padding: 2px 5px; border-radius: 4px; font-family: var(--font-mono); font-size: 12px; color: var(--accent-2); }
.markdown-body :deep(pre) { background: var(--bg-secondary); padding: 12px; border-radius: 6px; overflow-x: auto; }
.markdown-body :deep(pre code) { background: transparent; padding: 0; color: var(--text-primary); }

/* Upload Modal - File Upload */
.file-input-hidden { display: none; }
.file-upload-area { border: 2px dashed var(--border-color); border-radius: 10px; padding: 16px; text-align: center; cursor: pointer; transition: all 0.25s ease; }
.file-upload-area:hover { border-color: rgba(139, 92, 246, 0.5); background: rgba(139, 92, 246, 0.05); }
.upload-hint { display: flex; flex-direction: column; align-items: center; gap: 6px; color: var(--text-secondary); font-size: 13px; }
.upload-hint svg { color: var(--accent-1); }
.upload-hint-ext { font-size: 11px; color: var(--text-muted); }

.file-list { background: rgba(255, 255, 255, 0.02); border: 1px solid var(--border-color); border-radius: 10px; overflow: hidden; margin-top: 8px; }
.file-list-header { display: flex; align-items: center; gap: 12px; padding: 8px 14px; background: rgba(139, 92, 246, 0.05); border-bottom: 1px solid var(--border-color); font-size: 12px; color: var(--text-secondary); }
.total-size { margin-left: auto; color: var(--accent-1); font-size: 11px; }
.clear-files-btn { background: transparent; border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 4px; color: #ef4444; font-size: 11px; padding: 2px 8px; cursor: pointer; transition: all 0.2s ease; }
.clear-files-btn:hover { background: rgba(239, 68, 68, 0.1); }

.file-item { display: flex; align-items: center; gap: 10px; padding: 8px 14px; border-bottom: 1px solid var(--border-color); font-size: 12px; }
.file-item:last-child { border-bottom: none; }
.file-item svg { color: var(--text-muted); flex-shrink: 0; }
.file-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary); }
.file-size { color: var(--text-muted); font-size: 11px; flex-shrink: 0; }
.remove-file-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; padding: 3px; border-radius: 4px; display: flex; align-items: center; justify-content: center; transition: all 0.2s ease; }
.remove-file-btn:hover { color: #ef4444; background: rgba(239, 68, 68, 0.1); }

.upload-progress { margin-top: 8px; display: flex; flex-direction: column; gap: 6px; }
.progress-bar { height: 6px; background: rgba(255, 255, 255, 0.1); border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, var(--accent-1), var(--accent-2)); border-radius: 3px; transition: width 0.3s ease; }
.progress-text { font-size: 12px; color: var(--text-secondary); text-align: center; }

/* Upload Modal - Actions */
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; padding-top: 16px; border-top: 1px solid var(--border-color); }
.reset-btn { display: flex; align-items: center; gap: 8px; padding: 10px 18px; background: transparent; border: 1px solid var(--border-color); border-radius: 10px; color: var(--text-secondary); font-family: var(--font-display); font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.25s ease; }
.reset-btn:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.submit-btn { display: flex; align-items: center; gap: 8px; padding: 10px 22px; background: linear-gradient(135deg, var(--accent-1), var(--accent-2)); border: none; border-radius: 10px; color: white; font-family: var(--font-display); font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.25s ease; }
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

.loading-spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.6s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

/* Light theme overrides */
[data-theme="light"] .upload-modal,
[data-theme="light"] .mcp-modal {
  background: rgba(255, 255, 255, 0.95);
}

[data-theme="light"] .form-input,
[data-theme="light"] .form-select,
[data-theme="light"] .form-textarea {
  background: rgba(0, 0, 0, 0.02);
}

[data-theme="light"] .mcp-code-block {
  background: rgba(0, 0, 0, 0.05);
  color: #1a1a2e;
}

@media (max-width: 768px) {
  .hero-title { font-size: 32px; gap: 8px; }
  .filter-bar { flex-direction: column; align-items: stretch; }
  .search-wrapper { flex: none; width: 100%; }
  .sort-wrapper { flex: none; width: 100%; }
  .pills-right { margin-left: 0; margin-top: 4px; }
  .tools-grid { grid-template-columns: 1fr; }
  .mcp-float-btn { right: 16px; bottom: 50px; padding: 10px 14px; }
  .mcp-modal { width: 95%; margin: 16px; }
  .upload-modal { width: 95%; max-height: 90vh; }
  .upload-modal-body { padding: 16px 20px; }
}
</style>
