<template>
  <aside class="sidebar-nav">
    <router-link to="/forum" class="nav-item" :class="{ active: currentPath === '/forum' }">
      <LayoutGrid :size="18" />
      帖子列表
    </router-link>
    <a v-if="isLoggedIn" @click="goToMyPosts" class="nav-item" :class="{ active: currentPath === '/forum/my-posts' }">
      <FileText :size="18" />
      我的帖子
    </a>
    <a v-if="isLoggedIn" @click="goToMyFavorites" class="nav-item" :class="{ active: currentPath === '/forum/my-favorites' }">
      <Bookmark :size="18" />
      我的收藏
    </a>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { LayoutGrid, FileText, Bookmark } from '@lucide/vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const { isLoggedIn } = storeToRefs(authStore);
const currentPath = computed(() => route.path);

const goToMyPosts = () => {
  if (!isLoggedIn.value) {
    router.push('/login');
    return;
  }
  router.push('/forum/my-posts');
};

const goToMyFavorites = () => {
  if (!isLoggedIn.value) {
    router.push('/login');
    return;
  }
  router.push('/forum/my-favorites');
};
</script>

<style scoped>
.sidebar-nav {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  height: fit-content;
  position: sticky;
  top: 100px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
  cursor: pointer;
}

.nav-item:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--accent-1);
}

.nav-item.active {
  background: rgba(139, 92, 246, 0.15);
  color: var(--accent-1);
}
</style>