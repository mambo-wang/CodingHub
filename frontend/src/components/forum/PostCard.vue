<template>
  <div class="post-card" @click="goToDetail">
    <h3 class="post-title">{{ post.title }}</h3>
    <div class="post-meta">
      <span class="author">{{ post.authorName }}</span>
      <span class="category">{{ post.categoryName }}</span>
      <span class="date">{{ formatDate(post.createdAt) }}</span>
    </div>
    <div class="post-stats">
      <span>👁 {{ post.viewCount }}</span>
      <span>❤️ {{ post.likeCount }}</span>
      <span>💬 {{ post.commentCount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ForumPost } from '@/types/forum';
import { useRouter } from 'vue-router';

const props = defineProps<{ post: ForumPost }>();
const router = useRouter();

const goToDetail = () => {
  router.push(`/forum/posts/${props.post.id}`);
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN');
};
</script>

<style scoped>
.post-card {
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.post-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.post-title {
  margin: 0 0 8px;
}

.post-meta {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.post-stats {
  font-size: 12px;
  color: #999;
}

.post-stats span {
  margin-right: 12px;
}
</style>