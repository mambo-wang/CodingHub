<template>
  <div class="my-favorites-page">
    <GeneralizedSidebar :items="sidebarItems" />
    <div class="main-content">
      <div class="page-header">
        <h1>我的收藏</h1>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="favorites.length === 0" class="empty">暂无收藏</div>
      <div v-else class="post-list">
        <PostCard
          v-for="post in favorites"
          :key="post.id"
          :post="post"
          @click="goToDetail(post.id)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { LayoutGrid, FileText, Bookmark } from '@lucide/vue';
import { interactionApi } from '@/services/interaction';
import PostCard from '@/components/forum/PostCard.vue';
import GeneralizedSidebar, { type SidebarNavItem } from '@/components/common/GeneralizedSidebar.vue';
import type { ForumPost } from '@/types/forum';

const router = useRouter();

const sidebarItems: SidebarNavItem[] = [
  { label: '帖子列表', icon: LayoutGrid, to: '/forum' },
  { label: '我的帖子', icon: FileText, to: '/forum/my-posts', requiresAuth: true },
  { label: '我的收藏', icon: Bookmark, to: '/forum/my-favorites' }
];

const favorites = ref<ForumPost[]>([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const data = await interactionApi.getMyFavorites('FORUM_POST', 0, 100);
    favorites.value = data.content || [];
  } finally {
    loading.value = false;
  }
});

const goToDetail = (postId: number) => {
  router.push(`/forum/posts/${postId}`);
};
</script>

<style scoped>
.my-favorites-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px 80px;
  display: flex;
  gap: 32px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty, .loading {
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
</style>
