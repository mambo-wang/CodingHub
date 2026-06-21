<template>
  <button
    :class="['unified-like-btn', { liked: liked, loading: likeLoading }]"
    @click="handleClick"
    :disabled="likeLoading"
    :aria-label="liked ? '取消点赞' : '点赞'"
    :aria-pressed="liked"
  >
    <Heart :size="18" :fill="liked ? 'currentColor' : 'none'" />
    <span class="like-count">{{ likeCount }}</span>
  </button>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Heart } from '@lucide/vue'
import { useInteraction } from '@/composables/useInteraction'
import type { TargetType } from '@/services/interaction'

const props = defineProps<{
  targetType: TargetType
  targetId: number
  initialLiked?: boolean
  initialCount?: number
}>()

const emit = defineEmits<{
  (e: 'update', data: { liked: boolean; likeCount: number }): void
}>()

const { liked, likeCount, likeLoading, loadLikeStatus, toggleLike } = useInteraction(props.targetType, props.targetId)

onMounted(async () => {
  if (props.initialCount !== undefined) {
    likeCount.value = props.initialCount
    liked.value = props.initialLiked ?? false
  }
  await loadLikeStatus()
})

const handleClick = async () => {
  await toggleLike()
  emit('update', { liked: liked.value, likeCount: likeCount.value })
}
</script>

<style scoped>
.unified-like-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.unified-like-btn:hover:not(:disabled) {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  transform: scale(1.02);
}

.unified-like-btn.liked {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.4);
}

.unified-like-btn.liked:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.25);
}

.unified-like-btn.loading {
  opacity: 0.5;
  pointer-events: none;
}

.unified-like-btn:focus-visible {
  outline: 2px solid var(--accent-1);
  outline-offset: 2px;
}

.like-count {
  font-variant-numeric: tabular-nums;
}

[data-theme="light"] .unified-like-btn {
  color: #a1a1aa;
}

[data-theme="light"] .unified-like-btn.liked {
  color: #dc2626;
}

[data-theme="light"] .unified-like-btn:hover:not(:disabled) {
  color: #dc2626;
}
</style>
