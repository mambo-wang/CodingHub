<script setup lang="ts">
const props = defineProps<{
  displayName?: string | null
  preview?: string | null
  deleted?: boolean
}>()
const emit = defineEmits<{ (e: 'jump'): void }>()
</script>

<template>
  <div
    class="reply-quote"
    :class="{ deleted }"
    @click="!deleted && emit('jump')"
    :role="deleted ? undefined : 'button'"
    :tabindex="deleted ? undefined : 0"
    @keydown.enter="!deleted && emit('jump')"
  >
    <template v-if="deleted">原消息已删除</template>
    <template v-else><b>@{{ displayName }}</b>：{{ preview }}</template>
  </div>
</template>

<style scoped>
.reply-quote {
  border-left: 3px solid var(--accent-2);
  background: var(--bg-glass);
  border-radius: 6px;
  padding: 6px 10px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
}
.reply-quote:focus {
  outline: 2px solid var(--focus-ring, #00ffff);
  outline-offset: 2px;
}
.reply-quote.deleted {
  opacity: 0.6;
  cursor: default;
}
</style>
