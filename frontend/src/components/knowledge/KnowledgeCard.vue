<script setup lang="ts">
import { computed } from 'vue'
import { FileText, Pencil, Trash2 } from '@lucide/vue'
import UserAvatar from '@/components/UserAvatar.vue'
import type { KnowledgeBase } from '@/types/knowledge'

const props = withDefaults(defineProps<{
  kb: KnowledgeBase
  editable?: boolean
  deletable?: boolean
}>(), {
  editable: false,
  deletable: false,
})

const emit = defineEmits<{
  (e: 'edit', kbId: number): void
  (e: 'delete', kbId: number): void
}>()

const formatRelativeTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  const months = Math.floor(days / 30)
  const years = Math.floor(days / 365)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  if (months < 12) return `${months}个月前`
  return `${years}年前`
}

const ownerUser = computed(() => ({
  id: props.kb.ownerId,
  username: props.kb.ownerNickname || '',
  nickname: props.kb.ownerNickname,
}))

const displayName = computed(() => props.kb.ownerNickname || '未知用户')
</script>

<template>
  <router-link :to="`/knowledge/${kb.id}`" class="kb-card-link" :aria-label="`查看知识库: ${kb.name}`">
    <div class="kb-card">
      <div class="kb-cover">
        <div class="cover-placeholder">
          <FileText :size="36" aria-hidden="true" />
        </div>
      </div>

      <div class="card-actions" v-if="editable || deletable">
        <button
          v-if="editable"
          class="btn-icon-edit"
          aria-label="编辑知识库"
          @click.stop.prevent="emit('edit', kb.id)"
          @pointerdown.stop
        >
          <Pencil :size="16" />
        </button>
        <button
          v-if="deletable"
          class="btn-icon-delete"
          aria-label="删除知识库"
          @click.stop.prevent="emit('delete', kb.id)"
          @pointerdown.stop
        >
          <Trash2 :size="16" />
        </button>
      </div>

      <div class="kb-info">
        <h3 class="kb-title">{{ kb.name }}</h3>
        <p v-if="kb.description" class="kb-desc">{{ kb.description }}</p>

        <div class="kb-stats">
          <span class="stat-item">
            <FileText :size="14" aria-hidden="true" />
            <span>{{ kb.documentCount || 0 }} 篇文档</span>
          </span>
        </div>

        <div class="kb-uploader">
          <UserAvatar :user="ownerUser" size="sm" :display-name="displayName" />
          <span class="uploader-name">{{ displayName }}</span>
          <span class="upload-time">{{ formatRelativeTime(kb.createdAt) }}</span>
        </div>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.kb-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.kb-card {
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.2s ease;
  cursor: pointer;
  position: relative;
}

.kb-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.kb-cover {
  position: relative;
  aspect-ratio: 16 / 7;
  overflow: hidden;
  background: var(--bg-secondary);
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.15));
  color: var(--text-muted);
}

.kb-info {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kb-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-stats {
  display: flex;
  gap: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--text-muted);
}

.stat-item svg {
  opacity: 0.7;
}

.kb-uploader {
  display: flex;
  align-items: center;
  gap: 8px;
}

.uploader-name {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.upload-time {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
  z-index: 2;
  opacity: 0.35;
  transition: opacity 200ms ease;
}

.kb-card:hover .card-actions {
  opacity: 1;
}

.btn-icon-edit,
.btn-icon-delete {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1.5px solid rgba(255,255,255,0.3);
  background: rgba(0,0,0,0.5);
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  transition: all 200ms ease;
}

.btn-icon-edit:hover {
  color: #8B5CF6;
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.15);
}

.btn-icon-delete:hover {
  color: #EF4444;
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.15);
}

/* Light theme */
[data-theme="light"] .kb-card {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .kb-card:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .kb-card {
    transition: none;
  }
  .kb-card:hover {
    transform: none;
  }
}
</style>
