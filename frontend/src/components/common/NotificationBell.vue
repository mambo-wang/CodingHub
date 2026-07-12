<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Heart, MessageCircle, User, CheckCheck } from '@lucide/vue'
import { ElPopover, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { notificationService, type NotificationItem } from '@/services/notification'

const router = useRouter()
const authStore = useAuthStore()

const isLoggedIn = computed(() => authStore.isLoggedIn)
const unreadCount = ref(0)
const notifications = ref<NotificationItem[]>([])
const loading = ref(false)
const popoverVisible = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

const fetchUnreadCount = async () => {
  if (!isLoggedIn.value) return
  try {
    unreadCount.value = await notificationService.getUnreadCount()
  } catch {
    // silent
  }
}

const fetchNotifications = async () => {
  if (!isLoggedIn.value) return
  loading.value = true
  try {
    const page = await notificationService.getNotifications(0, 20)
    notifications.value = page.content
  } catch {
    // silent
  } finally {
    loading.value = false
  }
}

const startPolling = () => {
  stopPolling()
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(isLoggedIn, (val) => {
  if (val) {
    startPolling()
  } else {
    stopPolling()
    unreadCount.value = 0
    notifications.value = []
  }
})

onMounted(() => {
  if (isLoggedIn.value) {
    startPolling()
  }
})

onBeforeUnmount(() => {
  stopPolling()
})

const handlePopoverShow = () => {
  fetchNotifications()
}

const handleMarkAllRead = async () => {
  try {
    await notificationService.markAllAsRead()
    notifications.value = notifications.value.map(n => ({ ...n, isRead: true }))
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleNotificationClick = async (item: NotificationItem) => {
  if (!item.isRead) {
    try {
      await notificationService.markAsRead(item.id)
      item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {
      // silent
    }
  }

  popoverVisible.value = false
  navigateToTarget(item)
}

const navigateToTarget = (item: NotificationItem) => {
  switch (item.targetType) {
    case 'TOOL':
      router.push(`/tools/${item.targetId}`)
      break
    case 'FORUM_POST':
      router.push(`/forum/posts/${item.targetId}`)
      break
    case 'VIDEO':
      router.push(`/videos/${item.targetId}`)
      break
    default:
      break
  }
}

const getNotificationIcon = (type: string) => {
  switch (type) {
    case 'LIKE':
      return Heart
    case 'COMMENT_REPLY':
      return MessageCircle
    case 'ADMIN_APPROVED':
    case 'ADMIN_REJECTED':
      return User
    default:
      return Bell
  }
}

const getNotificationIconColor = (type: string) => {
  switch (type) {
    case 'LIKE':
      return '#ec4899'
    case 'COMMENT_REPLY':
      return '#06b6d4'
    case 'ADMIN_APPROVED':
    case 'ADMIN_REJECTED':
      return '#8b5cf6'
    default:
      return 'var(--text-secondary)'
  }
}

const formatTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<template>
  <div v-if="isLoggedIn" class="notification-bell">
    <ElPopover
      v-model:visible="popoverVisible"
      placement="bottom-end"
      :width="400"
      trigger="click"
      popper-class="notification-popover"
      @show="handlePopoverShow"
    >
      <template #reference>
        <button class="bell-btn" aria-label="通知">
          <Bell :size="18" />
          <span v-if="unreadCount > 0" class="badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </template>

      <div class="notification-panel">
        <div class="panel-header">
          <span class="panel-title">通知</span>
          <button
            v-if="unreadCount > 0"
            class="mark-all-btn"
            @click="handleMarkAllRead"
          >
            <CheckCheck :size="14" />
            全部已读
          </button>
        </div>

        <div class="panel-body">
          <div v-if="loading" class="panel-loading">
            加载中...
          </div>
          <div v-else-if="notifications.length === 0" class="panel-empty">
            <Bell :size="32" :stroke-width="1" />
            <span>暂无通知</span>
          </div>
          <div v-else class="notification-list">
            <button
              v-for="item in notifications"
              :key="item.id"
              class="notification-item"
              :class="{ 'is-unread': !item.isRead }"
              @click="handleNotificationClick(item)"
            >
              <div class="notif-icon" :style="{ color: getNotificationIconColor(item.type) }">
                <component :is="getNotificationIcon(item.type)" :size="16" />
              </div>
              <div class="notif-content">
                <div class="notif-message">
                  <span v-if="item.actorName" class="notif-actor">{{ item.actorName }}</span>
                  {{ item.message }}
                </div>
                <div class="notif-time">{{ formatTime(item.createdAt) }}</div>
              </div>
              <div v-if="!item.isRead" class="notif-dot"></div>
            </button>
          </div>
        </div>
      </div>
    </ElPopover>
  </div>
</template>

<style scoped>
.notification-bell {
  position: relative;
}

.bell-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.25s ease;
}

.bell-btn:hover {
  color: var(--accent-1);
  background: rgba(139, 92, 246, 0.1);
  border-color: var(--accent-1);
  transform: translateY(-1px);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}

.notification-panel {
  margin: -12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.mark-all-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mark-all-btn:hover {
  color: var(--accent-1);
  border-color: var(--accent-1);
  background: rgba(139, 92, 246, 0.08);
}

.panel-body {
  max-height: 400px;
  overflow-y: auto;
}

.panel-loading,
.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 20px;
  color: var(--text-muted);
  font-size: 14px;
}

.notification-list {
  padding: 4px 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 12px 20px;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background 0.15s ease;
  text-align: left;
  position: relative;
}

.notification-item:hover {
  background: rgba(139, 92, 246, 0.06);
}

.notification-item.is-unread {
  background: rgba(139, 92, 246, 0.03);
}

.notif-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  margin-top: 2px;
}

.notif-content {
  flex: 1;
  min-width: 0;
}

.notif-message {
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
  word-break: break-word;
}

.notif-actor {
  font-weight: 600;
  color: var(--accent-1);
}

.notif-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}

.notif-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent-1);
  box-shadow: 0 0 8px rgba(139, 92, 246, 0.5);
  margin-top: 6px;
}

/* Scrollbar styling */
.panel-body::-webkit-scrollbar {
  width: 4px;
}

.panel-body::-webkit-scrollbar-track {
  background: transparent;
}

.panel-body::-webkit-scrollbar-thumb {
  background: rgba(139, 92, 246, 0.2);
  border-radius: 2px;
}

.panel-body::-webkit-scrollbar-thumb:hover {
  background: rgba(139, 92, 246, 0.4);
}
</style>

<style>
/* Global styles for the popover (unscoped because ElPopover teleports) */
.notification-popover.el-popover.el-popper {
  padding: 0 !important;
  background: var(--bg-glass, rgba(15, 15, 20, 0.95)) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.08)) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4) !important;
}

.notification-popover .el-popper__arrow {
  display: none;
}
</style>
