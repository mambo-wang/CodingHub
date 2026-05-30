<template>
  <div class="post-content" v-html="processedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import markdownIt from 'markdown-it';

const props = defineProps<{
  content: string;
}>();

const md = markdownIt({
  html: true,
  linkify: true
});

const processedHtml = computed(() => {
  let html = md.render(props.content);

  // 识别 /tools/\d+ 链接，添加 target="_blank"
  html = html.replace(/href="(\/tools\/\d+)"/g, 'href="$1" target="_blank" rel="noopener"');

  // 识别外部链接，添加标记
  html = html.replace(/href="(https?:\/\/(?!localhost|127\.0\.0\.1)[^"]+)"/g,
    'href="$1" target="_blank" rel="noopener" class="external-link"');

  return html;
});
</script>

<style scoped>
.post-content {
  line-height: 1.6;
}

.post-content :deep(h1) {
  font-size: 1.5em;
  margin: 1em 0;
}

.post-content :deep(code) {
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 4px;
}

.post-content :deep(.external-link)::after {
  content: ' ↗';
  font-size: 0.8em;
}
</style>