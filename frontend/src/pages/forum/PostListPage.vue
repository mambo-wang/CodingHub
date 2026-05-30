<template>
  <div class="post-list-page">
    <div class="page-header">
      <h1>论坛</h1>
      <button v-if="isLoggedIn" @click="goToEditor" class="create-btn">
        发布帖子
      </button>
    </div>

    <CategoryFilter
      :categories="categories"
      :selectedCategory="selectedCategory"
      @select="handleCategorySelect"
    />

    <div class="search-bar">
      <input
        v-model="keyword"
        @keydown.enter="handleSearch"
        placeholder="搜索帖子标题..."
      />
      <button @click="handleSearch">搜索</button>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="post-list">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @click="goToDetail(post.id)"
      />
      <div v-if="posts.length === 0" class="empty">
        暂无帖子
      </div>
    </div>

    <div class="pagination" v-if="totalPages > 1">
      <button @click="changePage(page - 1)" :disabled="page === 0">
        上一页
      </button>
      <span>{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="changePage(page + 1)" :disabled="page >= totalPages - 1">
        下一页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import PostCard from '@/components/forum/PostCard.vue';
import CategoryFilter from '@/components/forum/CategoryFilter.vue';

const router = useRouter();
const forumStore = useForumStore();
const authStore = useAuthStore();

const { posts, categories, pagination, loading } = storeToRefs(forumStore);
const { isLoggedIn } = storeToRefs(authStore);

const keyword = ref('');
const selectedCategory = ref<number | null>(null);

const page = computed(() => pagination.value.page);
const totalPages = computed(() => pagination.value.totalPages);

onMounted(async () => {
  await Promise.all([
    forumStore.fetchPosts(),
    forumStore.fetchCategories()
  ]);
});

const handleCategorySelect = async (categoryId: number | null) => {
  selectedCategory.value = categoryId;
  await forumStore.fetchPosts({
    category: categoryId ?? undefined,
    page: 0
  });
};

const handleSearch = async () => {
  await forumStore.fetchPosts({
    keyword: keyword.value || undefined,
    page: 0
  });
};

const changePage = async (newPage: number) => {
  await forumStore.fetchPosts({
    category: selectedCategory.value ?? undefined,
    keyword: keyword.value || undefined,
    page: newPage
  });
};

const goToDetail = (postId: number) => {
  router.push(`/forum/posts/${postId}`);
};

const goToEditor = () => {
  router.push('/forum/editor');
};
</script>

<style scoped>
.post-list-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
}

.create-btn {
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin: 16px 0;
}

.search-bar input {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.search-bar button {
  padding: 8px 16px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0;
}

.empty {
  text-align: center;
  color: #999;
  padding: 32px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.pagination button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 32px;
  color: #666;
}
</style>