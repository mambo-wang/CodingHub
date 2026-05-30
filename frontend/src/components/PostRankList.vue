<template>
  <div class="post-rank-list">
    <div class="tab-container">
      <button v-for="cat in ['全部', ...categories]" :key="cat"
        :class="['tab-btn', { active: selectedCategory === (cat === '全部' ? null : cat) }]"
        @click="$emit('select', cat === '全部' ? null : cat)">{{ cat }}</button>
    </div>
    <div class="rank-panel">
      <div v-if="loading" class="loading-state">
        <div v-for="i in 5" :key="i" class="skeleton h-10 w-full mb-2"></div>
      </div>
      <div v-else-if="items.length === 0" class="empty-state"><p>暂无数据</p></div>
      <div v-else class="rank-items">
        <RankItem v-for="(item, index) in items" :key="item.postTitle" :rank="index + 1"
          :title="item.postTitle" :count="item.commentCount" @click="handleClick(item)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import RankItem from './RankItem.vue';
import type { PostRankDto } from '@/types/overview';

const props = defineProps<{ categories: string[]; selectedCategory: string | null; items: PostRankDto[]; loading?: boolean; }>();
const emit = defineEmits<{ (e: 'select', category: string | null): void; }>();
const handleClick = (item: PostRankDto) => { console.log('post clicked', item); };
</script>

<style scoped>
/* 与 ToolRankList.vue 相同样式 */
.tab-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; border: 1.5px solid #1E293B; border-radius: 20px; background: rgba(15, 23, 42, 0.9); color: #94A3B8; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; }
.tab-btn:hover { border-color: #00FFFF; color: #00FFFF; }
.tab-btn.active { background: linear-gradient(135deg, #00FFFF, #FF00FF); border-color: transparent; color: #0F172A; font-weight: 600; }
.rank-panel { background: rgba(15, 23, 42, 0.9); border: 1px solid rgba(255, 255, 255, 0.08); border-top: none; border-radius: 0 0 16px 16px; padding: 12px; }
</style>