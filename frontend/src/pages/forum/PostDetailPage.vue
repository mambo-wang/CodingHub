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
              <AuthorBadge
                :username="post.authorName"
                :nickname="post.authorNickname"
                size="md"
              />
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

        <div class="post-actions">
          <UnifiedLikeButton
            target-type="FORUM_POST"
            :target-id="post.id"
            :initial-count="post.likeCount"
          />
          <UnifiedFavoriteButton
            target-type="FORUM_POST"
            :target-id="post.id"
          />
          <button
            v-if="canModify"
            class="action-btn"
            @click="handleEdit"
            aria-label="编辑帖子"
          >
            <Pencil :size="18" />
            编辑
          </button>
          <button
            v-if="canModify"
            data-testid="delete-post-btn"
            class="action-btn btn-delete"
            :disabled="deleting"
            @click="handleDeleteClick"
          >
            <Trash2 :size="18" />
            删除
          </button>
        </div>

        <PostContent :content="post.content" />
      </div>

      <div class="comments-section">
        <UnifiedCommentSection
          target-type="FORUM_POST"
          :target-id="Number(route.params.id)"
          @comment-added="handleCommentAdded"
        />
      </div>

      <ConfirmDialog
        :visible="dialogVisible"
        title="删除帖子"
        description="确定要删除这篇帖子吗？此操作不可撤销。"
        confirm-text="确认删除"
        cancel-text="取消"
        :danger="true"
        :loading="deleting"
        @confirm="handleConfirmDelete"
        @cancel="handleDialogCancel"
        @update:visible="dialogVisible = $event"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { User, Eye, MessageCircle, Heart, Trash2, Loader2, Pencil } from '@lucide/vue';
import { ElMessage } from 'element-plus';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import PostContent from '@/components/forum/PostContent.vue';
import UnifiedLikeButton from '@/components/common/UnifiedLikeButton.vue';
import UnifiedFavoriteButton from '@/components/common/UnifiedFavoriteButton.vue';
import UnifiedCommentSection from '@/components/common/UnifiedCommentSection.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
import type { CommentResponse } from '@/services/interaction';
import AuthorBadge from '@/components/AuthorBadge.vue';

const route = useRoute();
const router = useRouter();
const forumStore = useForumStore();
const authStore = useAuthStore();

const { currentPost: post } = storeToRefs(forumStore);

const canModify = computed(() => {
  if (!authStore.isLoggedIn || !post.value) return false;
  return authStore.user?.id === post.value.authorId || authStore.isAdmin;
});

const handleEdit = () => {
  if (post.value) router.push(`/forum/posts/${post.value.id}/edit`);
};

const loading = ref(true);
const dialogVisible = ref(false);
const deleting = ref(false);

const categoryColors: Record<number, string> = {
  1: '#7C3AED',
  2: '#2563EB',
  3: '#059669',
  4: '#DC2626',
  5: '#D97706',
};

const getCategoryBg = (categoryId: number) =>
  `${categoryColors[categoryId] || '#7C3AED'}15`;

onMounted(async () => {
  const postId = Number(route.params.id);
  try {
    await forumStore.fetchPostById(postId);
  } finally {
    loading.value = false;
  }
});

const handleCommentAdded = (_comment: CommentResponse) => {
  if (post.value) {
    post.value.commentCount += 1;
  }
};

const handleDeleteClick = () => {
  dialogVisible.value = true;
};

const handleDialogCancel = () => {
  dialogVisible.value = false;
};

const handleConfirmDelete = async () => {
  if (!post.value) return;
  deleting.value = true;

  try {
    const result = await forumStore.deletePost(post.value.id);
    if (result.success) {
      ElMessage.success('帖子已删除');
      dialogVisible.value = false;
      router.push('/forum');
    } else {
      handleDeleteError(result.errorCode!);
    }
  } catch (e) {
    ElMessage.error('删除失败，请稍后重试');
  } finally {
    deleting.value = false;
  }
};

const handleDeleteError = (errorCode: string) => {
  switch (errorCode) {
    case 'AUTH':
      ElMessage.warning('请先登录');
      break;
    case 'FORBIDDEN':
      ElMessage.warning('您不是该帖子的作者，无权删除');
      break;
    case 'NOT_FOUND':
      ElMessage.warning('帖子不存在或已被删除');
      dialogVisible.value = false;
      router.push('/forum');
      break;
    default:
      ElMessage.error('删除失败，请稍后重试');
      dialogVisible.value = false;
      break;
  }
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

.post-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.action-btn {
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

.action-btn:hover:not(:disabled) {
  border-color: var(--accent-3);
  color: var(--accent-3);
  background: rgba(236, 72, 153, 0.1);
}

.action-btn.liked {
  border-color: var(--accent-3);
  background: rgba(236, 72, 153, 0.15);
  color: var(--accent-3);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-delete {
  margin-left: auto;
  background: var(--color-destructive);
  color: #FFFFFF;
  border-color: transparent;
}

.btn-delete:hover:not(:disabled) {
  background: #DC2626;
  border-color: transparent !important;
  color: #FFFFFF !important;
  box-shadow: 0 0 20px var(--delete-shadow);
  transform: translateY(-1px);
}

.btn-delete:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.btn-delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.comments-section {
  margin-top: 24px;
}
</style>