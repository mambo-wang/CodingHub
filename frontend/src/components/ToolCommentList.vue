<template>
  <div class="comment-list">
    <div class="comment-header">
      <MessageSquare :size="20" />
      <h3>评论 ({{ comments.length }})</h3>
    </div>
    <div class="comment-items">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          <User :size="16" />
        </div>
        <div class="comment-body">
          <div class="comment-header-row">
            <span class="author">{{ comment.username }}</span>
            <span class="date">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
        </div>
      </div>
      <div v-if="comments.length === 0" class="empty-comments">
        暂无评论，快来抢沙发吧
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MessageSquare, User } from '@lucide/vue';
import type { Comment } from '@/services/tool';

defineProps<{
  comments: Comment[];
}>();

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
.comment-list {
  margin-top: 24px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  color: var(--accent-1);
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

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

.comment-header-row {
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