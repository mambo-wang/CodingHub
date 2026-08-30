<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, FileArchive, UploadCloud, Info, Loader2 } from '@lucide/vue'
import { pluginApi } from '@/services/plugin'
import LogoUploader from '@/components/common/LogoUploader.vue'
import TagSelector from '@/components/common/TagSelector.vue'
import type { PluginDetail } from '@/types/plugin'
import type { Tag } from '@/types'

const route = useRoute()
const router = useRouter()

const pluginId = Number(route.params.id)
const detail = ref<PluginDetail | null>(null)
const loading = ref(true)

const file = ref<File | null>(null)
const source = ref('')
const logoUrl = ref<string>('')
const selectedTags = ref<Tag[]>([])
const progress = ref(0)
const uploading = ref(false)

// source 为选填：未填写时后端使用默认值 internal/local，填写时需符合格式
const sourceValid = computed(() => {
  const s = source.value.trim()
  if (!s) return true
  return /^(https?:\/\/\S+|[a-zA-Z0-9_-]+\/[a-zA-Z0-9._-]+)$/.test(s)
})

const canSubmit = computed(() => file.value !== null && sourceValid.value && !uploading.value)

onMounted(async () => {
  try {
    detail.value = await pluginApi.getDetail(pluginId)
    source.value = detail.value.source ?? ''
    logoUrl.value = detail.value.logoUrl ?? ''
    selectedTags.value = detail.value.tags ?? []
  } catch {
    router.replace('/plugins')
  } finally {
    loading.value = false
  }
})

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
    const updated = await pluginApi.update(
      pluginId,
      file.value,
      source.value.trim() || undefined,
      logoUrl.value,
      (p) => {
        progress.value = p
      },
      selectedTags.value.map(t => t.id)
    )
    ElMessage.success(`插件 ${updated.name} 已更新至 v${updated.version}`)
    router.push(`/plugins/${pluginId}`)
  } catch {
    /* handled by interceptor */
  } finally {
    uploading.value = false
  }
}

const back = () => router.push(`/plugins/${pluginId}`)
</script>

<template>
  <div class="plugin-edit">
    <div class="edit-header">
      <button class="back-btn" @click="back">
        <ArrowLeft :size="15" aria-hidden="true" />
        返回详情
      </button>
      <h1 class="edit-title">更新插件</h1>
      <p class="edit-subtitle">
        上传新版本 zip 包完成版本升级。平台将重新解析
        <code class="inline-code">.codebuddy-plugin/plugin.json</code>
        并覆盖原插件文件，同步刷新组件摘要。
      </p>
    </div>

    <div v-if="loading" class="loading-wrap">
      <Loader2 class="spin" :size="24" aria-hidden="true" />
    </div>

    <template v-if="detail">
      <!-- 当前插件信息 -->
      <div class="plugin-card animate-fade-in-up">
        <div class="plugin-logo">
          <img v-if="detail.logoUrl" :src="detail.logoUrl" alt="" />
          <span v-else class="logo-fallback">{{ detail.name.slice(0, 2).toUpperCase() }}</span>
        </div>
        <div class="plugin-info">
          <div class="name-row">
            <h2 class="plugin-name">{{ detail.name }}</h2>
            <span class="version-tag">v{{ detail.version }}</span>
          </div>
          <p class="plugin-desc">{{ detail.description || '暂无描述' }}</p>
          <p class="plugin-source">
            <span class="src-label">Source</span>
            <code class="src-value">{{ detail.source || 'internal/local' }}</code>
          </p>
        </div>
      </div>

      <!-- 更新表单 -->
      <div class="edit-card animate-fade-in-up">
        <div class="form-group">
          <label class="form-label">
            插件 zip 包 <span class="required">*</span>
            <span class="label-hint">新版本 zip 包，平台取 plugin.json 中的 version 完成覆盖式升级</span>
          </label>
          <label class="dropzone" :class="{ dragged: file }">
            <input type="file" accept=".zip" class="file-input" @change="onFileChange" />
            <template v-if="file">
              <FileArchive :size="24" class="file-icon" aria-hidden="true" />
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ (file.size / 1024 / 1024).toFixed(2) }} MB</span>
            </template>
            <template v-else>
              <UploadCloud :size="24" class="file-icon" aria-hidden="true" />
              <span class="drop-hint">点击选择 .zip 文件</span>
              <span class="drop-sub">最大 50MB</span>
            </template>
          </label>
        </div>

        <div class="form-group">
          <label class="form-label">
            插件图标
            <span class="label-hint">选填。支持本地上传；不上传则使用 plugin.json 中 icon 字段或默认图标</span>
          </label>
        </div>
        <div class="form-group">
          <label class="form-label">
            标签
            <span class="label-hint">选填。更新插件的标签分类</span>
          </label>
          <TagSelector v-model="selectedTags" tagType="PLUGIN" />
        </div>

        <div class="form-group">
          <LogoUploader
            v-model="logoUrl"
            hint="支持 jpg/png/gif/webp/svg，最大 10MB。不上传则使用 plugin.json 中 icon 或默认图标。"
          />
        </div>

        <div class="form-group">
          <label class="form-label">
            Source
            <span class="label-hint">选填。插件市场引用地址（GitHub owner/repo 或绝对 URL）；不填时默认使用 internal/local</span>
          </label>
          <input
            v-model="source"
            class="form-input"
            placeholder="选填，例如：octocat/my-plugin"
          />
          <p v-if="source && !sourceValid" class="field-error">source 格式不合法：应为 owner/repo 或绝对 URL</p>
        </div>

        <div v-if="uploading && progress > 0" class="progress-wrap">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progress + '%' }"></div>
          </div>
          <span class="progress-text">{{ Math.round(progress) }}%</span>
        </div>

        <div class="form-actions">
          <button class="cancel-btn" :disabled="uploading" @click="back">取消</button>
          <button class="submit-btn" :disabled="!canSubmit" @click="submit">
            <Loader2 v-if="uploading" class="spin" :size="14" aria-hidden="true" />
            <template v-else>
              <UploadCloud :size="14" aria-hidden="true" />
              更新插件
            </template>
          </button>
        </div>
      </div>

      <!-- 更新须知 -->
      <div class="hint-panel animate-fade-in-up">
        <h3 class="hint-title">
          <Info :size="14" class="hint-icon" aria-hidden="true" />
          更新须知
        </h3>
        <ul class="hint-list">
          <li>新版本号必须高于当前版本（<code>v{{ detail.version }}</code>），否则将被拒绝。</li>
          <li>更新为覆盖式：新 zip 会替换原有插件文件，组件摘要与插件配置同步刷新。</li>
          <li>Source 与图标均为选填：Source 不填沿用 <code>internal/local</code>；图标不传时回退到 plugin.json 的 icon 字段。</li>
        </ul>
      </div>
    </template>
  </div>
</template>

<style scoped>
.plugin-edit {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

/* ---------- 头部 ---------- */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

.edit-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  font-family: var(--font-display);
  letter-spacing: -0.5px;
}

.edit-subtitle {
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

/* ---------- 当前插件信息卡 ---------- */
.plugin-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.plugin-logo {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 14px;
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid rgba(139, 92, 246, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.plugin-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-fallback {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.plugin-info {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.plugin-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-display);
  letter-spacing: -0.3px;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-tag {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(6, 182, 212, 0.15);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: #22d3ee;
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.plugin-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.plugin-source {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 12px;
}

.src-label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.src-value {
  font-family: var(--font-mono);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  padding: 2px 8px;
  border-radius: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 表单卡 ---------- */
.edit-card {
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
  color: var(--text-secondary);
}

.dropzone:hover .file-icon,
.dropzone.dragged .file-icon {
  color: var(--accent-1);
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

.cancel-btn:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
}

.cancel-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
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

/* ---------- 更新须知 ---------- */
.hint-panel {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 20px;
}

.hint-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
  font-family: var(--font-display);
}

.hint-icon {
  color: var(--accent-1);
}

.hint-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hint-list li {
  position: relative;
  padding-left: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.hint-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8b5cf6, #06b6d4);
}

.hint-list code {
  font-family: var(--font-mono);
  color: #a78bfa;
  font-size: 12px;
}

/* ---------- 加载 / 通用 ---------- */
.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-muted);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.animate-fade-in-up {
  animation: fadeInUp 0.35s ease both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
