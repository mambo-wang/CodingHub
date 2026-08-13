<template>
  <div class="post-list-page">
    <GeneralizedSidebar :items="sidebarItems" />

    <div class="main-content">
      <div class="page-header">
        <h1>论坛</h1>
        <button v-if="isLoggedIn" @click="goToEditor" class="create-btn">
          <Plus :size="16" />
          发布帖子
        </button>
      </div>

      <div class="filter-bar">
        <div class="tag-filter" ref="tagDropdownRef">
          <button
            type="button"
            class="tag-filter-trigger"
            :class="{ active: selectedTag !== null, open: tagDropdownOpen }"
            aria-haspopup="listbox"
            :aria-expanded="tagDropdownOpen"
            @click="toggleTagDropdown"
          >
            <Tag :size="14" class="tag-filter-icon" />
            <span class="tag-filter-value">{{ selectedTagName || '全部标签' }}</span>
            <ChevronDown :size="14" class="tag-filter-arrow" :class="{ open: tagDropdownOpen }" />
          </button>
          <div v-if="tagDropdownOpen" class="tag-dropdown-panel" role="listbox" aria-label="按标签筛选">
            <div
              class="tag-dropdown-item"
              :class="{ selected: selectedTag === null }"
              role="option"
              :aria-selected="selectedTag === null"
              @click="handleTagSelect(null)"
            >
              <span class="tag-radio"></span>
              <span class="tag-option-name">全部标签</span>
            </div>
            <div
              v-for="tag in tags"
              :key="tag.id"
              class="tag-dropdown-item"
              :class="{ selected: selectedTag === tag.id }"
              role="option"
              :aria-selected="selectedTag === tag.id"
              @click="handleTagSelect(tag.id)"
            >
              <span class="tag-radio"></span>
              <span class="tag-option-name">{{ tag.name }}</span>
            </div>
          </div>
        </div>

        <CategoryFilter
          class="category-filter-wrap"
          :categories="categories"
          :selectedCategory="selectedCategory"
          @select="handleCategorySelect"
        />
      </div>

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

      <div class="sort-row">
        <SortTab v-model="sortBy" @update:modelValue="handleSortChange" />
      </div>

      <div v-if="loading" class="loading">加载中...</div>
      <div v-else class="post-list">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
          :editable="canEditPost(post.authorId)"
          :deletable="canDeletePost(post.authorId)"
          :isHot="hotTop5Ids.has(post.id)"
          @edit="goToEdit"
          @delete="handleDeletePost"
          @tag-click="handleTagSelect"
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

    <ConfirmDialog
      :visible="deleteDialogVisible"
      title="删除帖子"
      description="确定要删除这篇帖子吗？此操作不可撤销。"
      confirm-text="确认删除"
      cancel-text="取消"
      :danger="true"
      :loading="deleting"
      @confirm="handleConfirmDelete"
      @cancel="handleDialogCancel"
      @update:visible="deleteDialogVisible = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Plus, Search, ChevronLeft, ChevronRight, ChevronDown, LayoutGrid, FileText, Bookmark, Tag } from '@lucide/vue';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import forumService from '@/services/forum';
import PostCard from '@/components/forum/PostCard.vue';
import CategoryFilter from '@/components/forum/CategoryFilter.vue';
import SortTab from '@/components/common/SortTab.vue';
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const forumStore = useForumStore();
const authStore = useAuthStore();

const sidebarItems: SidebarNavItem[] = [
  { label: '帖子列表', icon: LayoutGrid, to: '/forum' },
  { label: '我的帖子', icon: FileText, to: '/forum/my-posts', requiresAuth: true },
  { label: '我的收藏', icon: Bookmark, to: '/forum/my-favorites', requiresAuth: true }
];

const { posts, categories, tags, pagination, loading } = storeToRefs(forumStore);
const { isLoggedIn } = storeToRefs(authStore);

const keyword = ref('');
const selectedCategory = ref<number | null>(null);
const selectedTag = ref<number | null>(null);
const sortBy = ref('hot');
const hotTop5Ids = ref<Set<number>>(new Set());
const deleteDialogVisible = ref(false);
const deleteTargetId = ref<number | null>(null);
const deleting = ref(false);

const page = computed(() => pagination.value.page);
const totalPages = computed(() => pagination.value.totalPages);

onMounted(async () => {
  await Promise.all([
    forumStore.fetchPosts({ sortBy: sortBy.value }),
    forumStore.fetchCategories(),
    forumStore.fetchTags(),
    fetchHotTop5()
  ]);
  document.addEventListener('click', handleTagOutsideClick);
});

onUnmounted(() => {
  document.removeEventListener('click', handleTagOutsideClick);
});

const fetchHotTop5 = async () => {
  try {
    const ids = await forumService.getHotTop5()
    hotTop5Ids.value = new Set(ids)
  } catch (error) {
    console.error('Failed to fetch hot top 5:', error)
  }
}

const handleCategorySelect = async (categoryId: number | null) => {
  selectedCategory.value = categoryId;
  await forumStore.fetchPosts({
    category: categoryId ?? undefined,
    tag: selectedTag.value ?? undefined,
    sortBy: sortBy.value,
    page: 0
  });
};

const tagDropdownOpen = ref(false);
const tagDropdownRef = ref<HTMLElement | null>(null);

const selectedTagName = computed(() => {
  if (selectedTag.value === null) return null;
  return tags.value.find(t => t.id === selectedTag.value)?.name || null;
});

const toggleTagDropdown = () => {
  tagDropdownOpen.value = !tagDropdownOpen.value;
};

const handleTagOutsideClick = (event: MouseEvent) => {
  if (tagDropdownRef.value && !tagDropdownRef.value.contains(event.target as Node)) {
    tagDropdownOpen.value = false;
  }
};

const handleTagSelect = async (tagId: number | null) => {
  selectedTag.value = tagId;
  tagDropdownOpen.value = false;
  await forumStore.fetchPosts({
    category: selectedCategory.value ?? undefined,
    tag: selectedTag.value ?? undefined,
    sortBy: sortBy.value,
    page: 0
  });
};

const handleSearch = async () => {
  await forumStore.fetchPosts({
    category: selectedCategory.value ?? undefined,
    tag: selectedTag.value ?? undefined,
    keyword: keyword.value || undefined,
    sortBy: sortBy.value,
    page: 0
  });
};

const changePage = async (newPage: number) => {
  await forumStore.fetchPosts({
    category: selectedCategory.value ?? undefined,
    tag: selectedTag.value ?? undefined,
    keyword: keyword.value || undefined,
    sortBy: sortBy.value,
    page: newPage
  });
};

const handleSortChange = async (value: string) => {
  sortBy.value = value;
  await forumStore.fetchPosts({
    category: selectedCategory.value ?? undefined,
    tag: selectedTag.value ?? undefined,
    keyword: keyword.value || undefined,
    sortBy: value,
    page: 0
  });
};

const goToDetail = (postId: number) => {
  router.push(`/forum/posts/${postId}`);
};

const goToEditor = () => {
  router.push('/forum/editor');
};

const canEditPost = (authorId: number) => {
  if (!authStore.isLoggedIn) return false
  return authStore.user?.id === authorId || authStore.isAdmin
}
const canDeletePost = canEditPost // same permission

const goToEdit = (postId: number) => {
  router.push(`/forum/posts/${postId}/edit`)
}

const handleDeletePost = (postId: number) => {
  deleteTargetId.value = postId
  deleteDialogVisible.value = true
}

const handleConfirmDelete = async () => {
  if (deleteTargetId.value === null) return
  deleting.value = true
  try {
    await forumService.deletePost(deleteTargetId.value)
    posts.value = posts.value.filter(p => p.id !== deleteTargetId.value)
    ElMessage.success('帖子已删除')
    deleteDialogVisible.value = false
  } catch (e: any) {
    if (e.response?.status === 403) {
      ElMessage.warning('无权操作此内容')
    } else {
      ElMessage.error('删除失败，请稍后重试')
    }
  } finally {
    deleting.value = false
  }
}

const handleDialogCancel = () => {
  deleteDialogVisible.value = false
}
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.category-filter-wrap {
  flex: 1;
  min-width: 0;
}

/* Tag Filter Dropdown */
.tag-filter {
  position: relative;
  flex: 0 0 auto;
}

.tag-filter-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  outline: none;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.tag-filter-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.tag-filter-trigger:hover {
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
}

.tag-filter-trigger.active {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.15));
  border-color: rgba(139, 92, 246, 0.4);
  color: var(--text-primary);
}

.tag-filter-trigger.open {
  border-color: rgba(139, 92, 246, 0.5);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.tag-filter-value {
  color: inherit;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-filter-arrow {
  color: var(--text-muted);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.tag-filter-arrow.open {
  transform: rotate(180deg);
}

.tag-dropdown-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 50;
  width: 180px;
  max-height: 320px;
  overflow-y: auto;
  padding: 6px;
  background: var(--bg-glass, rgba(20, 20, 30, 0.95));
  border: 1px solid var(--border-color);
  border-radius: 12px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.35);
  animation: tagDropIn 0.15s ease;
}

@keyframes tagDropIn {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

.tag-dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tag-dropdown-item:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--text-primary);
}

.tag-dropdown-item.selected {
  color: var(--text-primary);
  font-weight: 500;
}

.tag-option-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-radio {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1.5px solid var(--border-color);
  flex-shrink: 0;
  position: relative;
  transition: all 0.15s ease;
}

.tag-dropdown-item:hover .tag-radio {
  border-color: rgba(139, 92, 246, 0.5);
}

.tag-dropdown-item.selected .tag-radio {
  border-color: var(--accent-1, #8b5cf6);
}

.tag-dropdown-item.selected .tag-radio::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: var(--accent-1, #8b5cf6);
}

[data-theme="light"] .tag-dropdown-panel {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
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

.sort-row {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
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