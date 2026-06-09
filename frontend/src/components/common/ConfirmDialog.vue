<template>
  <Teleport to="body">
    <div v-if="visible" class="dialog-overlay" @click.self="handleCancel">
      <div
        ref="dialogRef"
        class="dialog-container"
        role="dialog"
        :aria-modal="true"
        :aria-labelledby="titleId"
        :aria-describedby="description ? descId : undefined"
      >
        <div class="dialog-content">
          <div class="dialog-header">
            <h3 :id="titleId" class="dialog-title">{{ title }}</h3>
          </div>
          <p v-if="description" :id="descId" class="dialog-description">{{ description }}</p>
          <div class="dialog-actions">
            <button
              ref="cancelBtnRef"
              class="btn-cancel"
              :disabled="loading"
              @click="handleCancel"
            >
              {{ cancelText }}
            </button>
            <button
              ref="confirmBtnRef"
              :class="['btn-confirm', { 'btn-danger': danger }]"
              :disabled="loading"
              @click="handleConfirm"
            >
              <span v-if="loading" class="spinner"></span>
              <Trash2 v-if="!loading && danger" :size="16" aria-hidden="true" />
              {{ loading ? '删除中...' : confirmText }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { Trash2 } from '@lucide/vue';

const props = withDefaults(defineProps<{
  visible: boolean;
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  loading?: boolean;
}>(), {
  confirmText: '确认',
  cancelText: '取消',
  danger: false,
  loading: false,
});

const emit = defineEmits<{
  (e: 'confirm'): void;
  (e: 'cancel'): void;
  (e: 'update:visible', value: boolean): void;
}>();

const dialogRef = ref<HTMLElement | null>(null);
const confirmBtnRef = ref<HTMLElement | null>(null);
const cancelBtnRef = ref<HTMLElement | null>(null);
const titleId = 'confirm-dialog-title';
const descId = 'confirm-dialog-desc';

// Focus management: when visible becomes true, focus the confirm button
watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => {
      confirmBtnRef.value?.focus();
    });
  }
});

// Esc key handler
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) {
    handleCancel();
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown);
});

function handleConfirm() {
  emit('confirm');
}

function handleCancel() {
  emit('cancel');
  emit('update:visible', false);
}
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--overlay-bg);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  animation: overlayIn 200ms ease-out;
}

.dialog-container {
  background: var(--modal-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--modal-border);
  border-radius: 16px;
  padding: 0;
  max-width: 400px;
  width: calc(100% - 32px);
  box-shadow: var(--shadow-md);
  animation: dialogIn 200ms ease-out;
}

.dialog-content {
  padding: 24px;
}

.dialog-header {
  margin-bottom: 12px;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-display);
}

.dialog-description {
  margin: 0 0 24px;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-cancel {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1.5px solid var(--btn-cancel-border);
  background: transparent;
  color: var(--btn-cancel-text);
  font-size: 14px;
  font-weight: 500;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 200ms ease;
}

.btn-cancel:hover:not(:disabled) {
  border-color: var(--btn-cancel-hover-border);
  background: var(--btn-cancel-hover-bg);
  color: var(--accent-1);
}

.btn-cancel:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-confirm {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 200ms ease;
  background: var(--accent-1);
  color: #FFFFFF;
}

.btn-confirm:hover:not(:disabled) {
  box-shadow: 0 0 20px var(--border-glow);
  transform: translateY(-1px);
}

.btn-confirm:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-danger {
  background: var(--color-destructive);
}

.btn-danger:hover:not(:disabled) {
  background: #DC2626;
  box-shadow: 0 0 20px var(--delete-shadow);
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes overlayIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .dialog-overlay,
  .dialog-container,
  .spinner {
    animation: none;
  }
  .dialog-overlay {
    opacity: 1;
  }
}
</style>
