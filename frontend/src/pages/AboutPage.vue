<script setup lang="ts">
import { ref, onMounted } from 'vue'

const readmeContent = ref('')
const loading = ref(true)
const error = ref('')

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
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>Loading...</span>
      </div>

      <div v-else-if="error" class="error-message">
        {{ error }}
      </div>

      <div v-else class="readme-content" v-html="renderMarkdown(readmeContent)"></div>
    </div>
  </div>
</template>

<script lang="ts">
// Simple markdown renderer
function renderMarkdown(text: string): string {
  if (!text) return ''
  
  return text
    .replace(/^### (.*$)/gm, '<h3>$1</h3>')
    .replace(/^## (.*$)/gm, '<h2>$1</h2>')
    .replace(/^# (.*$)/gm, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^\- (.*$)/gm, '<li>$1</li>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/^(?!<[hpuol]|<\/|<a)/gm, '<p>')
    .replace(/(?<![>])$/gm, '</p>')
}
</script>

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
</style>
