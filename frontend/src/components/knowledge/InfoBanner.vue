<script setup lang="ts">
import { ref } from 'vue'
import { Info, X } from '@lucide/vue'

defineProps<{
  message: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const closing = ref(false)

const handleClose = () => {
  closing.value = true
  setTimeout(() => {
    emit('close')
  }, 300)
}
</script>

<template>
  <div
    v-if="!closing || true"
    class="info-banner"
    :class="{ closing }"
    role="status"
    aria-live="polite"
  >
    <Info :size="16" class="info-icon" aria-hidden="true" />
    <span class="info-message">{{ message }}</span>
    <button class="close-btn" @click="handleClose" aria-label="关闭提示">
      <X :size="14" />
    </button>
  </div>
</template>

<style scoped>
.info-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  transition: all 0.3s ease-out;
  opacity: 1;
  transform: translateY(0);
}

.info-banner.closing {
  opacity: 0;
  transform: translateY(-10px);
}

.info-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.info-message {
  flex: 1;
}

.close-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0.6;
}

.close-btn:hover {
  opacity: 1;
}

/* Dark theme */
:root:not([data-theme="light"]) .info-banner {
  background: rgba(6, 182, 212, 0.1);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: #e2e8f0;
}

:root:not([data-theme="light"]) .info-icon {
  color: #06b6d4;
}

:root:not([data-theme="light"]) .close-btn {
  color: #a1a1aa;
}

:root:not([data-theme="light"]) .close-btn:hover {
  background: rgba(6, 182, 212, 0.15);
  color: #e2e8f0;
}

/* Light theme */
[data-theme="light"] .info-banner {
  background: rgba(8, 145, 178, 0.05);
  border: 1px solid rgba(8, 145, 178, 0.2);
  color: #0f172a;
}

[data-theme="light"] .info-icon {
  color: #0891b2;
}

[data-theme="light"] .close-btn {
  color: #94a3b8;
}

[data-theme="light"] .close-btn:hover {
  background: rgba(8, 145, 178, 0.1);
  color: #0f172a;
}
</style>
