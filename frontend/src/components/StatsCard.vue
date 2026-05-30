<template>
  <div class="stats-card glass-card">
    <div class="flex items-center gap-3">
      <div class="icon-wrapper" :style="iconStyle">
        <component :is="iconComponent" :size="20" />
      </div>
      <div>
        <p class="text-xs text-muted">{{ label }}</p>
        <p class="font-code text-2xl font-bold text-main">{{ formattedValue }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Users, MessageSquare, Wrench } from '@lucide/vue';

const props = defineProps<{
  label: string;
  value: number;
  icon: 'users' | 'message-square' | 'wrench';
}>();

const iconComponent = computed(() => {
  const icons = { users: Users, 'message-square': MessageSquare, wrench: Wrench };
  return icons[props.icon];
});

const iconStyle = computed(() => {
  const styles = {
    users: { background: 'rgba(0,255,255,0.1)', border: '1px solid rgba(0,255,255,0.2)', color: 'var(--color-accent-cyan)' },
    'message-square': { background: 'rgba(255,0,255,0.1)', border: '1px solid rgba(255,0,255,0.2)', color: 'var(--color-accent-magenta)' },
    wrench: { background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)', color: 'var(--color-accent-green)' }
  };
  return styles[props.icon];
});

const formattedValue = computed(() => props.value.toLocaleString('zh-CN'));
</script>

<style scoped>
.stats-card {
  position: relative;
  padding: var(--space-lg, 16px);
  overflow: hidden;
}
.stats-card::before {
  content: '';
  position: absolute;
  inset: -100%;
  background: radial-gradient(circle, rgba(0,255,255,0.08) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 400ms ease;
  pointer-events: none;
}
.stats-card:hover::before {
  opacity: 1;
}
.glass-card {
  background: var(--color-surface, rgba(15, 23, 42, 0.9));
  backdrop-filter: blur(12px);
  border: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
  border-radius: var(--radius-lg, 12px);
  transition: all 200ms ease;
}
.glass-card:hover {
  border-color: var(--color-accent-cyan, #00FFFF);
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.15);
}
.icon-wrapper {
  padding: var(--space-sm, 8px);
  border-radius: var(--radius-md, 8px);
}
.text-muted {
  color: var(--color-text-muted, #94A3B8);
}
.text-main {
  color: var(--color-text, #F8FAFC);
}
</style>