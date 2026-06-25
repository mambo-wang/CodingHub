<template>
  <span class="tag-badge" :class="{ removable }">
    <span class="tag-name">{{ tag.name }}</span>
    <button
      v-if="removable"
      class="tag-remove"
      @click.stop="$emit('remove', tag.id)"
      aria-label="移除标签"
    >
      <X :size="12" />
    </button>
  </span>
</template>

<script setup lang="ts">
import { X } from '@lucide/vue'
import type { Tag } from '@/types'

defineProps<{
  tag: Tag
  removable?: boolean
}>()

defineEmits<{
  (e: 'remove', tagId: number): void
}>()
</script>

<style scoped>
.tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.4;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.12);
  color: var(--accent-1, #8b5cf6);
  border: 1px solid rgba(139, 92, 246, 0.2);
  white-space: nowrap;
  transition: all 0.2s ease;
}

.tag-badge:hover {
  background: rgba(139, 92, 246, 0.2);
  border-color: rgba(139, 92, 246, 0.35);
}

.tag-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  margin-left: 2px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.15s ease;
}

.tag-remove:hover {
  opacity: 1;
  background: rgba(139, 92, 246, 0.3);
}

[data-theme="light"] .tag-badge {
  background: rgba(139, 92, 246, 0.08);
  border-color: rgba(139, 92, 246, 0.25);
}

[data-theme="light"] .tag-remove:hover {
  background: rgba(139, 92, 246, 0.15);
}

@media (prefers-reduced-motion: reduce) {
  .tag-badge,
  .tag-remove {
    transition: none;
  }
}
</style>
