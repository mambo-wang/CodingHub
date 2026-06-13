<script setup lang="ts">
import { ref } from 'vue'

const copySuccess = ref(false)

// 后端端口可通过 VITE_BACKEND_PORT 覆盖，默认 8082
const mcpBackendPort = (import.meta.env.VITE_BACKEND_PORT as string) || '8082'
const mcpConfig = {
  "mcpServers": {
    "CodingHub-mcp": {
      "type": "sse",
      "url": `http://${window.location.hostname}:${mcpBackendPort}/sse`,
      "description": "CodingHub MCP Server",
      "disabled": false
    }
  }
}

const mcpConfigJson = JSON.stringify(mcpConfig, null, 2)

const copyConfig = async () => {
  try {
    await navigator.clipboard.writeText(mcpConfigJson)
    copySuccess.value = true
    setTimeout(() => { copySuccess.value = false }, 2000)
  } catch (e) {
    console.error(e)
  }
}

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
        <p class="page-subtitle">快速集成 MCP 服务到你的 AI 助手</p>
      </div>

      <!-- MCP Config Section -->
      <section class="section glass-card">
        <div class="section-header">
          <div class="section-badge">01</div>
          <h2 class="section-title">配置 MCP</h2>
        </div>

        <p class="section-desc">将以下配置添加到 CodeBuddy 的 MCP 配置中，即可连接 CodingHub MCP 服务：</p>

        <div class="config-card">
          <div class="config-label">MCP 配置</div>
          <pre class="config-code">{{ mcpConfigJson }}</pre>
          <button class="copy-btn" :class="{ success: copySuccess }" @click="copyConfig">
            <svg v-if="!copySuccess" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2"/>
              <path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>
            </svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
            {{ copySuccess ? '已复制' : '一键复制' }}
          </button>
        </div>
      </section>

      <!-- MCP Tools Section -->
      <section class="section glass-card">
        <div class="section-header">
          <div class="section-badge">02</div>
          <h2 class="section-title">MCP 工具列表</h2>
        </div>

        <p class="section-desc">CodingHub MCP 提供以下工具函数，可通过 AI 助手直接调用：</p>

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

      <!-- Usage Tips -->
      <section class="section glass-card">
        <div class="section-header">
          <div class="section-badge">03</div>
          <h2 class="section-title">提示词示例</h2>
        </div>

        <div class="tips-grid">
          <div class="tip-card">
            <div class="tip-icon">🔍</div>
            <h3>搜索工具</h3>
            <p>使用CodingHub MCP查询工具列表, 并根据现有skill的tools.version版本号排查有无需要升级的工具</p>
          </div>
          <div class="tip-card">
            <div class="tip-icon">📦</div>
            <h3>获取详情</h3>
            <p>使用CodingHub MCP获取using-superpowers-wb SKILL的完整信息和文件列表，安装到当前项目，并把工具版本号写到skill文件夹的tools.version文件中。如果本地已有该skill，则覆盖安装。</p>
          </div>
          <div class="tip-card">
            <div class="tip-icon">💬</div>
            <h3>社区交流</h3>
            <p>使用CodingHub MCP新建帖子，把XXX.md 把这个文档发布到论坛</p>
          </div>
          <div class="tip-card">
            <div class="tip-icon">⬇️</div>
            <h3>分享工具</h3>
            <p>使用CodingHub MCP把XXX这个skill发布到CodingHub工具广场，工具描述中添加工具介绍和安装方法，把skill相关文件压缩为zip包上传到工具附件。</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.quickstart-page {
  min-height: 100vh;
  padding: 48px 24px 80px;
  background: var(--bg-primary);
  background-image:
    radial-gradient(ellipse at 20% 20%, rgba(139, 92, 246, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(6, 182, 212, 0.08) 0%, transparent 50%);
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.page-title {
  font-size: 40px;
  font-weight: 700;
  background: linear-gradient(135deg, #8b5cf6, #06b6d4, #ec4899);
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
  padding: 32px;
  margin-bottom: 24px;
  border-radius: 20px;
  background: rgba(15, 15, 20, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(20px);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.section-badge {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #8b5cf6, #06b6d4);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: white;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  line-height: 1.6;
}

.config-card {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
  position: relative;
}

.config-label {
  font-size: 12px;
  font-weight: 600;
  color: #8b5cf6;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.config-code {
  font-family: 'Fira Code', monospace;
  font-size: 13px;
  color: #ffffff;
  line-height: 1.6;
  white-space: pre;
  overflow-x: auto;
  margin: 0 0 16px 0;
}

.copy-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.8), rgba(6, 182, 212, 0.8));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.copy-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(139, 92, 246, 0.3);
}

.copy-btn.success {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.8), rgba(6, 182, 212, 0.8));
}

.tools-table {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.tool-row {
  display: grid;
  grid-template-columns: 200px 1fr 160px;
  gap: 16px;
  padding: 14px 16px;
  background: rgba(0, 0, 0, 0.3);
  font-size: 13px;
}

.tool-row.header {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.1));
  font-weight: 600;
  color: var(--text-primary);
}

.tool-name code {
  color: #8b5cf6;
  font-size: 12px;
  font-family: 'Fira Code', monospace;
}

.tool-desc {
  color: var(--text-secondary);
}

.tool-params {
  font-family: 'Fira Code', monospace;
  font-size: 11px;
  color: var(--text-tertiary);
}

.tips-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.tip-card {
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.25s ease;
}

.tip-card:hover {
  border-color: rgba(139, 92, 246, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.tip-icon {
  font-size: 28px;
  margin-bottom: 12px;
}

.tip-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.tip-card p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .tools-table {
    grid-template-columns: 1fr;
  }

  .tool-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .tips-grid {
    grid-template-columns: 1fr;
  }

  .section {
    padding: 24px 20px;
  }
}

/* Light theme fixes */
[data-theme="light"] .config-card {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 0, 0, 0.1);
}

[data-theme="light"] .config-code {
  color: #1a1a2e;
}

[data-theme="light"] .section {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.08);
}

[data-theme="light"] .tool-row {
  background: rgba(255, 255, 255, 0.7);
}

[data-theme="light"] .tool-name code {
  color: #7c3aed;
}

[data-theme="light"] .tip-card {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.08);
}

[data-theme="light"] .tip-card:hover {
  border-color: rgba(139, 92, 246, 0.4);
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.1);
}

[data-theme="light"] .page-title {
  background: linear-gradient(135deg, #7c3aed, #0891b2, #db2777);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

[data-theme="light"] .section-badge {
  background: linear-gradient(135deg, #7c3aed, #0891b2);
}
</style>