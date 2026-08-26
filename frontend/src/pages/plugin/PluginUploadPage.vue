<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pluginApi } from '@/services/plugin'

const router = useRouter()

const file = ref<File | null>(null)
const source = ref('')
const progress = ref(0)
const uploading = ref(false)

const sourceValid = computed(() => {
  const s = source.value.trim()
  if (!s) return false
  return /^(https?:\/\/\S+|[a-zA-Z0-9_-]+\/[a-zA-Z0-9._-]+)$/.test(s)
})

const canSubmit = computed(() => file.value !== null && sourceValid.value && !uploading.value)

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const f = input.files?.[0] || null
  if (f && !f.name.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('仅支持 .zip 格式的插件包')
    file.value = null
    input.value = ''
    return
  }
  if (f && f.size > 50 * 1024 * 1024) {
    ElMessage.warning('zip 文件大小不能超过 50MB')
    file.value = null
    input.value = ''
    return
  }
  file.value = f
}

const submit = async () => {
  if (!file.value || !sourceValid.value) return
  uploading.value = true
  progress.value = 0
  try {
    const detail = await pluginApi.upload(file.value, source.value.trim(), (p) => {
      progress.value = p
    })
    ElMessage.success(`插件 ${detail.name} 上传成功`)
    router.push(`/plugins/${detail.id}`)
  } catch {
    /* handled by interceptor */
  } finally {
    uploading.value = false
  }
}

const back = () => router.push('/plugins')
</script>

<template>
  <div class="plugin-upload">
    <div class="upload-header">
      <button class="back-btn" @click="back">← 返回市场</button>
      <h1 class="upload-title">上传插件</h1>
      <p class="upload-subtitle">
        上传你的 CodeBuddy 插件 zip 包。平台会自动解析
        <code class="inline-code">.codebuddy-plugin/plugin.json</code>
        并生成组件摘要（skills / agents / commands / hooks / MCP / LSP / bin / settings）。
      </p>
    </div>

    <div class="upload-card">
      <div class="form-group">
        <label class="form-label">
          Source <span class="required">*</span>
          <span class="label-hint">插件市场引用地址（GitHub owner/repo 或绝对 URL，HTTP 市场 source 不能为相对路径）</span>
        </label>
        <input
          v-model="source"
          class="form-input"
          placeholder="例如：octocat/my-plugin 或 https://github.com/octocat/my-plugin"
        />
        <p v-if="source && !sourceValid" class="field-error">source 格式不合法：应为 owner/repo 或绝对 URL</p>
      </div>

      <div class="form-group">
        <label class="form-label">
          插件 zip 包 <span class="required">*</span>
          <span class="label-hint">包含 .codebuddy-plugin/plugin.json（name / version 必填，kebab-case 插件名）</span>
        </label>
        <label class="dropzone" :class="{ dragged: file }">
          <input type="file" accept=".zip" class="file-input" @change="onFileChange" />
          <template v-if="file">
            <span class="file-icon">📦</span>
            <span class="file-name">{{ file.name }}</span>
            <span class="file-size">{{ (file.size / 1024 / 1024).toFixed(2) }} MB</span>
          </template>
          <template v-else>
            <span class="file-icon">⬆️</span>
            <span class="drop-hint">点击选择 .zip 文件</span>
            <span class="drop-sub">最大 50MB</span>
          </template>
        </label>
      </div>

      <div v-if="uploading" class="progress-wrap">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ progress }}%</span>
      </div>

      <div class="form-actions">
        <button class="cancel-btn" @click="back">取消</button>
        <button class="submit-btn" :disabled="!canSubmit" @click="submit">
          {{ uploading ? '上传中…' : '上传插件' }}
        </button>
      </div>
    </div>

    <div class="hint-panel">
      <h3 class="hint-title">📦 插件包结构要求</h3>
      <pre class="hint-code">your-plugin.zip
├── .codebuddy-plugin/
│   └── plugin.json          # 必填：{"name": "my-plugin", "version": "1.0.0", "description": "…"}
├── skills/                  # 可选：每个子目录一个 skill
├── agents/                  # 可选：每个子目录一个 agent
├── commands/                # 可选：每个子目录一个 command
├── hooks/                   # 可选：hooks 目录
├── .mcp.json                # 可选：MCP 服务器配置
├── .lsp.json                # 可选：LSP 服务器配置
├── bin/                     # 可选：本地二进制
└── settings.json            # 可选：设置项</pre>
      <p class="hint-note">
        提示：覆盖式更新时请使用「版本升级」流程——重新上传 zip 包，版本号变化后平台会覆盖原文件；
        版本号未变化将拒绝更新。
      </p>
    </div>
  </div>
</template>

<style scoped>
.plugin-upload {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.back-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 13px;
  font-family: var(--font-display);
  cursor: pointer;
  padding: 0;
  margin-bottom: 16px;
  transition: color 0.2s ease;
}

.back-btn:hover {
  color: var(--accent-1);
}

.upload-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  font-family: var(--font-display);
  letter-spacing: -0.5px;
}

.upload-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.7;
  margin: 0 0 24px;
}

.inline-code {
  background: rgba(139, 92, 246, 0.15);
  border: 1px solid rgba(139, 92, 246, 0.3);
  color: #a78bfa;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-family: var(--font-mono);
}

.upload-card {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-display);
  margin-bottom: 8px;
}

.required {
  color: #ef4444;
}

.label-hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
  line-height: 1.5;
}

.form-input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-mono);
  outline: none;
  transition: border-color 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--accent-1);
}

.field-error {
  font-size: 12px;
  color: #ef4444;
  margin-top: 6px;
}

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 120px;
  border: 2px dashed var(--border-color);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.02);
  padding: 16px;
  text-align: center;
}

.dropzone:hover,
.dropzone.dragged {
  border-color: rgba(139, 92, 246, 0.6);
  background: rgba(139, 92, 246, 0.05);
}

.file-input {
  display: none;
}

.file-icon {
  font-size: 24px;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-mono);
  word-break: break-all;
}

.file-size,
.drop-sub {
  font-size: 12px;
  color: var(--text-muted);
}

.drop-hint {
  font-size: 14px;
  color: var(--text-secondary);
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #8b5cf6, #06b6d4);
  border-radius: 4px;
  transition: width 0.2s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  width: 40px;
  text-align: right;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.cancel-btn {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
}

.submit-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.25));
  border: 1px solid rgba(139, 92, 246, 0.45);
  border-radius: 10px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.25s ease;
}

.submit-btn:hover:not(:disabled) {
  border-color: rgba(139, 92, 246, 0.7);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.25);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.hint-panel {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 20px;
}

.hint-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
  font-family: var(--font-display);
}

.hint-code {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  overflow-x: auto;
  margin: 0 0 12px;
}

.hint-note {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.7;
  margin: 0;
}
</style>
