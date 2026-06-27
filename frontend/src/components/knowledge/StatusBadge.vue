<script setup lang="ts">
import { Loader2, FileText, Layers, Cpu, CheckCircle2, AlertCircle } from '@lucide/vue'
import type { DocumentStatus } from '@/types/knowledge'

defineProps<{
  status: DocumentStatus
  errorMessage?: string | null
}>()

const statusConfig: Record<DocumentStatus, { label: string; class: string }> = {
  UPLOADING: { label: '上传中', class: 'status-uploading' },
  CONVERTING: { label: '转换中', class: 'status-converting' },
  CHUNKING: { label: '分块中', class: 'status-chunking' },
  EMBEDDING: { label: '向量化中', class: 'status-embedding' },
  READY: { label: '已解析', class: 'status-ready' },
  FAILED: { label: '失败', class: 'status-failed' },
}
</script>

<template>
  <span class="status-badge" :class="statusConfig[status]?.class" :title="errorMessage || undefined">
    <Loader2 v-if="status === 'UPLOADING'" :size="12" class="spin" aria-hidden="true" />
    <FileText v-else-if="status === 'CONVERTING'" :size="12" aria-hidden="true" />
    <Layers v-else-if="status === 'CHUNKING'" :size="12" aria-hidden="true" />
    <Cpu v-else-if="status === 'EMBEDDING'" :size="12" aria-hidden="true" />
    <CheckCircle2 v-else-if="status === 'READY'" :size="12" aria-hidden="true" />
    <AlertCircle v-else-if="status === 'FAILED'" :size="12" aria-hidden="true" />
    <span class="status-label">{{ statusConfig[status]?.label || status }}</span>
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  font-family: var(--font-display);
  white-space: nowrap;
  transition: all 0.2s ease;
}

/* Processing states pulse */
.status-uploading,
.status-converting,
.status-chunking,
.status-embedding {
  animation: pulse-processing 2s ease-in-out infinite;
}

/* Dark theme colors */
:root:not([data-theme="light"]) .status-uploading {
  background: #3f3f46;
  color: #a1a1aa;
  border: 1px solid #52525b;
}

:root:not([data-theme="light"]) .status-converting {
  background: #422006;
  color: #fbbf24;
  border: 1px solid #78350f;
}

:root:not([data-theme="light"]) .status-chunking {
  background: #1e3a8a;
  color: #60a5fa;
  border: 1px solid #1e40af;
}

:root:not([data-theme="light"]) .status-embedding {
  background: #4c1d95;
  color: #c084fc;
  border: 1px solid #5b21b6;
}

:root:not([data-theme="light"]) .status-ready {
  background: #064e3b;
  color: #34d399;
  border: 1px solid #065f46;
}

:root:not([data-theme="light"]) .status-failed {
  background: #7f1d1d;
  color: #fca5a5;
  border: 1px solid #991b1b;
}

/* Light theme colors */
[data-theme="light"] .status-uploading {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #cbd5e1;
}

[data-theme="light"] .status-converting {
  background: #fef3c7;
  color: #b45309;
  border: 1px solid #fde68a;
}

[data-theme="light"] .status-chunking {
  background: #dbeafe;
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}

[data-theme="light"] .status-embedding {
  background: #f3e8ff;
  color: #7c3aed;
  border: 1px solid #c4b5fd;
}

[data-theme="light"] .status-ready {
  background: #d1fae5;
  color: #059669;
  border: 1px solid #6ee7b7;
}

[data-theme="light"] .status-failed {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fca5a5;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes pulse-processing {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

@media (prefers-reduced-motion: reduce) {
  .status-uploading,
  .status-converting,
  .status-chunking,
  .status-embedding {
    animation: none;
  }
  .spin {
    animation: none;
  }
}
</style>
