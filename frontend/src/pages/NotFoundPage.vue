<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => router.push('/')
</script>

<template>
  <div class="not-found-page">
    <div class="not-found-bg">
      <div class="bg-orb bg-orb-1"></div>
      <div class="bg-orb bg-orb-2"></div>
    </div>

    <div class="not-found-container">
      <div class="not-found-card glass-card">
        <div class="error-code">404</div>
        <div class="error-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10"/>
            <path d="M16 16s-1.5-2-4-2-4 2-4 2M9 9h.01M15 9h.01"/>
          </svg>
        </div>
        <h1 class="error-title">页面不存在</h1>
        <p class="error-desc">抱歉，您访问的页面已不存在或被移除</p>
        <button class="home-btn" @click="goHome">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          返回首页
        </button>
      </div>
    </div>

    <a href="https://deerflow.tech" target="_blank" class="deerflow-brand">✦ Created By Deerflow</a>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  position: relative;
}

.not-found-bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}

.bg-orb-1 {
  width: 400px;
  height: 400px;
  background: rgba(239, 68, 68, 0.3);
  top: 20%;
  left: 10%;
}

.bg-orb-2 {
  width: 300px;
  height: 300px;
  background: rgba(139, 92, 246, 0.2);
  bottom: 20%;
  right: 10%;
}

.not-found-container {
  position: relative;
  z-index: 1;
}

.not-found-card {
  text-align: center;
  padding: 60px 48px;
  min-width: 400px;
}

.error-code {
  font-size: 72px;
  font-weight: 800;
  font-family: var(--font-mono);
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.5), rgba(139, 92, 246, 0.5));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 24px;
  letter-spacing: -2px;
}

.error-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ef4444;
}

.error-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.error-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.home-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border: none;
  border-radius: 10px;
  color: white;
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
}

.home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35);
}
</style>