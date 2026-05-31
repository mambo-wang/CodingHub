<template>
  <button :class="['like-btn', { 'liked': isLiked, 'loading': loading }]" @click="handleClick" :disabled="loading">
    <ThumbsUp :size="18" :fill="isLiked ? 'currentColor' : 'none'" />
    <span>{{ likeCount }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ThumbsUp } from '@lucide/vue';
import { likeTool, unlikeTool } from '@/services/tool';

const props = defineProps<{
  toolId: number;
  initialLiked: boolean;
  initialCount: number;
}>();

const emit = defineEmits<{
  (e: 'update', data: { isLiked: boolean; likeCount: number }): void;
  (e: 'require-login'): void;
}>();

const isLiked = ref(props.initialLiked);
const likeCount = ref(props.initialCount);
const loading = ref(false);

const handleClick = async () => {
  if (loading.value) return;

  loading.value = true;
  try {
    if (isLiked.value) {
      await unlikeTool(props.toolId);
      likeCount.value--;
      isLiked.value = false;
    } else {
      await likeTool(props.toolId);
      likeCount.value++;
      isLiked.value = true;
    }
    emit('update', { isLiked: isLiked.value, likeCount: likeCount.value });
  } catch (error: any) {
    if (error?.response?.status === 401) {
      emit('require-login');
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.like-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.like-btn.liked {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
}

.like-btn.liked:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.25);
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.like-btn.loading {
  pointer-events: none;
}
</style>