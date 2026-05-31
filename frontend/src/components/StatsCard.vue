<template>
  <div class="stats-card" :class="`accent-${icon}`">
    <div class="scan-line"></div>
    <div class="card-inner">
      <div class="icon-ring">
        <div class="ring-glow"></div>
        <component :is="iconComponent" :size="22" />
      </div>
      <div class="data-display">
        <span class="data-label">{{ label }}</span>
        <span class="data-value font-code">{{ animatedValue }}</span>
      </div>
    </div>
    <div class="corner-accent"></div>
    <div class="grid-overlay"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue';
import { Users, MessageSquare, Wrench } from '@lucide/vue';

const props = defineProps<{
  label: string;
  value: number;
  icon: 'users' | 'message-square' | 'wrench';
}>();

const displayValue = ref(0);
const animatedValue = computed(() => displayValue.value.toLocaleString('zh-CN'));

const iconComponent = computed(() => {
  const icons = { users: Users, 'message-square': MessageSquare, wrench: Wrench };
  return icons[props.icon];
});

watch(() => props.value, (newVal) => {
  const duration = 1200;
  const start = displayValue.value;
  const diff = newVal - start;
  const startTime = performance.now();
  const easeOutQuart = (t: number) => 1 - Math.pow(1 - t, 4);

  const animate = (currentTime: number) => {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    displayValue.value = Math.round(start + diff * easeOutQuart(progress));
    if (progress < 1) requestAnimationFrame(animate);
  };
  requestAnimationFrame(animate);
}, { immediate: true });
</script>

<style scoped>
.stats-card {
  position: relative;
  padding: 24px;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(10, 14, 23, 0.9);
  border: 1px solid rgba(0, 255, 255, 0.15);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.stats-card:hover {
  transform: translateY(-6px) scale(1.02);
  border-color: rgba(0, 255, 255, 0.4);
}

.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--accent-color), transparent);
  animation: scan 3s linear infinite;
  opacity: 0.6;
}

@keyframes scan {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(400px); }
}

.card-inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-ring {
  position: relative;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(0, 255, 255, 0.05);
  border: 2px solid var(--accent-color);
  box-shadow: 0 0 20px var(--accent-color), inset 0 0 15px rgba(0, 255, 255, 0.1);
}

.ring-glow {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--accent-color) 0%, transparent 70%);
  opacity: 0.3;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.2; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.1); }
}

.data-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.data-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: rgba(148, 163, 184, 0.7);
}

.data-value {
  font-size: 36px;
  font-weight: 700;
  color: #F8FAFC;
  text-shadow: 0 0 20px var(--accent-color);
}

.corner-accent {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: radial-gradient(circle at bottom right, var(--accent-color) 0%, transparent 70%);
  opacity: 0.1;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,255,255,0.03) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

/* Accent Colors */
.accent-users { --accent-color: #00FFFF; }
.accent-message-square { --accent-color: #FF00FF; }
.accent-wrench { --accent-color: #00FF88; }

.stats-card:hover .icon-ring {
  box-shadow: 0 0 30px var(--accent-color), 0 0 60px var(--accent-color), inset 0 0 20px rgba(0, 255, 255, 0.2);
}
</style>