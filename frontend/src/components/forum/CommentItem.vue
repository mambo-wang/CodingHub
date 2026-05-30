<template>
  <div class="comment-item" :class="{ 'is-reply': comment.parentId !== null }">
    <div class="comment-avatar">
      <User :size="16" />
    </div>
    <div class="comment-body">
      <div class="comment-header">
        <span class="author">{{ comment.authorName || '访客' }}</span>
        <span class="date">{{ formatDate(comment.createdAt) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <button @click="handleLike" :class="{ liked: hasLiked }">
          <Heart :size="14" :fill="hasLiked ? 'currentColor' : 'none'" />
          {{ comment.likeCount }}
        </button>
        <button @click="handleReply" class="reply-btn">
          <MessageCircle :size="14" />
          回复
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { User, Heart, MessageCircle } from '@lucide/vue';
import type { ForumComment } from '@/types/forum';

const props = defineProps<{
  comment: ForumComment;
  children?: ForumComment[];
}>();

const emit = defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'like', commentId: number): void;
}>();

const hasLiked = ref(false);

const handleReply = () => emit('reply', props.comment.id);
const handleLike = () => {
  hasLiked.value = !hasLiked.value;
  emit('like', props.comment.id);
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
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  transition: all 0.2s;
}

.comment-item:hover {
  border-color: var(--border-glow);
}

.comment-item.is-reply {
  margin-left: 32px;
  position: relative;
  background: rgba(255, 255, 255, 0.02);
}

.comment-item.is-reply::before {
  content: '';
  position: absolute;
  left: -16px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, var(--accent-1), var(--accent-2));
  border-radius: 1px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.author {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.date {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 16px;
}

.comment-actions button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.comment-actions button:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
}

.comment-actions button.liked {
  background: rgba(239, 68, 68, 0.15);
  color: var(--accent-3);
}

.comment-actions button.liked:hover {
  background: rgba(239, 68, 68, 0.25);
}

.reply-btn:hover {
  background: rgba(139, 92, 246, 0.15);
  color: var(--accent-1);
}
</style>