<script setup lang="ts">
import { ref } from 'vue'
import { Loader2, AlertCircle } from '@lucide/vue'

defineProps<{
  src: string
  title: string
}>()

const loading = ref(true)
const error = ref('')

const onCanPlay = () => {
  loading.value = false
}

const onError = () => {
  loading.value = false
  error.value = '视频加载失败，请稍后重试'
}
</script>

<template>
  <div class="video-player" aria-label="视频播放器">
    <div class="player-container">
      <!-- Loading State -->
      <div v-if="loading && !error" class="player-overlay">
        <Loader2 :size="40" class="spin" aria-hidden="true" />
        <span class="overlay-text">加载中...</span>
      </div>

      <!-- Error State -->
      <div v-if="error" class="player-overlay player-error" role="alert">
        <AlertCircle :size="40" aria-hidden="true" />
        <span class="overlay-text">{{ error }}</span>
      </div>

      <!-- Video Element -->
      <video
        :src="src"
        controls
        :aria-label="title"
        class="player-video"
        @canplay="onCanPlay"
        @error="onError"
      >
        您的浏览器不支持视频播放
      </video>
    </div>
  </div>
</template>

<style scoped>
.video-player {
  width: 100%;
}

.player-container {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  overflow: hidden;
}

.player-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.player-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  z-index: 2;
  color: var(--accent-2);
}

.player-error {
  color: #ef4444;
}

.overlay-text {
  font-size: 14px;
  color: var(--text-secondary);
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .spin {
    animation: none;
  }
}
</style>
