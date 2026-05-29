<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { ToolSummary, PageResponse } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const tools = ref<ToolSummary[]>([])
const loading = ref(false)
const deletingId = ref<number | null>(null)
const pagination = ref({
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0
})

const fetchMyTools = async () => {
  loading.value = true
  try {
    const response = await api.get('/users/me/tools', {
      params: {
        page: pagination.value.page,
        size: pagination.value.size
      }
    })
    const data: PageResponse<ToolSummary> = response.data.data
    tools.value = data.content
    pagination.value = {
      page: data.page,
      size: data.size,
      totalElements: data.totalElements,
      totalPages: data.totalPages
    }
  } catch (error) {
    ElMessage.error('加载工具列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (toolId: number) => {
  router.push(`/me/tools/${toolId}/edit`)
}

const handleDelete = async (toolId: number, toolName: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除工具"${toolName}"吗？删除后无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    deletingId.value = toolId
    try {
      await api.delete(`/tools/${toolId}`)
      ElMessage.success('删除成功')
      fetchMyTools()
    } catch (error) {
      ElMessage.error('删除失败')
    } finally {
      deletingId.value = null
    }
  } catch {
    // User cancelled
  }
}

const handlePageChange = (page: number) => {
  pagination.value.page = page - 1
  fetchMyTools()
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  fetchMyTools()
})
</script>

<template>
  <div class="my-tools-page">
    <div class="app-container">
      <div class="page-header">
        <h1>我的工具</h1>
        <p class="subtitle">管理您上传的所有工具</p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- Empty State -->
      <el-empty v-else-if="tools.length === 0" description="您还没有上传任何工具">
        <el-button type="primary" @click="router.push('/tools/upload')">
          上传第一个工具
        </el-button>
      </el-empty>

      <!-- Tools List -->
      <div v-else class="tools-list">
        <div
          v-for="tool in tools"
          :key="tool.id"
          class="tool-item"
        >
          <div class="tool-info">
            <span class="category-badge">
              {{ tool.categoryIcon }} {{ tool.categoryName }}
            </span>
            <h3 class="tool-name">{{ tool.name }}</h3>
            <div class="tool-meta">
              <span>上传于 {{ new Date(tool.createdAt).toLocaleDateString() }}</span>
            </div>
          </div>
          <div class="tool-actions">
            <el-button
              type="primary"
              @click="handleEdit(tool.id)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              :loading="deletingId === tool.id"
              @click="handleDelete(tool.id, tool.name)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="pagination.totalPages > 1" class="pagination-container">
        <el-pagination
          :current-page="pagination.page + 1"
          :page-size="pagination.size"
          :total="pagination.totalElements"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.my-tools-page {
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.app-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.loading-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.tools-list {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.tool-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.tool-item:last-child {
  border-bottom: none;
}

.tool-info {
  flex: 1;
}

.category-badge {
  display: inline-block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
}

.tool-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #303133;
}

.tool-meta {
  font-size: 12px;
  color: #909399;
}

.tool-actions {
  display: flex;
  gap: 8px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
