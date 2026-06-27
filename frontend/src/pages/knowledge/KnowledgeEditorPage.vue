<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, Save } from '@lucide/vue'
import { knowledgeService } from '@/services/knowledge'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const kbId = computed(() => Number(route.params.id) || null)

const form = ref({
  name: '',
  description: '',
  chunkMode: 'structural',
  chunkSize: 800,
  chunkOverlap: 50,
  rerank: true,
})

const submitting = ref(false)
const pageLoading = ref(false)

const chunkModeOptions = [
  { value: 'structural', label: '结构分块 (Structural)' },
  { value: 'semantic', label: '语义分块 (Semantic)' },
  { value: 'recursive', label: '递归分块 (Recursive)' },
]

const pageTitle = computed(() => isEdit.value ? '编辑知识库' : '创建知识库')

onMounted(async () => {
  if (isEdit.value && kbId.value) {
    pageLoading.value = true
    try {
      const kb = await knowledgeService.getDetail(kbId.value)
      form.value.name = kb.name
      form.value.description = kb.description || ''
      // Load config
      try {
        const config = await knowledgeService.getConfig(kbId.value)
        form.value.chunkMode = config.chunk_mode || 'structural'
        form.value.chunkSize = config.chunk_size || 800
        form.value.chunkOverlap = config.chunk_overlap ?? 50
        form.value.rerank = config.rerank ?? true
      } catch {}
    } catch (e) {
      ElMessage.error('加载知识库信息失败')
      router.push('/knowledge')
    } finally {
      pageLoading.value = false
    }
  }
})

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value && kbId.value) {
      // Update mode
      await knowledgeService.update(kbId.value, {
        name: form.value.name.trim(),
        description: form.value.description.trim() || undefined,
      })
      await knowledgeService.updateConfig(kbId.value, {
        chunkMode: form.value.chunkMode,
        chunkSize: form.value.chunkSize,
        chunkOverlap: form.value.chunkOverlap,
        rerank: form.value.rerank,
        description: form.value.description.trim() || undefined,
      })
      ElMessage.success('知识库已更新')
      router.push(`/knowledge/${kbId.value}`)
    } else {
      // Create mode
      const kb = await knowledgeService.create({
        name: form.value.name.trim(),
        description: form.value.description.trim() || undefined,
        chunkMode: form.value.chunkMode,
        chunkSize: form.value.chunkSize,
        chunkOverlap: form.value.chunkOverlap,
        rerank: form.value.rerank,
      })
      ElMessage.success('知识库创建成功')
      router.push(`/knowledge/${kb.id}`)
    }
  } catch (e: any) {
    if (e.response?.status === 409) {
      ElMessage.error('知识库名称已存在，请使用其他名称')
    } else {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="knowledge-editor-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <!-- Back button -->
      <div class="page-header animate-fade-in-up">
        <button class="back-btn" @click="router.back()">
          <ArrowLeft :size="18" />
          <span class="back-label">返回</span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="pageLoading" class="editor-loading">
        <Loader2 :size="32" class="spin" />
        <span>加载中...</span>
      </div>

      <!-- Form -->
      <div v-else class="editor-card glass-card animate-fade-in-up">
        <h1 class="editor-title">{{ pageTitle }}</h1>

        <form @submit.prevent="handleSubmit" class="editor-form">
          <div class="form-group">
            <label class="form-label">知识库名称 <span class="required">*</span></label>
            <input
              v-model="form.name"
              type="text"
              class="form-input"
              placeholder="输入知识库名称"
              maxlength="100"
              required
            />
            <span class="form-hint">名称将作为 RAG 集合的标识，不可重复</span>
          </div>

          <div class="form-group">
            <label class="form-label">描述</label>
            <textarea
              v-model="form.description"
              class="form-textarea"
              rows="4"
              placeholder="描述知识库的内容和用途..."
              maxlength="500"
            ></textarea>
          </div>

          <!-- Advanced config (collapsible) -->
          <details class="advanced-section">
            <summary class="advanced-toggle">
              <span>高级配置</span>
              <span class="toggle-hint">分块参数与搜索设置</span>
            </summary>

            <div class="advanced-content">
              <div class="form-group">
                <label class="form-label">分块模式</label>
                <select v-model="form.chunkMode" class="form-select">
                  <option v-for="opt in chunkModeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <span class="form-hint">决定文档如何被切分为搜索片段</span>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label class="form-label">分块大小</label>
                  <input v-model.number="form.chunkSize" type="number" class="form-input" min="100" max="4000" />
                  <span class="form-hint">每个分块的目标 token 数</span>
                </div>

                <div class="form-group">
                  <label class="form-label">分块重叠</label>
                  <input v-model.number="form.chunkOverlap" type="number" class="form-input" min="0" max="500" />
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
                    @click="form.rerank = !form.rerank"
                  >
                    <span class="toggle-knob"></span>
                  </button>
                </label>
                <span class="form-hint">提升搜索结果精度，略有性能开销</span>
              </div>
            </div>
          </details>

          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="router.back()">取消</button>
            <button type="submit" class="submit-btn" :disabled="submitting || !form.name.trim()">
              <Loader2 v-if="submitting" :size="16" class="spin" />
              <Save v-else :size="16" />
              <span>{{ submitting ? '提交中...' : (isEdit ? '保存修改' : '创建知识库') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.knowledge-editor-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 24px 80px;
  max-width: 720px;
  margin: 0 auto;
}

.page-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.bg-orb-1 {
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-1), transparent 70%);
}

.bg-orb-2 {
  bottom: -100px;
  left: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-2), transparent 70%);
}

.app-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  align-items: center;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.editor-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px;
  color: var(--text-muted);
}

.editor-card {
  padding: 32px;
}

.editor-title {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 28px;
}

.editor-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.required {
  color: #EF4444;
}

.form-hint {
  font-size: 12px;
  color: var(--text-muted);
}

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

.form-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
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
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.form-select {
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

.form-select:focus {
  border-color: var(--accent-1);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* Advanced section */
.advanced-section {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
}

.advanced-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-glass);
  transition: background 0.2s ease;
  list-style: none;
}

.advanced-toggle::-webkit-details-marker {
  display: none;
}

.advanced-toggle:hover {
  background: rgba(139, 92, 246, 0.05);
}

.toggle-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.advanced-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  border-top: 1px solid var(--border-color);
}

/* Toggle button */
.toggle-label {
  flex-direction: row !important;
  align-items: center;
  justify-content: space-between;
}

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

/* Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

.cancel-btn {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.cancel-btn:hover {
  border-color: var(--text-muted);
  color: var(--text-primary);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme */
[data-theme="light"] .editor-card {
  background: var(--bg-card);
  box-shadow: var(--shadow-md);
}

[data-theme="light"] .form-input,
[data-theme="light"] .form-textarea,
[data-theme="light"] .form-select {
  background: var(--bg-secondary);
}

[data-theme="light"] .back-btn {
  background: var(--bg-card);
}

[data-theme="light"] .advanced-toggle {
  background: var(--bg-secondary);
}

@media (max-width: 768px) {
  .knowledge-editor-page {
    padding: 16px 12px 60px;
  }

  .editor-card {
    padding: 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
