<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown, ChevronUp, Tag } from '@lucide/vue'

const props = defineProps<{
  index: number
  text: string
  charCount: number
  contextHeader?: string
}>()

const expanded = ref(false)

const needsTruncate = props.text.split('\n').length > 3 || props.text.length > 300
</script>

<template>
  <div class="chunk-card" :class="{ expanded }" @click="needsTruncate && (expanded = !expanded)">
    <div class="chunk-header">
      <span class="chunk-index">#{{ index + 1 }}</span>
      <span class="chunk-chars">{{ charCount }} chars</span>
      <span v-if="contextHeader" class="chunk-context">
        <Tag :size="10" aria-hidden="true" />
        {{ contextHeader }}
      </span>
      <button
        v-if="needsTruncate"
        class="expand-btn"
        @click.stop="expanded = !expanded"
        :aria-label="expanded ? '收起' : '展开'"
      >
        <ChevronUp v-if="expanded" :size="14" />
        <ChevronDown v-else :size="14" />
      </button>
    </div>
    <div class="chunk-body" :class="{ truncated: needsTruncate && !expanded }">
      <pre>{{ text }}</pre>
    </div>
  </div>
</template>

<style scoped>
.chunk-card {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
  background: var(--bg-glass);
  transition: all 0.2s ease;
}

.chunk-card:hover {
  border-color: var(--accent-1);
}

.chunk-card.expanded {
  border-color: var(--accent-1);
}

.chunk-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.chunk-index {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-1);
  font-family: var(--font-display);
}

.chunk-chars {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono, monospace);
}

.chunk-context {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 1px 6px;
  border-radius: 4px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expand-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.expand-btn:hover {
  color: var(--accent-1);
  background: var(--bg-secondary);
}

.chunk-body pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono, monospace);
}

.chunk-body.truncated pre {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
