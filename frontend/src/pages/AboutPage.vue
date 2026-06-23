<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const readmeContent = ref('')
const loading = ref(true)
const error = ref('')

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
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
  if (!readmeContent.value) return ''
  return md.render(readmeContent.value)
})

onMounted(async () => {
  try {
    const response = await fetch('/api/v1/readme')
    if (!response.ok) {
      throw new Error('Failed to load README')
    }
    readmeContent.value = await response.text()
  } catch (e: any) {
    error.value = e.message || 'Failed to load README'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="about-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">关于</h1>
        <p class="page-subtitle">CodingHub - 发现和使用 AI 工具的一站式平台</p>
        <a
          class="github-link"
          href="https://github.com/mambo-wang/CodingHub"
          target="_blank"
          rel="noopener"
        >
          <svg class="github-icon" viewBox="0 0 16 16" width="18" height="18" fill="currentColor">
            <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
          </svg>
          GitHub 源码
        </a>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>Loading...</span>
      </div>

      <div v-else-if="error" class="error-message">
        {{ error }}
      </div>

      <div v-else class="readme-content" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<style scoped>
.about-page {
  min-height: 100vh;
  padding: 32px 24px 64px;
  background: var(--bg-primary);
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.github-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  text-decoration: none;
  transition: all 0.25s ease;
}

.github-link:hover {
  border-color: var(--accent-1);
  box-shadow: 0 0 16px rgba(139, 92, 246, 0.2);
  transform: translateY(-1px);
}

.github-icon {
  flex-shrink: 0;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 64px;
  color: var(--text-secondary);
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-color);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  padding: 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  color: #ef4444;
  text-align: center;
}

.readme-content {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 32px;
  line-height: 1.8;
  font-size: 15px;
  color: var(--text-primary);
}

.readme-content :deep(h1) {
  font-size: 28px;
  font-weight: 700;
  margin: 32px 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.readme-content :deep(h2) {
  font-size: 22px;
  font-weight: 600;
  margin: 24px 0 12px;
}

.readme-content :deep(h3) {
  font-size: 18px;
  font-weight: 600;
  margin: 20px 0 8px;
}

.readme-content :deep(p) {
  margin: 12px 0;
}

.readme-content :deep(blockquote) {
  margin: 0 0 16px;
  padding: 16px 20px;
  border-left: 3px solid var(--accent-1);
  background: rgba(139, 92, 246, 0.05);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
}

.readme-content :deep(blockquote p) {
  margin: 0;
}

.readme-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.readme-content :deep(th),
.readme-content :deep(td) {
  padding: 10px 14px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.readme-content :deep(th) {
  background: rgba(139, 92, 246, 0.08);
  font-weight: 600;
  color: var(--text-primary);
}

.readme-content :deep(tr:last-child td) {
  border-bottom: none;
}

.readme-content :deep(ul),
.readme-content :deep(ol) {
  margin: 8px 0 16px;
  padding-left: 24px;
}

.readme-content :deep(li) {
  margin: 6px 0;
}

.readme-content :deep(code) {
  font-family: 'SF Mono', Monaco, monospace;
  font-size: 13px;
  padding: 2px 6px;
  background: rgba(139, 92, 246, 0.15);
  border-radius: 4px;
  color: var(--accent-1);
}

.readme-content :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
}

.readme-content :deep(pre code) {
  padding: 0;
  background: transparent;
  color: var(--text-primary);
}

.readme-content :deep(strong) {
  font-weight: 600;
  color: var(--text-primary);
}

.readme-content :deep(a) {
  color: var(--accent-1);
  text-decoration: none;
}

.readme-content :deep(a:hover) {
  text-decoration: underline;
}

.readme-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 16px 0;
}

.readme-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 24px 0;
}

.readme-content :deep(em) {
  font-style: italic;
  color: var(--text-secondary);
}
</style>
