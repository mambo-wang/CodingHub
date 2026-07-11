<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Eye, EyeOff } from '@lucide/vue'
import api from '@/services/api'

interface DanmakuItem {
  id: number
  content: string
  timeSeconds: number
  danmakuType: string
  color: string
}

interface ActiveDanmaku extends DanmakuItem {
  key: string
  top: number
  animating: boolean
}

const props = defineProps<{
  videoId: number
  currentTime: number
  duration: number
  initialVisible?: boolean
}>()

const allDanmaku = ref<DanmakuItem[]>([])
const activeItems = ref<ActiveDanmaku[]>([])
const visible = ref(props.initialVisible !== false)
const opacity = ref(0.85)
const containerRef = ref<HTMLElement | null>(null)

const SCROLL_DURATION = 8
const DANMAKU_HEIGHT = 28
const MAX_LANES = 12
const SHOW_WINDOW = 0.5 // seconds tolerance for showing danmaku

// Track which danmaku have been shown to avoid duplicates
const shownIds = ref<Set<number>>(new Set())
// Lane tracking for SCROLL type: stores the time when each lane becomes free
const laneFreeAt = ref<number[]>(new Array(MAX_LANES).fill(0))

let lastTime = -1
let animFrame: number | null = null

const fetchDanmaku = async () => {
  try {
    const response = await api.get(`/videos/${props.videoId}/danmaku`)
    const data = response.data.data
    allDanmaku.value = Array.isArray(data) ? data : (data?.content || [])
  } catch {
    allDanmaku.value = []
  }
}

onMounted(() => {
  fetchDanmaku()
})

onBeforeUnmount(() => {
  if (animFrame) cancelAnimationFrame(animFrame)
})

// Find the next available lane for SCROLL type
const findAvailableLane = (currentTime: number): number => {
  for (let i = 0; i < MAX_LANES; i++) {
    if (laneFreeAt.value[i] <= currentTime) {
      return i
    }
  }
  // All lanes busy, use the one that frees up soonest
  let minIdx = 0
  for (let i = 1; i < MAX_LANES; i++) {
    if (laneFreeAt.value[i] < laneFreeAt.value[minIdx]) {
      minIdx = i
    }
  }
  return minIdx
}

// Watch currentTime to manage active danmaku
watch(() => props.currentTime, (newTime) => {
  if (!visible.value) return

  // Detect seek: if time jumped backwards significantly, reset
  if (newTime < lastTime - 1) {
    activeItems.value = []
    shownIds.value.clear()
    laneFreeAt.value = new Array(MAX_LANES).fill(0)
  }
  lastTime = newTime

  // Add new danmaku that should appear
  const toAdd: ActiveDanmaku[] = []
  for (const dm of allDanmaku.value) {
    if (shownIds.value.has(dm.id)) continue
    if (dm.timeSeconds > newTime + SHOW_WINDOW) continue
    if (dm.timeSeconds < newTime - 1) continue // skip past ones (except very recent)

    shownIds.value.add(dm.id)

    if (dm.danmakuType === 'SCROLL') {
      const lane = findAvailableLane(newTime)
      laneFreeAt.value[lane] = newTime + SCROLL_DURATION * 0.4
      toAdd.push({
        ...dm,
        key: `dm-${dm.id}-${newTime.toFixed(2)}`,
        top: lane * DANMAKU_HEIGHT + 8,
        animating: true
      })
    } else {
      // TOP or BOTTOM
      toAdd.push({
        ...dm,
        key: `dm-${dm.id}-${newTime.toFixed(2)}`,
        top: 0,
        animating: true
      })
    }
  }

  if (toAdd.length > 0) {
    activeItems.value = [...activeItems.value, ...toAdd]
  }

  // Remove expired danmaku
  activeItems.value = activeItems.value.filter(item => {
    if (item.danmakuType === 'SCROLL') {
      return newTime - item.timeSeconds < SCROLL_DURATION
    }
    // TOP and BOTTOM: show for 4 seconds
    return newTime - item.timeSeconds < 4
  })
}, { flush: 'post' })

// Clean up when visibility toggled off
watch(visible, (val) => {
  if (!val) {
    activeItems.value = []
  }
})

const getDanmakuStyle = (item: ActiveDanmaku): Record<string, string> => {
  const base: Record<string, string> = {
    color: item.color || '#ffffff',
    opacity: String(opacity.value)
  }

  if (item.danmakuType === 'SCROLL') {
    base.top = `${item.top}px`
    base['--travel'] = `${travelDistance.value}px`
  } else if (item.danmakuType === 'TOP') {
    base.top = `${getFixedPosition(item, 'top')}px`
    base.left = '50%'
    base.transform = 'translateX(-50%)'
    base.animationName = 'danmaku-fixed'
  } else {
    // BOTTOM
    base.bottom = `${getFixedPosition(item, 'bottom')}px`
    base.left = '50%'
    base.transform = 'translateX(-50%)'
    base.animationName = 'danmaku-fixed'
  }

  return base
}

// Calculate stacked position for TOP/BOTTOM danmaku
const getFixedPosition = (item: ActiveDanmaku, _direction: 'top' | 'bottom'): number => {
  const sameType = activeItems.value.filter(
    d => d.danmakuType === item.danmakuType && d.id !== item.id && d.timeSeconds <= item.timeSeconds
  )
  const index = sameType.length % MAX_LANES
  return index * DANMAKU_HEIGHT + 8
}

// Container width for computing accurate scroll travel distance
const travelDistance = computed(() => {
  return containerRef.value?.offsetWidth || 960
})

const toggleVisibility = () => {
  visible.value = !visible.value
}
</script>

<template>
  <div ref="containerRef" class="danmaku-container">
    <div v-if="visible" class="danmaku-layer">
      <div
        v-for="item in activeItems"
        :key="item.key"
        class="danmaku-item"
        :class="{
          'danmaku-scroll': item.danmakuType === 'SCROLL',
          'danmaku-top': item.danmakuType === 'TOP',
          'danmaku-bottom': item.danmakuType === 'BOTTOM'
        }"
        :style="getDanmakuStyle(item)"
      >
        {{ item.content }}
      </div>
    </div>

    <div class="danmaku-controls">
      <button
        class="danmaku-toggle-btn"
        :title="visible ? '隐藏弹幕' : '显示弹幕'"
        @click.stop="toggleVisibility"
      >
        <EyeOff v-if="visible" :size="14" />
        <Eye v-else :size="14" />
        <span>{{ visible ? '弹幕' : '弹幕' }}</span>
      </button>
      <div v-if="visible" class="danmaku-opacity" @click.stop>
        <span class="opacity-label">透明</span>
        <input
          v-model.number="opacity"
          type="range"
          min="0.1"
          max="1"
          step="0.1"
          class="opacity-slider"
        />
        <span class="opacity-label">不透明</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.danmaku-container {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 5;
}

.danmaku-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.danmaku-item {
  position: absolute;
  white-space: nowrap;
  font-size: 18px;
  font-weight: 700;
  text-shadow:
    -1px -1px 0 rgba(0, 0, 0, 0.7),
    1px -1px 0 rgba(0, 0, 0, 0.7),
    -1px 1px 0 rgba(0, 0, 0, 0.7),
    1px 1px 0 rgba(0, 0, 0, 0.7);
  pointer-events: none;
  will-change: transform;
}

.danmaku-scroll {
  right: 0;
  animation: danmaku-scroll 8s linear forwards;
}

.danmaku-top,
.danmaku-bottom {
  animation: danmaku-fixed 0.3s ease forwards;
}

@keyframes danmaku-scroll {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(calc(-100% - var(--travel, 100vw)));
  }
}

@keyframes danmaku-fixed {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.danmaku-controls {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  pointer-events: auto;
  z-index: 10;
}

.danmaku-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  backdrop-filter: blur(4px);
}

.danmaku-toggle-btn:hover {
  background: rgba(0, 0, 0, 0.7);
  border-color: rgba(255, 255, 255, 0.3);
}

.danmaku-opacity {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 6px;
  backdrop-filter: blur(4px);
}

.opacity-label {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
}

.opacity-slider {
  width: 60px;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}

.opacity-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}

.opacity-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  cursor: pointer;
  border: none;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.3);
}
</style>
