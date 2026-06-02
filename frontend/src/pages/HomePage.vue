<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import type { ToolSummary, Category, PageResponse } from '@/types'
import AuthorBadge from '@/components/AuthorBadge.vue'

const router = useRouter()

const tools = ref<ToolSummary[]>([])
const categories = ref<Category[]>([])
const selectedCategory = ref<number | null>(null)
const searchKeyword = ref('')
const sortBy = ref('latest')
const loading = ref(false)
const showMcpModal = ref(false)
const copySuccess = ref(false)
const pagination = ref({
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0
})

const mcpConfig = {
  "CodingHub-mcp": {
    type: "sse",
    url: `http://${window.location.hostname}:8080/sse`,
    description: "CodingHub MCP Server"
  }
}

const mcpConfigJson = JSON.stringify(mcpConfig, null, 2)

const copyMcpConfig = async () => {
  try {
    await navigator.clipboard.writeText(mcpConfigJson)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (error) {
    console.error('Failed to copy:', error)
  }
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

const fetchTools = async () => {
  loading.value = true
  try {
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
    pagination.value = {
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages
    }
  } catch (error) {
    console.error('Failed to fetch tools:', error)
  } finally {
    loading.value = false
  }
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

const goToDetail = (toolId: number) => {
  router.push(`/tools/${toolId}`)
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

          <!-- Category Pills -->
          <div class="category-pills">
            <button
              class="category-pill"
              :class="{ active: selectedCategory === null }"
              @click="handleCategoryChange(null)"
            >
              全部
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              class="category-pill"
              :class="{ active: selectedCategory === cat.id }"
              @click="handleCategoryChange(cat.id)"
            >
              <span class="cat-icon">{{ cat.icon }}</span>
              {{ cat.name }}
            </button>
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

    <!-- Tools Grid -->
    <section class="tools-section">
      <div class="app-container">
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
          </div>
        </div>

        <!-- Empty State -->
        <div v-else class="empty-state glass-card">
          <svg class="empty-icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M8 15s1.5-2 4-2 4 2 4 2M9 9h.01M15 9h.01"/>
          </svg>
          <h3 class="empty-title">暂无工具</h3>
          <p class="empty-desc">还没有任何工具，快来成为第一个上传者吧</p>
        </div>

        <!-- Pagination -->
        <div v-if="pagination.totalPages > 1" class="pagination-wrapper">
          <div class="pagination glass-card">
            <button
              class="page-btn"
              :disabled="pagination.page === 0"
              @click="handlePageChange(pagination.page)"
            >
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
            <button
              class="page-btn"
              :disabled="pagination.page >= pagination.totalPages - 1"
              @click="handlePageChange(pagination.page + 2)"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 18l6-6-6-6"/>
              </svg>
            </button>
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

    <!-- Deerflow Branding -->
    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">
      ✦ Created By Deerflow
    </a>
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

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.hero-orb-1 {
  width: 300px;
  height: 300px;
  background: rgba(139, 92, 246, 0.3);
  top: -80px;
  left: 10%;
  animation: float 8s ease-in-out infinite;
}

.hero-orb-2 {
  width: 200px;
  height: 200px;
  background: rgba(6, 182, 212, 0.25);
  top: 20px;
  right: 15%;
  animation: float 10s ease-in-out infinite reverse;
}

.hero-orb-3 {
  width: 180px;
  height: 180px;
  background: rgba(236, 72, 153, 0.2);
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  animation: float 12s ease-in-out infinite;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

.hero-title {
  font-size: 48px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -2px;
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.inline-title span {
  display: inline-block;
}

.gradient-text {
  background: linear-gradient(135deg, #8b5cf6, #06b6d4, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  background-size: 200% 200%;
  animation: gradientShift 4s ease infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.hero-subtitle {
  font-size: 18px;
  color: var(--text-secondary);
  font-weight: 400;
}

/* Filter Section */
.filter-section {
  padding: 0 0 40px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  flex-wrap: wrap;
}

.search-wrapper {
  position: relative;
  flex: 0 0 280px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
}

.search-input {
  width: 100%;
  padding: 12px 12px 12px 44px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  outline: none;
  transition: all 0.25s ease;
}

.search-input:focus {
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.search-input::placeholder {
  color: var(--text-muted);
}

.category-pills {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  flex: 1;
}

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

.category-pill:hover {
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.3);
  color: var(--text-primary);
}

.category-pill.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.15);
}

.cat-icon {
  font-size: 14px;
}

.sort-wrapper {
  position: relative;
  flex: 0 0 140px;
}

.sort-select {
  width: 100%;
  padding: 10px 36px 10px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 13px;
  cursor: pointer;
  outline: none;
  appearance: none;
  transition: all 0.25s ease;
}

.sort-select:focus {
  border-color: rgba(139, 92, 246, 0.5);
}

.select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

/* Tools Grid */
.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.tool-card {
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.tool-card-inner {
  padding: 24px;
  position: relative;
  z-index: 1;
}

.tool-card-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(6, 182, 212, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.tool-card:hover .tool-card-glow {
  opacity: 1;
}

.tool-category-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(139, 92, 246, 0.1);
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 16px;
  font-size: 12px;
  color: var(--accent-1);
  margin-bottom: 16px;
}

.tool-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
  line-height: 1.3;
}

.tool-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tool-uploader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, var(--accent-2), var(--accent-3));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: white;
}

.uploader-name {
  font-size: 13px;
  color: var(--text-secondary);
}

.tool-date {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

/* Loading Skeleton */
.tool-card-skeleton {
  padding: 24px;
}

.skeleton-header {
  width: 100px;
  height: 28px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  margin-bottom: 16px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-title {
  width: 70%;
  height: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  margin-bottom: 12px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-meta {
  width: 50%;
  height: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 80px 40px;
}

.empty-icon {
  color: var(--text-muted);
  margin-bottom: 20px;
}

.empty-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 48px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
}

.page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.1);
  border-color: rgba(139, 92, 246, 0.3);
  color: var(--text-primary);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 4px;
}

.page-number {
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-number:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.page-number.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
  box-shadow: 0 0 16px rgba(139, 92, 246, 0.2);
}

/* Deerflow Branding */
.deerflow-brand {
  position: fixed;
  bottom: 20px;
  right: 20px;
  color: var(--text-muted);
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s ease;
  z-index: 10;
}

.deerflow-brand:hover {
  color: var(--text-secondary);
}

/* MCP Float Button */
.mcp-float-btn {
  position: fixed;
  right: 24px;
  bottom: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.9), rgba(6, 182, 212, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  cursor: pointer;
  z-index: 100;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.3);
}

.mcp-float-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(139, 92, 246, 0.4);
}

.mcp-float-icon {
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  font-weight: 700;
  color: white;
  letter-spacing: 1px;
}

.mcp-float-text {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
}

/* MCP Modal */
.mcp-modal-overlay {
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

.mcp-modal {
  width: 90%;
  max-width: 520px;
  padding: 0;
  animation: slideUp 0.3s ease;
}

.mcp-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.mcp-modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.mcp-badge {
  padding: 4px 10px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-1);
  font-family: var(--font-mono);
}

.mcp-modal-close {
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

.mcp-modal-close:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
}

.mcp-modal-body {
  padding: 24px;
}

.mcp-modal-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.mcp-code-block {
  padding: 16px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
  color: #ffffff;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre;
}

.mcp-modal-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
}

.mcp-copy-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.8), rgba(6, 182, 212, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: white;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.mcp-copy-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.mcp-copy-btn.success {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.8), rgba(6, 182, 212, 0.8));
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
    gap: 8px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-wrapper {
    flex: none;
    width: 100%;
  }

  .sort-wrapper {
    flex: none;
    width: 100%;
  }

  .tools-grid {
    grid-template-columns: 1fr;
  }

  .mcp-float-btn {
    right: 16px;
    bottom: 50px;
    padding: 10px 14px;
  }

  .mcp-modal {
    width: 95%;
    margin: 16px;
  }
}
</style>