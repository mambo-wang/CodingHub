<template>
  <div class="post-card" @click="goToDetail">
    <div class="card-accent" :style="{ background: getCategoryColor(post.categoryId) }"></div>
    <button
      v-if="editable"
      class="btn-icon-edit"
      aria-label="编辑此帖"
      @click.stop="$emit('edit', post.id)"
    >
      <Pencil :size="16" />
    </button>
    <button
      v-if="deletable"
      class="btn-icon-delete"
      aria-label="删除此帖"
      @click.stop="$emit('delete', post.id)"
    >
      <Trash2 :size="16" />
    </button>
    <button
      v-if="canPin"
      class="btn-icon-pin"
      :aria-label="post.pinned ? '取消置顶' : '置顶'"
      :disabled="pinLoading"
      @click.stop="handlePin"
    >
      <PinOff v-if="post.pinned" :size="14" />
      <Pin v-else :size="14" />
    </button>
    <div class="card-badges">
      <span v-if="post.pinned" class="badge-pill badge-pinned">
        <ArrowUp :size="12" aria-hidden="true" />
        <span>置顶</span>
      </span>
      <span v-if="isHot" class="badge-pill badge-hot">
        <Flame :size="12" aria-hidden="true" />
        <span>热门</span>
      </span>
    </div>
    <div class="card-content">
      <div class="card-header">
        <span class="category-tag" :style="{ background: getCategoryBg(post.categoryId) }">
          {{ post.categoryName }}
        </span>
        <h3 class="post-title">{{ post.title }}</h3>
      </div>
      <div class="card-meta">
        <div class="author-info">
          <div class="avatar-placeholder">
            <User :size="14" />
          </div>
          <AuthorBadge
            :username="post.authorName"
            :nickname="post.authorNickname"
            size="sm"
          />
        </div>
        <span class="separator">·</span>
        <span class="post-date">{{ formatDate(post.createdAt) }}</span>
      </div>
      <div class="card-stats">
        <span class="stat-item">
          <Eye :size="14" />
          {{ formatCount(post.viewCount) }}
        </span>
        <span class="stat-item">
          <MessageCircle :size="14" />
          {{ post.commentCount }}
        </span>
        <span class="stat-item">
          <Heart :size="14" />
          {{ post.likeCount }}
        </span>
        <span class="stat-item favorite-btn" :class="{ active: post.isFavorited }" @click.stop="toggleFavorite">
          <Bookmark :size="14" :fill="post.isFavorited ? 'currentColor' : 'none'" />
          {{ post.favoriteCount || 0 }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { User, Eye, MessageCircle, Heart, Bookmark, Trash2, Pencil, ArrowUp, Flame, Pin, PinOff } from '@lucide/vue';
import type { ForumPost } from '@/types/forum';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { interactionApi } from '@/services/interaction';
import forumService from '@/services/forum';
import AuthorBadge from '@/components/AuthorBadge.vue';

const props = withDefaults(defineProps<{ post: ForumPost; deletable?: boolean; editable?: boolean; isHot?: boolean }>(), {
  deletable: false,
  editable: false,
  isHot: false,
});
const emit = defineEmits<{
  (e: 'delete', postId: number): void;
  (e: 'edit', postId: number): void;
  (e: 'pin-changed'): void;
}>();
const router = useRouter();
const authStore = useAuthStore();
const pinLoading = ref(false);

const canPin = computed(() => authStore.isAdmin);

const handlePin = async () => {
  if (pinLoading.value) return;
  pinLoading.value = true;
  try {
    if (props.post.pinned) {
      await forumService.unpinPost(props.post.id);
    } else {
      await forumService.pinPost(props.post.id);
    }
    props.post.pinned = !props.post.pinned;
    emit('pin-changed');
  } catch (error) {
    console.error('Pin/unpin failed:', error);
  } finally {
    pinLoading.value = false;
  }
};

// 分类颜色映射
const categoryColors: Record<number, string> = {
  1: '#7C3AED', // 紫色
  2: '#2563EB', // 蓝色
  3: '#059669', // 绿色
  4: '#DC2626', // 红色
  5: '#D97706', // 橙色
};

const getCategoryColor = (categoryId: number) => categoryColors[categoryId] || '#7C3AED';
const getCategoryBg = (categoryId: number) => `${categoryColors[categoryId] || '#7C3AED'}15`;

const goToDetail = () => {
  router.push(`/forum/posts/${props.post.id}`);
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const days = Math.floor(hours / 24);

  if (hours < 1) return '刚刚';
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
};

const formatCount = (count: number) => {
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`;
  return count.toString();
};

const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    router.push('/login');
    return;
  }
  try {
    const result = await interactionApi.toggleFavorite('FORUM_POST', props.post.id);
    props.post.isFavorited = result.favorited;
    props.post.favoriteCount = (props.post.favoriteCount || 0) + (result.favorited ? 1 : -1);
  } catch (error) {
    console.error('Toggle favorite failed:', error);
  }
};
</script>

<style scoped>
.post-card {
  display: flex;
  position: relative;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.post-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-glow);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3), 0 0 40px rgba(139, 92, 246, 0.15);
}

.card-accent {
  width: 4px;
  flex-shrink: 0;
}

.card-content {
  flex: 1;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--accent-1);
  white-space: nowrap;
}

.post-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.avatar-placeholder {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.author-name {
  color: var(--text-primary);
  font-weight: 500;
}

.separator {
  color: var(--text-muted);
}

.post-date {
  color: var(--text-muted);
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-top: 4px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.stat-item svg {
  opacity: 0.7;
}

.favorite-btn {
  cursor: pointer;
  transition: color 0.2s;
}

.favorite-btn:hover {
  color: var(--accent-1);
}

.favorite-btn.active {
  color: var(--accent-1);
}

.favorite-btn.active svg {
  opacity: 1;
}

.btn-icon-delete,
.btn-icon-edit {
  opacity: 0.35;
}

.post-card:hover .btn-icon-delete,
.post-card:hover .btn-icon-edit {
  opacity: 1;
}

.btn-icon-edit {
  position: absolute;
  top: 12px;
  right: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 6px;
  border-radius: 8px;
  border: 1.5px solid var(--border-color);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 200ms ease;
  z-index: 2;
}

.btn-icon-edit:hover {
  color: var(--accent-1);
  border-color: color-mix(in srgb, var(--accent-1) 30%, transparent);
  background: color-mix(in srgb, var(--accent-1) 10%, transparent);
  box-shadow: 0 0 12px rgba(139, 92, 246, 0.2);
}

.btn-icon-edit:active {
  transform: scale(0.95);
}

.btn-icon-delete {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 6px;
  border-radius: 8px;
  border: 1.5px solid var(--border-color);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 200ms ease;
  z-index: 2;
}

.btn-icon-delete:hover {
  color: var(--color-destructive);
  border-color: color-mix(in srgb, var(--color-destructive) 30%, transparent);
  background: var(--icon-del-hover-bg);
  box-shadow: 0 0 12px var(--delete-shadow);
}

.btn-icon-delete:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.btn-icon-delete:active {
  transform: scale(0.95);
}

.card-badges {
  position: absolute;
  top: 12px;
  left: 16px;
  display: flex;
  gap: 6px;
  z-index: 2;
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px 3px 6px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;
}

.badge-pill:hover {
  transform: translateY(-1px);
}

.badge-pinned {
  background: rgba(139, 92, 246, 0.12);
  color: #a78bfa;
  border: 1px solid rgba(139, 92, 246, 0.2);
}

.badge-pinned:hover {
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.25);
}

.badge-hot {
  background: rgba(245, 158, 11, 0.12);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.badge-hot:hover {
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.25);
}

[data-theme="light"] .badge-pinned {
  background: rgba(124, 58, 237, 0.08);
  color: #7c3aed;
  border-color: rgba(124, 58, 237, 0.15);
}

[data-theme="light"] .badge-hot {
  background: rgba(217, 119, 6, 0.08);
  color: #b45309;
  border-color: rgba(217, 119, 6, 0.15);
}

.btn-icon-pin {
  position: absolute;
  top: 12px;
  right: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 6px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 200ms ease;
  z-index: 2;
  opacity: 0;
}

.post-card:hover .btn-icon-pin {
  opacity: 1;
}

.btn-icon-pin:hover {
  color: var(--accent-1);
}

.btn-icon-pin:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon-pin:focus-visible {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .badge-pill:hover {
    transform: none;
  }
}
</style>