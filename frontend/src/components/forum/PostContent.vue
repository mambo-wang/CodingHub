<template>
  <div
    class="post-content markdown-body"
    :data-color-mode="themeStore.theme"
    v-html="processedHtml"
  ></div>
</template>

<script setup lang="ts">
import { computed, watch, nextTick, onMounted } from 'vue'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import mermaid from 'mermaid'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const props = defineProps<{
  content: string
}>()

// 根据当前主题初始化 mermaid
const initMermaid = () => {
  mermaid.initialize({
    startOnLoad: false,
    theme: themeStore.theme === 'dark' ? 'dark' : 'default',
    securityLevel: 'loose',
    fontFamily: 'inherit',
  })
}

initMermaid()

let mermaidId = 0
let renderCount = 0

const md = markdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight(str: string, lang: string): string {
    // mermaid 代码块：输出占位容器，稍后异步渲染为 SVG
    if (lang === 'mermaid') {
      const id = `mermaid-${mermaidId++}`
      return `<div class="mermaid-container" id="${id}" data-code="${encodeURIComponent(str.trim())}"><pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre></div>`
    }

    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // fall through
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

const processedHtml = computed(() => {
  mermaidId = 0
  let html = md.render(props.content)

  // 内部工具链接添加 target="_blank"
  html = html.replace(
    /href="(\/tools\/\d+)"/g,
    'href="$1" target="_blank" rel="noopener"',
  )

  // 外部链接添加标记和 noopener
  html = html.replace(
    /href="(https?:\/\/(?!localhost|127\.0\.0\.1)[^"]+)"/g,
    'href="$1" target="_blank" rel="noopener" class="external-link"',
  )

  return html
})

// 将 mermaid 占位容器渲染为 SVG
const renderMermaidBlocks = async () => {
  renderCount++
  const containers = document.querySelectorAll('.mermaid-container[data-code]')
  for (const el of containers) {
    const code = decodeURIComponent(el.getAttribute('data-code') || '')
    const id = el.id || `mermaid-auto-${Math.random().toString(36).slice(2, 9)}`
    try {
      const { svg } = await mermaid.render(`${id}-r${renderCount}`, code)
      el.innerHTML = svg
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '渲染失败'
      el.innerHTML = `<pre class="mermaid-error"><code>Mermaid 语法错误: ${msg}</code></pre>`
    }
  }
}

// 首次挂载后渲染 mermaid 图表
onMounted(() => {
  renderMermaidBlocks()
})

// 内容或主题变化后重新渲染 mermaid 图表
watch([processedHtml, () => themeStore.theme], () => {
  initMermaid()
  nextTick(() => renderMermaidBlocks())
}, { flush: 'post' })
</script>

<style scoped>
.post-content {
  /* 覆盖 GitHub 的纯色背景，让帖子卡片的毛玻璃效果透出来 */
  background-color: transparent !important;
  font-size: 15px;
}

.post-content :deep(.external-link)::after {
  content: ' ↗';
  font-size: 0.8em;
  opacity: 0.6;
}

/* highlight.js 暗色/亮色主题切换 */
.post-content :deep(.hljs) {
  background: #0d1117;
  color: #e6edf3;
  border-radius: 6px;
}

:root[data-theme='light'] .post-content :deep(.hljs) {
  background: #f6f8fa;
  color: #1f2328;
}

/* 确保代码块在帖子容器中有适当的边距 */
.post-content :deep(pre) {
  margin: 16px 0;
  border-radius: 6px;
}

.post-content :deep(pre code) {
  font-size: 85%;
}

/* 图片在帖子内容中自适应宽度 */
.post-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

/* mermaid 图表容器 */
.post-content :deep(.mermaid-container) {
  display: flex;
  justify-content: center;
  margin: 16px 0;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
}

.post-content :deep(.mermaid-container svg) {
  max-width: 100%;
  height: auto;
}

/* mermaid 语法错误提示 */
.post-content :deep(.mermaid-error) {
  background: #3d1f1f;
  color: #f97583;
  border-radius: 6px;
  padding: 12px;
  font-size: 85%;
}

:root[data-theme='light'] .post-content :deep(.mermaid-error) {
  background: #ffeef0;
  color: #cf222e;
}
</style>
