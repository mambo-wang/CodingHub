<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useChatStore } from '@/stores/chat'
import { MessageCircle, X } from '@lucide/vue'
import ChatRoom from './ChatRoom.vue'

const chatStore = useChatStore()

const isOpen = computed(() => chatStore.drawerOpen)
const unread = computed(() => chatStore.unreadCount)

function toggle() {
  if (isOpen.value) {
    chatStore.closeDrawer()
  } else {
    chatStore.openDrawer()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && isOpen.value) {
    chatStore.closeDrawer()
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="chat-launcher">
    <!-- Floating button -->
    <button
      class="chat-fab"
      @click="toggle"
      :aria-label="isOpen ? '关闭聊天室' : '打开聊天室'"
      :aria-expanded="isOpen"
    >
      <X v-if="isOpen" :size="22" />
      <MessageCircle v-else :size="22" />
      <span v-if="!isOpen && unread > 0" class="unread-badge">{{ unread > 99 ? '99+' : unread }}</span>
    </button>

    <!-- Drawer -->
    <Transition name="drawer">
      <div
        v-if="isOpen"
        class="chat-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="chat-drawer-title"
      >
        <div class="drawer-header">
          <span id="chat-drawer-title" class="drawer-title">聊天室</span>
          <button class="drawer-close" @click="chatStore.closeDrawer()" aria-label="关闭聊天室">
            <X :size="18" />
          </button>
        </div>
        <div class="drawer-body">
          <ChatRoom />
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.chat-launcher {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 999;
}

.chat-fab {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  color: var(--accent-1);
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.chat-fab:hover {
  border-color: var(--border-glow);
  box-shadow: var(--shadow-glow);
  transform: translateY(-2px);
}

.chat-fab:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 10px;
  background: var(--accent-3);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  font-family: var(--font-mono);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.chat-drawer {
  position: absolute;
  bottom: 64px;
  right: 0;
  width: 400px;
  height: 520px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.drawer-title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
}

.drawer-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.drawer-close:hover {
  color: var(--text-primary);
  border-color: var(--border-glow);
}

.drawer-close:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}

.drawer-body {
  flex: 1;
  overflow: hidden;
}

/* Drawer transition */
.drawer-enter-active, .drawer-leave-active {
  transition: all 0.25s ease;
}

.drawer-enter-from, .drawer-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}

/* Responsive */
@media (max-width: 640px) {
  .chat-drawer {
    position: fixed;
    bottom: 0;
    right: 0;
    left: 0;
    width: 100%;
    height: 70vh;
    border-radius: 16px 16px 0 0;
  }

  .chat-launcher {
    bottom: 16px;
    right: 16px;
  }
}

/* Light theme focus ring */
[data-theme="light"] .chat-fab:focus-visible {
  outline-color: #7c3aed;
}

[data-theme="light"] .drawer-close:focus-visible {
  outline-color: #7c3aed;
}
</style>
