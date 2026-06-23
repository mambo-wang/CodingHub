<template>
  <div
    class="post-content markdown-body"
    :data-color-mode="themeStore.theme"
    v-html="processedHtml"
  ></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const props = defineProps<{
  content: string
}>()

const md = markdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight(str: string, lang: string): string {
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
</style>
