<script setup lang="ts">
import ChatRoom from '@/components/chat/ChatRoom.vue'
</script>

<template>
  <div class="chat-page">
    <div class="chat-page-container">
      <ChatRoom />
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.chat-page-container {
  flex: 1;
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: var(--shadow-md);
}
</style>
