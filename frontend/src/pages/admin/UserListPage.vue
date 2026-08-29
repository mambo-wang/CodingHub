<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '@/services/api'
import type { AdminUser, PageResponse, ApiResponse } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const users = ref<AdminUser[]>([])
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const loading = ref(false)

const filterRole = ref('')
const filterStatus = ref('')
const searchKeyword = ref('')

const isSuperAdmin = computed(() => authStore.user?.role === 'SUPER_ADMIN')

const fetchUsers = async () => {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterRole.value) params.role = filterRole.value
    if (filterStatus.value) params.status = filterStatus.value
    if (searchKeyword.value) params.keyword = searchKeyword.value

    const response = await api.get<ApiResponse<PageResponse<AdminUser>>>('/admin/users', { params })
    const data = response.data.data
    users.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (error) {
    console.error('Failed to fetch users:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page - 1
  fetchUsers()
}

const handleFilterChange = () => {
  currentPage.value = 0
  fetchUsers()
}

const handleSearch = () => {
  currentPage.value = 0
  fetchUsers()
}

const roleLabel = (role: string) => {
  switch (role) {
    case 'USER': return '普通用户'
    case 'ADMIN': return '管理员'
    case 'SUPER_ADMIN': return '超级管理员'
    default: return role
  }
}

const statusLabel = (status: string) => {
  switch (status) {
    case 'ACTIVE': return '正常'
    case 'PENDING': return '待审批'
    case 'REJECTED': return '已拒绝'
    case 'DISABLED': return '已禁用'
    default: return status
  }
}

const handleBan = async (user: AdminUser) => {
  try {
    await ElMessageBox.confirm(
      `确认封禁用户 ${user.nickname || user.username}？封禁后该用户将无法登录。`,
      '确认封禁',
      { confirmButtonText: '封禁', cancelButtonText: '取消', type: 'warning' }
    )
    await api.put(`/admin/users/${user.id}/status`, { status: 'DISABLED' })
    ElMessage.success('已封禁')
    fetchUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

const handleUnban = async (user: AdminUser) => {
  try {
    await api.put(`/admin/users/${user.id}/status`, { status: 'ACTIVE' })
    ElMessage.success('已解禁')
    fetchUsers()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (user: AdminUser) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 ${user.nickname || user.username}？删除后该用户的用户名将被释放，可以重新注册。此操作不可撤销。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error' }
    )
    await api.delete(`/admin/users/${user.id}`)
    ElMessage.success('已删除')
    fetchUsers()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-subtitle">共 {{ totalElements }} 个用户</p>
    </div>

    <!-- Filters -->
    <div class="filters-bar">
      <div class="search-box">
        <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索用户名或昵称"
          @keyup.enter="handleSearch"
        />
      </div>
      <select v-model="filterRole" class="filter-select" @change="handleFilterChange">
        <option value="">全部角色</option>
        <option value="USER">普通用户</option>
        <option value="ADMIN">管理员</option>
        <option value="SUPER_ADMIN">超级管理员</option>
      </select>
      <select v-model="filterStatus" class="filter-select" @change="handleFilterChange">
        <option value="">全部状态</option>
        <option value="ACTIVE">正常</option>
        <option value="PENDING">待审批</option>
        <option value="REJECTED">已拒绝</option>
        <option value="DISABLED">已禁用</option>
      </select>
    </div>

    <!-- Table -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner-large"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="users.length === 0" class="empty-state">
      <p>暂无用户数据</p>
    </div>

    <div v-else class="table-wrapper glass-card">
      <table class="user-table">
        <thead>
          <tr>
            <th>用户</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>最后登录</th>
            <th v-if="isSuperAdmin">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>
              <div class="user-cell">
                <div class="avatar-sm">
                  {{ (user.nickname || user.username)?.charAt(0)?.toUpperCase() }}
                </div>
                <div>
                  <div class="cell-name">{{ user.nickname || user.username }}</div>
                  <div class="cell-username">@{{ user.username }}</div>
                </div>
              </div>
            </td>
            <td>
              <span class="role-badge" :class="'role-badge--' + user.role.toLowerCase()">
                {{ roleLabel(user.role) }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="'status-badge--' + user.status.toLowerCase()">
                {{ statusLabel(user.status) }}
              </span>
            </td>
            <td class="cell-date">{{ user.createdAt ? new Date(user.createdAt).toLocaleDateString('zh-CN') : '-' }}</td>
            <td class="cell-date">{{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString('zh-CN') : '从未登录' }}</td>
            <td v-if="isSuperAdmin">
              <div class="action-group">
                <button
                  v-if="user.status === 'ACTIVE'"
                  class="action-btn-sm action-btn-sm--warn"
                  @click="handleBan(user)"
                  title="封禁"
                >封禁</button>
                <button
                  v-if="user.status === 'DISABLED'"
                  class="action-btn-sm action-btn-sm--success"
                  @click="handleUnban(user)"
                  title="解禁"
                >解禁</button>
                <button
                  class="action-btn-sm action-btn-sm--danger"
                  @click="handleDelete(user)"
                  title="删除"
                >删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="currentPage === 0"
        @click="handlePageChange(currentPage)"
      >上一页</button>
      <span class="page-info">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      <button
        class="page-btn"
        :disabled="currentPage >= totalPages - 1"
        @click="handlePageChange(currentPage + 2)"
      >下一页</button>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.filters-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 36px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: var(--accent-1);
}

.filter-select {
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  cursor: pointer;
}

.filter-select option {
  background: #1a1a2e;
  color: var(--text-primary);
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
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.table-wrapper {
  overflow-x: auto;
  padding: 0;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
}

.user-table th {
  text-align: left;
  padding: 14px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-color);
}

.user-table td {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  font-size: 13px;
}

.user-table tr:last-child td {
  border-bottom: none;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-sm {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: white;
  flex-shrink: 0;
}

.cell-name {
  font-weight: 500;
  color: var(--text-primary);
}

.cell-username {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.cell-date {
  color: var(--text-muted);
  font-size: 12px;
}

.role-badge {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.role-badge--user {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.role-badge--admin {
  background: rgba(234, 179, 8, 0.15);
  color: #eab308;
  border: 1px solid rgba(234, 179, 8, 0.3);
}

.role-badge--super_admin {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
  border: 1px solid rgba(168, 85, 247, 0.3);
}

.status-badge {
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge--active {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.status-badge--pending {
  background: rgba(234, 179, 8, 0.15);
  color: #eab308;
}

.status-badge--rejected {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.status-badge--disabled {
  background: rgba(107, 114, 128, 0.15);
  color: #6b7280;
}

.action-group {
  display: flex;
  gap: 8px;
}

.action-btn-sm {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid;
  font-family: var(--font-display);
}

.action-btn-sm--warn {
  background: rgba(234, 179, 8, 0.1);
  border-color: rgba(234, 179, 8, 0.3);
  color: #eab308;
}

.action-btn-sm--warn:hover {
  background: rgba(234, 179, 8, 0.2);
}

.action-btn-sm--success {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.3);
  color: #22c55e;
}

.action-btn-sm--success:hover {
  background: rgba(34, 197, 94, 0.2);
}

.action-btn-sm--danger {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.action-btn-sm--danger:hover {
  background: rgba(239, 68, 68, 0.2);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: var(--font-display);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent-1);
  color: var(--accent-1);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--text-muted);
}
</style>
