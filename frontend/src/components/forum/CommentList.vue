<template>
  <div class="comment-list">
    <h3>评论 ({{ comments.length }})</h3>
    <CommentItem
      v-for="comment in rootComments"
      :key="comment.id"
      :comment="comment"
      :children="getChildren(comment.id)"
      @reply="handleReply"
      @like="handleLike"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ForumComment } from '@/types/forum';
import CommentItem from './CommentItem.vue';

const props = defineProps<{
  comments: ForumComment[];
}>();

const emit = defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'like', commentId: number): void;
}>();

const rootComments = computed(() =>
  props.comments.filter(c => c.parentId === null || c.parentId === c.id)
);

const getChildren = (parentId: number) =>
  props.comments.filter(c => c.parentId === parentId);

const handleReply = (id: number) => emit('reply', id);
const handleLike = (id: number) => emit('like', id);
</script>

<style scoped>
.comment-list {
  margin-top: 24px;
}

.comment-list h3 {
  margin-bottom: 16px;
}
</style>