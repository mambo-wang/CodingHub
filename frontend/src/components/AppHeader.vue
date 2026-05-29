<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const username = computed(() => authStore.user?.username)

const handleLogout = () => {
  authStore.logout()
  router.push('/')
}

const goToLogin = () => {
  router.push('/login')
}

const goToRegister = () => {
  router.push('/register')
}

const goToUpload = () => {
  router.push('/tools/upload')
}

const goToMyTools = () => {
  router.push('/me/tools')
}

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <header class="app-header">
    <div class="header-content">
      <div class="logo" @click="goHome">
        🛠️ AI 工具广场
      </div>

      <nav class="nav-links">
        <template v-if="isLoggedIn">
          <el-button type="primary" @click="goToUpload">
            上传工具
          </el-button>
          <el-button @click="goToMyTools">
            我的工具
          </el-button>
          <el-dropdown>
            <span class="user-dropdown">
              {{ username }}
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button @click="goToLogin">登录</el-button>
          <el-button type="primary" @click="goToRegister">注册</el-button>
        </template>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-links {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
}

.user-dropdown:hover {
  color: #409eff;
}
</style>
