<template>
  <div class="category-filter">
    <button
      :class="['filter-btn', { active: !selectedCategory }]"
      @click="select(null)"
    >
      <LayoutGrid :size="14" />
      全部
    </button>
    <button
      v-for="category in categories"
      :key="category.id"
      :class="['filter-btn', { active: selectedCategory === category.id }]"
      :style="selectedCategory === category.id ? { background: getCategoryColor(category.id), borderColor: getCategoryColor(category.id) } : {}"
      @click="select(category.id)"
    >
      {{ category.name }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { LayoutGrid } from '@lucide/vue';
import type { ForumCategory } from '@/types/forum';

defineProps<{
  categories: ForumCategory[];
  selectedCategory: number | null;
}>();

const emit = defineEmits<{
  (e: 'select', categoryId: number | null): void;
}>();

const categoryColors: Record<number, string> = {
  1: '#7C3AED',
  2: '#2563EB',
  3: '#059669',
  4: '#DC2626',
  5: '#D97706',
};

const getCategoryColor = (categoryId: number) => categoryColors[categoryId] || '#7C3AED';

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

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1.5px solid #E5E7EB;
  border-radius: 20px;
  background: white;
  color: #6B7280;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  border-color: var(--accent-1);
  color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
}

.filter-btn.active {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.filter-btn.active:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(139, 92, 246, 0.4);
}
</style>