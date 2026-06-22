<template>
  <div class="video-rank-list">
    <div class="panel-header">
      <div class="header-content">
        <div class="pulse-indicator"></div>
        <div class="header-text">
          <h3 class="panel-title">微课热榜</h3>
          <span class="panel-subtitle">VIDEO RANKINGS</span>
        </div>
      </div>
      <div class="live-badge">
        <span class="live-dot"></span>
        <span class="live-text">实时</span>
      </div>
    </div>

    <div class="rank-scroll">
      <div v-if="loading" class="skeleton-list">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton sk-badge"></div>
          <div class="skeleton sk-content"></div>
          <div class="skeleton sk-score"></div>
        </div>
      </div>
      <div v-else-if="items.length === 0" class="empty-state">
        <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <polygon points="23 7 16 12 23 17 23 7"/>
          <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
        </svg>
        <span>暂无数据</span>
      </div>
      <div v-else class="rank-list">
        <div
          v-for="(item, index) in items"
          :key="item.id"
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
            <span class="item-name">{{ item.videoTitle }}</span>
            <span class="item-meta">
              <span class="meta-views"><Eye :size="11" />{{ item.viewCount }}</span>
              <span class="meta-likes"><Heart :size="11" />{{ item.likeCount }}</span>
            </span>
          </div>
          <div class="score-badge">
            <Eye :size="12" />
            <span>{{ item.viewCount }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Eye, Heart } from '@lucide/vue';
import type { VideoRankDto } from '@/types/overview';

defineProps<{
  items: VideoRankDto[];
  loading?: boolean;
}>();

const router = useRouter();

const getRankClass = (rank: number) => ({
  'gold': rank === 1,
  'silver': rank === 2,
  'bronze': rank === 3
});

const handleClick = (item: VideoRankDto) => {
  router.push('/videos/' + item.id);
};
</script>

<style scoped>
.video-rank-list {
  background: linear-gradient(135deg, rgba(10, 25, 20, 0.95), rgba(8, 18, 15, 0.98));
  border: 1px solid rgba(0, 255, 136, 0.12);
  border-radius: 20px;
  overflow: hidden;
  backdrop-filter: blur(20px);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 255, 136, 0.08);
  background: linear-gradient(90deg, rgba(0,255,136,0.03), transparent);
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
  background: #00FF88;
  box-shadow: 0 0 10px #00FF88;
  animation: pulse-glow 1.5s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { opacity: 1; box-shadow: 0 0 10px #00FF88; }
  50% { opacity: 0.5; box-shadow: 0 0 20px #00FF88, 0 0 30px #00FF88; }
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
  color: rgba(0, 255, 136, 0.5);
  letter-spacing: 3px;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: rgba(0, 255, 136, 0.08);
  border: 1px solid rgba(0, 255, 136, 0.2);
  border-radius: 20px;
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00FF88;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.live-text {
  font-size: 11px;
  color: #00FF88;
  font-weight: 600;
  letter-spacing: 1px;
}

.rank-scroll {
  padding: 12px;
  max-height: 448px;
  overflow-y: auto;
}

.rank-scroll::-webkit-scrollbar {
  width: 4px;
}

.rank-scroll::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
}

.rank-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 255, 136, 0.2);
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
  background: linear-gradient(90deg, rgba(0,255,136,0.05) 25%, rgba(0,255,136,0.1) 50%, rgba(0,255,136,0.05) 75%);
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
  background: rgba(0, 255, 136, 0.05);
  border-color: rgba(0, 255, 136, 0.15);
  transform: translateX(4px);
}

.rank-row.top-tier {
  background: linear-gradient(90deg, rgba(0,255,136,0.08), rgba(0,255,136,0.02));
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

.item-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.meta-views,
.meta-likes {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748B;
}

.score-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(0, 255, 136, 0.15), rgba(0, 255, 136, 0.08));
  border: 1px solid rgba(0, 255, 136, 0.3);
  border-radius: 12px;
  color: #00FF88;
  font-size: 13px;
  font-weight: 600;
  font-family: 'Fira Code', monospace;
}

/* Light theme */
[data-theme="light"] .video-rank-list {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 252, 250, 0.95));
  border: 1px solid rgba(16, 185, 129, 0.2);
}

[data-theme="light"] .panel-header {
  border-bottom: 1px solid rgba(16, 185, 129, 0.1);
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05), transparent);
}

[data-theme="light"] .pulse-indicator {
  background: #10b981;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

[data-theme="light"] .panel-title {
  color: #1E293B;
}

[data-theme="light"] .panel-subtitle {
  color: rgba(16, 185, 129, 0.6);
}

[data-theme="light"] .live-badge {
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
}

[data-theme="light"] .live-dot {
  background: #10b981;
}

[data-theme="light"] .live-text {
  color: #10b981;
}

[data-theme="light"] .rank-scroll::-webkit-scrollbar-thumb {
  background: rgba(16, 185, 129, 0.2);
}

[data-theme="light"] .skeleton {
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05) 25%, rgba(16, 185, 129, 0.1) 50%, rgba(16, 185, 129, 0.05) 75%);
}

[data-theme="light"] .empty-state {
  color: #94A3B8;
}

[data-theme="light"] .rank-row {
  background: rgba(16, 185, 129, 0.02);
}

[data-theme="light"] .rank-row:hover {
  background: rgba(16, 185, 129, 0.06);
  border-color: rgba(16, 185, 129, 0.15);
}

[data-theme="light"] .rank-row.top-tier {
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.08), rgba(16, 185, 129, 0.02));
}

[data-theme="light"] .rank-indicator {
  background: rgba(16, 185, 129, 0.1);
  color: #64748B;
}

[data-theme="light"] .item-name {
  color: #1E293B;
}

[data-theme="light"] .meta-views,
[data-theme="light"] .meta-likes {
  color: #94A3B8;
}

[data-theme="light"] .score-badge {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(16, 185, 129, 0.08));
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: #059669;
}
</style>
