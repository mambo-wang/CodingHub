<template>
  <div class="tag-input">
    <div class="selected-tags">
      <span
        v-for="(tag, index) in selectedTags"
        :key="index"
        class="tag"
      >
        {{ tag }}
        <button @click="removeTag(index)">×</button>
      </span>
    </div>
    <input
      v-model="inputValue"
      @keydown.enter.prevent="addTag"
      placeholder="输入标签后回车"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  selectedTags: string[];
  suggestions?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:selectedTags', tags: string[]): void;
}>();

const inputValue = ref('');

const addTag = () => {
  if (!inputValue.value.trim()) return;
  if (props.selectedTags.includes(inputValue.value)) return;

  const newTags = [...props.selectedTags, inputValue.value.trim()];
  emit('update:selectedTags', newTags);
  inputValue.value = '';
};

const removeTag = (index: number) => {
  const newTags = props.selectedTags.filter((_, i) => i !== index);
  emit('update:selectedTags', newTags);
};
</script>

<style scoped>
.tag-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  background: #e0e0e0;
  border-radius: 4px;
}

.tag button {
  margin-left: 4px;
  background: none;
  border: none;
  cursor: pointer;
}

input {
  flex: 1;
  min-width: 200px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>