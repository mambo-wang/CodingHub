<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link, Download, Trash2, ArrowLeft, Loader2, Star, Pencil } from '@lucide/vue'
import { pluginApi } from '@/services/plugin'
import { useInteraction } from '@/composables/useInteraction'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/UserAvatar.vue'
import UnifiedLikeButton from '@/components/common/UnifiedLikeButton.vue'
import UnifiedFavoriteButton from '@/components/common/UnifiedFavoriteButton.vue'
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
    await interaction.loadComments(0)
  } catch {
    router.replace('/plugins')
  } finally {
    loading.value = false
  }
}

// 后端端口可通过 VITE_BACKEND_PORT 覆盖，默认 8082（与快速入门 MCP 地址拼接一致）
const marketBackendPort = (import.meta.env.VITE_BACKEND_PORT as string) || '8082'

// 与快速入门一致的复制封装：非安全上下文(如 http)时回退到 execCommand
async function copyToClipboard(text: string) {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
}

const copyMarketUrl = async () => {
  try {
    await copyToClipboard(`http://${window.location.hostname}:${marketBackendPort}/api/v1/plugin-market/marketplace.json`)
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

const authorUser = computed(() => {
  if (!detail.value) return null
  return {
    id: detail.value.authorId,
    username: detail.value.authorUsername,
    nickname: detail.value.authorNickname
  }
})

const authorName = computed(() => detail.value?.authorNickname || detail.value?.authorUsername)

const fmtCount = (n: number | null | undefined) => {
  const c = Number(n ?? 0)
  if (c >= 1000000) return `${(c / 1000000).toFixed(1)}M`
  if (c >= 1000) return `${(c / 1000).toFixed(1)}k`
  return String(c)
}

const onLikeUpdate = (data: { liked: boolean; likeCount: number }) => {
  if (detail.value) {
    detail.value.likeCount = data.likeCount
  }
}

const onFavoriteUpdate = (_data: { favorited: boolean }) => {
  // 收藏状态由组件内部维护，无需额外处理
}

onMounted(load)
</script>

<template>
  <div class="plugin-detail">
    <div class="page-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <button class="back-btn animate-fade-in-up" @click="router.push('/plugins')">
      <ArrowLeft :size="18" />
      <span>返回插件市场</span>
    </button>

    <div v-if="loading" class="detail-loading animate-fade-in-up">
      <Loader2 :size="32" class="spin" />
      <span>加载插件...</span>
    </div>

    <div v-else-if="detail" class="detail-wrap">
      <!-- 头部信息 -->
      <div class="detail-header glass-card animate-fade-in-up">
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
          <div v-if="detail.tags && detail.tags.length" class="tag-row">
            <TagBadge v-for="t in detail.tags" :key="t.id" :tag="t" />
          </div>
          <div class="meta-row">
            <span v-if="authorUser" class="meta-item author-meta">
              <UserAvatar :user="authorUser" size="sm" :display-name="authorName" />
              作者：{{ authorName }}
            </span>
            <span class="meta-item">
              <Star :size="13" aria-hidden="true" />
              热度：{{ fmtCount(detail.score) }}
            </span>
            <span class="meta-item">点赞：{{ fmtCount(detail.likeCount ?? 0) }}</span>
            <span class="meta-item">收藏：{{ fmtCount(detail.favoriteCount ?? 0) }}</span>
            <span class="meta-item">浏览：{{ fmtCount(detail.viewCount) }}</span>
            <span class="meta-item">发布于：{{ fmtTime(detail.createdAt) }}</span>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="copyMarketUrl">
            <Link :size="14" aria-hidden="true" />
            复制市场地址
          </button>
          <button class="action-btn" @click="pluginApi.download(pluginId)">
            <Download :size="14" aria-hidden="true" />
            下载 zip
          </button>
          <button v-if="canManage" class="action-btn" @click="router.push(`/plugins/${pluginId}/edit`)">
            <Pencil :size="14" aria-hidden="true" />
            编辑
          </button>
          <button v-if="canManage" class="action-btn danger" @click="removePlugin">
            <Trash2 :size="14" aria-hidden="true" />
            删除
          </button>
        </div>
      </div>

      <!-- 互动操作条：点赞 / 收藏 -->
      <div class="detail-actions glass-card animate-fade-in-up">
        <div class="actions-inner">
          <UnifiedLikeButton
            target-type="PLUGIN"
            :target-id="pluginId"
            :initial-count="detail.likeCount"
            @update="onLikeUpdate"
          />
          <UnifiedFavoriteButton
            target-type="PLUGIN"
            :target-id="pluginId"
            @update="onFavoriteUpdate"
          />
        </div>
      </div>

      <!-- 组件摘要 + plugin.json -->
      <div class="detail-main animate-fade-in-up">
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
              <div class="comp-group">
                <span class="comp-label">MCP Servers</span>
                <div v-if="c.mcpServers.length" class="comp-tags">
                  <span v-for="s in c.mcpServers" :key="s" class="comp-tag mcp">{{ s }}</span>
                </div>
                <span v-else class="comp-empty">无</span>
              </div>
              <div class="comp-bools">
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

      <!-- 评论区 -->
      <section class="panel comment-section animate-fade-in-up">
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
</template>

<style scoped>
.plugin-detail {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.page-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.bg-orb-1 {
  top: -100px;
  right: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-1), transparent 70%);
}

.bg-orb-2 {
  bottom: -100px;
  left: -100px;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, var(--accent-2), transparent 70%);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  margin-bottom: 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  font-family: var(--font-display);
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-btn:hover {
  border-color: var(--accent-1);
  color: var(--text-primary);
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px;
  color: var(--text-muted);
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
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
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

.tag-row { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }

.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.meta-item svg {
  opacity: 0.7;
}

.author-meta {
  gap: 6px;
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

.action-btn.danger:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.5);
  background: rgba(239, 68, 68, 0.1);
}

/* 互动操作条 */
.detail-actions {
  margin-bottom: 20px;
}

.actions-inner {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 主体单栏 */
.detail-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

.comment-section {
  margin-top: 0;
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
.comp-tag.mcp  { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3); }

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

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Light theme */
[data-theme="light"] .detail-header,
[data-theme="light"] .panel {
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
}

[data-theme="light"] .back-btn {
  background: var(--bg-card);
}

@media (max-width: 960px) {
  .detail-header {
    flex-direction: column;
  }

  .header-actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .actions-inner {
    flex-direction: column;
    align-items: stretch;
  }

  .actions-inner :deep(.unified-like-btn),
  .actions-inner :deep(.unified-fav-btn) {
    justify-content: center;
  }
}
</style>
