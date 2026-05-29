<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import api from '@/services/api'
import type { ToolDetail } from '@/types'

const route = useRoute()
const router = useRouter()

const tool = ref<ToolDetail | null>(null)
const loading = ref(false)
const error = ref(false)

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

const renderedContent = computed(() => {
  if (!tool.value) return ''
  return md.render(tool.value.content)
})

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

const fetchTool = async () => {
  loading.value = true
  error.value = false
  try {
    const response = await api.get(`/tools/${route.params.id}`)
    tool.value = response.data.data
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

const goBack = () => router.push('/')

onMounted(() => {
  fetchTool()
})
</script>

<template>
  <div class="detail-page">
    <!-- Background effects -->
    <div class="detail-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-container glass-card">
        <div class="skeleton-wrapper">
          <div class="skeleton-badge"></div>
          <div class="skeleton-title"></div>
          <div class="skeleton-meta"></div>
          <div class="skeleton-content">
            <div class="skeleton-line"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-container glass-card">
        <div class="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M15 9l-6 6M9 9l6 6"/>
          </svg>
        </div>
        <h2 class="error-title">工具不存在或已删除</h2>
        <p class="error-desc">该工具可能已被删除或链接无效</p>
        <button class="back-btn" @click="goBack">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          返回首页
        </button>
      </div>

      <!-- Tool Detail -->
      <div v-else-if="tool" class="tool-detail glass-card animate-fade-in-up">
        <!-- Header -->
        <div class="tool-header">
          <div class="header-top">
            <button class="back-btn" @click="goBack">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 12H5M12 19l-7-7 7-7"/>
              </svg>
              返回
            </button>
          </div>

          <div class="tool-meta-header">
            <div class="category-tag">
              <span class="cat-icon">{{ tool.categoryIcon }}</span>
              <span>{{ tool.categoryName }}</span>
            </div>

            <h1 class="tool-title">{{ tool.name }}</h1>

            <div class="tool-meta">
              <div class="uploader-info">
                <div class="uploader-avatar">
                  {{ tool.uploaderUsername?.charAt(0).toUpperCase() }}
                </div>
                <span class="uploader-name">{{ tool.uploaderUsername }}</span>
              </div>
              <span class="meta-separator">•</span>
              <span class="meta-date">{{ formatDate(tool.createdAt) }}</span>
              <template v-if="tool.updatedAt !== tool.createdAt">
                <span class="meta-separator">•</span>
                <span class="meta-date">更新于 {{ formatDate(tool.updatedAt) }}</span>
              </template>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="tool-content">
          <div class="markdown-body" v-html="renderedContent"></div>
        </div>
      </div>
    </div>

    <!-- Deerflow Branding -->
    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">
      ✦ Created By Deerflow
    </a>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: calc(100vh - 60px);
  padding: 40px 20px 80px;
  position: relative;
}

.detail-bg {
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
  width: 400px;
  height: 400px;
  background: rgba(139, 92, 246, 0.3);
  top: -100px;
  right: -100px;
}

.bg-orb-2 {
  width: 300px;
  height: 300px;
  background: rgba(6, 182, 212, 0.2);
  bottom: 100px;
  left: -100px;
}

.app-container {
  max-width: 900px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Loading */
.loading-container {
  padding: 48px;
}

.skeleton-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-badge {
  width: 100px;
  height: 28px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-title {
  width: 60%;
  height: 40px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-meta {
  width: 40%;
  height: 20px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-content {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  width: 100%;
  height: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}

.skeleton-line.short {
  width: 70%;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Error */
.error-container {
  text-align: center;
  padding: 80px 40px;
}

.error-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

/* Tool Detail */
.tool-detail {
  overflow: hidden;
}

.tool-header {
  padding: 32px 32px 24px;
  border-bottom: 1px solid var(--border-color);
}

.header-top {
  margin-bottom: 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
  border-color: rgba(255, 255, 255, 0.15);
}

.tool-meta-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.1));
  border: 1px solid rgba(139, 92, 246, 0.2);
  border-radius: 20px;
  font-size: 13px;
  color: var(--accent-1);
  width: fit-content;
}

.cat-icon {
  font-size: 16px;
}

.tool-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.uploader-info {
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
  font-size: 14px;
  color: var(--text-secondary);
}

.meta-separator {
  color: var(--text-muted);
}

.meta-date {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

/* Content */
.tool-content {
  padding: 32px;
}

/* Markdown Styles */
.markdown-body {
  line-height: 1.8;
  color: var(--text-secondary);
}

.markdown-body :deep(h1) {
  font-size: 28px;
  margin: 32px 0 20px;
  color: var(--text-primary);
  font-weight: 600;
  letter-spacing: -0.3px;
}

.markdown-body :deep(h2) {
  font-size: 22px;
  margin: 28px 0 16px;
  color: var(--text-primary);
  font-weight: 600;
}

.markdown-body :deep(h3) {
  font-size: 18px;
  margin: 24px 0 12px;
  color: var(--text-primary);
  font-weight: 600;
}

.markdown-body :deep(p) {
  margin: 0 0 16px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 16px;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin-bottom: 8px;
}

.markdown-body :deep(code) {
  background: rgba(139, 92, 246, 0.1);
  padding: 3px 8px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--accent-2);
}

.markdown-body :deep(pre) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 0 0 20px;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.6;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 16px;
  padding: 16px 20px;
  border-left: 3px solid var(--accent-1);
  background: rgba(139, 92, 246, 0.05);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.markdown-body :deep(th) {
  background: rgba(139, 92, 246, 0.08);
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: none;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
}

.markdown-body :deep(a) {
  color: var(--accent-2);
  text-decoration: none;
  transition: color 0.2s ease;
}

.markdown-body :deep(a:hover) {
  color: var(--accent-1);
}

/* Responsive */
@media (max-width: 768px) {
  .tool-header {
    padding: 24px 20px;
  }

  .tool-content {
    padding: 24px 20px;
  }

  .tool-title {
    font-size: 28px;
  }
}
</style>