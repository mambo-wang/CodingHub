<template>
  <div class="category-filter">
    <button
      :class="{ active: !selectedCategory }"
      @click="select(null)"
    >
      全部
    </button>
    <button
      v-for="category in categories"
      :key="category.id"
      :class="{ active: selectedCategory === category.id }"
      @click="select(category.id)"
    >
      {{ category.name }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { ForumCategory } from '@/types/forum';

defineProps<{
  categories: ForumCategory[];
  selectedCategory: number | null;
}>();

const emit = defineEmits<{
  (e: 'select', categoryId: number | null): void;
}>();

const select = (categoryId: number | null) => {
  emit('select', categoryId);
};
</script>

<style scoped>
.category-filter {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
}

button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}
</style>