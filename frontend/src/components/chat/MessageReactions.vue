<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  reactions: Record<string, number>
  myReactions: string[]
}>()
const emit = defineEmits<{ (e: 'react', emoji: string): void }>()

const EMOJIS = ['👍', '❤️', '😂', '🎉', '🚀', '👀', '🙏', '🔥']
const entries = computed(() => Object.entries(props.reactions || {}))
</script>

<template>
  <div class="reactions">
    <button
      v-for="[emoji, count] in entries"
      :key="emoji"
      class="reaction"
      :class="{ active: myReactions.includes(emoji) }"
      @click="emit('react', emoji)"
      :aria-label="`回应 ${emoji}`"
    >
      <span class="emoji">{{ emoji }}</span>
      <span class="count">{{ count }}</span>
    </button>
    <div class="picker" role="group" aria-label="选择表情">
      <button
        v-for="e in EMOJIS"
        :key="e"
        class="emoji-btn"
        :title="e"
        @click="emit('react', e)"
      >
        {{ e }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.reactions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.reaction {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: 0.2s;
}
.reaction:hover {
  border-color: var(--border-glow);
}
.reaction.active {
  background: rgba(139, 92, 246, 0.18);
  color: var(--accent-1);
  border-color: var(--border-glow);
}
.picker {
  display: flex;
  gap: 2px;
  margin-left: 4px;
}
.emoji-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 16px;
  border-radius: 6px;
  padding: 2px;
  line-height: 1;
}
.emoji-btn:hover {
  background: var(--bg-glass);
}
:deep([data-theme='light']) .reaction {
  background: rgba(15, 23, 42, 0.04);
}
</style>
