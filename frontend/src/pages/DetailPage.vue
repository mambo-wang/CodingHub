<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import api from '@/services/api'
import type { ToolDetail } from '@/types'

const route = useRoute()
const router = useRouter()

const tool = ref<ToolDetail | null>(null)
const loading = ref(false)
const error = ref(false)

// Markdown renderer with syntax highlighting
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

const fetchTool = async () => {
  loading.value = true
  error.value = false
  try {
    const response = await api.get(`/tools/${route.params.id}`)
    tool.value = response.data.data
  } catch (err: any) {
    if (err.response?.status === 404) {
      error.value = true
    }
    ElMessage.error('加载工具详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  fetchTool()
})
</script>

<template>
  <div class="detail-page">
    <div class="app-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <!-- Error State -->
      <el-result
        v-else-if="error"
        icon="error"
        title="工具不存在或已删除"
        sub-title="该工具可能已被删除或链接无效"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回首页</el-button>
        </template>
      </el-result>

      <!-- Tool Detail -->
      <div v-else-if="tool" class="tool-detail">
        <div class="tool-header">
          <div class="tool-info">
            <span class="category-badge">
              {{ tool.categoryIcon }} {{ tool.categoryName }}
            </span>
            <h1 class="tool-title">{{ tool.name }}</h1>
            <div class="tool-meta">
              <span class="uploader">👤 {{ tool.uploaderUsername }}</span>
              <span class="separator">•</span>
              <span class="date">上传于 {{ new Date(tool.createdAt).toLocaleDateString() }}</span>
              <span v-if="tool.updatedAt !== tool.createdAt" class="separator">•</span>
              <span v-if="tool.updatedAt !== tool.createdAt" class="date">
                更新于 {{ new Date(tool.updatedAt).toLocaleDateString() }}
              </span>
            </div>
          </div>
          <el-button @click="goBack">← 返回</el-button>
        </div>

        <div class="tool-content">
          <div class="markdown-body" v-html="renderedContent"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.app-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.loading-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.tool-detail {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.tool-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px;
  border-bottom: 1px solid #ebeef5;
}

.tool-info {
  flex: 1;
}

.category-badge {
  display: inline-block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.tool-title {
  margin: 0 0 12px 0;
  font-size: 28px;
  color: #303133;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #909399;
}

.separator {
  color: #dcdfe6;
}

.tool-content {
  padding: 24px;
}

/* Markdown Styles */
.markdown-body {
  line-height: 1.8;
  color: #303133;
}

.markdown-body :deep(h1) {
  font-size: 24px;
  margin: 24px 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.markdown-body :deep(h2) {
  font-size: 20px;
  margin: 24px 0 12px 0;
}

.markdown-body :deep(h3) {
  font-size: 16px;
  margin: 20px 0 8px 0;
}

.markdown-body :deep(p) {
  margin: 0 0 16px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 16px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin-bottom: 8px;
}

.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #1e1e1e;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0 0 16px 0;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #d4d4d4;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 16px 0;
  padding: 12px 16px;
  border-left: 4px solid #409eff;
  background: #f5f7fa;
  color: #606266;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 16px 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid #ebeef5;
  padding: 8px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}

.markdown-body :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}
</style>
