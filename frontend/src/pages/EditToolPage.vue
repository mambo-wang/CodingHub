<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '@/services/api'
import { useAuthStore } from '@/stores/auth'
import type { Category, ToolDetail, UpdateToolRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)
const categories = ref<Category[]>([])
const toolId = ref<number>(0)
const previewContent = ref('')

const form = ref<UpdateToolRequest>({
  name: '',
  categoryId: 0,
  content: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入工具名称', trigger: 'blur' },
    { min: 1, max: 100, message: '工具名称长度为1-100字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入介绍内容', trigger: 'blur' },
    { max: 5000, message: '介绍内容最大5000字符', trigger: 'blur' }
  ]
}

const md = new MarkdownIt()
const renderedPreview = () => {
  previewContent.value = md.render(form.value.content || '')
}

const fetchCategories = async () => {
  try {
    const response = await api.get('/categories')
    categories.value = response.data.data
  } catch (error) {
    ElMessage.error('加载分类失败')
  }
}

const fetchTool = async () => {
  loading.value = true
  try {
    const response = await api.get(`/tools/${toolId.value}`)
    const tool: ToolDetail = response.data.data

    // Check ownership
    if (tool.uploaderId !== authStore.user?.id) {
      ElMessage.error('您只能编辑自己的工具')
      router.push('/me/tools')
      return
    }

    form.value.name = tool.name
    form.value.content = tool.content

    // Find category id by name
    const category = categories.value.find(c => c.name === tool.categoryName)
    if (category) {
      form.value.categoryId = category.id
    }

    renderedPreview()
  } catch (error) {
    ElMessage.error('加载工具详情失败')
    router.push('/me/tools')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await api.put(`/tools/${toolId.value}`, form.value)
      ElMessage.success('更新成功')
      router.push('/me/tools')
    } catch (error: any) {
      const message = error.response?.data?.message || '更新失败'
      ElMessage.error(message)
    } finally {
      submitting.value = false
    }
  })
}

const goBack = () => {
  router.push('/me/tools')
}

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }

  toolId.value = Number(route.params.id)
  if (!toolId.value) {
    router.push('/me/tools')
    return
  }

  await fetchCategories()
  await fetchTool()
})
</script>

<template>
  <div class="edit-page">
    <div class="app-container">
      <div class="page-header">
        <h1>编辑工具</h1>
        <p class="subtitle">修改工具信息</p>
      </div>

      <div class="form-container">
        <el-skeleton v-if="loading" :rows="10" animated />

        <el-form
          v-else
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="tool-form"
        >
          <el-form-item label="工具名称" prop="name">
            <el-input
              v-model="form.name"
              placeholder="请输入工具名称"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="`${cat.icon} ${cat.name}`"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="Markdown 介绍" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              placeholder="请输入工具介绍（支持 Markdown 格式）"
              maxlength="5000"
              show-word-limit
              @input="renderedPreview"
            />
          </el-form-item>

          <div v-if="form.content" class="preview-section">
            <h3>预览效果</h3>
            <div class="markdown-body" v-html="previewContent"></div>
          </div>

          <el-form-item class="form-actions">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              保存
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}

.app-container {
  max-width: 800px;
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

.form-container {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.tool-form {
  max-width: 100%;
}

.preview-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.preview-section h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.markdown-body {
  line-height: 1.6;
  color: #303133;
}

.markdown-body :deep(h1) {
  font-size: 20px;
  margin: 16px 0 8px 0;
}

.markdown-body :deep(h2) {
  font-size: 18px;
  margin: 14px 0 8px 0;
}

.markdown-body :deep(p) {
  margin: 0 0 12px 0;
}

.markdown-body :deep(code) {
  background: #ebeef5;
  padding: 2px 4px;
  border-radius: 2px;
  font-size: 13px;
}

.markdown-body :deep(pre) {
  background: #1e1e1e;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

.markdown-body :deep(pre code) {
  background: transparent;
  color: #d4d4d4;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
