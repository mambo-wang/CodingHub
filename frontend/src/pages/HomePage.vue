<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/services/api'
import type { ToolSummary, Category, PageResponse } from '@/types'

const router = useRouter()

const tools = ref<ToolSummary[]>([])
const categories = ref<Category[]>([])
const selectedCategory = ref<number | null>(null)
const searchKeyword = ref('')
const sortBy = ref('latest')
const loading = ref(false)
const pagination = ref({
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0
})

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
  } catch (error) {
    console.error('Failed to fetch categories:', error)
  }
}

const fetchTools = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      page: pagination.value.page,
      size: pagination.value.size,
      sortBy: sortBy.value
    }
    if (selectedCategory.value) params.categoryId = selectedCategory.value
    if (searchKeyword.value) params.keyword = searchKeyword.value

    const response = await api.get('/tools', { params })
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

const handleSearch = () => {
  pagination.value.page = 0
  fetchTools()
}

const handleCategoryChange = (categoryId: number | null) => {
  selectedCategory.value = categoryId
  pagination.value.page = 0
  fetchTools()
}

const handleSortChange = (value: string) => {
  sortBy.value = value
  fetchTools()
}

const handlePageChange = (page: number) => {
  pagination.value.page = page - 1
  fetchTools()
}

const goToDetail = (toolId: number) => {
  router.push(`/tools/${toolId}`)
}

onMounted(() => {
  fetchCategories()
  fetchTools()
})
</script>

<template>
  <div class="home-page">
    <div class="app-container">
      <!-- Search and Filter Bar -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索工具名称..."
          class="search-input"
          @keyup.enter="handleSearch"
          clearable
        >
          <template #prefix>
            <el-icon><search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="selectedCategory"
          placeholder="全部分类"
          class="category-select"
          clearable
          @change="handleCategoryChange"
        >
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          >
            <span>{{ cat.icon }} {{ cat.name }}</span>
          </el-option>
        </el-select>

        <el-select
          v-model="sortBy"
          class="sort-select"
          @change="handleSortChange"
        >
          <el-option label="最新上传" value="latest" />
          <el-option label="按名称" value="name" />
        </el-select>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- Tools Grid -->
      <div v-else-if="tools.length > 0" class="tools-grid">
        <div
          v-for="tool in tools"
          :key="tool.id"
          class="tool-card"
          @click="goToDetail(tool.id)"
        >
          <div class="tool-card-header">
            <span class="category-badge">
              {{ tool.categoryIcon }} {{ tool.categoryName }}
            </span>
          </div>
          <h3 class="tool-name">{{ tool.name }}</h3>
          <div class="tool-meta">
            <span class="uploader">👤 {{ tool.uploaderUsername }}</span>
            <span class="date">{{ new Date(tool.createdAt).toLocaleDateString() }}</span>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <el-empty v-else description="暂无工具" />

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
.home-page {
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-input {
  flex: 1;
  max-width: 300px;
}

.category-select,
.sort-select {
  width: 150px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.tool-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.tool-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.tool-card-header {
  margin-bottom: 12px;
}

.category-badge {
  font-size: 12px;
  color: #606266;
}

.tool-name {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.loading-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
