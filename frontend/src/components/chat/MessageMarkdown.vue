<script setup lang="ts">
import { computed, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const props = defineProps<{ content: string }>()

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang }).value +
          '</code></pre>'
        )
      } catch (e) {
        /* ignore highlight error */
      }
    }
    return '<pre class="hljs"><code>' + escapeHtml(str) + '</code></pre>'
  },
})

md.renderer.rules.link_open = (tokens, idx, options, _env, self) => {
  const token = tokens[idx]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

const rendered = computed(() => md.render(props.content || ''))
const copied = ref(false)

function onCodeClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  const pre = target.closest('pre.hljs')
  if (pre) {
    navigator.clipboard?.writeText(pre.textContent || '')
    copied.value = true
    window.setTimeout(() => (copied.value = false), 1500)
  }
}
</script>

<template>
  <div>
    <div class="markdown-body" v-html="rendered" @click="onCodeClick"></div>
    <span v-if="copied" class="copied-toast" role="status">已复制</span>
  </div>
</template>

<style scoped>
.markdown-body :deep(pre.hljs) {
  position: relative;
  background: var(--bg-primary, rgba(0, 0, 0, 0.25));
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
  overflow-x: auto;
  cursor: pointer;
  margin: 6px 0;
}
.markdown-body :deep(code) {
  font-family: var(--font-mono);
  font-size: 13px;
}
.markdown-body :deep(p) {
  margin: 4px 0;
}
.markdown-body :deep(a) {
  color: var(--accent-1);
  text-decoration: underline;
}
.copied-toast {
  font-size: 11px;
  color: var(--accent-1);
  margin-left: 6px;
}
</style>
