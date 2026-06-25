<template>
  <div class="cover-picker">
    <label class="cover-label">封面设置</label>

    <!-- Current cover preview -->
    <div v-if="currentCoverUrl" class="cover-preview">
      <img :src="currentCoverUrl" alt="封面预览" class="preview-img" />
      <div class="cover-actions">
        <button class="btn-small" @click="resetCover">重新选择</button>
        <button class="btn-small" @click="triggerFileUpload">上传图片</button>
      </div>
    </div>

    <!-- Video frame picker -->
    <div v-else-if="videoSrc && !videoError" class="frame-picker">
      <video
        ref="videoRef"
        :src="videoSrc"
        crossorigin="anonymous"
        preload="auto"
        class="picker-video"
        @loadeddata="onVideoLoaded"
        @error="onVideoError"
      />
      <canvas ref="canvasRef" class="picker-canvas" />

      <div class="slider-row">
        <span class="slider-label">选择时间点</span>
        <input
          ref="sliderRef"
          type="range"
          min="0"
          max="100"
          value="0"
          class="time-slider"
          @input="onSliderInput"
        />
        <span class="slider-time">{{ formatTime(currentTime) }}</span>
      </div>

      <div class="cover-actions">
        <button class="btn-primary-small" @click="captureFrame" :disabled="!videoReady">
          截取当前帧
        </button>
        <button class="btn-small" @click="triggerFileUpload">上传图片</button>
      </div>
    </div>

    <!-- Video error fallback -->
    <div v-else-if="videoError" class="cover-fallback">
      <p class="fallback-text">视频编码不支持预览，请上传图片作为封面</p>
      <button class="btn-small" @click="triggerFileUpload">上传图片</button>
    </div>

    <!-- No video yet -->
    <div v-else class="cover-empty">
      <p class="empty-text">请先上传视频以设置封面</p>
    </div>

    <!-- Hidden file input -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png"
      class="hidden-input"
      @change="onFileSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'

const props = defineProps<{
  videoSrc: string | null
  coverUrl: string | null
}>()

const emit = defineEmits<{
  (e: 'cover-capture', blob: Blob): void
  (e: 'cover-upload', file: File): void
  (e: 'cover-remove'): void
}>()

const videoRef = ref<HTMLVideoElement>()
const canvasRef = ref<HTMLCanvasElement>()
const sliderRef = ref<HTMLInputElement>()
const fileInputRef = ref<HTMLInputElement>()
const videoReady = ref(false)
const videoError = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const currentCoverUrl = ref<string | null>(props.coverUrl || null)

// Watch for external coverUrl changes
import { watch } from 'vue'
watch(() => props.coverUrl, (val) => {
  currentCoverUrl.value = val
})

function onVideoLoaded() {
  if (videoRef.value) {
    duration.value = videoRef.value.duration || 0
    videoReady.value = true
    videoError.value = false
    // Seek to 1s for a better default frame
    videoRef.value.currentTime = Math.min(1, duration.value * 0.1)
  }
}

function onVideoError() {
  videoError.value = true
  videoReady.value = false
}

function onSliderInput() {
  if (!videoRef.value || !sliderRef.value) return
  const pct = Number(sliderRef.value.value) / 100
  const time = pct * duration.value
  videoRef.value.currentTime = time
  currentTime.value = time
}

function captureFrame() {
  if (!videoRef.value || !canvasRef.value) return
  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth || 640
  canvas.height = video.videoHeight || 360
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
  canvas.toBlob((blob) => {
    if (blob) {
      currentCoverUrl.value = URL.createObjectURL(blob)
      emit('cover-capture', blob)
    }
  }, 'image/jpeg', 0.9)
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  // Validate
  if (!['image/jpeg', 'image/png'].includes(file.type)) return
  if (file.size > 5 * 1024 * 1024) return
  currentCoverUrl.value = URL.createObjectURL(file)
  emit('cover-upload', file)
  // Reset input so same file can be re-selected
  input.value = ''
}

function resetCover() {
  currentCoverUrl.value = null
  emit('cover-remove')
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

onBeforeUnmount(() => {
  // Revoke object URLs to prevent memory leaks
  if (currentCoverUrl.value && currentCoverUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(currentCoverUrl.value)
  }
})
</script>

<style scoped>
.cover-picker {
  width: 100%;
}

.cover-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.cover-preview {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.preview-img {
  width: 100%;
  max-height: 240px;
  object-fit: cover;
  display: block;
}

.frame-picker {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-secondary);
}

.picker-video {
  width: 100%;
  max-height: 200px;
  display: block;
  background: #000;
}

.picker-canvas {
  display: none;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
}

.slider-label {
  font-size: 12px;
  color: var(--text-muted);
  white-space: nowrap;
}

.time-slider {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--border-color);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}

.time-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--accent-1);
  cursor: pointer;
  border: 2px solid var(--bg-card);
  box-shadow: 0 0 4px rgba(139, 92, 246, 0.4);
}

.slider-time {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono, monospace);
  min-width: 32px;
  text-align: right;
}

.cover-actions {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
}

.cover-fallback,
.cover-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 16px;
  border: 2px dashed var(--border-color);
  border-radius: 10px;
}

.fallback-text,
.empty-text {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  margin: 0;
}

.btn-small {
  padding: 5px 12px;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-small:hover {
  border-color: var(--border-glow);
  color: var(--text-primary);
}

.btn-primary-small {
  padding: 5px 14px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: var(--accent-1);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary-small:hover {
  opacity: 0.9;
}

.btn-primary-small:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hidden-input {
  display: none;
}

/* Light theme */
[data-theme="light"] .frame-picker {
  background: #f8f9fa;
}

[data-theme="light"] .btn-small:hover {
  background: #f0f0f0;
}

@media (prefers-reduced-motion: reduce) {
  .btn-small,
  .btn-primary-small {
    transition: none;
  }
}
</style>
