<script setup lang="ts">
defineProps<{ modelValue: string }>()
defineEmits<{ (e: 'update:modelValue', value: string): void }>()
</script>

<template>
  <div class="sort-tab" role="tablist" aria-label="排序方式">
    <button
      role="tab"
      class="sort-tab-item"
      :class="{ active: modelValue === 'hot' }"
      :aria-selected="modelValue === 'hot'"
      @click="$emit('update:modelValue', 'hot')"
    >
      热度
    </button>
    <button
      role="tab"
      class="sort-tab-item"
      :class="{ active: modelValue === 'latest' }"
      :aria-selected="modelValue === 'latest'"
      @click="$emit('update:modelValue', 'latest')"
    >
      最新
    </button>
  </div>
</template>

<style scoped>
.sort-tab {
  display: inline-flex;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.sort-tab-item {
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-bottom: 2px solid transparent;
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.sort-tab-item:hover:not(.active) {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.05);
}

:root:not([data-theme="light"]) .sort-tab-item:hover:not(.active) {
  background: rgba(255, 255, 255, 0.05);
}

[data-theme="light"] .sort-tab-item:hover:not(.active) {
  background: rgba(0, 0, 0, 0.03);
}

.sort-tab-item.active {
  color: var(--accent-1);
  font-weight: 600;
  border-bottom-color: var(--accent-1);
}

:root:not([data-theme="light"]) .sort-tab-item.active {
  background: rgba(139, 92, 246, 0.15);
}

[data-theme="light"] .sort-tab-item.active {
  background: rgba(124, 58, 237, 0.1);
}

.sort-tab-item:focus {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .sort-tab {
    width: 100%;
  }
  .sort-tab-item {
    flex: 1;
    text-align: center;
  }
}
</style>
