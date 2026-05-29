<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref<LoginRequest>({
  email: '',
  password: ''
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await api.post('/auth/login', form.value)
      const { accessToken, refreshToken, user } = response.data.data

      authStore.setTokens(accessToken, refreshToken)
      authStore.setUser(user)

      ElMessage.success('登录成功')

      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    } catch (error: any) {
      const message = error.response?.data?.message || '登录失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-card">
        <h1 class="auth-title">登录</h1>
        <p class="auth-subtitle">欢迎回到 AI 工具广场</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              size="large"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              style="width: 100%"
              @click="handleSubmit"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          <span>还没有账号？</span>
          <el-link type="primary" @click="goToRegister">立即注册</el-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-container {
  width: 100%;
  max-width: 400px;
}

.auth-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.auth-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  color: #303133;
  text-align: center;
}

.auth-subtitle {
  margin: 0 0 32px 0;
  color: #909399;
  text-align: center;
  font-size: 14px;
}

.auth-form {
  margin-bottom: 24px;
}

.auth-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.form-actions {
  margin-top: 8px;
}

.auth-footer {
  text-align: center;
  color: #606266;
  font-size: 14px;
}
</style>
