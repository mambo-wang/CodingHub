<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface AvatarUser {
  id: number
  username: string
  nickname?: string | null
  avatarUrl?: string | null
}

const props = withDefaults(defineProps<{
  user: AvatarUser
  size?: 'sm' | 'md' | 'lg' | 'xl'
  displayName?: string | null
}>(), { size: 'md' })

const PALETTE = ['#8b5cf6', '#06b6d4', '#ec4899', '#f59e0b', '#3b82f6', '#10b981']
const sizeMap = { sm: 24, md: 32, lg: 40, xl: 96 }

const sizePx = computed(() => sizeMap[props.size])
const initial = computed(() => {
  const source = props.displayName || props.user.nickname || props.user.username || '?'
  return source.charAt(0).toUpperCase()
})
const paletteColor = computed(() => PALETTE[Math.abs(props.user.id) % PALETTE.length])
const imgError = ref(false)

// Reset error state when avatarUrl changes (e.g. after upload)
watch(() => props.user.avatarUrl, () => { imgError.value = false })

const showImage = computed(() => !!props.user.avatarUrl && !imgError.value)
const fontSize = computed(() => Math.floor(sizePx.value * 0.45))
const xlFontSize = computed(() => Math.floor(sizePx.value * 0.4))

const onError = () => { imgError.value = true }
</script>

<template>
  <div
    class="user-avatar"
    :class="[`user-avatar--${size}`]"
    :style="{
      width: `${sizePx}px`,
      height: `${sizePx}px`,
      fontSize: size === 'xl' ? `${xlFontSize}px` : `${fontSize}px`
    }"
    :title="`${user.username} 的头像`"
    role="img"
    :aria-label="`${user.username} 的头像`"
  >
    <img
      v-if="showImage"
      :src="user.avatarUrl!"
      :alt="`${user.username} 的头像`"
      class="user-avatar__img"
      loading="lazy"
      @error="onError"
    />
    <span
      v-else
      class="user-avatar__initial"
      :style="{ background: paletteColor }"
    >{{ initial }}</span>
  </div>
</template>

<style scoped>
.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
  position: relative;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}
.user-avatar:hover {
  border-color: var(--border-glow);
}
.user-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.user-avatar__initial {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-family: var(--font-display);
}
.user-avatar:focus-visible {
  outline: 2px solid var(--focus-ring, #00FFFF);
  outline-offset: 2px;
}
@media (prefers-reduced-motion: reduce) {
  .user-avatar { transition: none; }
}
</style>
