<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import ChatLauncher from '@/components/chat/ChatLauncher.vue'
</script>

<template>
  <div id="app">
    <AppHeader />
    <main class="main-content">
      <RouterView />
    </main>
    <ChatLauncher />
  </div>
</template>

<style scoped>
.main-content {
  min-height: calc(100vh - 60px);
  padding: 20px 0;
}
</style>
