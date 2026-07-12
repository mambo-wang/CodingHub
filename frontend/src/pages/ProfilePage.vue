<script setup lang="ts">
import { ref, onMounted, computed, watch, type Component } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import { Upload, Trash2, Loader2, ArrowLeft, CheckCircle2, Save, Lock, User as UserIcon, MessageCircle, Bookmark, Heart, Inbox, Wrench, FileText, Video, ChevronRight } from '@lucide/vue'
import { interactionApi } from '@/services/interaction'
import type { TargetType, MyCommentItem } from '@/services/interaction'

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

// === Profile editing ===
const profileForm = ref({
  nickname: '',
  bio: ''
})
const profileSaving = ref(false)
const profileError = ref('')
const profileSuccess = ref(false)

// === Password change ===
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordSaving = ref(false)
const passwordError = ref('')
const passwordSuccess = ref(false)

const hasAvatar = computed(() => !!currentUser.value?.avatarUrl)

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
      profileForm.value.nickname = response.data.data.nickname || ''
      profileForm.value.bio = response.data.data.bio || ''
    }
  } catch (e) {
    // 静默失败，使用现有数据
  }
  loadComments(false)
})

// === Avatar management ===
const allowedExt = ['jpg', 'jpeg', 'png', 'webp', 'gif']
const maxSize = 2 * 1024 * 1024 // 2MB

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

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

const handleSaveProfile = async () => {
  profileSaving.value = true
  profileError.value = ''
  profileSuccess.value = false
  try {
    await api.put('/users/me/profile', {
      nickname: profileForm.value.nickname,
      bio: profileForm.value.bio
    })
    if (authStore.user) {
      authStore.user.nickname = profileForm.value.nickname
      authStore.setUser(authStore.user)
    }
    profileSuccess.value = true
    if (successTimer.value) window.clearTimeout(successTimer.value)
    successTimer.value = window.setTimeout(() => { profileSuccess.value = false }, 2000)
  } catch (e: any) {
    profileError.value = e?.response?.data?.message || '保存失败, 请重试'
  } finally {
    profileSaving.value = false
  }
}

const handleChangePassword = async () => {
  passwordError.value = ''
  passwordSuccess.value = false

  if (passwordForm.value.newPassword.length < 6) {
    passwordError.value = '新密码至少 6 个字符'
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordError.value = '两次输入的密码不一致'
    return
  }

  passwordSaving.value = true
  try {
    await api.put('/users/me/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    passwordSuccess.value = true
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    if (successTimer.value) window.clearTimeout(successTimer.value)
    successTimer.value = window.setTimeout(() => { passwordSuccess.value = false }, 2000)
  } catch (e: any) {
    passwordError.value = e?.response?.data?.message || '修改密码失败, 请重试'
  } finally {
    passwordSaving.value = false
  }
}

// === My interactions (comments / favorites / likes) ===
type InteractionTab = 'comments' | 'favorites' | 'likes'
const activeTab = ref<InteractionTab>('comments')
const activeType = ref<TargetType>('TOOL')

const TAB_LABELS: Record<InteractionTab, string> = {
  comments: '我的评论',
  favorites: '我的收藏',
  likes: '我的点赞'
}
const TYPE_LABELS: Record<TargetType, string> = {
  TOOL: '工具',
  FORUM_POST: '帖子',
  VIDEO: '微课'
}
const TYPE_ICONS: Record<TargetType, Component> = {
  TOOL: Wrench,
  FORUM_POST: FileText,
  VIDEO: Video
}

const comments = ref<MyCommentItem[]>([])
const favorites = ref<any[]>([])
const likes = ref<any[]>([])

const commentsLoading = ref(false)
const favoritesLoading = ref(false)
const likesLoading = ref(false)
const commentsError = ref('')
const favoritesError = ref('')
const likesError = ref('')

const commentsExpanded = ref(false)
const favoritesExpanded = ref(false)
const likesExpanded = ref(false)

function itemTitle(item: any): string {
  return item?.name || item?.title || '未命名'
}

async function loadComments(all = false) {
  commentsLoading.value = true
  commentsError.value = ''
  try {
    const res = await interactionApi.getMyComments(0, all ? 50 : 10)
    comments.value = res.content || []
  } catch (e: any) {
    commentsError.value = e?.response?.data?.message || '评论加载失败'
  } finally {
    commentsLoading.value = false
  }
}

async function loadFavorites(all = false) {
  favoritesLoading.value = true
  favoritesError.value = ''
  try {
    const res = await interactionApi.getMyFavorites(activeType.value, 0, all ? 50 : 10)
    favorites.value = res.content || []
  } catch (e: any) {
    favoritesError.value = e?.response?.data?.message || '收藏加载失败'
  } finally {
    favoritesLoading.value = false
  }
}

async function loadLikes(all = false) {
  likesLoading.value = true
  likesError.value = ''
  try {
    const res = await interactionApi.getMyLikes(activeType.value, 0, all ? 50 : 10)
    likes.value = res.content || []
  } catch (e: any) {
    likesError.value = e?.response?.data?.message || '点赞加载失败'
  } finally {
    likesLoading.value = false
  }
}

function toggleCommentsAll() {
  commentsExpanded.value = !commentsExpanded.value
  loadComments(commentsExpanded.value)
}
function toggleFavoritesAll() {
  favoritesExpanded.value = !favoritesExpanded.value
  loadFavorites(favoritesExpanded.value)
}
function toggleLikesAll() {
  likesExpanded.value = !likesExpanded.value
  loadLikes(likesExpanded.value)
}

function openDetail(targetType: TargetType, targetId: number) {
  const routes: Record<TargetType, string> = {
    TOOL: `/tools/${targetId}`,
    FORUM_POST: `/forum/posts/${targetId}`,
    VIDEO: `/videos/${targetId}`
  }
  router.push(routes[targetType])
}

watch(activeTab, (tab) => {
  if (tab === 'comments') loadComments(commentsExpanded.value)
  else if (tab === 'favorites') loadFavorites(favoritesExpanded.value)
  else if (tab === 'likes') loadLikes(likesExpanded.value)
})
watch(activeType, () => {
  if (activeTab.value === 'favorites') loadFavorites(favoritesExpanded.value)
  else if (activeTab.value === 'likes') loadLikes(likesExpanded.value)
})

function formatDate(value?: string): string {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
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

        <!-- Profile Editing Card -->
        <div class="edit-card glass-card">
          <div class="edit-card-header">
            <UserIcon :size="20" class="edit-card-icon" />
            <h3 class="edit-card-title">编辑资料</h3>
          </div>

          <div class="form-group">
            <label class="form-label" for="profile-nickname">昵称</label>
            <input
              id="profile-nickname"
              v-model="profileForm.nickname"
              type="text"
              class="form-input"
              placeholder="请输入昵称"
              maxlength="50"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="profile-bio">
              个人简介
              <span class="char-count">{{ profileForm.bio.length }} / 500</span>
            </label>
            <textarea
              id="profile-bio"
              v-model="profileForm.bio"
              class="form-textarea"
              placeholder="介绍一下自己..."
              maxlength="500"
              rows="4"
            ></textarea>
          </div>

          <div class="form-actions">
            <button
              class="btn btn-primary"
              :disabled="profileSaving"
              @click="handleSaveProfile"
            >
              <Loader2 v-if="profileSaving" :size="16" class="spin" />
              <Save v-else :size="16" />
              <span>{{ profileSaving ? '保存中...' : '保存资料' }}</span>
            </button>
          </div>

          <div v-if="profileError" class="alert alert-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
            <span>{{ profileError }}</span>
          </div>

          <Transition name="fade">
            <div v-if="profileSuccess" class="alert alert-success" role="status">
              <CheckCircle2 :size="16" />
              <span>资料已更新</span>
            </div>
          </Transition>
        </div>

        <!-- Password Change Card -->
        <div class="edit-card glass-card">
          <div class="edit-card-header">
            <Lock :size="20" class="edit-card-icon" />
            <h3 class="edit-card-title">修改密码</h3>
          </div>

          <div class="form-group">
            <label class="form-label" for="old-password">当前密码</label>
            <input
              id="old-password"
              v-model="passwordForm.oldPassword"
              type="password"
              class="form-input"
              placeholder="请输入当前密码"
              autocomplete="current-password"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="new-password">新密码</label>
            <input
              id="new-password"
              v-model="passwordForm.newPassword"
              type="password"
              class="form-input"
              placeholder="至少 6 个字符"
              minlength="6"
              autocomplete="new-password"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="confirm-password">确认新密码</label>
            <input
              id="confirm-password"
              v-model="passwordForm.confirmPassword"
              type="password"
              class="form-input"
              placeholder="再次输入新密码"
              autocomplete="new-password"
            />
          </div>

          <div class="form-actions">
            <button
              class="btn btn-primary"
              :disabled="passwordSaving || !passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword"
              @click="handleChangePassword"
            >
              <Loader2 v-if="passwordSaving" :size="16" class="spin" />
              <Lock v-else :size="16" />
              <span>{{ passwordSaving ? '修改中...' : '修改密码' }}</span>
            </button>
          </div>

          <div v-if="passwordError" class="alert alert-error" role="alert">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
            <span>{{ passwordError }}</span>
          </div>

          <Transition name="fade">
            <div v-if="passwordSuccess" class="alert alert-success" role="status">
              <CheckCircle2 :size="16" />
              <span>密码已更新</span>
            </div>
          </Transition>
        </div>

        <!-- 我的互动 -->
        <div class="interactions-card glass-card">
          <h3 class="section-title">我的互动</h3>

          <div class="int-tabs" role="tablist" aria-label="我的互动类型">
            <button
              v-for="(label, key) in TAB_LABELS"
              :key="key"
              class="int-tab"
              :class="{ active: activeTab === key }"
              role="tab"
              :aria-selected="activeTab === key"
              @click="activeTab = key"
            >
              <component
                :is="key === 'comments' ? MessageCircle : key === 'favorites' ? Bookmark : Heart"
                :size="16"
                aria-hidden="true"
              />
              <span>{{ label }}</span>
            </button>
          </div>

          <div v-if="activeTab !== 'comments'" class="int-chips" role="group" aria-label="目标类型">
            <button
              v-for="(label, t) in TYPE_LABELS"
              :key="t"
              class="int-chip"
              :class="{ active: activeType === t }"
              role="tab"
              :aria-selected="activeType === t"
              @click="activeType = t"
            >{{ label }}</button>
          </div>

          <!-- 评论面板 -->
          <div v-show="activeTab === 'comments'" role="tabpanel" aria-label="我的评论">
            <div v-if="commentsLoading" class="int-skeletons" aria-hidden="true">
              <div v-for="n in 3" :key="n" class="int-skeleton"></div>
            </div>
            <div v-else-if="commentsError" class="alert alert-error" role="alert">{{ commentsError }}</div>
            <div v-else-if="comments.length === 0" class="int-empty">
              <Inbox :size="40" aria-hidden="true" />
              <span>还没有评论</span>
            </div>
            <template v-else>
              <button
                v-for="c in comments"
                :key="c.id"
                class="int-item"
                :aria-label="`查看 ${c.targetTitle} 的评论`"
                @click="openDetail(c.targetType, c.targetId)"
              >
                <span class="int-item-type"><component :is="TYPE_ICONS[c.targetType]" :size="14" aria-hidden="true" />{{ TYPE_LABELS[c.targetType] }}</span>
                <span class="int-item-body">
                  <span class="int-item-title">{{ c.targetTitle }}</span>
                  <span class="int-item-meta">我的评论：{{ c.content }} · {{ formatDate(c.createdAt) }}</span>
                </span>
                <ChevronRight class="int-item-arrow" :size="18" aria-hidden="true" />
              </button>
              <button class="int-more" @click="toggleCommentsAll">
                {{ commentsExpanded ? '收起' : '查看全部评论' }}
              </button>
            </template>
          </div>

          <!-- 收藏面板 -->
          <div v-show="activeTab === 'favorites'" role="tabpanel" aria-label="我的收藏">
            <div v-if="favoritesLoading" class="int-skeletons" aria-hidden="true">
              <div v-for="n in 3" :key="n" class="int-skeleton"></div>
            </div>
            <div v-else-if="favoritesError" class="alert alert-error" role="alert">{{ favoritesError }}</div>
            <div v-else-if="favorites.length === 0" class="int-empty">
              <Inbox :size="40" aria-hidden="true" />
              <span>还没有收藏</span>
            </div>
            <template v-else>
              <button
                v-for="item in favorites"
                :key="item.id"
                class="int-item"
                :aria-label="`查看 ${itemTitle(item)}`"
                @click="openDetail(activeType, item.id)"
              >
                <span class="int-item-type"><component :is="TYPE_ICONS[activeType]" :size="14" aria-hidden="true" />{{ TYPE_LABELS[activeType] }}</span>
                <span class="int-item-body">
                  <span class="int-item-title">{{ itemTitle(item) }}</span>
                  <span class="int-item-meta">{{ TYPE_LABELS[activeType] }} · 收藏</span>
                </span>
                <ChevronRight class="int-item-arrow" :size="18" aria-hidden="true" />
              </button>
              <button class="int-more" @click="toggleFavoritesAll">
                {{ favoritesExpanded ? '收起' : '查看全部收藏' }}
              </button>
            </template>
          </div>

          <!-- 点赞面板 -->
          <div v-show="activeTab === 'likes'" role="tabpanel" aria-label="我的点赞">
            <div v-if="likesLoading" class="int-skeletons" aria-hidden="true">
              <div v-for="n in 3" :key="n" class="int-skeleton"></div>
            </div>
            <div v-else-if="likesError" class="alert alert-error" role="alert">{{ likesError }}</div>
            <div v-else-if="likes.length === 0" class="int-empty">
              <Inbox :size="40" aria-hidden="true" />
              <span>还没有点赞</span>
            </div>
            <template v-else>
              <button
                v-for="item in likes"
                :key="item.id"
                class="int-item"
                :aria-label="`查看 ${itemTitle(item)}`"
                @click="openDetail(activeType, item.id)"
              >
                <span class="int-item-type"><component :is="TYPE_ICONS[activeType]" :size="14" aria-hidden="true" />{{ TYPE_LABELS[activeType] }}</span>
                <span class="int-item-body">
                  <span class="int-item-title">{{ itemTitle(item) }}</span>
                  <span class="int-item-meta">{{ TYPE_LABELS[activeType] }} · 点赞</span>
                </span>
                <ChevronRight class="int-item-arrow" :size="18" aria-hidden="true" />
              </button>
              <button class="int-more" @click="toggleLikesAll">
                {{ likesExpanded ? '收起' : '查看全部点赞' }}
              </button>
            </template>
          </div>
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

/* Profile card */
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

/* Edit cards */
.edit-card {
  padding: 28px 32px;
  border-radius: 16px;
}

.edit-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.edit-card-icon {
  color: var(--accent-1);
}

.edit-card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.char-count {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-textarea {
  width: 100%;
  padding: 10px 14px;
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
  resize: vertical;
  min-height: 80px;
  box-sizing: border-box;
}

.form-textarea:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.15);
}

.form-textarea::placeholder {
  color: var(--text-muted);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

/* Light theme form elements */
[data-theme="light"] .form-input,
[data-theme="light"] .form-textarea {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.1);
}

[data-theme="light"] .form-input:focus,
[data-theme="light"] .form-textarea:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.1);
}

/* My interactions */
.interactions-card {
  padding: 28px 32px;
  border-radius: 16px;
}
.int-tabs {
  display: flex;
  gap: 8px;
  margin: 16px 0 20px;
  flex-wrap: wrap;
}
.int-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  cursor: pointer;
  background: var(--bg-glass);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-family: var(--font-display);
  font-size: 14px;
  transition: all 0.2s ease;
}
.int-tab:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}
.int-tab:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.int-tab.active {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border-color: transparent;
}
.int-chips {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.int-chip {
  padding: 6px 14px;
  cursor: pointer;
  background: var(--bg-glass);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.2s ease;
}
.int-chip:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}
.int-chip:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.int-chip.active {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: #fff;
  border-color: transparent;
}
.int-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  margin-bottom: 12px;
  cursor: pointer;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  color: var(--text-primary);
  text-align: left;
  font-family: var(--font-display);
  transition: all 0.2s ease;
}
.int-item:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}
.int-item:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
.int-item-type {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(139, 92, 246, 0.12);
  color: var(--accent-1);
  font-size: 12px;
}
.int-item-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.int-item-title {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.int-item-meta {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.int-item-arrow {
  color: var(--text-muted);
  flex-shrink: 0;
}
.int-empty {
  text-align: center;
  padding: 40px 16px;
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.int-skeletons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.int-skeleton {
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(90deg, rgba(139, 92, 246, 0.05) 25%, rgba(139, 92, 246, 0.12) 50%, rgba(139, 92, 246, 0.05) 75%);
  background-size: 200% 100%;
  animation: int-shimmer 1.5s infinite;
}
@keyframes int-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.int-more {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  padding: 10px 20px;
  cursor: pointer;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 14px;
  transition: all 0.2s ease;
}
.int-more:hover {
  border-color: var(--accent-1);
  color: var(--accent-1);
}
.int-more:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .back-btn,
  .btn {
    transition: none;
  }
  .back-btn:hover {
    transform: none;
  }
  .spin {
    animation: none;
  }
  .int-item:hover {
    transform: none;
  }
  .int-skeleton {
    animation: none;
  }
}
</style>
