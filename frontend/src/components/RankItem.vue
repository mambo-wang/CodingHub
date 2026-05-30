<template>
  <div class="rank-item" @click="$emit('click')">
    <span class="rank-badge" :class="{ 'top-3': rank <= 3 }">{{ rank }}</span>
    <span class="flex-1 text-sm truncate">{{ title }}</span>
    <span class="text-xs font-code" style="color: var(--color-muted);">{{ count }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ rank: number; title: string; count: number; }>();
defineEmits<{ (e: 'click'): void; }>();
</script>

<style scoped>
.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 150ms ease;
  cursor: pointer;
}
.rank-item:hover {
  background: #1E293B;
}
.rank-badge {
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #00FFFF, #FF00FF);
  color: #0F172A;
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  font-weight: 700;
  border-radius: 6px;
}
.rank-badge.top-3 {
  background: linear-gradient(135deg, #FBBF24, #22C55E);
}
</style>