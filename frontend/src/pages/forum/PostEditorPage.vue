<template>
  <div class="post-editor-page">
    <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div class="form-group">
      <input
        v-model="title"
        placeholder="标题"
        class="title-input"
      />
    </div>

    <div class="form-group">
      <label>分类</label>
      <select v-model="categoryId">
        <option value="">选择分类</option>
        <option
          v-for="cat in categories"
          :key="cat.id"
          :value="cat.id"
        >
          {{ cat.name }}
        </option>
      </select>
    </div>

    <div class="form-group">
      <label>内容（Markdown）</label>
      <textarea
        v-model="content"
        placeholder="输入内容..."
        class="content-input"
        rows="15"
      ></textarea>
    </div>

    <div class="form-actions">
      <button @click="publish" class="publish-btn">发布</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useForumStore } from '@/stores/forum';
import forumService from '@/services/forum';

const router = useRouter();
const route = useRoute();
const forumStore = useForumStore();
const { categories } = storeToRefs(forumStore);

const isEdit = computed(() => !!route.params.id);

const title = ref('');
const categoryId = ref<number | ''>('');
const content = ref('');
const errorMessage = ref('');
const loading = ref(false);

onMounted(async () => {
  await forumStore.fetchCategories();
});

const publish = async () => {
  errorMessage.value = '';

  if (!title.value.trim()) {
    errorMessage.value = '请填写标题';
    return;
  }

  if (!categoryId.value) {
    errorMessage.value = '请选择分类';
    return;
  }

  loading.value = true;
  try {
    await forumService.createPost({
      title: title.value,
      content: content.value,
      categoryId: categoryId.value as number
    });

    router.push('/forum');
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || '发布失败，请重试';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.post-editor-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 4px;
  font-weight: bold;
}

.title-input {
  width: 100%;
  padding: 12px;
  font-size: 18px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.content-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: monospace;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.form-actions button {
  padding: 12px 24px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.form-actions .publish-btn {
  background: #007bff;
  color: white;
  border: none;
}

.form-actions .publish-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-message {
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
  color: #ef4444;
  font-size: 14px;
  margin-bottom: 20px;
}
</style>