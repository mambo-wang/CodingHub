<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import { videoService } from '@/services/video'
import UserAvatar from '@/components/UserAvatar.vue'
import VideoCard from '@/components/video/VideoCard.vue'
import { Upload, Trash2, Loader2, ArrowLeft, CheckCircle2, VideoOff, Play, User, Bookmark } from '@lucide/vue'
import type { VideoListItem } from '@/types/video'

const router = useRouter()
const authStore = useAuthStore()

const currentUser = computed(() => authStore.user)
const previewUrl = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const removing = ref(false)
const error = ref('')
const success = ref(false)
const successTimer = ref<number | null>(null)

const hasAvatar = computed(() => !!currentUser.value?.avatarUrl)

// Tab state
type TabId = 'profile' | 'my-videos' | 'my-favorites'
const activeTab = ref<TabId>('profile')

// My Videos state
const myVideos = ref<VideoListItem[]>([])
const videosLoading = ref(false)
const videosPage = ref(0)
const videosTotalPages = ref(0)
const videosHasMore = computed(() => videosPage.value < videosTotalPages.value - 1)
const videosLoadingMore = ref(false)
const deletingVideoId = ref<number | null>(null)

// My Favorites state
const myFavorites = ref<VideoListItem[]>([])
const favoritesLoading = ref(false)
const favoritesPage = ref(0)
const favoritesTotalPages = ref(0)
const favoritesHasMore = computed(() => favoritesPage.value < favoritesTotalPages.value - 1)
const favoritesLoadingMore = ref(false)

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login?redirect=/me/profile')
    return
  }
  // 拉取最新用户信息以确保 avatarUrl 是最新的
  try {
    const response = await api.get('/users/me')
    if (response.data?.data) {
      authStore.setUser(response.data.data)
    }
  } catch (e) {
    // 静默失败，使用现有数据
  }
})

const handleTabChange = (tab: TabId) => {
  activeTab.value = tab
  if (tab === 'my-videos' && myVideos.value.length === 0 && !videosLoading.value) {
    loadMyVideos()
  }
  if (tab === 'my-favorites' && myFavorites.value.length === 0 && !favoritesLoading.value) {
    loadMyFavorites()
  }
}

// === Avatar management ===
const allowedExt = ['jpg', 'jpeg', 'png', 'webp', 'gif']
const maxSize = 2 * 1024 * 1024 // 2MB

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  // 客户端校验
  const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!allowedExt.includes(ext)) {
    error.value = '仅支持 jpg / png / webp / gif 格式'
    input.value = ''
    return
  }
  if (file.size > maxSize) {
    error.value = '头像不能超过 2MB'
    input.value = ''
    return
  }

  // 预览
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  selectedFile.value = file
  error.value = ''
}

const handleCancelPreview = () => {
  selectedFile.value = null
  previewUrl.value = null
  error.value = ''
  const input = document.getElementById('avatar-input') as HTMLInputElement | null
  if (input) input.value = ''
}

const handleUpload = async () => {
  if (!selectedFile.value) return
  uploading.value = true
  error.value = ''
  try {
    const formData = new FormData()
    formData.append('avatar', selectedFile.value)
    const response = await api.post('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const newAvatarUrl: string = response.data?.data?.avatarUrl
    if (newAvatarUrl && authStore.user) {
      authStore.user.avatarUrl = newAvatarUrl
      authStore.setUser(authStore.user)
    }
    success.value = true
    if (successTimer.value) window.clearTimeout(successTimer.value)
    successTimer.value = window.setTimeout(() => { success.value = false }, 2000)
    selectedFile.value = null
    previewUrl.value = null
    const input = document.getElementById('avatar-input') as HTMLInputElement | null
    if (input) input.value = ''
  } catch (e: any) {
    error.value = e?.response?.data?.message || '上传失败, 请重试'
  } finally {
    uploading.value = false
  }
}

const handleRemove = async () => {
  if (!hasAvatar.value) return
  if (!confirm('确定要移除头像吗?')) return
  removing.value = true
  error.value = ''
  try {
    await api.delete('/users/me/avatar')
    if (authStore.user) {
      authStore.user.avatarUrl = null
      authStore.setUser(authStore.user)
    }
    success.value = true
    if (successTimer.value) window.clearTimeout(successTimer.value)
    successTimer.value = window.setTimeout(() => { success.value = false }, 2000)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '移除失败, 请重试'
  } finally {
    removing.value = false
  }
}

// === My Videos ===
const loadMyVideos = async () => {
  videosLoading.value = true
  try {
    const data = await videoService.getMyVideos(0, 12)
    myVideos.value = data.content
    videosTotalPages.value = data.totalPages
    videosPage.value = data.page
  } catch (e) {
    console.error('Failed to load my videos:', e)
  } finally {
    videosLoading.value = false
  }
}

const loadMoreVideos = async () => {
  if (videosLoadingMore.value || !videosHasMore.value) return
  videosLoadingMore.value = true
  try {
    const nextPage = videosPage.value + 1
    const data = await videoService.getMyVideos(nextPage, 12)
    myVideos.value = [...myVideos.value, ...data.content]
    videosPage.value = data.page
    videosTotalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more videos:', e)
  } finally {
    videosLoadingMore.value = false
  }
}

const handleDeleteVideo = async (videoId: number) => {
  if (!confirm('确定要删除这个视频吗？此操作不可撤销。')) return
  deletingVideoId.value = videoId
  try {
    await videoService.deleteVideo(videoId)
    myVideos.value = myVideos.value.filter(v => v.id !== videoId)
  } catch (e) {
    console.error('Failed to delete video:', e)
  } finally {
    deletingVideoId.value = null
  }
}

// === My Favorites ===
const loadMyFavorites = async () => {
  favoritesLoading.value = true
  try {
    const data = await videoService.getMyFavorites(0, 12)
    myFavorites.value = data.content
    favoritesTotalPages.value = data.totalPages
    favoritesPage.value = data.page
  } catch (e) {
    console.error('Failed to load favorites:', e)
  } finally {
    favoritesLoading.value = false
  }
}

const loadMoreFavorites = async () => {
  if (favoritesLoadingMore.value || !favoritesHasMore.value) return
  favoritesLoadingMore.value = true
  try {
    const nextPage = favoritesPage.value + 1
    const data = await videoService.getMyFavorites(nextPage, 12)
    myFavorites.value = [...myFavorites.value, ...data.content]
    favoritesPage.value = data.page
    favoritesTotalPages.value = data.totalPages
  } catch (e) {
    console.error('Failed to load more favorites:', e)
  } finally {
    favoritesLoadingMore.value = false
  }
}

const goBack = () => router.back()
</script>

<template>
  <div class="profile-page">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="app-container">
      <div class="page-header animate-fade-in-up">
        <button class="back-btn" @click="goBack" aria-label="返回">
          <ArrowLeft :size="20" aria-hidden="true" />
        </button>
        <div>
          <h1 class="page-title">
            <span class="title-icon">👤</span>
            个人中心
          </h1>
          <p class="page-subtitle">管理你的个人资料与内容</p>
        </div>
      </div>

      <div v-if="!currentUser" class="loading glass-card">
        加载中...
      </div>

      <div v-else class="profile-content">
        <!-- Tab Bar -->
        <div class="tab-bar" role="tablist" aria-label="个人中心标签">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'profile' }"
            role="tab"
            :aria-selected="activeTab === 'profile'"
            @click="handleTabChange('profile')"
          >
            <User :size="16" aria-hidden="true" />
            <span>个人资料</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'my-videos' }"
            role="tab"
            :aria-selected="activeTab === 'my-videos'"
            @click="handleTabChange('my-videos')"
          >
            <Play :size="16" aria-hidden="true" />
            <span>我的视频</span>
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'my-favorites' }"
            role="tab"
            :aria-selected="activeTab === 'my-favorites'"
            @click="handleTabChange('my-favorites')"
          >
            <Bookmark :size="16" aria-hidden="true" />
            <span>我的收藏</span>
          </button>
        </div>

        <!-- Profile Tab Content -->
        <div v-if="activeTab === 'profile'" class="tab-panel" role="tabpanel">
          <div class="profile-card glass-card">
            <div class="profile-avatar-section">
              <div class="avatar-large-wrap">
                <UserAvatar
                  v-if="!previewUrl"
                  :user="currentUser"
                  size="xl"
                  class="avatar-large"
                />
                <img v-else :src="previewUrl" alt="头像预览" class="avatar-preview" />
              </div>
              <div class="profile-info">
                <div class="profile-name">
                  <strong>{{ currentUser.nickname || currentUser.username }}</strong>
                  <span class="username-suffix">@{{ currentUser.username }}</span>
                </div>
                <div class="profile-meta">
                  <span class="meta-item">ID: {{ currentUser.id }}</span>
                  <span v-if="hasAvatar" class="meta-item meta-item--success">
                    <span class="dot dot-success"></span>已上传头像
                  </span>
                  <span v-else class="meta-item meta-item--muted">
                    <span class="dot dot-muted"></span>未上传头像, 使用首字母兜底
                  </span>
                </div>
              </div>
            </div>

            <div class="profile-actions">
              <h3 class="section-title">头像管理</h3>
              <p class="section-desc">
                支持 JPG / PNG / WebP / GIF 格式, 文件大小不超过 2MB。<br>
                建议使用正方形图片以获得最佳显示效果。
              </p>

              <div v-if="!selectedFile" class="action-row">
                <label class="btn btn-primary" for="avatar-input">
                  <Upload :size="16" aria-hidden="true" />
                  <span>更换头像</span>
                </label>
                <input
                  id="avatar-input"
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif"
                  class="hidden-input"
                  @change="handleFileChange"
                  aria-label="选择头像文件"
                />
                <button
                  v-if="hasAvatar"
                  class="btn btn-danger"
                  type="button"
                  :disabled="removing"
                  @click="handleRemove"
                >
                  <span v-if="removing" class="spinner" aria-hidden="true"></span>
                  <Trash2 v-else :size="16" aria-hidden="true" />
                  <span>{{ removing ? '移除中...' : '移除头像' }}</span>
                </button>
              </div>

              <div v-else class="action-row action-row--confirm">
                <div class="confirm-info">
                  <span class="confirm-label">待上传: </span>
                  <span class="confirm-filename">{{ selectedFile.name }}</span>
                  <span class="confirm-size">({{ Math.round(selectedFile.size / 1024) }} KB)</span>
                </div>
                <div class="confirm-btns">
                  <button class="btn btn-primary" type="button" :disabled="uploading" @click="handleUpload">
                    <Loader2 v-if="uploading" :size="16" class="spin" aria-hidden="true" />
                    <span>{{ uploading ? '上传中...' : '确认上传' }}</span>
                  </button>
                  <button class="btn" type="button" :disabled="uploading" @click="handleCancelPreview">
                    取消
                  </button>
                </div>
              </div>

              <div v-if="error" class="alert alert-error" role="alert">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 8v4M12 16h.01"/>
                </svg>
                <span>{{ error }}</span>
              </div>

              <Transition name="fade">
                <div v-if="success" class="alert alert-success" role="status">
                  <CheckCircle2 :size="16" aria-hidden="true" />
                  <span>操作成功</span>
                </div>
              </Transition>
            </div>
          </div>
        </div>

        <!-- My Videos Tab Content -->
        <div v-if="activeTab === 'my-videos'" class="tab-panel" role="tabpanel">
          <div v-if="videosLoading" class="tab-loading">
            <Loader2 :size="24" class="spin" aria-hidden="true" />
            <span>加载视频中...</span>
          </div>
          <div v-else-if="myVideos.length === 0" class="tab-empty">
            <VideoOff :size="48" aria-hidden="true" />
            <p class="empty-title">还没有上传任何视频</p>
            <p class="empty-subtitle">去上传你的第一个微课视频吧</p>
            <router-link to="/videos/upload" class="empty-action-btn">
              <Upload :size="16" aria-hidden="true" />
              <span>上传视频</span>
            </router-link>
          </div>
          <template v-else>
            <div class="video-grid">
              <div v-for="video in myVideos" :key="video.id" class="video-grid-item">
                <VideoCard :video="video" />
                <button
                  class="delete-video-btn"
                  :disabled="deletingVideoId === video.id"
                  @click="handleDeleteVideo(video.id)"
                  :aria-label="`删除视频: ${video.title}`"
                >
                  <Loader2 v-if="deletingVideoId === video.id" :size="14" class="spin" aria-hidden="true" />
                  <Trash2 v-else :size="14" aria-hidden="true" />
                </button>
              </div>
            </div>
            <div v-if="videosHasMore" class="load-more-wrap">
              <button class="load-more-btn" :disabled="videosLoadingMore" @click="loadMoreVideos">
                <Loader2 v-if="videosLoadingMore" :size="14" class="spin" aria-hidden="true" />
                <span>{{ videosLoadingMore ? '加载中...' : '加载更多' }}</span>
              </button>
            </div>
          </template>
        </div>

        <!-- My Favorites Tab Content -->
        <div v-if="activeTab === 'my-favorites'" class="tab-panel" role="tabpanel">
          <div v-if="favoritesLoading" class="tab-loading">
            <Loader2 :size="24" class="spin" aria-hidden="true" />
            <span>加载收藏中...</span>
          </div>
          <div v-else-if="myFavorites.length === 0" class="tab-empty">
            <Bookmark :size="48" aria-hidden="true" />
            <p class="empty-title">还没有收藏任何视频</p>
            <p class="empty-subtitle">浏览微课时点击收藏按钮来添加</p>
            <router-link to="/videos" class="empty-action-btn">
              <Play :size="16" aria-hidden="true" />
              <span>浏览微课</span>
            </router-link>
          </div>
          <template v-else>
            <div class="video-grid">
              <VideoCard
                v-for="video in myFavorites"
                :key="video.id"
                :video="video"
              />
            </div>
            <div v-if="favoritesHasMore" class="load-more-wrap">
              <button class="load-more-btn" :disabled="favoritesLoadingMore" @click="loadMoreFavorites">
                <Loader2 v-if="favoritesLoadingMore" :size="14" class="spin" aria-hidden="true" />
                <span>{{ favoritesLoadingMore ? '加载中...' : '加载更多' }}</span>
              </button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  position: relative;
  min-height: calc(100vh - 80px);
  padding: 32px 0;
}
.page-bg {
  position: absolute;
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
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}
.page-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 32px;
}
.back-btn {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  margin-top: 4px;
}
.back-btn:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--text-primary);
  border-color: var(--accent-1);
  transform: translateY(-1px);
}
.back-btn:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}
.title-icon { -webkit-text-fill-color: initial; font-size: 24px; }
.page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 4px;
}
.loading {
  padding: 48px;
  text-align: center;
  color: var(--text-secondary);
  border-radius: 16px;
}
.profile-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Tab Bar */
.tab-bar {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  flex: 1;
  justify-content: center;
}

.tab-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
}

.tab-btn.active {
  color: var(--text-primary);
  background: rgba(139, 92, 246, 0.15);
  border: 1px solid var(--border-glow);
}

.tab-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Tab Panel */
.tab-panel {
  min-height: 200px;
}

/* Profile card (existing) */
.profile-card {
  padding: 32px;
  border-radius: 16px;
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 32px;
}
@media (max-width: 768px) {
  .profile-card {
    grid-template-columns: 1fr;
    padding: 24px;
  }
  .tab-btn {
    padding: 10px 12px;
    font-size: 13px;
  }
}
.profile-avatar-section {
  text-align: center;
}
.avatar-large-wrap {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}
.avatar-large {
  border: 2px solid var(--border-glow);
  box-shadow: var(--shadow-md);
}
.avatar-preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border-glow);
  box-shadow: var(--shadow-md);
}
@media (min-width: 768px) {
  .avatar-preview { width: 160px; height: 160px; }
}
.profile-name {
  font-size: 16px;
  margin-bottom: 8px;
  color: var(--text-primary);
}
.username-suffix {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 6px;
  font-family: var(--font-mono);
}
.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  align-items: center;
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.meta-item--success { color: #10b981; }
.meta-item--muted { color: var(--text-muted); }
.dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  display: inline-block;
}
.dot-success { background: #10b981; box-shadow: 0 0 8px rgba(16,185,129,0.5); }
.dot-muted { background: var(--text-muted); }
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.section-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 20px;
}
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.action-row--confirm {
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-glow);
  border-radius: 12px;
}
.confirm-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}
.confirm-label { color: var(--text-muted); }
.confirm-filename { color: var(--text-primary); font-weight: 500; }
.confirm-size { color: var(--text-muted); font-family: var(--font-mono); }
.confirm-btns { display: flex; gap: 12px; flex-wrap: wrap; }
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
  background: var(--bg-glass);
  color: var(--text-primary);
}
.btn:hover:not(:disabled) {
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
}
.btn:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.btn-primary {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border: none;
}
.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-glow);
}
.btn-danger {
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
  background: transparent;
}
.btn-danger:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.5);
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.hidden-input { display: none; }
@media (max-width: 640px) {
  .action-row .btn { width: 100%; justify-content: center; }
  .confirm-btns .btn { flex: 1; justify-content: center; }
}
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.alert-error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
}
[data-theme="light"] .alert-error {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.05);
}
.alert-success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #6ee7b7;
}
[data-theme="light"] .alert-success {
  color: #047857;
  background: rgba(16, 185, 129, 0.05);
}
.fade-enter-active, .fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-4px); }

/* Tab Loading */
.tab-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 64px 24px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  color: var(--accent-2);
  font-size: 15px;
}

/* Tab Empty */
.tab-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  color: var(--text-muted);
  gap: 10px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-secondary);
}

.empty-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.empty-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 8px;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.empty-action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.empty-action-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Video Grid */
.video-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

@media (min-width: 640px) {
  .video-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 960px) {
  .video-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.video-grid-item {
  position: relative;
}

.delete-video-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 6px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
  z-index: 2;
}

.delete-video-btn:hover:not(:disabled) {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
  box-shadow: 0 0 12px rgba(239, 68, 68, 0.2);
}

.delete-video-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-video-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Load More */
.load-more-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.load-more-btn:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.load-more-btn:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

/* Light theme */
[data-theme="light"] .tab-btn:hover {
  background: rgba(124, 58, 237, 0.05);
}

[data-theme="light"] .tab-btn.active {
  background: rgba(124, 58, 237, 0.1);
}

[data-theme="light"] .delete-video-btn {
  background: rgba(255, 255, 255, 0.8);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .tab-btn,
  .back-btn,
  .btn,
  .delete-video-btn,
  .load-more-btn,
  .empty-action-btn {
    transition: none;
  }
  .back-btn:hover,
  .empty-action-btn:hover {
    transform: none;
  }
  .spin {
    animation: none;
  }
}
</style>
