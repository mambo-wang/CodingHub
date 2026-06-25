<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { Loader2, AlertCircle, RotateCcw } from '@lucide/vue'

const props = defineProps<{
  src: string
  title: string
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const loading = ref(true)
const error = ref('')
const retryCount = ref(0)
const currentTime = ref(0)
const duration = ref(0)
const MAX_RETRIES = 3
const RETRY_DELAYS = [1000, 3000, 5000] // exponential backoff

let retryTimer: ReturnType<typeof setTimeout> | null = null

const ERROR_MESSAGES: Record<number, string> = {
  1: '视频加载被中断',
  2: '网络连接异常，请检查网络后重试',
  3: '视频解码失败，格式可能不兼容',
  4: '视频格式不支持，请使用现代浏览器'
}

const onCanPlay = () => {
  loading.value = false
  error.value = ''
}

const onError = () => {
  const videoEl = videoRef.value
  if (!videoEl) return

  const mediaError = videoEl.error
  const code = mediaError?.code ?? 0

  // code 1 = MEDIA_ERR_ABORTED — user navigated away, not a real error
  if (code === 1) return

  const message = ERROR_MESSAGES[code] || '视频加载失败，请稍后重试'

  // Auto-retry with backoff
  if (retryCount.value < MAX_RETRIES && code !== 4) {
    const delay = RETRY_DELAYS[retryCount.value] ?? 5000
    retryCount.value++
    error.value = `${message}，第 ${retryCount.value} 次重试中...`
    loading.value = true

    retryTimer = setTimeout(() => {
      reloadVideo()
    }, delay)
  } else {
    loading.value = false
    error.value = message
  }
}

const onStalled = () => {
  // Video stalled — could be buffering issue, don't treat as fatal
  if (!error.value) {
    loading.value = true
    // Auto-recover: if still stalled after 5s, trigger retry
    retryTimer = setTimeout(() => {
      if (videoRef.value?.readyState !== undefined && videoRef.value.readyState < 3) {
        onError()
      }
    }, 5000)
  }
}

const onPlaying = () => {
  loading.value = false
  error.value = ''
  retryCount.value = 0
  if (retryTimer) {
    clearTimeout(retryTimer)
    retryTimer = null
  }
}

const onTimeUpdate = () => {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

const onDurationChange = () => {
  if (videoRef.value) {
    duration.value = videoRef.value.duration
  }
}

defineExpose({ currentTime, duration })

const reloadVideo = () => {
  const videoEl = videoRef.value
  if (!videoEl) return
  // Force browser to re-fetch by appending cache-bust param
  const separator = props.src.includes('?') ? '&' : '?'
  videoEl.src = `${props.src}${separator}_t=${Date.now()}`
  videoEl.load()
}

const manualRetry = () => {
  retryCount.value = 0
  loading.value = true
  error.value = ''
  reloadVideo()
}

watch(() => props.src, () => {
  retryCount.value = 0
  loading.value = true
  error.value = ''
})

onBeforeUnmount(() => {
  if (retryTimer) {
    clearTimeout(retryTimer)
  }
})
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
      <div v-if="error && !loading" class="player-overlay player-error" role="alert">
        <AlertCircle :size="40" aria-hidden="true" />
        <span class="overlay-text">{{ error }}</span>
        <button class="retry-btn" @click="manualRetry">
          <RotateCcw :size="16" />
          重新加载
        </button>
      </div>

      <!-- Video Element -->
      <video
        ref="videoRef"
        :src="src"
        controls
        preload="metadata"
        crossorigin="anonymous"
        :aria-label="title"
        class="player-video"
        @canplay="onCanPlay"
        @error="onError"
        @stalled="onStalled"
        @playing="onPlaying"
        @timeupdate="onTimeUpdate"
        @durationchange="onDurationChange"
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
  text-align: center;
  max-width: 80%;
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  margin-top: 4px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-glass);
  color: var(--accent-2);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background: var(--accent-2);
  color: #fff;
  border-color: var(--accent-2);
  box-shadow: 0 0 12px rgba(0, 212, 255, 0.3);
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
