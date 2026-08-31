<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Upload, PackageOpen, Eye, Heart, MessageCircle, Link, Bookmark, ArrowUp, Flame, Pin, PinOff } from '@lucide/vue'
import { ElMessage } from 'element-plus'
import api from '@/services/api'
import { pluginApi } from '@/services/plugin'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import TagBadge from '@/components/common/TagBadge.vue'
import type { PluginSummary } from '@/types/plugin'
import type { Tag } from '@/types'

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
const hotTop5Ids = ref<Set<number>>(new Set())
const pinLoadingId = ref<number | null>(null)

// 标签筛选（对齐工具广场 HomePage）
const pluginTags = ref<Tag[]>([])
const selectedTagId = ref<number | null>(null)
const tagDropdownOpen = ref(false)
const tagDropdownRef = ref<HTMLElement | null>(null)

// 当前选中标签的显示名称（null 表示"全部标签"）
const selectedTagName = computed(() => {
  if (!selectedTagId.value) return null
  return pluginTags.value.find(t => t.id === selectedTagId.value)?.name || null
})

const loadPluginTags = async () => {
  try {
    const response = await api.get('/tags', { params: { type: 'PLUGIN' } })
    pluginTags.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch plugin tags:', error)
  }
}

const toggleTagDropdown = () => {
  tagDropdownOpen.value = !tagDropdownOpen.value
}

const handleTagSelect = (tagId: number | null) => {
  selectedTagId.value = tagId
  tagDropdownOpen.value = false
  page.value = 0
  load()
}

const handleTagBadgeClick = (tag: Tag) => {
  handleTagSelect(tag.id)
}

const handleTagOutsideClick = (event: MouseEvent) => {
  if (tagDropdownRef.value && !tagDropdownRef.value.contains(event.target as Node)) {
    tagDropdownOpen.value = false
  }
}

const loadHotTop5 = async () => {
  try {
    hotTop5Ids.value = new Set(await pluginApi.getHotTop5())
  } catch { /* 静默降级：不影响列表 */ }
}

const handlePinPlugin = async (p: PluginSummary) => {
  if (pinLoadingId.value === p.id) return
  pinLoadingId.value = p.id
  try {
    if (p.pinned) {
      await pluginApi.unpin(p.id)
    } else {
      await pluginApi.pin(p.id)
    }
    p.pinned = !p.pinned
  } catch {
    ElMessage.error('操作失败')
  } finally {
    pinLoadingId.value = null
  }
}

const load = async () => {
  loading.value = true
  try {
    const data = await pluginApi.list({
      keyword: keyword.value.trim() || undefined,
      tagId: selectedTagId.value ?? undefined,
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

onMounted(() => {
  load()
  loadHotTop5()
  loadPluginTags()
  document.addEventListener('click', handleTagOutsideClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleTagOutsideClick)
})
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
        <div class="tag-filter" ref="tagDropdownRef">
          <button
            type="button"
            class="tag-filter-trigger"
            :class="{ active: selectedTagId !== null, open: tagDropdownOpen }"
            aria-haspopup="listbox"
            :aria-expanded="tagDropdownOpen"
            @click="toggleTagDropdown"
          >
            <span class="tag-filter-label">标签:</span>
            <span class="tag-filter-value">{{ selectedTagName || '全部标签' }}</span>
            <svg class="tag-filter-arrow" :class="{ open: tagDropdownOpen }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </button>
          <div v-if="tagDropdownOpen" class="tag-dropdown-panel" role="listbox" aria-label="按标签筛选">
            <div
              class="tag-dropdown-item"
              :class="{ selected: selectedTagId === null }"
              role="option"
              :aria-selected="selectedTagId === null"
              @click="handleTagSelect(null)"
            >
              <span class="tag-radio"></span>
              <span class="tag-option-name">全部标签</span>
            </div>
            <div
              v-for="tag in pluginTags"
              :key="tag.id"
              class="tag-dropdown-item"
              :class="{ selected: selectedTagId === tag.id }"
              role="option"
              :aria-selected="selectedTagId === tag.id"
              @click="handleTagSelect(tag.id)"
            >
              <span class="tag-radio"></span>
              <span class="tag-option-name">{{ tag.name }}</span>
            </div>
          </div>
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
        <button
          v-if="authStore.isAdmin"
          class="btn-icon-pin"
          :aria-label="p.pinned ? '取消置顶' : '置顶'"
          :disabled="pinLoadingId === p.id"
          @click.stop="handlePinPlugin(p)"
        >
          <PinOff v-if="p.pinned" :size="14" />
          <Pin v-else :size="14" />
        </button>
        <div class="card-top">
          <div class="plugin-logo">
            <img v-if="p.logoUrl" :src="p.logoUrl" alt="" />
            <span v-else class="logo-fallback">{{ p.name.slice(0, 2).toUpperCase() }}</span>
          </div>
          <div class="plugin-meta">
            <div class="plugin-name-row">
              <span class="plugin-name" :title="p.name">{{ p.name }}</span>
              <span class="version-tag">v{{ p.version }}</span>
              <span v-if="p.pinned" class="badge-pill badge-pinned">
                <ArrowUp :size="12" aria-hidden="true" />
                <span>置顶</span>
              </span>
              <span v-if="hotTop5Ids.has(p.id)" class="badge-pill badge-hot">
                <Flame :size="12" aria-hidden="true" />
                <span>热门</span>
              </span>
            </div>
            <div class="plugin-author">
              <UserAvatar :user="authorUser(p)" size="sm" :display-name="authorName(p)" />
              <span>{{ authorName(p) }}</span>
            </div>
          </div>
        </div>

        <p class="plugin-desc">{{ p.description }}</p>

        <div v-if="p.tags && p.tags.length" class="plugin-tags">
          <TagBadge v-for="t in p.tags.slice(0, 3)" :key="t.id" :tag="t" :clickable="true" @click="handleTagBadgeClick" />
          <span v-if="p.tags.length > 3" class="tags-more">+{{ p.tags.length - 3 }}</span>
        </div>

        <div class="card-footer">
          <div class="stats">
            <span class="stat">
              <Heart :size="14" aria-hidden="true" />
              {{ fmtCount(p.likeCount) }}
            </span>
            <span class="stat" title="收藏数">
              <Bookmark :size="14" aria-hidden="true" />
              {{ fmtCount(p.favoriteCount ?? 0) }}
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
/* 置顶/热门角标与置顶按钮（对齐 HomePage 工具卡片） */
.badge-pill { display: inline-flex; align-items: center; gap: 3px; padding: 3px 8px 3px 6px; border-radius: 10px; font-size: 11px; font-weight: 600; letter-spacing: 0.3px; cursor: default; flex-shrink: 0; }
.badge-pinned { background: rgba(139, 92, 246, 0.12); color: #a78bfa; border: 1px solid rgba(139, 92, 246, 0.2); }
.badge-hot { background: rgba(245, 158, 11, 0.12); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.2); }
[data-theme="light"] .badge-pinned { background: rgba(124, 58, 237, 0.08); color: #7c3aed; border-color: rgba(124, 58, 237, 0.15); }
[data-theme="light"] .badge-hot { background: rgba(217, 119, 6, 0.08); color: #b45309; border-color: rgba(217, 119, 6, 0.15); }
.btn-icon-pin { position: absolute; top: 10px; right: 10px; display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 8px; border: 1.5px solid var(--border-color); background: var(--bg-glass); color: var(--text-muted); cursor: pointer; transition: all 200ms ease; opacity: 0; }
.plugin-card:hover .btn-icon-pin, .plugin-card:focus-within .btn-icon-pin { opacity: 1; }
.btn-icon-pin:hover { color: var(--accent-1); border-color: color-mix(in srgb, var(--accent-1) 30%, transparent); }
.btn-icon-pin:disabled { opacity: 0.5; cursor: not-allowed; }
.plugin-tags { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; margin-top: 8px; min-height: 22px; }
.tags-more { font-size: 11px; color: var(--text-muted); }
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
  position: relative;
  z-index: 30;
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

/* Tag Filter Dropdown（对齐工具广场 HomePage，套用插件市场玻璃拟态风格） */
.tag-filter { position: relative; flex-shrink: 0; }
.tag-filter-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 42px;
  padding: 0 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.tag-filter-trigger:hover { border-color: var(--accent-1); color: var(--text-primary); }
.tag-filter-trigger:focus-visible { border-color: var(--accent-1); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15); }
.tag-filter-trigger.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.25));
  border-color: rgba(139, 92, 246, 0.45);
  color: var(--text-primary);
}
.tag-filter-trigger.open { border-color: var(--accent-1); box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15); }
.tag-filter-label { color: var(--text-muted); }
.tag-filter-value { color: inherit; font-weight: 500; max-width: 140px; overflow: hidden; text-overflow: ellipsis; }
.tag-filter-arrow { color: var(--text-muted); transition: transform 0.2s ease; flex-shrink: 0; }
.tag-filter-arrow.open { transform: rotate(180deg); }

.tag-dropdown-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 60;
  width: 200px;
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
  background: var(--bg-glass, rgba(20, 20, 30, 0.95));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
  animation: tagDropIn 0.15s ease;
}
@keyframes tagDropIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }
.tag-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tag-dropdown-item:hover { background: rgba(139, 92, 246, 0.1); color: var(--text-primary); }
.tag-dropdown-item.selected { color: var(--text-primary); font-weight: 500; }
.tag-option-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag-radio {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1.5px solid var(--border-color);
  flex-shrink: 0;
  position: relative;
  transition: all 0.15s ease;
}
.tag-dropdown-item:hover .tag-radio { border-color: rgba(139, 92, 246, 0.5); }
.tag-dropdown-item.selected .tag-radio { border-color: var(--accent-1, #8b5cf6); }
.tag-dropdown-item.selected .tag-radio::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--accent-1, #8b5cf6);
}
[data-theme="light"] .tag-dropdown-panel {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
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
  position: relative;
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
