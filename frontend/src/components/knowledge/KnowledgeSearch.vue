<script setup lang="ts">
import { ref } from 'vue'
import { Search, Loader2, FileText, Settings } from '@lucide/vue'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { knowledgeService } from '@/services/knowledge'
import InfoBanner from './InfoBanner.vue'
import type { KbSearchResult } from '@/types/knowledge'

const props = defineProps<{
  kbId: number
}>()

const query = ref('')
const results = ref<KbSearchResult[]>([])
const searching = ref(false)
const searched = ref(false)
const topK = ref(5)
const rerank = ref(true)
const expandContext = ref(1)
const showConfig = ref(false)
const hintVisible = ref(true)

// Markdown renderer: html:false for XSS safety, highlight.js for code blocks
const md = markdownIt({
  html: false,
  linkify: true,
  typographer: false,
  highlight(str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch { /* fall through */ }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const renderMarkdown = (text: string): string => {
  try {
    return md.render(text)
  } catch {
    // Fallback: escape and return as plain text
    return md.utils.escapeHtml(text)
  }
}

const handleSearch = async () => {
  if (!query.value.trim()) return
  searching.value = true
  searched.value = true
  try {
    results.value = await knowledgeService.search(props.kbId, {
      query: query.value.trim(),
      topK: topK.value,
      rerank: rerank.value,
      expandContext: expandContext.value,
    })
  } catch (e) {
    console.error('Search failed:', e)
    results.value = []
  } finally {
    searching.value = false
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') handleSearch()
}
</script>

<template>
  <div class="knowledge-search">
    <InfoBanner
      v-if="hintVisible"
      message="本页面仅基于向量距离检索相关文档片段，建议使用 AI 编程助手接入 MCP 获得更智能的检索体验。"
      @close="hintVisible = false"
    />

    <div class="search-bar">
      <div class="search-input-wrap">
        <Search :size="18" class="search-icon" aria-hidden="true" />
        <input
          v-model="query"
          type="text"
          class="search-input"
          placeholder="输入问题，语义搜索知识库内容..."
          @keydown="handleKeydown"
        />
        <button
          class="search-btn"
          :disabled="searching || !query.trim()"
          @click="handleSearch"
        >
          <Loader2 v-if="searching" :size="16" class="spin" aria-hidden="true" />
          <span v-else>搜索</span>
        </button>
        <button
          type="button"
          class="config-toggle"
          :class="{ active: showConfig }"
          @click="showConfig = !showConfig"
        >
          <Settings :size="14" aria-hidden="true" />
          <span>检索参数</span>
        </button>
      </div>
    </div>

    <!-- 检索参数配置 -->
    <div v-if="showConfig" class="search-config glass-card">
      <div class="config-row">
        <div class="config-item">
          <label class="config-label">topK (返回数量)</label>
          <input v-model.number="topK" type="number" class="config-input" min="1" max="20" />
        </div>
        <div class="config-item">
          <label class="config-label">expandContext (上下文扩展)</label>
          <input v-model.number="expandContext" type="number" class="config-input" min="0" max="10" />
        </div>
        <div class="config-item">
          <label class="config-label toggle-label">
            <span>重排序 (Rerank)</span>
            <button
              type="button"
              class="toggle-btn"
              :class="{ active: rerank }"
              @click="rerank = !rerank"
            >
              <span class="toggle-knob"></span>
            </button>
          </label>
        </div>
      </div>
    </div>

    <!-- Results -->
    <div v-if="searched && !searching" class="search-results">
      <div v-if="results.length === 0" class="no-results">
        <p>未找到相关内容</p>
      </div>
      <div v-else class="results-list stagger-children">
        <div v-for="(result, idx) in results" :key="idx" class="result-card glass-card">
          <div class="result-header">
            <span class="result-source">
              <FileText :size="14" aria-hidden="true" />
              {{ result.source || '未知来源' }}
            </span>
            <span class="result-score">{{ (result.score * 100).toFixed(1) }}%</span>
          </div>
          <div class="result-text markdown-body" v-html="renderMarkdown(result.text)"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.knowledge-search {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-bar {
  width: 100%;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 4px 4px 4px 14px;
  gap: 8px;
  transition: border-color 0.2s ease;
}

.search-input-wrap:focus-within {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.search-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-display);
  outline: none;
  padding: 10px 0;
  min-width: 0;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 18px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.search-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.config-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.config-toggle:hover {
  border-color: var(--accent-1);
  color: var(--accent-1);
}

.config-toggle.active {
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  color: var(--accent-1);
}

.search-config {
  padding: 16px;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: flex-start;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}

.config-label.toggle-label {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.config-input {
  padding: 6px 10px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: var(--font-display);
  outline: none;
  width: 80px;
  transition: border-color 0.2s ease;
}

.config-input:focus {
  border-color: var(--accent-1);
}

/* Toggle button */
.toggle-btn {
  position: relative;
  width: 36px;
  height: 20px;
  border-radius: 10px;
  border: none;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.toggle-btn.active {
  background: var(--accent-1);
}

.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: white;
  transition: transform 0.2s ease;
}

.toggle-btn.active .toggle-knob {
  transform: translateX(16px);
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-card {
  padding: 16px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.result-source {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--accent-1);
}

.result-score {
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--text-muted);
  background: var(--bg-secondary);
  padding: 2px 8px;
  border-radius: 6px;
}

.result-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
  word-break: break-word;
}

.result-text :deep(h1),
.result-text :deep(h2),
.result-text :deep(h3) {
  color: var(--text-primary);
  margin: 12px 0 8px;
  line-height: 1.4;
}

.result-text :deep(h1) { font-size: 20px; }
.result-text :deep(h2) { font-size: 17px; }
.result-text :deep(h3) { font-size: 15px; }

.result-text :deep(p) {
  margin: 6px 0;
}

.result-text :deep(ul),
.result-text :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}

.result-text :deep(li) {
  margin: 2px 0;
}

.result-text :deep(code) {
  background: var(--inline-code-bg, rgba(255,255,255,0.06));
  padding: 2px 6px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 85%;
}

.result-text :deep(pre) {
  background: #0d1117;
  color: #e6edf3;
  border-radius: 6px;
  padding: 12px 16px;
  margin: 10px 0;
  overflow-x: auto;
  font-size: 13px;
  line-height: 1.5;
}

.result-text :deep(pre code) {
  background: none;
  padding: 0;
  font-size: inherit;
}

.result-text :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
  font-size: 13px;
}

.result-text :deep(th),
.result-text :deep(td) {
  border: 1px solid var(--border-color);
  padding: 6px 10px;
  text-align: left;
}

.result-text :deep(th) {
  background: var(--bg-secondary);
  font-weight: 600;
}

.result-text :deep(blockquote) {
  border-left: 3px solid var(--accent-1);
  padding-left: 12px;
  margin: 8px 0;
  color: var(--text-muted);
}

/* Light theme code block */
:root[data-theme="light"] .result-text :deep(pre) {
  background: #f6f8fa;
  color: #1f2328;
}

:root[data-theme="light"] .result-text :deep(code) {
  background: rgba(0,0,0,0.06);
}

.no-results {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme */
[data-theme="light"] .search-input-wrap {
  background: var(--bg-card);
}

[data-theme="light"] .result-card {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}
</style>
