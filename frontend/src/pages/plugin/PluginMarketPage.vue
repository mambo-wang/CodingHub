<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Upload, PackageOpen, Eye, Heart, MessageCircle, Link } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import { pluginApi } from '@/services/plugin'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import type { PluginSummary } from '@/types/plugin'

const router = useRouter()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)

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

const goDetail = (id: number) => router.push(`/plugins/${id}`)
const goUpload = () => router.push('/plugins/upload')

// 后端端口可通过 VITE_BACKEND_PORT 覆盖，默认 8082（与快速入门 MCP 地址拼接一致）
const marketBackendPort = (import.meta.env.VITE_BACKEND_PORT as string) || '8082'
const marketUrl = computed(() => `http://${window.location.hostname}:${marketBackendPort}/api/v1/plugin-market/marketplace.json`)

// 与快速入门一致的复制封装：非安全上下文(如 http)时回退到 execCommand
async function copyToClipboard(text: string) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
}

const copyMarketUrl = async () => {
  try {
    await copyToClipboard(marketUrl.value)
    ElMessage.success('市场地址已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const fmtCount = (count: number | null | undefined) => {
  const c = Number(count ?? 0)
  if (c >= 1000000) return `${(c / 1000000).toFixed(1)}M`
  if (c >= 1000) return `${(c / 1000).toFixed(1)}k`
  return String(c)
}

const authorUser = (p: PluginSummary) => ({
  id: p.authorId,
  username: p.authorUsername,
  nickname: p.authorNickname
})

const authorName = (p: PluginSummary) => p.authorNickname || p.authorUsername

onMounted(load)
</script>

<template>
  <div class="plugin-market">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="market-hero animate-fade-in-up">
      <h1 class="market-title">
        <span class="title-icon">🧩</span>
        插件市场
        <span class="title-badge">BETA</span>
      </h1>
      <div class="market-subtitle-row">
        <p class="market-subtitle">上传并分享你的 CodeBuddy 插件 —— 支持 skills、agents、commands、hooks、MCP 等组件</p>
        <button class="copy-market-btn" title="复制插件市场地址" @click="copyMarketUrl">
          <Link :size="14" aria-hidden="true" />
          复制市场地址
        </button>
      </div>

      <div class="market-toolbar">
        <div class="search-box">
          <Search :size="16" aria-hidden="true" />
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
        <button v-if="isLoggedIn" class="upload-btn" @click="goUpload">
          <Upload :size="16" aria-hidden="true" />
          上传插件
        </button>
      </div>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="plugin-grid" aria-hidden="true">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <div class="skeleton-head">
          <div class="skeleton-logo"></div>
          <div class="skeleton-meta">
            <div class="skeleton-title"></div>
            <div class="skeleton-sub"></div>
          </div>
        </div>
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="plugins.length === 0" class="empty-state animate-fade-in-up">
      <PackageOpen :size="48" aria-hidden="true" />
      <p class="empty-title">暂无插件</p>
      <p class="empty-sub">成为第一个上传插件的人吧</p>
      <button v-if="isLoggedIn" class="upload-btn" @click="goUpload">
        <Upload :size="16" aria-hidden="true" />
        上传插件
      </button>
    </div>

    <!-- Plugin grid -->
    <div v-else class="plugin-grid stagger-children">
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
            <div class="plugin-author">
              <UserAvatar :user="authorUser(p)" size="sm" :display-name="authorName(p)" />
              <span>{{ authorName(p) }}</span>
            </div>
          </div>
        </div>

        <p class="plugin-desc">{{ p.description }}</p>

        <div class="card-footer">
          <div class="stats">
            <span class="stat">
              <Heart :size="14" aria-hidden="true" />
              {{ fmtCount(p.likeCount) }}
            </span>
            <span class="stat">
              <MessageCircle :size="14" aria-hidden="true" />
              {{ fmtCount(p.commentCount) }}
            </span>
            <span class="stat">
              <Eye :size="14" aria-hidden="true" />
              {{ fmtCount(p.viewCount) }}
            </span>
          </div>
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
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 64px;
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

.market-hero {
  margin-bottom: 28px;
}

.market-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 8px;
  font-family: var(--font-display);
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  -webkit-text-fill-color: initial;
  font-size: 28px;
}

.title-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
  -webkit-text-fill-color: initial;
  white-space: nowrap;
}

.market-subtitle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 20px;
}

.market-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
  flex: 1;
}

.copy-market-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 7px;
  color: var(--text-secondary);
  font-size: 12px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.copy-market-btn:hover {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.1);
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
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-muted);
  transition: border-color 0.2s ease;
}

.search-box:focus-within {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
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
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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

.sort-tab:hover {
  color: var(--text-primary);
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
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
  white-space: nowrap;
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.upload-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.plugin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.plugin-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plugin-card:hover {
  border-color: var(--border-glow);
  transform: translateY(-2px);
  box-shadow: var(--shadow-glow);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plugin-logo {
  width: 48px;
  height: 48px;
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
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 5px;
  min-width: 0;
  overflow: hidden;
}

.plugin-author span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
  margin-top: auto;
}

.stats {
  display: flex;
  gap: 14px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.stat svg {
  opacity: 0.7;
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
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
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

/* Skeleton */
.skeleton-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-head {
  display: flex;
  gap: 12px;
  align-items: center;
}

.skeleton-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-title {
  height: 16px;
  width: 60%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-sub {
  height: 12px;
  width: 40%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.1s;
}

.skeleton-line {
  height: 12px;
  width: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.1) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  animation-delay: 0.2s;
}

.skeleton-line.short {
  width: 70%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Empty state */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 24px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  color: var(--text-muted);
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
}

.empty-sub {
  margin-bottom: 10px;
}

/* Light theme */
[data-theme="light"] .plugin-card,
[data-theme="light"] .skeleton-card,
[data-theme="light"] .empty-state {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .plugin-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-md);
}

[data-theme="light"] .skeleton-logo,
[data-theme="light"] .skeleton-title,
[data-theme="light"] .skeleton-sub,
[data-theme="light"] .skeleton-line {
  background: linear-gradient(90deg, rgba(124, 58, 237, 0.05) 25%, rgba(124, 58, 237, 0.08) 50%, rgba(124, 58, 237, 0.05) 75%);
  background-size: 200% 100%;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .skeleton-logo,
  .skeleton-title,
  .skeleton-sub,
  .skeleton-line {
    animation: none;
  }
  .plugin-card:hover,
  .upload-btn:hover {
    transform: none;
  }
}

@media (max-width: 768px) {
  .plugin-market {
    padding: 20px 16px 48px;
  }
  .market-toolbar {
    gap: 10px;
  }
}
</style>
