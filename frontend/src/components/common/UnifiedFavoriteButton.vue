<template>
  <button
    :class="['unified-fav-btn', { favorited: favorited, loading: favoriteLoading }]"
    @click="handleClick"
    :disabled="favoriteLoading"
    :aria-label="favorited ? '取消收藏' : '收藏'"
    :aria-pressed="favorited"
  >
    <Bookmark :size="18" :fill="favorited ? 'currentColor' : 'none'" />
    <span class="fav-label">{{ favorited ? '已收藏' : '收藏' }}</span>
  </button>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Bookmark } from '@lucide/vue'
import { useInteraction } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import type { TargetType } from '@/services/interaction'

const props = defineProps<{
  targetType: TargetType
  targetId: number
  initialFavorited?: boolean
}>()

const emit = defineEmits<{
  (e: 'update', data: { favorited: boolean }): void
}>()

const authStore = useAuthStore()
const router = useRouter()
const { favorited, favoriteLoading, loadFavoriteStatus, toggleFavorite } = useInteraction(props.targetType, props.targetId)

onMounted(async () => {
  if (props.initialFavorited !== undefined) {
    favorited.value = props.initialFavorited
  }
  if (authStore.isLoggedIn) {
    await loadFavoriteStatus()
  }
})

const handleClick = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  await toggleFavorite()
  emit('update', { favorited: favorited.value })
}
</script>

<style scoped>
.unified-fav-btn {
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

.unified-fav-btn:hover:not(:disabled) {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.3);
  transform: scale(1.02);
}

.unified-fav-btn.favorited {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.4);
}

.unified-fav-btn.favorited:hover:not(:disabled) {
  background: rgba(245, 158, 11, 0.25);
}

.unified-fav-btn.loading {
  opacity: 0.5;
  pointer-events: none;
}

.unified-fav-btn:focus-visible {
  outline: 2px solid var(--accent-1);
  outline-offset: 2px;
}

[data-theme="light"] .unified-fav-btn {
  color: #a1a1aa;
}

[data-theme="light"] .unified-fav-btn.favorited {
  color: #d97706;
}

[data-theme="light"] .unified-fav-btn:hover:not(:disabled) {
  color: #d97706;
}
</style>
