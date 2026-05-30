<template>
  <div class="comment-editor">
    <div v-if="!isLoggedIn" class="anonymous-input">
      <input
        v-model="authorName"
        placeholder="输入昵称"
        class="nickname-input"
      />
    </div>
    <textarea
      v-model="content"
      placeholder="发表评论..."
      class="content-input"
      rows="3"
    ></textarea>
    <button @click="submit" :disabled="!canSubmit">提交</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const props = defineProps<{
  isLoggedIn: boolean;
  parentId?: number;
}>();

const emit = defineEmits<{
  (e: 'submit', data: { content: string; authorName?: string; parentId?: number }): void;
}>();

const content = ref('');
const authorName = ref('');

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
</script>

<style scoped>
.comment-editor {
  margin-top: 16px;
}

.anonymous-input {
  margin-bottom: 8px;
}

.nickname-input {
  width: 200px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.content-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
}

button {
  margin-top: 8px;
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>