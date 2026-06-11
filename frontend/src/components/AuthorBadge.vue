<script setup lang="ts">
import { computed } from 'vue'
import UserAvatar from './UserAvatar.vue'

const props = withDefaults(defineProps<{
  username: string
  nickname?: string | null
  avatarUrl?: string | null
  avatarSize?: 'sm' | 'md' | 'lg'
  size?: 'sm' | 'md' | 'lg'
  userId?: number
}>(), {
  size: 'md',
  avatarSize: 'sm',
  userId: 0
})

const displayName = computed(() => {
  if (props.nickname) {
    return `${props.nickname}(${props.username})`
  }
  return props.username
})

const tooltip = computed(() => `账号: ${props.username}`)
const showAvatar = computed(() => !!props.avatarUrl)

const fakeUser = computed(() => ({
  id: props.userId || 0,
  username: props.username,
  nickname: props.nickname,
  avatarUrl: props.avatarUrl
}))
</script>

<template>
  <span class="author-badge" :class="[`author-badge--${size}`]" :title="tooltip">
    <UserAvatar
      v-if="showAvatar"
      :user="fakeUser"
      :size="avatarSize"
    />
    {{ displayName }}
  </span>
</template>

<style scoped>
.author-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 4px;
  background: rgba(139, 92, 246, 0.15);
  border-radius: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.author-badge:hover {
  background: rgba(139, 92, 246, 0.25);
  color: var(--text-primary);
}

.author-badge--sm {
  font-size: 12px;
  padding: 2px 8px 2px 2px;
}

.author-badge--md {
  font-size: 13px;
}

.author-badge--lg {
  font-size: 14px;
  padding: 6px 14px 6px 6px;
}

[data-theme="light"] .author-badge {
  background: rgba(124, 58, 237, 0.1);
}

[data-theme="light"] .author-badge:hover {
  background: rgba(124, 58, 237, 0.2);
}
</style>
