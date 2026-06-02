<template>
  <div class="post-card" @click="goToDetail">
    <div class="card-accent" :style="{ background: getCategoryColor(post.categoryId) }"></div>
    <div class="card-content">
      <div class="card-header">
        <span class="category-tag" :style="{ background: getCategoryBg(post.categoryId) }">
          {{ post.categoryName }}
        </span>
        <h3 class="post-title">{{ post.title }}</h3>
      </div>
      <div class="card-meta">
        <div class="author-info">
          <div class="avatar-placeholder">
            <User :size="14" />
          </div>
          <AuthorBadge
            :username="post.authorName"
            :nickname="post.authorNickname"
            size="sm"
          />
        </div>
        <span class="separator">·</span>
        <span class="post-date">{{ formatDate(post.createdAt) }}</span>
      </div>
      <div class="card-stats">
        <span class="stat-item">
          <Eye :size="14" />
          {{ formatCount(post.viewCount) }}
        </span>
        <span class="stat-item">
          <MessageCircle :size="14" />
          {{ post.commentCount }}
        </span>
        <span class="stat-item">
          <Heart :size="14" />
          {{ post.likeCount }}
        </span>
        <span class="stat-item favorite-btn" :class="{ active: post.isFavorited }" @click.stop="toggleFavorite">
          <Bookmark :size="14" :fill="post.isFavorited ? 'currentColor' : 'none'" />
          {{ post.favoriteCount || 0 }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, Eye, MessageCircle, Heart, Bookmark } from '@lucide/vue';
import type { ForumPost } from '@/types/forum';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { postFavoriteApi } from '@/services/api';
import AuthorBadge from '@/components/AuthorBadge.vue';

const props = defineProps<{ post: ForumPost }>();
const router = useRouter();
const authStore = useAuthStore();

// 分类颜色映射
const categoryColors: Record<number, string> = {
  1: '#7C3AED', // 紫色
  2: '#2563EB', // 蓝色
  3: '#059669', // 绿色
  4: '#DC2626', // 红色
  5: '#D97706', // 橙色
};

const getCategoryColor = (categoryId: number) => categoryColors[categoryId] || '#7C3AED';
const getCategoryBg = (categoryId: number) => `${categoryColors[categoryId] || '#7C3AED'}15`;

const goToDetail = () => {
  router.push(`/forum/posts/${props.post.id}`);
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const days = Math.floor(hours / 24);

  if (hours < 1) return '刚刚';
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
};

const formatCount = (count: number) => {
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`;
  return count.toString();
};

const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login');
    return;
  }
  try {
    await postFavoriteApi.toggleFavorite(props.post.id);
    props.post.isFavorited = !props.post.isFavorited;
    props.post.favoriteCount = (props.post.favoriteCount || 0) + (props.post.isFavorited ? 1 : -1);
  } catch (error) {
    console.error('Toggle favorite failed:', error);
  }
};
</script>

<style scoped>
.post-card {
  display: flex;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.post-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-glow);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3), 0 0 40px rgba(139, 92, 246, 0.15);
}

.card-accent {
  width: 4px;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--accent-1);
  white-space: nowrap;
}

.post-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.avatar-placeholder {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.author-name {
  color: var(--text-primary);
  font-weight: 500;
}

.separator {
  color: var(--text-muted);
}

.post-date {
  color: var(--text-muted);
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-top: 4px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.stat-item svg {
  opacity: 0.7;
}

.favorite-btn {
  cursor: pointer;
  transition: color 0.2s;
}

.favorite-btn:hover {
  color: var(--accent-1);
}

.favorite-btn.active {
  color: var(--accent-1);
}

.favorite-btn.active svg {
  opacity: 1;
}
</style>