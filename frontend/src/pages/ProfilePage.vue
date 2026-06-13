<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import { Upload, Trash2, Loader2, ArrowLeft, CheckCircle2 } from '@lucide/vue'

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
            个人资料
          </h1>
          <p class="page-subtitle">管理你的头像与展示信息</p>
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
  display: grid;
  gap: 24px;
}
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
</style>
