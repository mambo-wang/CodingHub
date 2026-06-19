<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/services/api'
import type { PendingUser, ApprovalResponse, ApiResponse } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const pendingUsers = ref<PendingUser[]>([])
const loading = ref(false)

const fetchPendingUsers = async () => {
  loading.value = true
  try {
    const response = await api.get<ApiResponse<PendingUser[]>>('/admin/pending-users')
    pendingUsers.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch pending users:', error)
  } finally {
    loading.value = false
  }
}

const handleApprove = async (user: PendingUser) => {
  try {
    await ElMessageBox.confirm(
      `确认通过 ${user.nickname || user.username} 的管理员注册申请？`,
      '确认审批',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'success' }
    )
    const response = await api.post<ApiResponse<ApprovalResponse>>(`/admin/approve/${user.id}`)
    ElMessage.success(response.data.message || '审批通过')
    pendingUsers.value = pendingUsers.value.filter(u => u.id !== user.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

const handleReject = async (user: PendingUser) => {
  try {
    await ElMessageBox.confirm(
      `确认拒绝 ${user.nickname || user.username} 的管理员注册申请？`,
      '确认拒绝',
      { confirmButtonText: '拒绝', cancelButtonText: '取消', type: 'warning' }
    )
    const response = await api.post<ApiResponse<ApprovalResponse>>(`/admin/reject/${user.id}`)
    ElMessage.success(response.data.message || '已拒绝')
    pendingUsers.value = pendingUsers.value.filter(u => u.id !== user.id)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

onMounted(fetchPendingUsers)
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">审批管理</h1>
      <p class="page-subtitle">审核管理员注册申请</p>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner-large"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="pendingUsers.length === 0" class="empty-state">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" opacity="0.3">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
        <polyline points="22 4 12 14.01 9 11.01"/>
      </svg>
      <p>暂无待审批用户</p>
    </div>

    <div v-else class="card-grid">
      <div v-for="user in pendingUsers" :key="user.id" class="user-card glass-card">
        <div class="user-card-header">
          <div class="user-avatar-lg">
            {{ (user.nickname || user.username)?.charAt(0)?.toUpperCase() }}
          </div>
          <div class="user-info">
            <h3 class="user-name">{{ user.nickname || user.username }}</h3>
            <span class="user-username">@{{ user.username }}</span>
          </div>
          <span class="role-badge role-badge--admin">ADMIN</span>
        </div>
        <div class="user-card-meta">
          <span class="meta-item">申请时间：{{ new Date(user.createdAt).toLocaleDateString('zh-CN') }}</span>
        </div>
        <div class="user-card-actions">
          <button class="action-btn action-btn--approve" @click="handleApprove(user)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            通过
          </button>
          <button class="action-btn action-btn--reject" @click="handleReject(user)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
            拒绝
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: var(--text-muted);
}

.loading-spinner-large {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.card-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  padding: 24px;
}

.user-card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.user-avatar-lg {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.user-username {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.role-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.role-badge--admin {
  background: rgba(234, 179, 8, 0.15);
  color: #eab308;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.user-card-meta {
  margin-bottom: 16px;
}

.meta-item {
  font-size: 13px;
  color: var(--text-muted);
}

.user-card-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: 1px solid;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: var(--font-display);
}

.action-btn--approve {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.3);
  color: #22c55e;
}

.action-btn--approve:hover {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.5);
}

.action-btn--reject {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn--reject:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
}
</style>
