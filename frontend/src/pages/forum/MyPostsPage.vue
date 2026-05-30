<template>
  <div class="my-posts-page">
    <div class="page-header">
      <h1>我的帖子</h1>
    </div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">暂无帖子</div>
    <div v-else class="post-list">
      <PostCard
        v-for="post in posts"
        :key="post.id"
        :post="post"
        @click="goToDetail(post.id)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useForumStore } from '@/stores/forum';
import PostCard from '@/components/forum/PostCard.vue';

const router = useRouter();
const forumStore = useForumStore();
const { posts, loading } = storeToRefs(forumStore);

onMounted(async () => {
  await forumStore.fetchMyPosts();
});

const goToDetail = (postId: number) => {
  router.push(`/forum/posts/${postId}`);
};
</script>

<style scoped>
.my-posts-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px 80px;
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
