<script setup lang="ts">
defineProps<{ names: string[] }>()
</script>

<template>
  <div v-if="names.length" class="typing" aria-live="polite">
    <span class="dots"><span></span><span></span><span></span></span>
    <span>{{ names.join('、') }} 正在输入…</span>
  </div>
</template>

<style scoped>
.typing {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
  margin-top: 6px;
}
.dots span {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--text-muted);
  margin: 0 1px;
  animation: blink 1.2s infinite;
}
.dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.dots span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.2;
  }
  40% {
    opacity: 1;
  }
}
@media (prefers-reduced-motion: reduce) {
  .dots span {
    animation: none;
  }
}
</style>
