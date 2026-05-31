<template>
  <div class="tool-rank-list">
    <div class="panel-header">
      <div class="header-content">
        <div class="pulse-indicator"></div>
        <div class="header-text">
          <h3 class="panel-title">工具热榜</h3>
          <span class="panel-subtitle">TOOL RANKINGS</span>
        </div>
      </div>
      <div class="live-badge">
        <span class="live-dot"></span>
        <span class="live-text">实时</span>
      </div>
    </div>

    <div class="category-tabs">
      <button
        v-for="cat in ['全部', ...categories]"
        :key="cat"
        :class="['tab-chip', { active: selectedCategory === (cat === '全部' ? null : cat) }]"
        @click="$emit('select', cat === '全部' ? null : cat)"
      >
        {{ cat }}
      </button>
    </div>

    <div class="rank-scroll">
      <div v-if="loading" class="skeleton-list">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton sk-badge"></div>
          <div class="skeleton sk-content"></div>
          <div class="skeleton sk-score"></div>
        </div>
      </div>
      <div v-else-if="displayItems.length === 0" class="empty-state">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
        </svg>
        <span>暂无数据</span>
      </div>
      <div v-else class="rank-list">
        <div
          v-for="(item, index) in displayItems"
          :key="item.toolName"
          :class="['rank-row', { 'top-tier': index < 3 }]"
          @click="handleClick(item)"
        >
          <div class="rank-indicator" :class="getRankClass(index + 1)">
            <span v-if="index < 3" class="tier-icon">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3 7h7l-5.5 4.5 2 7L12 16l-6.5 4.5 2-7L2 9h7l3-7z"/></svg>
            </span>
            <span v-else class="tier-num">{{ index + 1 }}</span>
          </div>
          <div class="item-info">
            <span class="item-name">{{ item.toolName }}</span>
            <span class="item-tag">{{ item.category }}</span>
          </div>
          <div class="score-badge">
            <Flame :size="12" />
            <span>{{ item.hotScore }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { Flame } from '@lucide/vue';
import type { ToolRankDto } from '@/types/overview';

const props = defineProps<{
  categories: string[];
  selectedCategory: string | null;
  items: ToolRankDto[];
  loading?: boolean;
}>();
const emit = defineEmits<{ (e: 'select', category: string | null): void }>();
const router = useRouter();

const displayItems = computed(() => {
  if (!props.selectedCategory) return props.items;
  return props.items.filter(item => item.category === props.selectedCategory);
});

const getRankClass = (rank: number) => ({
  'gold': rank === 1,
  'silver': rank === 2,
  'bronze': rank === 3
});

const handleClick = (item: ToolRankDto) => {
  router.push('/tools/' + item.id);
};
</script>

<style scoped>
.tool-rank-list {
  background: linear-gradient(135deg, rgba(13, 20, 30, 0.95), rgba(10, 14, 23, 0.98));
  border: 1px solid rgba(0, 255, 255, 0.12);
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 255, 255, 0.08);
  background: linear-gradient(90deg, rgba(0,255,255,0.03), transparent);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 14px;
}

.pulse-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #00FFFF;
  box-shadow: 0 0 10px #00FFFF;
  animation: pulse-glow 1.5s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; box-shadow: 0 0 10px #00FFFF; }
  50% { opacity: 0.5; box-shadow: 0 0 20px #00FFFF, 0 0 30px #00FFFF; }
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.panel-title {
  font-family: 'Fira Code', monospace;
  font-size: 16px;
  font-weight: 600;
  color: #F8FAFC;
  letter-spacing: 0.5px;
}

.panel-subtitle {
  font-size: 10px;
  color: rgba(0, 255, 255, 0.5);
  letter-spacing: 3px;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 255, 255, 0.08);
  border: 1px solid rgba(0, 255, 255, 0.2);
  border-radius: 20px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00FFFF;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.live-text {
  font-size: 11px;
  color: #00FFFF;
  font-weight: 600;
  letter-spacing: 1px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  overflow-x: auto;
}

.tab-chip {
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 500;
  color: #94A3B8;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.tab-chip:hover {
  color: #00FFFF;
  border-color: rgba(0, 255, 255, 0.4);
  background: rgba(0, 255, 255, 0.05);
}

.tab-chip.active {
  background: linear-gradient(135deg, #00FFFF, #00CCCC);
  border-color: transparent;
  color: #0A0E17;
  font-weight: 600;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.3);
}

.rank-scroll {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.rank-scroll::-webkit-scrollbar {
  width: 4px;
}

.rank-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.rank-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 255, 255, 0.2);
  border-radius: 2px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.skeleton {
  background: linear-gradient(90deg, rgba(0,255,255,0.05) 25%, rgba(0,255,255,0.1) 50%, rgba(0,255,255,0.05) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}

.sk-badge { width: 36px; height: 36px; border-radius: 50%; }
.sk-content { flex: 1; height: 20px; }
.sk-score { width: 60px; height: 28px; border-radius: 8px; }

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  color: #475569;
  gap: 12px;
}

.empty-icon {
  width: 48px;
  height: 48px;
  opacity: 0.3;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rank-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid transparent;
}

.rank-row:hover {
  background: rgba(0, 255, 255, 0.05);
  border-color: rgba(0, 255, 255, 0.15);
  transform: translateX(4px);
}

.rank-row.top-tier {
  background: linear-gradient(90deg, rgba(0,255,255,0.08), rgba(0,255,255,0.02));
}

.rank-indicator {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: #94A3B8;
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.rank-indicator.gold {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #fff;
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.5);
}
.rank-indicator.silver {
  background: linear-gradient(135deg, #94A3B8, #64748B);
  color: #fff;
  box-shadow: 0 0 12px rgba(148, 163, 184, 0.4);
}
.rank-indicator.bronze {
  background: linear-gradient(135deg, #CD7F32, #A0522D);
  color: #fff;
  box-shadow: 0 0 12px rgba(205, 127, 50, 0.4);
}

.tier-icon {
  display: flex;
  align-items: center;
}

.tier-icon svg {
  width: 16px;
  height: 16px;
}

.rank-row:hover .rank-indicator {
  transform: scale(1.15);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #F8FAFC;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-tag {
  display: inline-block;
  font-size: 10px;
  color: #00FFFF;
  background: rgba(0, 255, 255, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
  margin-top: 4px;
  letter-spacing: 0.5px;
}

.score-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(255, 0, 255, 0.15), rgba(255, 0, 255, 0.08));
  border: 1px solid rgba(255, 0, 255, 0.3);
  border-radius: 12px;
  color: #FF00FF;
  font-size: 13px;
  font-weight: 600;
  font-family: 'Fira Code', monospace;
}
</style>