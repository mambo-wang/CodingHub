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
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  animation: overlayIn 200ms ease-out;
}

.dialog-container {
  background: rgba(20, 20, 20, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 0;
  max-width: 400px;
  width: calc(100% - 32px);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.5), 0 0 40px rgba(0, 0, 0, 0.3);
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
  color: #FFFFFF;
  font-family: 'Fira Code', monospace;
}

.dialog-description {
  margin: 0 0 24px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
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
  border: 1.5px solid rgba(255, 255, 255, 0.2);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 200ms ease;
}

.btn-cancel:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.4);
  color: #FFFFFF;
  background: rgba(255, 255, 255, 0.05);
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
  cursor: pointer;
  transition: all 200ms ease;
  background: linear-gradient(135deg, #00FFFF, #00CCCC);
  color: #0D0D0D;
}

.btn-confirm:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.3);
  transform: translateY(-1px);
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-danger {
  background: linear-gradient(135deg, #EF4444, #DC2626);
  color: #FFFFFF;
}

.btn-danger:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.3);
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
