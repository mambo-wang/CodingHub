<script setup lang="ts">
import { ref, computed } from 'vue'
import { Play, Loader2, BarChart3 } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import type { ChunkPreviewResponse } from '@/services/knowledge'
import ChunkCard from './ChunkCard.vue'
import StrategyBadge from './StrategyBadge.vue'

const props = defineProps<{
  ragBaseUrl: string
  ragCollection: string
}>()

const text = ref('')
const strategy = ref('auto')
const chunkSize = ref(800)
const chunkOverlap = ref(50)
const loading = ref(false)
const error = ref('')
const result = ref<ChunkPreviewResponse | null>(null)

const strategyOptions = [
  { value: 'auto', label: 'Auto (自动选择)' },
  { value: 'structural', label: 'Structural (结构分块)' },
  { value: 'recursive', label: 'Recursive (递归分块)' },
]

const canRun = computed(() => text.value.trim().length > 0 && !loading.value)

const runPreview = async () => {
  if (!canRun.value) return
  loading.value = true
  error.value = ''
  result.value = null
  try {
    result.value = await knowledgeService.previewChunking(
      props.ragBaseUrl,
      props.ragCollection,
      text.value,
      {
        strategy: strategy.value,
        chunk_size: chunkSize.value,
        chunk_overlap: chunkOverlap.value,
      }
    )
  } catch (e: any) {
    error.value = e?.response?.data?.error || e?.message || '预览请求失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="preview-panel">
    <div class="preview-controls">
      <textarea
        v-model="text"
        class="preview-textarea"
        rows="6"
        placeholder="粘贴或输入文本，预览分片结果..."
      ></textarea>

      <div class="preview-options">
        <div class="option-group">
          <label class="option-label">策略</label>
          <select v-model="strategy" class="option-select">
            <option v-for="opt in strategyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="option-group">
          <label class="option-label">大小</label>
          <input v-model.number="chunkSize" type="number" class="option-input" min="100" max="4000" />
        </div>
        <div class="option-group">
          <label class="option-label">重叠</label>
          <input v-model.number="chunkOverlap" type="number" class="option-input" min="0" max="500" />
        </div>
        <button class="run-btn" :disabled="!canRun" @click="runPreview">
          <Loader2 v-if="loading" :size="14" class="spin" />
          <Play v-else :size="14" />
          <span>{{ loading ? '分析中...' : '预览' }}</span>
        </button>
      </div>
    </div>

    <div v-if="error" class="preview-error">{{ error }}</div>

    <div v-if="result" class="preview-results">
      <div class="results-summary">
        <StrategyBadge :strategy="result.strategy_used" />
        <span class="summary-stat">
          <BarChart3 :size="12" aria-hidden="true" />
          {{ result.stats.total_chunks }} chunks
        </span>
        <span class="summary-stat">avg {{ result.stats.avg_chars }} chars</span>
        <span class="summary-stat">max {{ result.stats.max_chars }}</span>
        <span class="summary-stat">min {{ result.stats.min_chars }}</span>
      </div>

      <div class="chunk-list">
        <ChunkCard
          v-for="chunk in result.chunks"
          :key="chunk.index"
          :index="chunk.index"
          :text="chunk.text"
          :char-count="chunk.char_count"
          :context-header="chunk.context_header || undefined"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: var(--font-mono, monospace);
  line-height: 1.5;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s ease;
}

.preview-textarea:focus {
  border-color: var(--accent-1);
}

.preview-options {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}

.option-select,
.option-input {
  padding: 6px 10px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s ease;
}

.option-select:focus,
.option-input:focus {
  border-color: var(--accent-1);
}

.option-input {
  width: 70px;
}

.run-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: auto;
}

.run-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(139, 92, 246, 0.3);
}

.run-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.preview-error {
  padding: 8px 12px;
  background: #7f1d1d22;
  border: 1px solid #991b1b;
  border-radius: 6px;
  color: #fca5a5;
  font-size: 12px;
}

[data-theme="light"] .preview-error {
  background: #fee2e2;
  border-color: #fca5a5;
  color: #dc2626;
}

.preview-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.results-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 8px 0;
}

.summary-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
  font-family: var(--font-mono, monospace);
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

[data-theme="light"] .preview-textarea,
[data-theme="light"] .option-select,
[data-theme="light"] .option-input {
  background: var(--bg-card);
}
</style>
