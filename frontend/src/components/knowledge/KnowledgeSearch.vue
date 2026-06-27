<script setup lang="ts">
import { ref } from 'vue'
import { Search, Loader2, FileText } from '@lucide/vue'
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
const hintVisible = ref(true)

const handleSearch = async () => {
  if (!query.value.trim()) return
  searching.value = true
  searched.value = true
  try {
    results.value = await knowledgeService.search(props.kbId, {
      query: query.value.trim(),
      topK: topK.value,
      rerank: rerank.value,
      expandContext: 1,
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
          <p class="result-text">{{ result.text }}</p>
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
  white-space: pre-wrap;
  word-break: break-word;
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
