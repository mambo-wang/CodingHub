<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Upload, Wrench } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Category } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const categories = ref<Category[]>([])
const loading = ref(false)
const uploadingId = ref<number | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const activeCategoryId = ref<number | null>(null)

const fetchCategories = async () => {
  loading.value = true
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  } finally {
    loading.value = false
  }
}

const triggerUpload = (categoryId: number) => {
  activeCategoryId.value = categoryId
  fileInputRef.value?.click()
}

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  const categoryId = activeCategoryId.value
  input.value = ''
  if (file && categoryId != null) void bindLogo(categoryId, file)
}

const bindLogo = async (categoryId: number, file: File) => {
  uploadingId.value = categoryId
  try {
    const formData = new FormData()
    formData.append('file', file)
    const upRes = await api.post<{ code: number; message: string; data: { url: string } }>(
      '/uploads/images',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    if (upRes.data.code !== 200 || !upRes.data.data?.url) {
      throw new Error(upRes.data.message || '图片上传失败')
    }
    const url = upRes.data.data.url

    const bindRes = await api.put<{ code: number; message: string }>(
      `/categories/${categoryId}/logo`,
      { logoUrl: url }
    )
    if (bindRes.data.code !== 200) {
      throw new Error(bindRes.data.message || '分类 Logo 更新失败')
    }

    const cat = categories.value.find(c => c.id === categoryId)
    if (cat) cat.logoUrl = url
    ElMessage.success('分类默认 Logo 已更新')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '操作失败，请重试')
  } finally {
    uploadingId.value = null
  }
}

const removeLogo = async (categoryId: number) => {
  uploadingId.value = categoryId
  try {
    const res = await api.put<{ code: number; message: string }>(
      `/categories/${categoryId}/logo`,
      { logoUrl: null }
    )
    if (res.data.code !== 200) throw new Error(res.data.message || '移除失败')
    const cat = categories.value.find(c => c.id === categoryId)
    if (cat) cat.logoUrl = null
    ElMessage.success('已移除分类默认 Logo')
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '操作失败，请重试')
  } finally {
    uploadingId.value = null
  }
}

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  if (!authStore.isAdmin) { router.push('/'); return }
  fetchCategories()
})
</script>

<template>
  <div class="category-manage-page">
    <div class="app-container">
      <div class="page-header">
        <h1 class="page-title">分类管理</h1>
        <p class="page-subtitle">为每个分类配置默认 Logo，工具未设置自定义 Logo 时将回退到分类默认 Logo</p>
      </div>

      <div class="category-list glass-card">
        <div v-if="loading" class="loading-state">加载中…</div>
        <div v-else-if="categories.length === 0" class="empty-state">暂无分类</div>
        <div v-else class="category-rows">
          <div v-for="cat in categories" :key="cat.id" class="category-row">
            <div class="category-info">
              <span class="category-icon">{{ cat.icon }}</span>
              <span class="category-name">{{ cat.name }}</span>
            </div>

            <div class="category-logo">
              <div class="logo-thumb" :class="{ 'logo-thumb--empty': !cat.logoUrl }">
                <img v-if="cat.logoUrl" :src="cat.logoUrl" :alt="cat.name + ' 默认 Logo'" class="logo-thumb-img" />
                <Wrench v-else :size="20" aria-hidden="true" class="logo-thumb-placeholder" />
              </div>
              <span class="logo-status">{{ cat.logoUrl ? '已设置' : '未设置' }}</span>
            </div>

            <div class="category-actions">
              <button
                class="cat-btn"
                :disabled="uploadingId === cat.id"
                @click="triggerUpload(cat.id)"
              >
                <Upload :size="14" aria-hidden="true" />
                <span>{{ uploadingId === cat.id ? '处理中…' : (cat.logoUrl ? '更换' : '上传') }}</span>
              </button>
              <button
                v-if="cat.logoUrl"
                class="cat-btn cat-btn--ghost"
                :disabled="uploadingId === cat.id"
                @click="removeLogo(cat.id)"
              >
                移除
              </button>
            </div>
          </div>
        </div>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp,image/svg+xml"
        style="display: none"
        @change="onFileChange"
      />
    </div>
  </div>
</template>

<style scoped>
.category-manage-page { min-height: calc(100vh - 60px); padding: 40px 20px 80px; }
.app-container { max-width: 800px; margin: 0 auto; }
.page-header { text-align: center; margin-bottom: 32px; }
.page-title { font-size: 32px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.page-subtitle { font-size: 14px; color: var(--text-secondary); }
.category-list { padding: 24px; }
.loading-state, .empty-state { text-align: center; color: var(--text-muted); padding: 40px 0; }
.category-rows { display: flex; flex-direction: column; gap: 12px; }
.category-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 14px 16px; border: 1px solid var(--border-color); border-radius: 12px; flex-wrap: wrap; }
.category-info { display: flex; align-items: center; gap: 10px; min-width: 140px; }
.category-icon { font-size: 20px; }
.category-name { font-size: 15px; font-weight: 500; color: var(--text-primary); }
.category-logo { display: flex; align-items: center; gap: 10px; }
.logo-thumb { width: 44px; height: 44px; border-radius: 10px; overflow: hidden; border: 1px solid var(--border-color); background: rgba(139, 92, 246, 0.08); display: flex; align-items: center; justify-content: center; }
.logo-thumb--empty { border-style: dashed; }
.logo-thumb-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.logo-thumb-placeholder { color: var(--text-muted); }
.logo-status { font-size: 12px; color: var(--text-muted); }
.category-actions { display: flex; gap: 8px; }
.cat-btn { display: inline-flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: 8px; border: 1.5px solid var(--border-color); background: var(--bg-glass); color: var(--text-secondary); font-size: 13px; cursor: pointer; transition: all 200ms ease; }
.cat-btn:hover:not(:disabled) { color: var(--accent-1); border-color: color-mix(in srgb, var(--accent-1) 30%, transparent); }
.cat-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.cat-btn--ghost:hover:not(:disabled) { color: var(--color-destructive); border-color: color-mix(in srgb, var(--color-destructive) 30%, transparent); }
</style>
