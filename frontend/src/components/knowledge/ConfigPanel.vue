<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Loader2, Save } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { ElMessage } from 'element-plus'
import type { KbConfig } from '@/types/knowledge'

const props = defineProps<{
  kbId: number
  isOwner: boolean
}>()

const config = ref<KbConfig | null>(null)
const loading = ref(true)
const saving = ref(false)

const form = ref({
  chunkMode: 'structural',
  chunkSize: 800,
  chunkOverlap: 50,
  rerank: true,
  description: '',
})

const chunkModeOptions = [
  { value: 'structural', label: '结构分块 (Structural)' },
  { value: 'semantic', label: '语义分块 (Semantic)' },
  { value: 'recursive', label: '递归分块 (Recursive)' },
]

const loadConfig = async () => {
  loading.value = true
  try {
    const data = await knowledgeService.getConfig(props.kbId)
    config.value = data
    form.value = {
      chunkMode: data.chunk_mode || 'structural',
      chunkSize: data.chunk_size || 800,
      chunkOverlap: data.chunk_overlap ?? 50,
      rerank: data.rerank ?? true,
      description: data.description || '',
    }
  } catch (e) {
    console.error('Failed to load config:', e)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await knowledgeService.updateConfig(props.kbId, {
      chunkMode: form.value.chunkMode,
      chunkSize: form.value.chunkSize,
      chunkOverlap: form.value.chunkOverlap,
      rerank: form.value.rerank,
      description: form.value.description || undefined,
    })
    ElMessage.success('配置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <div class="config-panel">
    <div v-if="loading" class="config-loading">
      <Loader2 :size="24" class="spin" />
      <span>加载配置...</span>
    </div>

    <form v-else @submit.prevent="handleSave" class="config-form">
      <div class="form-group">
        <label class="form-label">分块模式</label>
        <select v-model="form.chunkMode" class="form-select" :disabled="!isOwner">
          <option v-for="opt in chunkModeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <span class="form-hint">决定文档如何被切分为搜索片段</span>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label class="form-label">分块大小</label>
          <input v-model.number="form.chunkSize" type="number" class="form-input" min="100" max="4000" :disabled="!isOwner" />
          <span class="form-hint">每个分块的目标 token 数</span>
        </div>

        <div class="form-group">
          <label class="form-label">分块重叠</label>
          <input v-model.number="form.chunkOverlap" type="number" class="form-input" min="0" max="500" :disabled="!isOwner" />
          <span class="form-hint">相邻分块重叠的 token 数</span>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label toggle-label">
          <span>重排序 (Rerank)</span>
          <button
            type="button"
            class="toggle-btn"
            :class="{ active: form.rerank }"
            :disabled="!isOwner"
            @click="form.rerank = !form.rerank"
          >
            <span class="toggle-knob"></span>
          </button>
        </label>
        <span class="form-hint">提升搜索结果精度，略有性能开销</span>
      </div>

      <div class="form-group">
        <label class="form-label">知识库描述</label>
        <textarea
          v-model="form.description"
          class="form-textarea"
          rows="3"
          placeholder="描述知识库的内容和用途..."
          :disabled="!isOwner"
        ></textarea>
      </div>

      <button
        v-if="isOwner"
        type="submit"
        class="save-btn"
        :disabled="saving"
      >
        <Loader2 v-if="saving" :size="16" class="spin" />
        <Save v-else :size="16" />
        <span>{{ saving ? '保存中...' : '保存配置' }}</span>
      </button>
    </form>
  </div>
</template>

<style scoped>
.config-panel {
  width: 100%;
}

.config-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.toggle-label {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.form-select,
.form-input {
  padding: 10px 12px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-display);
  outline: none;
  transition: border-color 0.2s ease;
}

.form-select:focus,
.form-input:focus {
  border-color: var(--accent-1);
}

.form-select:disabled,
.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-textarea {
  padding: 10px 12px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-display);
  outline: none;
  resize: vertical;
  transition: border-color 0.2s ease;
}

.form-textarea:focus {
  border-color: var(--accent-1);
}

.form-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* Toggle button */
.toggle-btn {
  position: relative;
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--bg-secondary);
  cursor: pointer;
  transition: background 0.2s ease;
  flex-shrink: 0;
}

.toggle-btn.active {
  background: var(--accent-1);
}

.toggle-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: white;
  transition: transform 0.2s ease;
}

.toggle-btn.active .toggle-knob {
  transform: translateX(20px);
}

.save-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  align-self: flex-start;
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

[data-theme="light"] .form-select,
[data-theme="light"] .form-input,
[data-theme="light"] .form-textarea {
  background: var(--bg-card);
}

@media (max-width: 600px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
