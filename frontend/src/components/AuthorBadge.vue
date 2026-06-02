<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  username: string
  nickname?: string | null
  size?: 'sm' | 'md' | 'lg'
}>(), {
  size: 'md'
})

const displayName = computed(() => {
  if (props.nickname) {
    return `${props.nickname}(${props.username})`
  }
  return props.username
})

const tooltip = computed(() => `账号: ${props.username}`)
</script>

<template>
  <span class="author-badge" :class="[`author-badge--${size}`]" :title="tooltip">
    {{ displayName }}
  </span>
</template>

<style scoped>
.author-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
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
  padding: 2px 8px;
}

.author-badge--md {
  font-size: 13px;
}

.author-badge--lg {
  font-size: 14px;
  padding: 6px 14px;
}
</style>