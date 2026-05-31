<template>
  <div class="comment-editor">
    <div class="editor-card">
      <div class="textarea-wrapper">
        <textarea
          v-model="content"
          placeholder="发表评论..."
          class="content-input"
          rows="3"
        ></textarea>
      </div>
      <div class="editor-footer">
        <button @click="submit" :disabled="!canSubmit || loading" class="submit-btn">
          <Send :size="14" />
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Send } from '@lucide/vue';
import { addComment } from '@/services/tool';

const props = defineProps<{
  toolId: number;
}>();

const emit = defineEmits<{
  (e: 'submitted', comment: any): void;
  (e: 'require-login'): void;
}>();

const content = ref('');
const loading = ref(false);

const canSubmit = computed(() => content.value.trim().length > 0);

const submit = async () => {
  if (!canSubmit.value || loading.value) return;

  loading.value = true;
  try {
    const comment = await addComment(props.toolId, content.value.trim());
    emit('submitted', comment);
    content.value = '';
  } catch (error: any) {
    if (error?.response?.status === 401) {
      emit('require-login');
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.comment-editor {
  margin-top: 16px;
}

.editor-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s;
}

.editor-card:hover {
  border-color: var(--border-glow);
}

.textarea-wrapper {
  margin-bottom: 12px;
}

.content-input {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
  min-height: 80px;
  outline: none;
}

.content-input:focus {
  border-color: var(--accent-1);
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
}

.content-input::placeholder {
  color: var(--text-muted);
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--accent-2), var(--accent-1));
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(6, 182, 212, 0.4);
}

.submit-btn:disabled {
  background: var(--text-muted);
  box-shadow: none;
  cursor: not-allowed;
}
</style>