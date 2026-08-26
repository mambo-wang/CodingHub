<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pluginApi } from '@/services/plugin'
import { useInteraction } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import type { PluginDetail } from '@/types/plugin'
import type { CommentResponse } from '@/services/interaction'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const pluginId = Number(route.params.id)
const detail = ref<PluginDetail | null>(null)
const loading = ref(true)
const commentInput = ref('')
const replyTo = ref<CommentResponse | null>(null)

const interaction = useInteraction('PLUGIN', pluginId)

const isOwner = computed(() =>
  detail.value && authStore.user ? detail.value.authorId === authStore.user.id : false
)
const canManage = computed(() => isOwner.value || authStore.isAdmin || authStore.isSuperAdmin)

const load = async () => {
  loading.value = true
  try {
    detail.value = await pluginApi.getDetail(pluginId)
    await Promise.all([
      interaction.loadLikeStatus(),
      interaction.loadFavoriteStatus(),
      interaction.loadComments(0)
    ])
  } catch {
    router.replace('/plugins')
  } finally {
    loading.value = false
  }
}

const copyInstall = async () => {
  if (!detail.value) return
  const cmd = `codebuddy plugin install ${detail.value.name}`
  try {
    await navigator.clipboard.writeText(cmd)
    ElMessage.success('安装命令已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copyMarketUrl = async () => {
  try {
    await navigator.clipboard.writeText(`${location.origin}/api/v1/plugin-market/marketplace.json`)
    ElMessage.success('市场地址已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const submitComment = async () => {
  const content = commentInput.value.trim()
  if (!content) return
  try {
    await interaction.addComment(content, replyTo.value?.id)
    commentInput.value = ''
    replyTo.value = null
  } catch {
    /* handled by interceptor */
  }
}

const startReply = (c: CommentResponse) => {
  replyTo.value = replyTo.value?.id === c.id ? null : c
}

const removeComment = async (c: CommentResponse) => {
  try {
    await interaction.deleteComment(c.id)
    ElMessage.success('评论已删除')
  } catch {
    /* handled by interceptor */
  }
}

const removePlugin = async () => {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(`确定删除插件 ${detail.value.name} 吗？此操作不可恢复。`, '删除插件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await pluginApi.remove(pluginId)
    ElMessage.success('插件已删除')
    router.replace('/plugins')
  } catch {
    /* cancelled */
  }
}

const isMine = (c: CommentResponse) =>
  authStore.user ? c.userId === authStore.user.id : false

const fmtTime = (s: string) => {
  const d = new Date(s)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(load)
</script>

<template>
  <div class="plugin-detail">
    <div v-if="loading" class="state-hint">加载中…</div>

    <div v-else-if="detail" class="detail-wrap">
      <!-- 头部信息 -->
      <div class="detail-header">
        <div class="detail-logo">
          <img v-if="detail.logoUrl" :src="detail.logoUrl" alt="" />
          <span v-else class="logo-fallback">{{ detail.name.slice(0, 2).toUpperCase() }}</span>
        </div>
        <div class="detail-info">
          <div class="name-row">
            <h1 class="detail-name">{{ detail.name }}</h1>
            <span class="version-tag">v{{ detail.version }}</span>
          </div>
          <p class="detail-desc">{{ detail.description }}</p>
          <div class="meta-row">
            <span class="meta-item">作者：@{{ detail.authorUsername }}</span>
            <span class="meta-item">热度：{{ detail.score }}</span>
            <span class="meta-item">浏览：{{ detail.viewCount }}</span>
            <span class="meta-item">发布于：{{ fmtTime(detail.createdAt) }}</span>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn primary" @click="copyInstall">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            复制安装命令
          </button>
          <button class="action-btn" @click="copyMarketUrl">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
            </svg>
            复制市场地址
          </button>
          <button class="action-btn" @click="pluginApi.download(pluginId)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            下载 zip
          </button>
          <button v-if="canManage" class="action-btn danger" @click="removePlugin">删除</button>
        </div>
      </div>

      <div class="detail-body">
        <!-- 左列：组件摘要 + plugin.json -->
        <div class="detail-left">
          <section class="panel">
            <h2 class="panel-title">组件摘要</h2>
            <template v-if="detail.components && detail.components.length > 0">
              <div v-for="(c, idx) in detail.components" :key="idx" class="component-section">
                <div class="comp-group">
                  <span class="comp-label">Skills</span>
                  <div v-if="c.skills.length" class="comp-tags">
                    <span v-for="s in c.skills" :key="s" class="comp-tag skill">{{ s }}</span>
                  </div>
                  <span v-else class="comp-empty">无</span>
                </div>
                <div class="comp-group">
                  <span class="comp-label">Agents</span>
                  <div v-if="c.agents.length" class="comp-tags">
                    <span v-for="s in c.agents" :key="s" class="comp-tag agent">{{ s }}</span>
                  </div>
                  <span v-else class="comp-empty">无</span>
                </div>
                <div class="comp-group">
                  <span class="comp-label">Commands</span>
                  <div v-if="c.commands.length" class="comp-tags">
                    <span v-for="s in c.commands" :key="s" class="comp-tag cmd">{{ s }}</span>
                  </div>
                  <span v-else class="comp-empty">无</span>
                </div>
                <div class="comp-group">
                  <span class="comp-label">Hooks</span>
                  <div v-if="c.hooks.length" class="comp-tags">
                    <span v-for="s in c.hooks" :key="s" class="comp-tag hook">{{ s }}</span>
                  </div>
                  <span v-else class="comp-empty">无</span>
                </div>
                <div class="comp-bools">
                  <span class="bool-item" :class="{ on: c.mcpServers }">MCP {{ c.mcpServers ? '✓' : '✗' }}</span>
                  <span class="bool-item" :class="{ on: c.lspServers }">LSP {{ c.lspServers ? '✓' : '✗' }}</span>
                  <span class="bool-item" :class="{ on: c.hasBin }">Bin {{ c.hasBin ? '✓' : '✗' }}</span>
                  <span class="bool-item" :class="{ on: c.hasSettings }">Settings {{ c.hasSettings ? '✓' : '✗' }}</span>
                </div>
              </div>
            </template>
            <div v-else class="comp-empty">无组件信息</div>
          </section>

          <section class="panel">
            <h2 class="panel-title">plugin.json 元数据</h2>
            <pre class="json-view">{{ JSON.stringify(detail.pluginJson, null, 2) }}</pre>
          </section>
        </div>

        <!-- 右列：互动 -->
        <div class="detail-right">
          <section class="panel">
            <div class="interact-row">
              <button
                class="interact-btn"
                :class="{ active: interaction.liked.value }"
                :disabled="interaction.likeLoading.value"
                @click="interaction.toggleLike()"
              >
                <span class="heart">{{ interaction.liked.value ? '♥' : '♡' }}</span>
                <span>{{ interaction.likeCount.value }}</span>
              </button>
              <button
                class="interact-btn"
                :class="{ active: interaction.favorited.value }"
                :disabled="interaction.favoriteLoading.value"
                @click="interaction.toggleFavorite()"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                <span>收藏</span>
              </button>
            </div>
          </section>

          <section class="panel">
            <h2 class="panel-title">
              评论
              <span v-if="interaction.commentsTotalElements.value > 0" class="comment-count">
                {{ interaction.commentsTotalElements.value }}
              </span>
            </h2>

            <div class="comment-input-row">
              <input
                v-model="commentInput"
                class="comment-input"
                :placeholder="replyTo ? `回复 @${replyTo.userName || '匿名'}…` : '写下你的评论…'"
                @keyup.enter="submitComment"
              />
              <button class="submit-btn" :disabled="interaction.commentSubmitting.value" @click="submitComment">发送</button>
            </div>
            <div v-if="replyTo" class="reply-banner">
              回复中：@{{ replyTo.userName }}
              <button class="cancel-reply" @click="replyTo = null">取消</button>
            </div>

            <div v-if="interaction.commentsLoading.value" class="state-hint small">加载评论…</div>
            <div v-else-if="interaction.comments.value.length === 0" class="state-hint small">暂无评论，快来抢沙发</div>

            <div v-else class="comment-list">
              <div
                v-for="c in interaction.comments.value"
                :key="c.id"
                class="comment-item"
                :class="{ reply: c.parentId }"
              >
                <div class="comment-avatar">{{ (c.userNickname || c.userName || '匿')[0] }}</div>
                <div class="comment-main">
                  <div class="comment-head">
                    <span class="comment-user">{{ c.userNickname || c.userName || '匿名用户' }}</span>
                    <span class="comment-time">{{ fmtTime(c.createdAt) }}</span>
                  </div>
                  <p class="comment-content">{{ c.content }}</p>
                  <div class="comment-actions">
                    <button class="link-btn" @click="startReply(c)">回复</button>
                    <button
                      v-if="isMine(c) || authStore.isAdmin || authStore.isSuperAdmin"
                      class="link-btn danger"
                      @click="removeComment(c)"
                    >删除</button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="interaction.commentsTotalPages.value > 1" class="comment-pages">
              <button
                class="page-btn"
                :disabled="interaction.commentsPage.value === 0"
                @click="interaction.loadComments(interaction.commentsPage.value - 1)"
              >上一页</button>
              <span class="page-info">{{ interaction.commentsPage.value + 1 }} / {{ interaction.commentsTotalPages.value }}</span>
              <button
                class="page-btn"
                :disabled="interaction.commentsPage.value >= interaction.commentsTotalPages.value - 1"
                @click="interaction.loadComments(interaction.commentsPage.value + 1)"
              >下一页</button>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.plugin-detail {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.state-hint {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
  font-size: 14px;
}

.state-hint.small {
  padding: 24px 0;
  font-size: 13px;
}

/* 头部 */
.detail-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}

.detail-logo {
  width: 72px;
  height: 72px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-fallback {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.detail-name {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  font-family: var(--font-display);
  letter-spacing: -0.5px;
}

.version-tag {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(6, 182, 212, 0.15);
  border: 1px solid rgba(6, 182, 212, 0.3);
  color: #22d3ee;
  font-family: var(--font-mono);
}

.detail-desc {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
  margin: 0 0 10px;
}

.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.header-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.action-btn:hover {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.1);
}

.action-btn.primary {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.25));
  border-color: rgba(139, 92, 246, 0.45);
  color: var(--text-primary);
}

.action-btn.primary:hover {
  border-color: rgba(139, 92, 246, 0.7);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.25);
}

.action-btn.danger:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.1);
}

/* 主体两栏 */
.detail-body {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 20px;
  align-items: start;
}

.detail-left,
.detail-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px;
  font-family: var(--font-display);
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-count {
  font-size: 12px;
  padding: 2px 7px;
  border-radius: 5px;
  background: rgba(139, 92, 246, 0.15);
  color: var(--accent-1);
  font-family: var(--font-mono);
}

/* 组件摘要 */
.component-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}

.comp-group {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.comp-label {
  flex-shrink: 0;
  width: 78px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  padding-top: 3px;
}

.comp-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.comp-tag {
  font-size: 12px;
  padding: 3px 9px;
  border-radius: 6px;
  font-family: var(--font-mono);
}

.comp-tag.skill { background: rgba(139, 92, 246, 0.15); color: #a78bfa; border: 1px solid rgba(139, 92, 246, 0.3); }
.comp-tag.agent { background: rgba(6, 182, 212, 0.15); color: #22d3ee; border: 1px solid rgba(6, 182, 212, 0.3); }
.comp-tag.cmd  { background: rgba(236, 72, 153, 0.15); color: #f472b6; border: 1px solid rgba(236, 72, 153, 0.3); }
.comp-tag.hook { background: rgba(251, 191, 36, 0.15); color: #fbbf24; border: 1px solid rgba(251, 191, 36, 0.3); }

.comp-empty {
  font-size: 12px;
  color: var(--text-muted);
  padding-top: 3px;
}

.comp-bools {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.bool-item {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.bool-item.on {
  color: #22d3ee;
  border-color: rgba(6, 182, 212, 0.4);
  background: rgba(6, 182, 212, 0.08);
}

.json-view {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  overflow-x: auto;
  max-height: 300px;
  overflow-y: auto;
}

/* 互动 */
.interact-row {
  display: flex;
  gap: 12px;
}

.interact-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  color: var(--text-secondary);
  font-size: 14px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.interact-btn:hover:not(:disabled) {
  color: var(--text-primary);
  border-color: rgba(139, 92, 246, 0.5);
  background: rgba(139, 92, 246, 0.08);
}

.interact-btn.active {
  color: #f472b6;
  border-color: rgba(244, 114, 182, 0.5);
  background: rgba(244, 114, 182, 0.1);
}

.interact-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.heart {
  font-size: 18px;
  line-height: 1;
}

/* 评论 */
.comment-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-input {
  flex: 1;
  height: 38px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: var(--font-display);
  outline: none;
  transition: border-color 0.2s ease;
}

.comment-input:focus {
  border-color: var(--accent-1);
}

.submit-btn {
  padding: 0 16px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.25));
  border: 1px solid rgba(139, 92, 246, 0.45);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 13px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  border-color: rgba(139, 92, 246, 0.7);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reply-banner {
  font-size: 12px;
  color: var(--accent-1);
  padding: 6px 10px;
  margin-bottom: 10px;
  background: rgba(139, 92, 246, 0.1);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cancel-reply {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.cancel-reply:hover {
  color: var(--text-primary);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.comment-item {
  display: flex;
  gap: 10px;
}

.comment-item.reply {
  margin-left: 28px;
  padding-left: 12px;
  border-left: 2px solid var(--border-color);
}

.comment-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.3), rgba(6, 182, 212, 0.3));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-user {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.comment-content {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 4px 0 6px;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 10px;
}

.link-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s ease;
}

.link-btn:hover {
  color: var(--accent-1);
}

.link-btn.danger:hover {
  color: #ef4444;
}

.comment-pages {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 16px;
}

.page-btn {
  padding: 6px 14px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 7px;
  color: var(--text-secondary);
  font-family: var(--font-display);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled) {
  border-color: rgba(139, 92, 246, 0.5);
  color: var(--text-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

@media (max-width: 960px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .detail-header {
    flex-direction: column;
  }

  .header-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
