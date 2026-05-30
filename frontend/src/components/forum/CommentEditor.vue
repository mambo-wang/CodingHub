<template>
  <div class="comment-editor">
    <div class="editor-card">
      <div v-if="!isLoggedIn" class="anonymous-input">
        <div class="input-icon">
          <User :size="16" />
        </div>
        <input
          v-model="authorName"
          placeholder="输入昵称"
          class="nickname-input"
        />
      </div>
      <div class="textarea-wrapper">
        <textarea
          v-model="content"
          :placeholder="parentId ? '输入回复内容...' : '发表评论...'"
          class="content-input"
          rows="4"
        ></textarea>
      </div>
      <div class="editor-footer">
        <span class="hint" v-if="replyingTo">
          回复中: {{ replyingTo }}
          <button @click="cancelReply" class="cancel-btn">取消</button>
        </span>
        <button @click="submit" :disabled="!canSubmit" class="submit-btn">
          <Send :size="14" />
          提交
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { User, Send } from '@lucide/vue';

const props = defineProps<{
  isLoggedIn: boolean;
  parentId?: number;
  replyingToName?: string;
}>();

const emit = defineEmits<{
  (e: 'submit', data: { content: string; authorName?: string; parentId?: number }): void;
  (e: 'cancel'): void;
}>();

const content = ref('');
const authorName = ref('');

const replyingTo = computed(() => props.replyingToName);

const canSubmit = computed(() => {
  return content.value.trim().length > 0 &&
    (props.isLoggedIn || authorName.value.trim().length > 0);
});

const submit = () => {
  if (!canSubmit.value) return;

  emit('submit', {
    content: content.value,
    authorName: props.isLoggedIn ? undefined : authorName.value,
    parentId: props.parentId
  });

  content.value = '';
};

const cancelReply = () => {
  emit('cancel');
};
</script>

<style scoped>
.comment-editor {
  margin-top: 24px;
}

.editor-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s;
}

.editor-card:hover {
  border-color: var(--border-glow);
}

.anonymous-input {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.input-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.nickname-input {
  flex: 1;
  max-width: 200px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  outline: none;
  transition: border-color 0.2s;
}

.nickname-input:focus {
  border-color: var(--accent-1);
}

.nickname-input::placeholder {
  color: var(--text-muted);
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
  min-height: 100px;
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
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.cancel-btn {
  padding: 4px 8px;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
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