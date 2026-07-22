<script setup lang="ts">
import { Sparkles, Layers, GitBranch } from '@lucide/vue'

defineProps<{
  strategy: string
}>()

const strategyConfig: Record<string, { label: string; class: string }> = {
  auto: { label: 'Auto', class: 'strategy-auto' },
  structural: { label: 'Structural', class: 'strategy-structural' },
  recursive: { label: 'Recursive', class: 'strategy-recursive' },
}
</script>

<template>
  <span class="strategy-badge" :class="strategyConfig[strategy]?.class || 'strategy-auto'">
    <Sparkles v-if="strategy === 'auto'" :size="11" aria-hidden="true" />
    <Layers v-else-if="strategy === 'structural'" :size="11" aria-hidden="true" />
    <GitBranch v-else :size="11" aria-hidden="true" />
    <span>{{ strategyConfig[strategy]?.label || strategy }}</span>
  </span>
</template>

<style scoped>
.strategy-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-display);
  white-space: nowrap;
}

/* Dark theme */
:root:not([data-theme="light"]) .strategy-auto {
  background: #4c1d95;
  color: #c084fc;
  border: 1px solid #5b21b6;
}

:root:not([data-theme="light"]) .strategy-structural {
  background: #164e63;
  color: #22d3ee;
  border: 1px solid #155e75;
}

:root:not([data-theme="light"]) .strategy-recursive {
  background: #3f3f46;
  color: #a1a1aa;
  border: 1px solid #52525b;
}

/* Light theme */
[data-theme="light"] .strategy-auto {
  background: #f3e8ff;
  color: #7c3aed;
  border: 1px solid #c4b5fd;
}

[data-theme="light"] .strategy-structural {
  background: #cffafe;
  color: #0891b2;
  border: 1px solid #67e8f9;
}

[data-theme="light"] .strategy-recursive {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #cbd5e1;
}
</style>
