<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { RegisterRequest } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')

const form = ref<RegisterRequest>({
  username: '',
  nickname: '',
  password: '',
  role: 'USER'
})

const handleSubmit = async () => {
  errorMessage.value = ''

  if (!form.value.username || !form.value.nickname || !form.value.password) {
    errorMessage.value = '请填写所有字段'
    return
  }

  if (form.value.username.length < 4 || form.value.username.length > 20) {
    errorMessage.value = '用户名长度需在4-20字符之间'
    return
  }

  if (form.value.nickname.length < 2 || form.value.nickname.length > 10) {
    errorMessage.value = '昵称长度需在2-10字符之间'
    return
  }

  if (form.value.password.length < 6) {
    errorMessage.value = '密码至少6个字符'
    return
  }

  loading.value = true
  try {
    const response = await api.post('/auth/register', form.value)
    const data = response.data.data

    // ADMIN registration: pending approval
    if (!data.accessToken) {
      ElMessage.success('注册成功，等待超级管理员审批')
      router.push('/login')
      return
    }

    // USER registration: normal flow
    authStore.setTokens(data.accessToken, data.refreshToken)
    authStore.setUser(data.user)

    router.push('/')
  } catch (error: any) {
    errorMessage.value = error.response?.data?.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const goToLogin = () => router.push('/login')
</script>

<template>
  <div class="auth-page">
    <!-- Background effects -->
    <div class="auth-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="auth-container">
      <div class="auth-card glass-card">
        <!-- Header -->
        <div class="auth-header">
          <div class="auth-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="url(#iconGrad2)" stroke-width="2">
              <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
          </div>
          <h1 class="auth-title">创建账号</h1>
          <p class="auth-subtitle">加入 CodingHub</p>
        </div>

        <!-- Error Message -->
        <div v-if="errorMessage" class="error-message">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M15 9l-6 6M9 9l6 6"/>
          </svg>
          {{ errorMessage }}
        </div>

        <!-- Form -->
        <form class="auth-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input
                v-model="form.username"
                type="text"
                class="form-input"
                placeholder="选择一个好听的名字"
                maxlength="100"
                autocomplete="username"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">昵称</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
                <path d="M12 11l-1 4M12 11l1 4"/>
              </svg>
              <input
                v-model="form.nickname"
                type="text"
                class="form-input"
                placeholder="给自己起个好听的昵称"
                maxlength="10"
                autocomplete="nickname"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">密码</label>
            <div class="input-wrapper">
              <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
              <input
                v-model="form.password"
                type="password"
                class="form-input"
                placeholder="至少6个字符"
                autocomplete="new-password"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">注册身份</label>
            <div class="role-selector" role="radiogroup" aria-label="选择注册身份">
              <label
                class="role-card"
                :class="{ 'role-card--active': form.role === 'USER' }"
                @click="form.role = 'USER'"
              >
                <input type="radio" name="role" value="USER" :checked="form.role === 'USER'" class="sr-only" />
                <div class="role-card-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                </div>
                <div class="role-card-text">
                  <span class="role-card-title">普通用户</span>
                  <span class="role-card-desc">注册后即可使用</span>
                </div>
              </label>
              <label
                class="role-card"
                :class="{ 'role-card--active': form.role === 'ADMIN' }"
                @click="form.role = 'ADMIN'"
              >
                <input type="radio" name="role" value="ADMIN" :checked="form.role === 'ADMIN'" class="sr-only" />
                <div class="role-card-icon">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  </svg>
                </div>
                <div class="role-card-text">
                  <span class="role-card-title">管理员</span>
                  <span class="role-card-desc">需要超管审批</span>
                </div>
              </label>
            </div>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>注册</span>
          </button>
        </form>

        <!-- Footer -->
        <div class="auth-footer">
          <span class="footer-text">已有账号？</span>
          <button class="link-btn" @click="goToLogin">立即登录</button>
        </div>
      </div>
    </div>

    <!-- Deerflow Branding -->
    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">
      ✦ Created By Deerflow
    </a>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
}

.auth-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.4;
}

.bg-orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(236, 72, 153, 0.3);
  bottom: -200px;
  right: -100px;
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  background: rgba(6, 182, 212, 0.25);
  top: 100px;
  left: -150px;
}

.auth-container {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

.auth-card {
  padding: 48px 40px;
  animation: fadeInUp 0.6s ease;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(236, 72, 153, 0.3);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 24px;
  animation: fadeIn 0.3s ease;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  padding-left: 4px;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
}

.form-input {
  width: 100%;
  padding: 14px 14px 14px 46px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  outline: none;
  transition: all 0.25s ease;
}

.form-input:focus {
  border-color: rgba(236, 72, 153, 0.5);
  box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.1);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.submit-btn {
  width: 100%;
  padding: 14px;
  margin-top: 8px;
  background: linear-gradient(135deg, var(--accent-3), var(--accent-2));
  border: none;
  border-radius: 10px;
  color: white;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(236, 72, 153, 0.35);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.auth-footer {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.footer-text {
  font-size: 14px;
  color: var(--text-muted);
}

.link-btn {
  background: none;
  border: none;
  color: var(--accent-2);
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.2s ease;
}

.link-btn:hover {
  color: var(--accent-3);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

.role-selector {
  display: flex;
  gap: 12px;
}

.role-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.role-card:hover {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.05);
}

.role-card--active {
  border-color: rgba(236, 72, 153, 0.5);
  background: rgba(236, 72, 153, 0.1);
}

.role-card-icon {
  color: var(--text-muted);
  display: flex;
  align-items: center;
}

.role-card--active .role-card-icon {
  color: var(--accent-3);
}

.role-card-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.role-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.role-card-desc {
  font-size: 12px;
  color: var(--text-muted);
}
</style>