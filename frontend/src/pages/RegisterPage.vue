<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { RegisterRequest } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = ref<RegisterRequest>({
  email: '',
  password: '',
  username: ''
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
      message: '密码必须包含大小写字母和数字',
      trigger: 'blur'
    }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 100, message: '用户名长度为1-100字符', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await api.post('/auth/register', form.value)
      const { accessToken, refreshToken, user } = response.data.data

      authStore.setTokens(accessToken, refreshToken)
      authStore.setUser(user)

      ElMessage.success('注册成功')
      router.push('/')
    } catch (error: any) {
      const message = error.response?.data?.message || '注册失败'
      ElMessage.error(message)
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-card">
        <h1 class="auth-title">注册</h1>
        <p class="auth-subtitle">加入 AI 工具广场</p>

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

          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              size="large"
              maxlength="100"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码（至少8个字符，含大小写和数字）"
              size="large"
              show-password
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
              注册
            </el-button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
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
