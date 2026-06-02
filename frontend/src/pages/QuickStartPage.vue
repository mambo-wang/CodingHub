<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/services/api'

const tools = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const response = await api.get('/tools')
    tools.value = response.data.data?.content?.slice(0, 10) || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const mcpTools = [
  { name: 'h3_coding_hub_tool_search', desc: '搜索工具列表', params: 'keyword?: string' },
  { name: 'h3_coding_hub_tool_get', desc: '获取工具详情', params: 'toolId: number' },
  { name: 'h3_coding_hub_tool_files', desc: '获取工具文件列表', params: 'toolId: number' },
  { name: 'h3_coding_hub_post_search', desc: '搜索社区帖子', params: 'keyword?: string' },
  { name: 'h3_coding_hub_post_get', desc: '获取帖子详情', params: 'postId: number' },
  { name: 'h3_coding_hub_tool_download', desc: '获取工具文件下载链接', params: 'toolId: number, fileId: number' }
]
</script>

<template>
  <div class="quickstart-page">
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">快速开始</h1>
        <p class="page-subtitle">快速集成 MCP 服务到你的应用中</p>
      </div>

      <!-- MCP Config Section -->
      <section class="section">
        <h2 class="section-title">MCP 服务配置</h2>
        
        <div class="config-steps">
          <div class="step">
            <div class="step-number">1</div>
            <div class="step-content">
              <h3>安装 OpenClaw</h3>
              <pre><code>npm install -g openclaw</code></pre>
            </div>
          </div>
          
          <div class="step">
            <div class="step-number">2</div>
            <div class="step-content">
              <h3>配置 MCP Server</h3>
              <pre><code>openclaw mcp set iaihub '{"url":"http://localhost:8080/mcp","transport":"sse"}'</code></pre>
            </div>
          </div>
          
          <div class="step">
            <div class="step-number">3</div>
            <div class="step-content">
              <h3>验证连接</h3>
              <pre><code>openclaw mcp list</code></pre>
            </div>
          </div>
        </div>
      </section>

      <!-- MCP Tools Section -->
      <section class="section">
        <h2 class="section-title">MCP 工具函数</h2>
        <p class="section-desc">以下是可用的 MCP 工具函数列表：</p>
        
        <div class="tools-table">
          <div class="tool-row header">
            <span class="tool-name">函数名</span>
            <span class="tool-desc">描述</span>
            <span class="tool-params">参数</span>
          </div>
          <div v-for="tool in mcpTools" :key="tool.name" class="tool-row">
            <span class="tool-name"><code>{{ tool.name }}</code></span>
            <span class="tool-desc">{{ tool.desc }}</span>
            <span class="tool-params">{{ tool.params }}</span>
          </div>
        </div>
      </section>

      <!-- Usage Example -->
      <section class="section">
        <h2 class="section-title">使用示例</h2>
        
        <div class="code-examples">
          <div class="code-block">
            <h3>搜索工具</h3>
            <pre><code>{ "jsonrpc": "2.0", "method": "tools/call", "params": { "name": "h3_coding_hub_tool_search", "arguments": { "keyword": "harness" } } }</code></pre>
          </div>
          
          <div class="code-block">
            <h3>获取工具详情</h3>
            <pre><code>{ "jsonrpc": "2.0", "method": "tools/call", "params": { "name": "h3_coding_hub_tool_get", "arguments": { "toolId": 8 } } }</code></pre>
          </div>
          
          <div class="code-block">
            <h3>获取文件列表</h3>
            <pre><code>{ "jsonrpc": "2.0", "method": "tools/call", "params": { "name": "h3_coding_hub_tool_files", "arguments": { "toolId": 8 } } }</code></pre>
          </div>
        </div>
      </section>

      <!-- API Reference -->
      <section class="section">
        <h2 class="section-title">REST API 参考</h2>
        
        <div class="api-endpoints">
          <div class="endpoint">
            <span class="method get">GET</span>
            <span class="path">/api/v1/tools</span>
            <span class="desc">获取工具列表</span>
          </div>
          <div class="endpoint">
            <span class="method get">GET</span>
            <span class="path">/api/v1/tools/:id</span>
            <span class="desc">获取工具详情</span>
          </div>
          <div class="endpoint">
            <span class="method get">GET</span>
            <span class="path">/api/v1/tools/:id/files</span>
            <span class="desc">获取工具文件</span>
          </div>
          <div class="endpoint">
            <span class="method get">GET</span>
            <span class="path">/api/v1/categories</span>
            <span class="desc">获取分类</span>
          </div>
          <div class="endpoint">
            <span class="method get">GET</span>
            <span class="path">/api/v1/readme</span>
            <span class="desc">获取 README</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.quickstart-page {
  min-height: 100vh;
  padding: 32px 24px 64px;
  background: var(--bg-primary);
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 12px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
}

.section {
  background: var(--bg-glass);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.section-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.config-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
}

.step-number {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--accent-1), var(--accent-2));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.step-content h3 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
}

.step-content pre {
  margin: 0;
}

.tools-table {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tool-row {
  display: grid;
  grid-template-columns: 220px 1fr 1fr;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  font-size: 13px;
}

.tool-row.header {
  background: rgba(139, 92, 246, 0.1);
  font-weight: 600;
  border-radius: 8px 8px 0 0;
}

.tool-row:first-child {
  border-radius: 8px 8px 0 0;
}

.tool-name code {
  color: var(--accent-1);
  font-size: 12px;
}

.tool-desc {
  color: var(--text-secondary);
}

.tool-params {
  font-family: monospace;
  font-size: 11px;
  color: var(--text-tertiary);
}

.code-examples {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.code-block {
  padding: 16px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.code-block h3 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.code-block pre {
  margin: 0;
  overflow-x: auto;
}

.api-endpoints {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.endpoint {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8px;
}

.method {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.method.get {
  background: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.path {
  font-family: monospace;
  font-size: 13px;
}

.desc {
  color: var(--text-secondary);
  font-size: 13px;
}
</style>