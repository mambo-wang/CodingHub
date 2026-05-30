<template>
  <div class="post-detail-page">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="post" class="post-detail">
      <div class="post-header">
        <h1>{{ post.title }}</h1>
        <div class="post-meta">
          <span>作者：{{ post.authorName }}</span>
          <span>分类：{{ post.categoryName }}</span>
          <span>发布于：{{ formatDate(post.createdAt) }}</span>
        </div>
        <div class="post-stats">
          <span>👁 {{ post.viewCount }}</span>
          <span>❤️ {{ post.likeCount }}</span>
          <span>💬 {{ post.commentCount }}</span>
        </div>
      </div>

      <div class="like-section">
        <button @click="handleLike" :class="{ liked: hasLiked }">
          ❤️ 点赞 {{ post.likeCount }}
        </button>
      </div>

      <PostContent :content="post.content" />

      <CommentList
        :comments="comments"
        @reply="handleReply"
        @like="handleCommentLike"
      />

      <CommentEditor
        :isLoggedIn="isLoggedIn"
        :parentId="replyToId"
        @submit="handleCommentSubmit"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';
import PostContent from '@/components/forum/PostContent.vue';
import CommentList from '@/components/forum/CommentList.vue';
import CommentEditor from '@/components/forum/CommentEditor.vue';
import forumService from '@/services/forum';
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
    // 手动递增点赞数
    post.value.likeCount += 1;
  } catch (e) {
    // 已点赞等错误
  }
};

const handleReply = (commentId: number) => {
  replyToId.value = commentId;
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
</script>

<style scoped>
.post-detail-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.post-detail {
  background: white;
  padding: 24px;
  border-radius: 8px;
}

.post-header h1 {
  margin: 0 0 16px;
}

.post-meta {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.post-stats {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 14px;
}

.like-section {
  margin: 24px 0;
}

.like-section button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
}

.like-section button.liked {
  background: #ffe0e0;
  border-color: #ffcccc;
}

.loading {
  text-align: center;
  padding: 32px;
  color: #666;
}
</style>