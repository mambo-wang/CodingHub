<template>
  <div class="overview-page">
    <header class="page-header">
      <h1 class="font-code text-xl font-bold" style="color: #00FFFF;">数据概览</h1>
      <p class="text-sm mt-1" style="color: var(--color-muted);">Platform Overview</p>
    </header>

    <section class="stats-grid">
      <StatsCard label="用户" :value="stats.userCount" icon="users" />
      <StatsCard label="帖子" :value="stats.postCount" icon="message-square" />
      <StatsCard label="工具" :value="stats.toolCount" icon="wrench" />
    </section>

    <section class="main-content">
      <div class="rank-section">
        <h2 class="font-code text-base font-semibold flex items-center gap-2 mb-3">
          <Flame :size="16" style="color: #00FFFF;" /> 工具热榜
        </h2>
        <ToolRankList :categories="toolCategories" :selectedCategory="selectedToolCategory" :items="toolItems"
          @select="selectedToolCategory = $event" />
      </div>
      <div class="rank-section">
        <h2 class="font-code text-base font-semibold flex items-center gap-2 mb-3">
          <MessageCircle :size="16" style="color: #FF00FF;" /> 帖子热榜
        </h2>
        <PostRankList :categories="postCategories" :selectedCategory="selectedPostCategory" :items="postItems"
          @select="selectedPostCategory = $event" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Flame, MessageCircle } from '@lucide/vue';
import StatsCard from '@/components/StatsCard.vue';
import ToolRankList from '@/components/ToolRankList.vue';
import PostRankList from '@/components/PostRankList.vue';
import { fetchStats, fetchToolRanks, fetchPostRanks } from '@/services/overview';
import type { StatsDto, ToolRankDto, PostRankDto } from '@/types/overview';

const stats = ref<StatsDto>({ userCount: 0, postCount: 0, toolCount: 0 });
const toolCategories = ref<string[]>([]);
const postCategories = ref<string[]>([]);
const toolItems = ref<ToolRankDto[]>([]);
const postItems = ref<PostRankDto[]>([]);
const selectedToolCategory = ref<string | null>(null);
const selectedPostCategory = ref<string | null>(null);

onMounted(async () => {
  try {
    const [statsData, toolData, postData] = await Promise.all([
      fetchStats(), fetchToolRanks(), fetchPostRanks()
    ]);
    stats.value = statsData;
    toolItems.value = toolData;
    postItems.value = postData;
    toolCategories.value = [...new Set(toolData.map(t => t.category))];
    postCategories.value = [...new Set(postData.map(p => p.category))];
  } catch (error) {
    console.error('Failed to load overview data', error);
  }
});
</script>

<style scoped>
.overview-page { padding: 16px; max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.main-content { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.rank-section { background: rgba(15, 23, 42, 0.9); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 16px; }
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .main-content { grid-template-columns: 1fr; }
}
</style>