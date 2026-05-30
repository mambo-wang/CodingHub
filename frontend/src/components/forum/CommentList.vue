<template>
  <div class="comment-list">
    <div class="comment-header">
      <MessageSquare :size="20" />
      <h3>评论 ({{ comments.length }})</h3>
    </div>
    <div class="comment-tree">
      <CommentItem
        v-for="comment in rootComments"
        :key="comment.id"
        :comment="comment"
        :children="getChildren(comment.id)"
        @reply="handleReply"
        @like="handleLike"
      />
      <div v-if="rootComments.length === 0" class="empty-comments">
        暂无评论，快来抢沙发吧
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { MessageSquare } from '@lucide/vue';
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
  margin-top: 32px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  color: var(--accent-1);
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-tree {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-comments {
  text-align: center;
  padding: 32px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  color: var(--text-muted);
  font-size: 14px;
}
</style>