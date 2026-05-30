<template>
  <div class="post-detail-page">
    <div v-if="loading" class="loading">
      <Loader2 :size="24" class="spin" />
      加载中...
    </div>
    <div v-else-if="post" class="post-detail">
      <div class="post-card">
        <div class="post-header">
          <div class="category-tag" :style="{ background: getCategoryBg(post.categoryId) }">
            {{ post.categoryName }}
          </div>
          <h1 class="post-title">{{ post.title }}</h1>
          <div class="post-meta">
            <div class="author-info">
              <div class="author-avatar">
                <User :size="16" />
              </div>
              <span>{{ post.authorName }}</span>
            </div>
            <span class="separator">·</span>
            <span>{{ formatDate(post.createdAt) }}</span>
          </div>
        </div>

        <div class="post-stats">
          <span class="stat-item">
            <Eye :size="16" />
            {{ formatCount(post.viewCount) }} 浏览
          </span>
          <span class="stat-item">
            <MessageCircle :size="16" />
            {{ post.commentCount }} 评论
          </span>
          <span class="stat-item">
            <Heart :size="16" />
            {{ post.likeCount }} 点赞
          </span>
        </div>

        <div class="like-section">
          <button @click="handleLike" :class="['like-btn', { liked: hasLiked }]">
            <Heart :size="18" :fill="hasLiked ? 'currentColor' : 'none'" />
            {{ hasLiked ? '已赞' : '点赞' }}
          </button>
          <button @click="handleFavorite" :class="['like-btn', { liked: hasFavorited }]">
            <Bookmark :size="18" :fill="hasFavorited ? 'currentColor' : 'none'" />
            {{ hasFavorited ? '已收藏' : '收藏' }}
          </button>
        </div>

        <PostContent :content="post.content" />
      </div>

      <div class="comments-section">
        <CommentList
          :comments="comments"
          @reply="handleReply"
          @like="handleCommentLike"
        />

        <CommentEditor
          :isLoggedIn="isLoggedIn"
          :parentId="replyToId"
          :replyingToName="replyingToName"
          @submit="handleCommentSubmit"
          @cancel="handleCancelReply"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { User, Eye, MessageCircle, Heart, Bookmark, Loader2 } from '@lucide/vue';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import PostContent from '@/components/forum/PostContent.vue';
import CommentList from '@/components/forum/CommentList.vue';
import CommentEditor from '@/components/forum/CommentEditor.vue';
import forumService from '@/services/forum';
import { postFavoriteApi } from '@/services/api';
import type { ForumComment } from '@/types/forum';

const route = useRoute();
const forumStore = useForumStore();
const authStore = useAuthStore();

const { currentPost: post } = storeToRefs(forumStore);
const { isLoggedIn } = storeToRefs(authStore);

const loading = ref(true);
const comments = ref<ForumComment[]>([]);
const replyToId = ref<number | undefined>();
const hasLiked = ref(false);
const hasFavorited = ref(false);

const categoryColors: Record<number, string> = {
  1: '#7C3AED',
  2: '#2563EB',
  3: '#059669',
  4: '#DC2626',
  5: '#D97706',
};

const getCategoryBg = (categoryId: number) =>
  `${categoryColors[categoryId] || '#7C3AED'}15`;

const replyingToName = computed(() => {
  if (!replyToId.value) return undefined;
  const comment = comments.value.find(c => c.id === replyToId.value);
  return comment?.authorName || undefined;
});

onMounted(async () => {
  const postId = Number(route.params.id);

  try {
    await forumStore.fetchPostById(postId);
    comments.value = await forumService.getComments(postId);
  } finally {
    loading.value = false;
  }
});

const handleLike = async () => {
  if (!post.value) return;

  try {
    await forumService.like({ postId: post.value.id });
    hasLiked.value = true;
    post.value.likeCount += 1;
  } catch (e) {}
};

const handleFavorite = async () => {
  if (!post.value) return;

  try {
    await postFavoriteApi.toggleFavorite(post.value.id);
    hasFavorited.value = !hasFavorited.value;
  } catch (e) {}
};

const handleReply = (commentId: number) => {
  replyToId.value = commentId;
};

const handleCancelReply = () => {
  replyToId.value = undefined;
};

const handleCommentLike = async (commentId: number) => {
  try {
    await forumService.like({ commentId });
  } catch (e) {}
};

const handleCommentSubmit = async (data: { content: string; authorName?: string; parentId?: number }) => {
  if (!post.value) return;

  try {
    const newComment = await forumService.createComment(post.value.id, {
      content: data.content,
      parentId: data.parentId,
      authorName: data.authorName
    });

    comments.value.push(newComment);
    replyToId.value = undefined;
  } catch (e) {}
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN');
};

const formatCount = (count: number) => {
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`;
  return count.toString();
};
</script>

<style scoped>
.post-detail-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px 80px;
  position: relative;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--accent-1);
  font-size: 15px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.post-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.post-card:hover {
  border-color: var(--border-glow);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.post-header {
  margin-bottom: 24px;
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--accent-1);
  margin-bottom: 12px;
}

.post-title {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-secondary);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.separator {
  color: var(--text-muted);
}

.post-stats {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.like-section {
  margin-bottom: 32px;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid var(--border-color);
  border-radius: 24px;
  background: var(--bg-glass);
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.like-btn:hover {
  border-color: var(--accent-3);
  color: var(--accent-3);
  background: rgba(236, 72, 153, 0.1);
}

.like-btn.liked {
  border-color: var(--accent-3);
  background: rgba(236, 72, 153, 0.15);
  color: var(--accent-3);
}

.comments-section {
  margin-top: 24px;
}
</style>