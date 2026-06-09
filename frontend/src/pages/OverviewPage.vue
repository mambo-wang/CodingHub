<template>
  <div class="overview-page">
    <div class="page-container">
      <header class="page-header">
        <div class="header-content">
          <div class="header-left">
            <div class="logo-icon">
              <LayoutDashboard :size="24" />
            </div>
            <div class="header-text">
              <h1 class="title">热榜</h1>
              <p class="subtitle">Hot Rankings</p>
            </div>
          </div>
          <div class="header-right">
            <div class="update-time">
              <Clock :size="14" />
              <span>{{ currentTime }}</span>
            </div>
          </div>
        </div>
      </header>

      <section class="stats-section">
        <div class="stats-grid">
          <StatsCard label="用户总数" :value="stats.userCount" icon="users" />
          <StatsCard label="帖子总数" :value="stats.postCount" icon="message-square" />
          <StatsCard label="工具总数" :value="stats.toolCount" icon="wrench" />
        </div>
      </section>

      <section class="rankings-section">
        <div class="section-label">
          <span class="label-line"></span>
          <span class="label-text">实时热榜</span>
          <span class="label-line"></span>
        </div>
        <div class="rankings-grid">
          <ToolRankList
            :categories="toolCategories"
            :selectedCategory="selectedToolCategory"
            :items="toolItems"
            :loading="loading"
            @select="selectedToolCategory = $event"
          />
          <PostRankList
            :categories="postCategories"
            :selectedCategory="selectedPostCategory"
            :items="postItems"
            :loading="loading"
            @select="selectedPostCategory = $event"
          />
        </div>
      </section>

      <footer class="page-footer">
        <div class="footer-line"></div>
        <div class="footer-content">
          <span class="footer-text">CodingHub</span>
          <span class="footer-divider">|</span>
          <span class="footer-version">v1.0.0</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { LayoutDashboard, Clock } from '@lucide/vue';
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
const loading = ref(true);

const currentTime = ref('');

onMounted(async () => {
  try {
    loading.value = true;
    const [statsData, toolData, postData] = await Promise.all([
      fetchStats(),
      fetchToolRanks(),
      fetchPostRanks()
    ]);
    stats.value = statsData;
    toolItems.value = toolData;
    postItems.value = postData;
    toolCategories.value = [...new Set(toolData.map(t => t.category))];
    postCategories.value = [...new Set(postData.map(p => p.category))];
  } catch (error) {
    console.error('Failed to load overview data', error);
  } finally {
    loading.value = false;
  }

  const now = new Date();
  currentTime.value = now.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
});
</script>

<style scoped>
.overview-page {
  min-height: 100vh;
  padding: 24px;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(0, 255, 255, 0.12);
  border-radius: 20px;
  padding: 20px 24px;
  margin-bottom: 24px;
  backdrop-filter: blur(12px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0,255,255,0.15), rgba(255,0,255,0.1));
  border: 1px solid rgba(0,255,255,0.25);
  border-radius: 14px;
  color: #00FFFF;
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title {
  font-family: 'Fira Code', monospace;
  font-size: 22px;
  font-weight: 700;
  color: #F8FAFC;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 13px;
  color: #64748B;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.update-time {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255,255,255,0.03);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 10px;
  color: #94A3B8;
  font-size: 12px;
}

.stats-section {
  margin-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.rankings-section {
  margin-bottom: 24px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.label-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
}

.label-text {
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  color: #64748B;
  letter-spacing: 3px;
  text-transform: uppercase;
}

.rankings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.page-footer {
  padding-top: 24px;
}

.footer-line {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.06), transparent);
  margin-bottom: 16px;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.footer-text {
  font-family: 'Fira Code', monospace;
  font-size: 12px;
  color: #475569;
  letter-spacing: 1px;
}

.footer-divider {
  color: #334155;
}

.footer-version {
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  color: #475569;
}

@media (max-width: 1024px) {
  .rankings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-page {
    padding: 16px;
  }

  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}

/* Light theme */
[data-theme="light"] .page-header {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(139, 92, 246, 0.15);
}

[data-theme="light"] .logo-icon {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(139, 92, 246, 0.05));
  border: 1px solid rgba(139, 92, 246, 0.25);
  color: #8b5cf6;
}

[data-theme="light"] .title {
  color: #1E293B;
}

[data-theme="light"] .subtitle {
  color: #64748B;
}

[data-theme="light"] .update-time {
  background: rgba(139, 92, 246, 0.05);
  border: 1px solid rgba(139, 92, 246, 0.1);
  color: #64748B;
}

[data-theme="light"] .label-line {
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.1), transparent);
}

[data-theme="light"] .label-text {
  color: #64748B;
}

[data-theme="light"] .footer-line {
  background: linear-gradient(90deg, transparent, rgba(139, 92, 246, 0.08), transparent);
}

[data-theme="light"] .footer-text,
[data-theme="light"] .footer-version {
  color: #94A3B8;
}

[data-theme="light"] .footer-divider {
  color: #CBD5E1;
}
</style>