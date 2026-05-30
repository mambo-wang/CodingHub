<template>
  <div class="comment-item" :class="{ 'is-reply': comment.parentId !== null }">
    <div class="comment-header">
      <span class="author">{{ comment.authorName || '访客' }}</span>
      <span class="date">{{ formatDate(comment.createdAt) }}</span>
    </div>
    <div class="comment-content">{{ comment.content }}</div>
    <div class="comment-actions">
      <button @click="handleLike">❤️ {{ comment.likeCount }}</button>
      <button @click="handleReply">回复</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ForumComment } from '@/types/forum';

const props = defineProps<{
  comment: ForumComment;
  children?: ForumComment[];
}>();

const emit = defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'like', commentId: number): void;
}>();

const handleReply = () => emit('reply', props.comment.id);
const handleLike = () => emit('like', props.comment.id);

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN');
};
</script>

<style scoped>
.comment-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.comment-item.is-reply {
  margin-left: 24px;
  border-left: 2px solid #ddd;
}

.comment-header {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.comment-content {
  margin: 8px 0;
}

.comment-actions {
  font-size: 12px;
}

.comment-actions button {
  margin-right: 12px;
  background: none;
  border: none;
  cursor: pointer;
}
</style>