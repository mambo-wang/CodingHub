<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pluginApi } from '@/services/plugin'
import { useAuthStore } from '@/stores/auth'
import type { PluginSummary } from '@/types/plugin'

const router = useRouter()
const authStore = useAuthStore()

const keyword = ref('')
const sort = ref<'new' | 'hot'>('new')
const page = ref(0)
const size = ref(12)
const total = ref(0)
const loading = ref(false)
const plugins = ref<PluginSummary[]>([])

const load = async () => {
  loading.value = true
  try {
    const data = await pluginApi.list({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: size.value,
      sort: sort.value
    })
    plugins.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 0
  load()
}

const changeSort = (s: 'new' | 'hot') => {
  sort.value = s
  page.value = 0
  load()
}

const copyInstall = async (p: PluginSummary) => {
  const cmd = `codebuddy plugin install ${p.name}`
  try {
    await navigator.clipboard.writeText(cmd)
    ElMessage.success(`已复制: ${cmd}`)
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const goDetail = (id: number) => router.push(`/plugins/${id}`)
const goUpload = () => router.push('/plugins/upload')

const fmtScore = (score: number | null | undefined) => {
  const s = Number(score ?? 0)
  return s >= 10000 ? (s / 10000).toFixed(1) + 'w' : String(Math.round(s))
}

onMounted(load)
</script>

<template>
  <div class="plugin-market">
    <div class="market-hero">
      <h1 class="market-title">插件市场 <span class="title-badge">BETA</span></h1>
      <p class="market-subtitle">上传并分享你的 CodeBuddy 插件 —— 支持 skills、agents、commands、hooks、MCP 等组件</p>

      <div class="market-toolbar">
        <div class="search-box">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
          </svg>
          <input
            v-model="keyword"
            class="search-input"
            placeholder="搜索插件名或描述…"
            @keyup.enter="search"
          />
        </div>
        <div class="sort-tabs">
          <button
            class="sort-tab"
            :class="{ active: sort === 'new' }"
            @click="changeSort('new')"
          >最新</button>
          <button
            class="sort-tab"
            :class="{ active: sort === 'hot' }"
            @click="changeSort('hot')"
          >最热</button>
        </div>
        <button v-if="authStore.isLoggedIn" class="upload-btn" @click="goUpload">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          上传插件
        </button>
      </div>
    </div>

    <div v-if="loading" class="state-hint">加载中…</div>
    <div v-else-if="plugins.length === 0" class="state-hint">
      <p class="empty-title">暂无插件</p>
      <p class="empty-sub">成为第一个上传插件的人吧</p>
      <button v-if="authStore.isLoggedIn" class="upload-btn" @click="goUpload">上传插件</button>
    </div>

    <div v-else class="plugin-grid">
      <div v-for="p in plugins" :key="p.id" class="plugin-card" @click="goDetail(p.id)">
        <div class="card-top">
          <div class="plugin-logo">
            <img v-if="p.logoUrl" :src="p.logoUrl" alt="" />
            <span v-else class="logo-fallback">{{ p.name.slice(0, 2).toUpperCase() }}</span>
          </div>
          <div class="plugin-meta">
            <div class="plugin-name-row">
              <span class="plugin-name">{{ p.name }}</span>
              <span class="version-tag">v{{ p.version }}</span>
            </div>
            <div class="plugin-author">@{{ p.authorUsername }}</div>
          </div>
          <span class="hot-score" :title="`热度 ${fmtScore(p.score)}`">{{ fmtScore(p.score) }}</span>
        </div>

        <p class="plugin-desc">{{ p.description }}</p>

        <div class="plugin-source" :title="p.source">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"/>
          </svg>
          {{ p.source }}
        </div>

        <div class="card-footer">
          <div class="stats">
            <span class="stat"><span class="dot like">♥</span>{{ p.likeCount }}</span>
            <span class="stat"><span class="dot comment">💬</span>{{ p.commentCount }}</span>
            <span class="stat"><span class="dot view">👁</span>{{ p.viewCount }}</span>
          </div>
          <button class="install-btn" @click.stop="copyInstall(p)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            安装
          </button>
        </div>
      </div>
    </div>

    <div v-if="total > size" class="pagination">
      <button class="page-btn" :disabled="page === 0" @click="page--; load()">上一页</button>
      <span class="page-info">{{ page + 1 }} / {{ Math.ceil(total / size) }}</span>
      <button class="page-btn" :disabled="page >= Math.ceil(total / size) - 1" @click="page++; load()">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.plugin-market {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.market-hero {
  margin-bottom: 28px;
}

.market-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--text-primary);
  margin: 0 0 8px;
  font-family: var(--font-display);
}

.title-badge {
  font-size: 12px;
  font-weight: 600;
  vertical-align: middle;
  padding: 3px 8px;
  margin-left: 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
}

.market-subtitle {
  color: var(--text-muted);
  font-size: 14px;
  margin: 0 0 20px;
}

.market-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 240px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  height: 42px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-muted);
  transition: border-color 0.2s ease;
}

.search-box:focus-within {
  border-color: var(--accent-1);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  font-family: var(--font-display);
}

.sort-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 10px;
}

.sort-tab {
  padding: 8px 16px;
  background: transparent;
  border: none;
  border-radius: 7px;
  color: var(--text-secondary);
  font-size: 13px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-tab.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.25));
  color: var(--text-primary);
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 42px;
  padding: 0 18px;
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

.upload-btn:hover {
  border-color: rgba(139, 92, 246, 0.7);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.25);
  transform: translateY(-1px);
}

.plugin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.plugin-card {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plugin-card:hover {
  border-color: rgba(139, 92, 246, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plugin-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.plugin-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-fallback {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.plugin-meta {
  flex: 1;
  min-width: 0;
}

.plugin-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.plugin-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-display);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.version-tag {
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 5px;
  background: rgba(6, 182, 212, 0.15);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: #22d3ee;
  font-family: var(--font-mono);
  white-space: nowrap;
}

.plugin-author {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 3px;
}

.hot-score {
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-1);
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.plugin-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
}

.plugin-source {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.stats {
  display: flex;
  gap: 12px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.dot {
  font-size: 11px;
  opacity: 0.9;
}

.install-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  color: var(--text-secondary);
  font-size: 12px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.install-btn:hover {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.1);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}

.page-btn {
  padding: 8px 18px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: rgba(139, 92, 246, 0.5);
  color: var(--text-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.state-hint {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.empty-sub {
  margin-bottom: 18px;
}
</style>
