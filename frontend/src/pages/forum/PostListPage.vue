<template>
  <div class="post-list-page">
    <SidebarNav />

    <div class="main-content">
      <div class="page-header">
        <h1>论坛</h1>
        <button v-if="isLoggedIn" @click="goToEditor" class="create-btn">
          <Plus :size="16" />
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
        <button @click="handleSearch">
          <Search :size="16" />
          搜索
        </button>
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
          <ChevronLeft :size="16" />
          上一页
        </button>
        <span>{{ page + 1 }} / {{ totalPages }}</span>
        <button @click="changePage(page + 1)" :disabled="page >= totalPages - 1">
          下一页
          <ChevronRight :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Plus, Search, ChevronLeft, ChevronRight } from '@lucide/vue';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import PostCard from '@/components/forum/PostCard.vue';
import CategoryFilter from '@/components/forum/CategoryFilter.vue';
import SidebarNav from '@/components/forum/SidebarNav.vue';

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
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px 80px;
  position: relative;
  display: flex;
  gap: 32px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-header h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h1::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 32px;
  background: linear-gradient(180deg, var(--accent-1), var(--accent-2));
  border-radius: 2px;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  padding: 16px 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.search-bar input {
  flex: 1;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s;
}

.search-bar input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.search-bar input::placeholder {
  color: var(--text-muted);
}

.search-bar button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: var(--accent-1);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.search-bar button:hover {
  background: #7c3aed;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty {
  text-align: center;
  padding: 48px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 15px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
}

.pagination button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.pagination button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.pagination span {
  color: var(--text-secondary);
  font-size: 14px;
}

.loading {
  text-align: center;
  padding: 48px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-muted);
}
</style>