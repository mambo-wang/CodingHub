<script setup lang="ts">
import { ref, computed } from 'vue'
import { Upload, Wrench } from '@lucide/vue'
import api from '@/services/api'

const props = defineProps<{
  modelValue?: string | null
  toolId?: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', url: string | null): void
}>()

type UploadState = 'idle' | 'uploading' | 'success' | 'error'
const state = ref<UploadState>('idle')
const errorMsg = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string | null>(props.modelValue ?? null)

const hasLogo = computed(() => !!previewUrl.value)

const triggerPick = () => {
  fileInputRef.value?.click()
}

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) void upload(file)
  // 允许重复选择同一文件
  input.value = ''
}

const upload = async (file: File) => {
  state.value = 'uploading'
  errorMsg.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await api.post<{ code: number; message: string; data: { url: string } }>(
      '/uploads/images',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    if (res.data.code !== 200 || !res.data.data?.url) {
      throw new Error(res.data.message || '上传失败')
    }
    const url = res.data.data.url

    // 若已绑定工具，则调用绑定端点写入 tool.logo_url
    if (props.toolId) {
      const bindRes = await api.post<{ code: number; message: string }>(
        `/tools/${props.toolId}/logo`,
        { logoUrl: url }
      )
      if (bindRes.data.code !== 200) {
        throw new Error(bindRes.data.message || 'Logo 绑定失败')
      }
    }

    previewUrl.value = url
    state.value = 'success'
    emit('update:modelValue', url)
  } catch (err: any) {
    state.value = 'error'
    errorMsg.value = err?.response?.data?.message || err?.message || 'Logo 上传失败，请重试'
  }
}

const removeLogo = async () => {
  if (props.toolId) {
    try {
      await api.post(`/tools/${props.toolId}/logo`, { logoUrl: null })
    } catch {
      // 忽略绑定失败，仅清空本地预览
    }
  }
  previewUrl.value = null
  state.value = 'idle'
  errorMsg.value = ''
  emit('update:modelValue', null)
}
</script>

<template>
  <div class="logo-uploader">
    <div class="logo-preview" :class="{ 'logo-preview--empty': !hasLogo }">
      <img v-if="hasLogo" :src="previewUrl!" alt="工具 Logo 预览" class="logo-preview-img" />
      <Wrench v-else :size="28" aria-hidden="true" class="logo-preview-placeholder" />
      <span v-if="state === 'uploading'" class="logo-preview-mask">上传中…</span>
    </div>

    <div class="logo-actions">
      <button type="button" class="logo-btn" :disabled="state === 'uploading'" @click="triggerPick">
        <Upload :size="14" aria-hidden="true" />
        <span>{{ hasLogo ? '更换 Logo' : '上传 Logo' }}</span>
      </button>
      <button
        v-if="hasLogo"
        type="button"
        class="logo-btn logo-btn--ghost"
        :disabled="state === 'uploading'"
        @click="removeLogo"
      >
        移除
      </button>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp,image/svg+xml"
      class="logo-input"
      @change="onFileChange"
    />

    <p v-if="state === 'error'" class="logo-error" role="alert">{{ errorMsg }}</p>
    <p v-else class="logo-hint">支持 jpg/png/gif/webp/svg，最大 10MB。不上传则使用分类默认 Logo。</p>
  </div>
</template>

<style scoped>
.logo-uploader { display: flex; flex-direction: column; gap: 10px; }
.logo-preview { position: relative; width: 72px; height: 72px; border-radius: 16px; overflow: hidden; border: 1px solid var(--border-color); background: rgba(139, 92, 246, 0.08); display: flex; align-items: center; justify-content: center; }
.logo-preview--empty { border-style: dashed; }
.logo-preview-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.logo-preview-placeholder { color: var(--text-muted); }
.logo-preview-mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.5); color: #fff; font-size: 12px; }
.logo-actions { display: flex; gap: 8px; }
.logo-btn { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 8px; border: 1.5px solid var(--border-color); background: var(--bg-glass); color: var(--text-secondary); font-size: 13px; cursor: pointer; transition: all 200ms ease; }
.logo-btn:hover:not(:disabled) { color: var(--accent-1); border-color: color-mix(in srgb, var(--accent-1) 30%, transparent); }
.logo-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.logo-btn--ghost:hover:not(:disabled) { color: var(--color-destructive); border-color: color-mix(in srgb, var(--color-destructive) 30%, transparent); }
.logo-input { display: none; }
.logo-error { margin: 0; font-size: 12px; color: var(--color-destructive); }
.logo-hint { margin: 0; font-size: 12px; color: var(--text-muted); }
</style>
