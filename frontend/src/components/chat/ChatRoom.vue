<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import { Send, Users, Wifi, WifiOff, Trash2, X, Reply, Smile, Pencil, Undo2 } from '@lucide/vue'
import type { ChatMessage } from '@/types/chat'
import ReplyQuote from './ReplyQuote.vue'
import MessageReactions from './MessageReactions.vue'
import MessageMarkdown from './MessageMarkdown.vue'
import TypingIndicator from './TypingIndicator.vue'

const chatStore = useChatStore()
const authStore = useAuthStore()

const inputContent = ref('')
const guestNick = ref(localStorage.getItem('chatGuestNick') || '')
const showNickModal = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)

const replyTarget = ref<ChatMessage | null>(null)
const editingId = ref<number | null>(null)
const editContent = ref('')

const isLoggedIn = computed(() => authStore.isLoggedIn)
const isAdmin = computed(() => authStore.isAdmin || authStore.isSuperAdmin)
const myUserId = computed(() => authStore.user?.id)
const connected = computed(() => chatStore.connected)
const onlineCount = computed(() => chatStore.onlineCount)
const messages = computed(() => chatStore.messages)
const loading = computed(() => chatStore.loading)
const errorMsg = computed(() => chatStore.error)

const typingNames = computed(() => {
  const self = myUserId.value
  return chatStore.typingUsers
    .filter((u) => !(self != null && u.userId === self))
    .map((u) => u.displayName || '某人')
})

const charCount = computed(() => inputContent.value.length)
const isOverLimit = computed(() => charCount.value > 1000)

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(messages, () => {
  scrollToBottom()
}, { deep: true })

onMounted(() => {
  chatStore.connect()
  chatStore.loadHistory().finally(() => scrollToBottom())
  if (!isLoggedIn.value && !guestNick.value) {
    showNickModal.value = true
  }
})

function isSelf(msg: ChatMessage): boolean {
  if (msg.userId != null && myUserId.value != null) {
    return msg.userId === myUserId.value
  }
  if (!isLoggedIn.value && msg.guest && msg.displayName) {
    return msg.displayName === guestNick.value
  }
  return false
}

function isDeleted(msg: ChatMessage): boolean {
  return msg.status === 'DELETED'
}

function deletedText(msg: ChatMessage): string {
  return msg.deletedType === 'SELF' ? '该消息已被撤回' : '该消息已被删除'
}

function isRefDeleted(id: number | null | undefined): boolean {
  if (!id) return false
  const ref = messages.value.find((m) => m.id === id)
  return !!ref && ref.status === 'DELETED'
}

function canEditRecall(msg: ChatMessage): boolean {
  if (!isLoggedIn.value) return false
  if (msg.status !== 'ACTIVE') return false
  if (msg.userId == null || msg.userId !== myUserId.value) return false
  return Date.now() - new Date(msg.createdAt).getTime() <= 5 * 60 * 1000
}

function formatTime(dateStr: string): string {
  const d = new Date(dateStr)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `${hh}:${mm}`
  return `${d.getMonth() + 1}/${d.getDate()} ${hh}:${mm}`
}

function handleSend() {
  const content = inputContent.value.trim()
  if (!content || isOverLimit.value) return

  const payload: { roomId: string; content: string; displayName?: string; replyTo?: number | null } = {
    roomId: 'global',
    content,
  }
  if (!isLoggedIn.value) {
    if (!guestNick.value) {
      showNickModal.value = true
      return
    }
    payload.displayName = guestNick.value
  }
  if (replyTarget.value) payload.replyTo = replyTarget.value.id

  chatStore.send(payload)
  inputContent.value = ''
  replyTarget.value = null
  autoResize()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function onInput() {
  autoResize()
  chatStore.sendTyping(true)
}

function autoResize() {
  const el = textareaRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }
}

function saveNick() {
  const nick = guestNick.value.trim()
  if (nick) {
    localStorage.setItem('chatGuestNick', nick)
    showNickModal.value = false
  }
}

function startReply(msg: ChatMessage) {
  replyTarget.value = msg
  textareaRef.value?.focus()
}

function cancelReply() {
  replyTarget.value = null
}

function startEdit(msg: ChatMessage) {
  editingId.value = msg.id
  editContent.value = msg.content
  nextTick(() => autoResizeEdit())
}

function saveEdit() {
  if (editingId.value == null) return
  const content = editContent.value.trim()
  if (!content) return
  chatStore.editMessage(editingId.value, content)
  editingId.value = null
  editContent.value = ''
}

function cancelEdit() {
  editingId.value = null
  editContent.value = ''
}

function autoResizeEdit() {
  const el = document.querySelector('.edit-textarea') as HTMLTextAreaElement | null
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }
}

function handleRecall(msg: ChatMessage) {
  chatStore.recallMessage(msg.id)
}

function scrollToMessage(id: number) {
  const el = document.getElementById('msg-' + id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function handleDelete(id: number) {
  await chatStore.deleteMessage(id)
}

function dismissError() {
  chatStore.clearError()
}
</script>

<template>
  <div class="chat-room">
    <!-- Header bar -->
    <div class="chat-header">
      <div class="chat-header-left">
        <Users :size="16" aria-hidden="true" />
        <span class="online-count">{{ onlineCount }}</span>
        <span class="online-label">在线</span>
      </div>
      <div class="chat-header-right">
        <div class="connection-status" :class="{ connected }">
          <Wifi v-if="connected" :size="14" aria-hidden="true" />
          <WifiOff v-else :size="14" aria-hidden="true" />
          <span>{{ connected ? '已连接' : '连接中...' }}</span>
        </div>
      </div>
    </div>

    <!-- Error banner -->
    <Transition name="fade">
      <div v-if="errorMsg" class="chat-error" role="alert">
        <span>{{ errorMsg }}</span>
        <button class="error-dismiss" @click="dismissError" aria-label="关闭提示">
          <X :size="14" />
        </button>
      </div>
    </Transition>

    <!-- Messages -->
    <div class="chat-messages" ref="messagesContainer" aria-live="polite">
      <div v-if="loading" class="chat-state">
        <div class="loading-spinner"></div>
        <span>加载历史消息...</span>
      </div>
      <div v-else-if="messages.length === 0" class="chat-state">
        <span class="empty-text">还没有消息，来说点什么吧</span>
      </div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        :id="'msg-' + msg.id"
        class="message-row"
        :class="{ self: isSelf(msg), other: !isSelf(msg) }"
      >
        <div class="message-avatar" v-if="!isSelf(msg)">
          <img v-if="msg.avatarUrl" :src="msg.avatarUrl" :alt="msg.displayName" class="avatar-img" />
          <div v-else class="avatar-placeholder" :class="{ guest: msg.guest }">
            {{ msg.displayName?.charAt(0)?.toUpperCase() || '?' }}
          </div>
        </div>
        <div class="message-bubble" :class="{ self: isSelf(msg), guest: msg.guest }">
          <div class="message-meta">
            <span class="message-author">{{ msg.displayName }}</span>
            <span v-if="msg.guest" class="guest-badge">游客</span>
            <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
          </div>

          <ReplyQuote
            v-if="msg.replyTo"
            :display-name="msg.replyToDisplayName"
            :preview="msg.replyToContentPreview"
            :deleted="isRefDeleted(msg.replyTo)"
            @jump="scrollToMessage(msg.replyTo)"
          />

          <template v-if="isDeleted(msg)">
            <div class="recalled-text">{{ deletedText(msg) }}</div>
          </template>

          <template v-else-if="editingId === msg.id">
            <textarea
              v-model="editContent"
              class="edit-textarea"
              rows="2"
              @keydown.enter.exact.prevent="saveEdit"
            ></textarea>
            <div class="edit-actions">
              <button class="edit-save" @click="saveEdit">保存</button>
              <button class="edit-cancel" @click="cancelEdit">取消</button>
            </div>
          </template>

          <template v-else>
            <MessageMarkdown :content="msg.content" />
            <span v-if="msg.edited" class="edited-mark">（已编辑）</span>
            <MessageReactions
              v-if="msg.reactions && Object.keys(msg.reactions).length"
              :reactions="msg.reactions"
              :my-reactions="msg.myReactions || []"
              @react="(e: string) => chatStore.react(msg.id, e)"
            />
          </template>

          <div class="message-actions" v-if="!isDeleted(msg)">
            <button class="action-btn" @click="startReply(msg)" :aria-label="'回复'" title="回复">
              <Reply :size="13" />
            </button>
            <button
              class="action-btn"
              @click="chatStore.react(msg.id, '👍')"
              :aria-label="'表情回应'"
              title="表情回应"
            >
              <Smile :size="13" />
            </button>
            <button
              v-if="canEditRecall(msg)"
              class="action-btn"
              @click="startEdit(msg)"
              :aria-label="'编辑'"
              title="编辑"
            >
              <Pencil :size="13" />
            </button>
            <button
              v-if="canEditRecall(msg)"
              class="action-btn danger"
              @click="handleRecall(msg)"
              :aria-label="'撤回'"
              title="撤回"
            >
              <Undo2 :size="13" />
            </button>
            <button
              v-if="isAdmin && !isSelf(msg)"
              class="action-btn danger"
              @click="handleDelete(msg.id)"
              :aria-label="'删除消息'"
              title="删除消息"
            >
              <Trash2 :size="12" />
            </button>
          </div>
        </div>
        <div class="message-avatar" v-if="isSelf(msg)">
          <img v-if="msg.avatarUrl" :src="msg.avatarUrl" :alt="msg.displayName" class="avatar-img" />
          <div v-else class="avatar-placeholder self-avatar">
            {{ msg.displayName?.charAt(0)?.toUpperCase() || '?' }}
          </div>
        </div>
      </div>
    </div>

    <!-- Typing indicator -->
    <TypingIndicator :names="typingNames" />

    <!-- Input -->
    <div class="chat-input-area">
      <div v-if="replyTarget" class="reply-banner">
        <span>回复 @{{ replyTarget.displayName }}</span>
        <button class="reply-cancel" @click="cancelReply" aria-label="取消回复">
          <X :size="14" />
        </button>
      </div>
      <div class="input-wrapper">
        <textarea
          ref="textareaRef"
          v-model="inputContent"
          class="chat-textarea"
          :class="{ 'over-limit': isOverLimit }"
          :placeholder="isLoggedIn ? '输入消息... (Enter 发送, Shift+Enter 换行)' : '输入消息...'"
          :disabled="!connected"
          @keydown="handleKeydown"
          @input="onInput"
          rows="1"
          :aria-invalid="isOverLimit"
        ></textarea>
        <button
          class="send-btn"
          @click="handleSend"
          :disabled="!connected || !inputContent.trim() || isOverLimit"
          aria-label="发送"
        >
          <Send :size="18" />
        </button>
      </div>
      <div class="input-footer">
        <span class="char-count" :class="{ warn: charCount > 800, over: isOverLimit }">
          {{ charCount }}/1000
        </span>
        <span v-if="isOverLimit" class="over-limit-hint" role="alert">内容超过1000字限制</span>
      </div>
    </div>

    <!-- Guest nick modal -->
    <Transition name="fade">
      <div v-if="showNickModal" class="nick-modal-overlay" @click.self="() => {}">
        <div class="nick-modal" role="dialog" aria-modal="true" aria-labelledby="nick-modal-title">
          <h3 id="nick-modal-title">设置昵称</h3>
          <p>作为游客，请输入你的昵称参与聊天</p>
          <input
            v-model="guestNick"
            class="nick-input"
            placeholder="你的昵称"
            maxlength="30"
            @keydown.enter="saveNick"
          />
          <button class="nick-save-btn" @click="saveNick" :disabled="!guestNick.trim()">
            进入聊天室
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-primary);
  position: relative;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent-2);
  font-family: var(--font-mono);
  font-size: 13px;
}

.online-count {
  font-weight: 700;
  font-size: 15px;
}

.online-label {
  color: var(--text-secondary);
  font-family: var(--font-display);
}

.chat-header-right {
  display: flex;
  align-items: center;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.connection-status.connected {
  color: #22c55e;
}

.chat-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: rgba(239, 68, 68, 0.1);
  border-bottom: 1px solid rgba(239, 68, 68, 0.2);
  color: #ef4444;
  font-size: 13px;
}

.error-dismiss {
  background: none;
  border: none;
  color: #ef4444;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--text-muted);
  font-size: 14px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-color);
  border-top-color: var(--accent-1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-text {
  color: var(--text-muted);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 80%;
}

.message-row.self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-row.other {
  align-self: flex-start;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
}

.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
}

.avatar-placeholder.guest {
  background: var(--text-muted);
}

.avatar-placeholder.self-avatar {
  background: linear-gradient(135deg, var(--accent-2), var(--accent-3));
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  position: relative;
  max-width: 100%;
  word-break: break-word;
}

.message-bubble.self {
  background: rgba(139, 92, 246, 0.15);
  border-color: rgba(139, 92, 246, 0.3);
}

.message-bubble.guest {
  border-style: dashed;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 12px;
}

.message-author {
  font-weight: 600;
  color: var(--text-primary);
}

.guest-badge {
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.2);
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 500;
}

.message-time {
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 11px;
}

.edited-mark {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 4px;
}

.recalled-text {
  font-size: 13px;
  color: var(--text-muted);
  font-style: italic;
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  white-space: pre-wrap;
}

.message-actions {
  display: flex;
  gap: 4px;
  margin-top: 6px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.message-bubble:hover .message-actions {
  opacity: 1;
}

.action-btn {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  border-radius: 6px;
  padding: 3px 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  transition: 0.15s;
}

.action-btn:hover {
  border-color: var(--border-glow);
  color: var(--text-primary);
}

.action-btn.danger:hover {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.4);
}

.action-btn:focus-visible {
  outline: 2px solid var(--focus-ring, #00ffff);
  outline-offset: 2px;
}

.edit-textarea {
  width: 100%;
  resize: none;
  background: var(--bg-primary);
  border: 1px solid var(--border-glow);
  border-radius: 6px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  padding: 6px 8px;
  outline: none;
}

.edit-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.edit-save,
.edit-cancel {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
  background: var(--bg-glass);
  color: var(--text-primary);
}

.edit-save {
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border: none;
  color: #fff;
}

.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  margin-bottom: 8px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-left: 3px solid var(--accent-2);
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.reply-cancel {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.chat-textarea {
  flex: 1;
  resize: none;
  padding: 10px 12px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  line-height: 1.5;
  outline: none;
  transition: border-color 0.2s ease;
  max-height: 120px;
}

.chat-textarea:focus {
  border-color: var(--border-glow);
  outline: 2px solid var(--focus-ring, #00ffff);
  outline-offset: 2px;
}

.chat-textarea:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-textarea.over-limit {
  border-color: var(--accent-3);
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.4);
  transform: translateY(-1px);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  font-family: var(--font-mono);
}

.char-count {
  color: var(--text-muted);
}

.char-count.warn {
  color: #f59e0b;
}

.char-count.over {
  color: #ef4444;
}

.over-limit-hint {
  color: #ef4444;
  font-family: var(--font-display);
}

/* Nick modal */
.nick-modal-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  backdrop-filter: blur(4px);
}

.nick-modal {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  width: 300px;
  text-align: center;
}

.nick-modal h3 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
}

.nick-modal p {
  margin: 0 0 16px;
  color: var(--text-secondary);
  font-size: 13px;
}

.nick-input {
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 14px;
  outline: none;
  margin-bottom: 12px;
  box-sizing: border-box;
}

.nick-input:focus {
  border-color: var(--border-glow);
  outline: 2px solid var(--focus-ring, #00ffff);
  outline-offset: 2px;
}

.nick-save-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border: none;
  border-radius: 8px;
  color: #fff;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nick-save-btn:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.4);
}

.nick-save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Light theme adjustments */
[data-theme='light'] .chat-textarea:focus {
  outline-color: #7c3aed;
}

[data-theme='light'] .nick-input:focus {
  outline-color: #7c3aed;
}
</style>
