<template>
  <nav class="generalized-sidebar" aria-label="页面导航">
    <router-link
      v-for="(item, index) in visibleItems"
      :key="index"
      :to="item.to"
      class="nav-item"
      :class="{ active: isActive(item.to) }"
    >
      <component :is="item.icon" :size="18" />
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export interface SidebarNavItem {
  label: string
  icon: Component
  to: string
  requiresAuth?: boolean
}

const props = defineProps<{
  items: SidebarNavItem[]
}>()

const route = useRoute()
const authStore = useAuthStore()

const visibleItems = computed(() => {
  return props.items.filter(item => {
    if (item.requiresAuth && !authStore.isLoggedIn) return false
    return true
  })
})

const isActive = (to: string) => {
  return route.path === to
}
</script>

<style scoped>
.generalized-sidebar {
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
  border-radius: 16px;
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
  font-weight: 600;
  border-left: 3px solid var(--accent-1);
}

.nav-item:focus-visible {
  outline: 2px solid var(--accent-1);
  outline-offset: 2px;
}

/* Light theme overrides */
[data-theme="light"] .nav-item:hover {
  background: rgba(124, 58, 237, 0.08);
}

[data-theme="light"] .nav-item.active {
  background: rgba(124, 58, 237, 0.12);
  border-left-color: #7c3aed;
}

/* Responsive: tablet - icon only */
@media (min-width: 768px) and (max-width: 1023px) {
  .generalized-sidebar {
    width: 48px;
    padding: 12px 8px;
  }

  .nav-item span {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 12px;
  }
}

/* Responsive: mobile - hide sidebar */
@media (max-width: 767px) {
  .generalized-sidebar {
    display: none;
  }
}
</style>
