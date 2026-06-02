<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ToolSummary, PageResponse } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const tools = ref<ToolSummary[]>([])
const loading = ref(false)
const deletingId = ref<number | null>(null)
const pagination = ref({ page: 0, size: 12, totalElements: 0, totalPages: 0 })

const fetchMyTools = async () => {
  loading.value = true
  try {
    const response = await api.get('/users/me/tools', { params: { page: pagination.value.page, size: pagination.value.size } })
    const data: PageResponse<ToolSummary> = response.data.data
    tools.value = data.content
    pagination.value = { page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages }
  } catch (error) { console.error('Failed to fetch tools:', error) }
  finally { loading.value = false }
}

const handleEdit = (toolId: number) => router.push(`/me/tools/${toolId}/edit`)

const handleDelete = async (toolId: number, toolName: string) => {
  if (!confirm(`确定要删除工具"${toolName}"吗？删除后无法恢复。`)) return
  deletingId.value = toolId
  try {
    await api.delete(`/tools/${toolId}`)
    tools.value = tools.value.filter(t => t.id !== toolId)
  } catch (error) { console.error('Delete failed:', error) }
  finally { deletingId.value = null }
}

const formatDate = (dateStr: string) => new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })

onMounted(() => {
  if (!authStore.isLoggedIn) { router.push('/login'); return }
  fetchMyTools()
})
</script>

<template>
  <div class="my-tools-page">
    <div class="page-bg"><div class="bg-orb bg-orb-1"></div><div class="bg-orb bg-orb-2"></div></div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <h1 class="page-title">
          <span class="title-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="url(#toolsGrad)" stroke-width="2">
              <path d="M20 7h-9M14 17H5"/><circle cx="17" cy="17" r="3"/><circle cx="7" cy="7" r="3"/>
            </svg>
          </span>
          我的工具
        </h1>
        <p class="page-subtitle">管理您上传的所有工具</p>
      </div>

      <div class="header-actions">
        <button class="upload-btn" @click="router.push('/tools/upload')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
          </svg>
          上传工具
        </button>
      </div>

      <div v-if="loading" class="tools-list glass-card">
        <div v-for="i in 4" :key="i" class="tool-item-skeleton">
          <div class="skeleton-badge"></div><div class="skeleton-title"></div><div class="skeleton-date"></div>
        </div>
      </div>

      <div v-else-if="tools.length === 0" class="empty-state glass-card animate-fade-in-up">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="3" y="3" width="18" height="18" rx="2"/><path d="M12 8v8M8 12h8"/>
          </svg>
        </div>
        <h3 class="empty-title">还没有上传任何工具</h3>
        <p class="empty-desc">开始分享您的第一个 AI 工具吧</p>
        <button class="upload-btn" @click="router.push('/tools/upload')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
          </svg>
          上传第一个工具
        </button>
      </div>

      <div v-else class="tools-list glass-card animate-fade-in-up">
        <div v-for="tool in tools" :key="tool.id" class="tool-item">
          <div class="tool-info">
            <span class="category-badge">{{ tool.categoryIcon }} {{ tool.categoryName }}</span>
            <h3 class="tool-name">{{ tool.name }}</h3>
            <span class="tool-date">上传于 {{ formatDate(tool.createdAt) }}</span>
          </div>
          <div class="tool-actions">
            <button class="edit-btn" @click="handleEdit(tool.id)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑
            </button>
            <button class="delete-btn" :class="{ loading: deletingId === tool.id }" @click="handleDelete(tool.id, tool.name)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/>
              </svg>
              删除
            </button>
          </div>
        </div>
      </div>

      <div v-if="pagination.totalPages > 1" class="pagination-wrapper">
        <div class="pagination glass-card">
          <button class="page-btn" :disabled="pagination.page === 0" @click="handlePageChange(pagination.page)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
          <div class="page-numbers">
            <button v-for="p in pagination.totalPages" :key="p" class="page-number" :class="{ active: pagination.page === p - 1 }" @click="handlePageChange(p)">{{ p }}</button>
          </div>
          <button class="page-btn" :disabled="pagination.page >= pagination.totalPages - 1" @click="handlePageChange(pagination.page + 2)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </div>
      </div>
    </div>

    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">✦ Created By Deerflow</a>
  </div>
</template>

<style scoped>
.my-tools-page { min-height: calc(100vh - 60px); padding: 40px 20px 80px; position: relative; }
.page-bg { position: fixed; inset: 0; pointer-events: none; overflow: hidden; }
.bg-orb { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.3; }
.bg-orb-1 { width: 400px; height: 400px; background: rgba(139, 92, 246, 0.3); top: -100px; right: -100px; }
.bg-orb-2 { width: 300px; height: 300px; background: rgba(6, 182, 212, 0.2); bottom: 100px; left: -100px; }
.app-container { max-width: 900px; margin: 0 auto; position: relative; z-index: 1; }
.page-header { text-align: center; margin-bottom: 24px; }
.header-actions { display: flex; justify-content: center; margin-bottom: 24px; }
.page-title { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 36px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.title-icon { filter: drop-shadow(0 0 12px rgba(139, 92, 246, 0.5)); }
.page-subtitle { font-size: 16px; color: var(--text-secondary); }
.tools-list { overflow: hidden; }
.tool-item-skeleton { display: flex; align-items: center; gap: 16px; padding: 24px; border-bottom: 1px solid var(--border-color); }
.tool-item-skeleton:last-child { border-bottom: none; }
.skeleton-badge { width: 80px; height: 24px; background: rgba(255, 255, 255, 0.05); border-radius: 12px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-title { width: 200px; height: 20px; background: rgba(255, 255, 255, 0.05); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
.skeleton-date { width: 100px; height: 14px; background: rgba(255, 255, 255, 0.03); border-radius: 4px; animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.empty-state { text-align: center; padding: 80px 40px; }
.empty-icon { width: 100px; height: 100px; margin: 0 auto 24px; background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(6, 182, 212, 0.1)); border: 1px solid var(--border-color); border-radius: 24px; display: flex; align-items: center; justify-content: center; color: var(--text-muted); }
.empty-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: var(--text-secondary); margin-bottom: 32px; }
.upload-btn { display: inline-flex; align-items: center; gap: 8px; padding: 14px 28px; background: linear-gradient(135deg, var(--accent-1), var(--accent-2)); border: none; border-radius: 10px; color: white; font-family: var(--font-display); font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.25s ease; }
.upload-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35); }
.tool-item { display: flex; justify-content: space-between; align-items: center; padding: 24px; border-bottom: 1px solid var(--border-color); transition: background 0.2s ease; }
.tool-item:last-child { border-bottom: none; }
.tool-item:hover { background: rgba(255, 255, 255, 0.02); }
.tool-info { display: flex; flex-direction: column; gap: 8px; }
.category-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.2); border-radius: 16px; font-size: 12px; color: var(--accent-1); width: fit-content; }
.tool-name { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.tool-date { font-size: 13px; color: var(--text-muted); font-family: var(--font-mono); }
.tool-actions { display: flex; gap: 10px; }
.edit-btn { display: flex; align-items: center; gap: 6px; padding: 10px 16px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.2); border-radius: 8px; color: var(--accent-1); font-family: var(--font-display); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; }
.edit-btn:hover { background: rgba(139, 92, 246, 0.2); border-color: rgba(139, 92, 246, 0.4); }
.delete-btn { display: flex; align-items: center; gap: 6px; padding: 10px 16px; background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); border-radius: 8px; color: #ef4444; font-family: var(--font-display); font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; }
.delete-btn:hover { background: rgba(239, 68, 68, 0.2); border-color: rgba(239, 68, 68, 0.4); }
.delete-btn.loading { opacity: 0.6; cursor: not-allowed; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 32px; }
.pagination { display: flex; align-items: center; gap: 8px; padding: 8px; }
.page-btn { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: rgba(255, 255, 255, 0.03); border: 1px solid var(--border-color); border-radius: 8px; color: var(--text-secondary); cursor: pointer; transition: all 0.2s ease; }
.page-btn:hover:not(:disabled) { background: rgba(139, 92, 246, 0.1); border-color: rgba(139, 92, 246, 0.3); color: var(--text-primary); }
.page-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.page-numbers { display: flex; gap: 4px; }
.page-number { min-width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: transparent; border: 1px solid transparent; border-radius: 8px; color: var(--text-secondary); font-family: var(--font-mono); font-size: 13px; cursor: pointer; transition: all 0.2s ease; }
.page-number:hover { background: rgba(255, 255, 255, 0.05); color: var(--text-primary); }
.page-number.active { background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2)); border-color: rgba(139, 92, 246, 0.4); color: var(--text-primary); box-shadow: 0 0 16px rgba(139, 92, 246, 0.2); }
</style>