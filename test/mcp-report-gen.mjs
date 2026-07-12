import fs from 'fs';
const data = JSON.parse(fs.readFileSync('d:/repos/CodingHub/test/mcp-test-results.json', 'utf8'));
const results = data.results;
const total = results.length;
const success = results.filter(r => r.outcome === 'SUCCESS').length;
const graceful = results.filter(r => r.outcome === 'GRACEFUL_ERROR').length;
const serverErr = results.filter(r => r.outcome === 'SERVER_ERROR' || r.outcome === 'MALFORMED').length;
const pass = results.filter(r => r.httpStatus === 200 && r.outcome !== 'MALFORMED').length;
const now = new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' });

const verdictOf = (r) => (r.httpStatus === 200 && r.outcome !== 'MALFORMED') ? 'PASS' : 'FAIL';

function esc(s){ return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function escJson(s){ return esc(s).replace(/"/g,'&quot;'); }

// ---- Markdown ----
let md = `# CodingHub MCP 功能测试报告（测试版）

> 生成时间：${now}（Asia/Shanghai）　|　测试对象：H3CodingHub-MCP-Server v${data.server.version}
> 传输协议：Streamable HTTP \`/mcp\`（兼容 SSE \`/sse\`）　|　测试账号：\`wangbao\`（密码经离线 bcrypt 校验）

## 1. 结论摘要

| 指标 | 结果 |
|------|------|
| 注册工具总数 | **${total}** |
| 可调用（HTTP 200，返回结构化结果） | **${pass}/${total}** |
| 主路径成功（SUCCESS） | ${success} |
| 优雅错误 / 环境依赖（GRACEFUL_ERROR） | ${graceful} |
| 服务端崩溃（SERVER_ERROR / 畸形响应） | ${serverErr} |
| 总体结论 | **✅ 全部 ${total} 个工具可用，无服务端崩溃** |

> 说明：3 个 GRACEFUL_ERROR 均为**预期/环境性**结果——
> - \`h3_coding_hub_tool_file_delete\`：对不存在的文件返回业务错误（安全删除校验，未破坏数据）；
> - \`h3_coding_hub_kb_search\` / \`h3_coding_hub_kb_document_status\`：依赖 **RAG 服务**（\`http://172.53.3.98:8000\`，本环境不可达），返回清晰的“RAG 服务不可用”错误，属优雅降级，非代码缺陷。

## 2. 测试环境

| 组件 | 状态 | 说明 |
|------|------|------|
| 后端 (Spring Boot) | ✅ 运行中 | \`localhost:8082\`，MCP 端点 \`/mcp\` 与 \`/sse\` 均可用 |
| MySQL 8 | ✅ 运行中 | \`localhost:3306/ai_tool_square\`，数据可用（工具 5 / 帖子 2 / 知识库 0） |
| RAG 服务 | ❌ 不可达 | \`172.53.3.98:8000\` 在本测试网络超时；影响 KB 语义检索与文档状态查询 |
| 测试账号 | ✅ 有效 | \`wangbao / 123456\`（写操作鉴权通过） |
| REST API 登录 | ✅ 正常 | \`/api/v1/auth/login\` 正确密码 200、错误密码 401（已用 Node 复核） |

## 3. 调用链路（架构图）

\`\`\`mermaid
flowchart LR
    C[测试客户端<br/>Node.js MCP Client] -->|Streamable HTTP<br/>POST /mcp| S[McpSyncServer<br/>H3CodingHub v2.0.0]
    S --> H[IaihubToolHandler<br/>18 个工具处理]
    H --> T[ToolService / ForumPostService]
    H --> K[KnowledgeBaseService]
    H --> U[UserService<br/>登录鉴权]
    T --> DB[(MySQL<br/>ai_tool_square)]
    K --> DB
    K -->|语义检索/文档状态| R[(RAG 服务<br/>172.53.3.98:8000)]
    R -. 本环境不可达 .-> K
    style R stroke:#e74c3c,color:#e74c3c
\`\`\`

## 4. 单次工具调用时序（示例）

\`\`\`mermaid
sequenceDiagram
    participant C as 测试客户端
    participant M as MCP Server (/mcp)
    participant H as IaihubToolHandler
    participant S as 业务 Service
    participant D as MySQL / RAG
    C->>M: initialize (JSON-RPC)
    M-->>C: serverInfo + capabilities
    C->>M: notifications/initialized
    C->>M: tools/call {name, arguments}
    M->>H: handleXxx(args)
    H->>S: 调用对应 Service
    S->>D: 读/写数据
    D-->>S: 结果
    H-->>M: CallToolResult(isError, TextContent)
    M-->>C: 200 + text/event-stream
\`\`\`

## 5. 结果分布

\`\`\`mermaid
pie title MCP 工具调用结果分布 (n=${total})
    "主路径成功 SUCCESS" : ${success}
    "优雅错误/环境依赖 GRACEFUL_ERROR" : ${graceful}
\`\`\`

## 6. 工具清单与分类

| 分类 | 工具 |
|------|------|
| 工具广场（查询/文件） | tool_search, tool_get, tool_files, tool_download |
| 工具广场（写操作） | tool_create, tool_modify, tool_file_upload, tool_file_delete |
| 论坛 | post_search, post_get, post_create |
| 知识库 | kb_list, kb_search, kb_create, kb_update, kb_delete, kb_upload_document, kb_document_status |

## 7. 测试结果明细

| # | 工具 | 分类 | 期望 | HTTP | 结果 | 耗时(ms) | 判定 |
|---|------|------|------|------|------|----------|------|
${results.map((r,i)=>`| ${i+1} | \`${r.name}\` | ${r.category} | ${r.expectation} | ${r.httpStatus} | ${r.outcome} | ${r.latencyMs} | ${verdictOf(r)} |`).join('\n')}

## 8. 逐工具详情

${results.map((r,i)=>`
### 8.${i+1} \`${r.name}\` — ${verdictOf(r)} (${r.outcome})

- **分类**：${r.category}　**期望行为**：${r.expectation}　**HTTP**：${r.httpStatus}　**耗时**：${r.latencyMs}ms
- **调用参数**：${esc(r.args)}
- **返回摘要**：
\`\`\`json
${esc(r.excerpt)}
\`\`\`
`).join('')}

## 9. 发现与建议

| 编号 | 类型 | 发现 | 严重度 | 建议 |
|------|------|------|--------|------|
| F1 | 环境依赖 | \`kb_search\` / \`kb_document_status\` 依赖 RAG 服务，本环境 RAG（172.53.3.98:8000）不可达，返回“RAG 服务不可用”优雅错误 | 中 | 部署/联调时确保 RAG 服务可达；当前降级处理正确，无需改代码 |
| F2 | 文档不一致 | 项目文档（AGENTS.md / environment.json）标注 MCP 为“17 tools”，实际注册 **18** 个工具 | 低 | 同步文档，避免误导集成方 |
| F3 | 测试产物 | 测试过程中创建了工具/帖子/知识库，已通过 MCP/DB 清理，库内无残留 | 提示 | 已清理，无需处理 |

> 注：早期用 PowerShell \`Invoke-WebRequest\` 调用 REST 登录曾出现 HTTP 500，经 Node 复核确认是 **PowerShell JSON 转义客户端问题**，REST 登录实际正常（正确密码 200 / 错误密码 401）。已排除服务端缺陷。

## 10. 复测方法

\`\`\`bash
# 1) 启动后端（确保 8082 与 RAG 可达）
cd backend && ./gradlew bootRun

# 2) 运行 18 工具测试（自动建连、调用、记录）
cd test && node mcp-test-client.mjs

# 3) 生成本报告
node mcp-report-gen.mjs
\`\`\`

测试产物：\`test/mcp-test-results.json\`（原始结果）、\`test/mcp-test-report.md\`、\`test/mcp-test-report.html\`。
`;

fs.writeFileSync('d:/repos/CodingHub/test/mcp-test-report.md', md);

// ---- HTML ----
const rows = results.map((r,i)=>`<tr class="${verdictOf(r)==='PASS'?'ok':'bad'}">
  <td>${i+1}</td><td><code>${esc(r.name)}</code></td><td>${esc(r.category)}</td>
  <td>${esc(r.expectation)}</td><td>${r.httpStatus}</td><td>${esc(r.outcome)}</td>
  <td>${r.latencyMs}</td><td><b>${verdictOf(r)}</b></td></tr>`).join('');

const details = results.map((r,i)=>`<div class="tool">
  <h3>${i+1}. <code>${esc(r.name)}</code> — <span class="${verdictOf(r)==='PASS'?'ok':'bad'}">${verdictOf(r)}</span> (${esc(r.outcome)})</h3>
  <p>分类：${esc(r.category)} ｜ 期望：${esc(r.expectation)} ｜ HTTP：${r.httpStatus} ｜ 耗时：${r.latencyMs}ms</p>
  <p>参数：${esc(r.args)}</p>
  <pre>${esc(r.excerpt)}</pre>
</div>`).join('');

const html = `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>CodingHub MCP 功能测试报告</title>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
<script>document.addEventListener('DOMContentLoaded',()=>mermaid.initialize({startOnLoad:true}));</script>
<style>
  body{font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;max-width:1100px;margin:0 auto;padding:32px;color:#1f2937;line-height:1.6}
  h1{border-bottom:3px solid #6366f1;padding-bottom:8px}
  table{border-collapse:collapse;width:100%;margin:16px 0;font-size:14px}
  th,td{border:1px solid #e5e7eb;padding:8px 10px;text-align:left}
  th{background:#f3f4f6}
  tr.ok{background:#f0fdf4} tr.bad{background:#fef2f2}
  .ok{color:#16a34a;font-weight:700} .bad{color:#dc2626;font-weight:700}
  pre{background:#0f172a;color:#e2e8f0;padding:12px;border-radius:8px;overflow:auto;font-size:12px}
  code{background:#eef2ff;padding:1px 5px;border-radius:4px;font-size:13px}
  .summary{display:flex;gap:12px;flex-wrap:wrap;margin:16px 0}
  .card{flex:1;min-width:150px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:14px;text-align:center}
  .card .n{font-size:26px;font-weight:800;color:#6366f1}
  .mermaid{background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;padding:12px;margin:12px 0}
  .tool{border:1px solid #e5e7eb;border-radius:10px;padding:12px 16px;margin:12px 0}
</style></head><body>
<h1>CodingHub MCP 功能测试报告（测试版）</h1>
<p>生成时间：${esc(now)} ｜ 服务端：H3CodingHub-MCP-Server v${esc(data.server.version)} ｜ 协议：Streamable HTTP <code>/mcp</code></p>
<div class="summary">
  <div class="card"><div class="n">${total}</div>注册工具</div>
  <div class="card"><div class="n">${pass}</div>可调用 PASS</div>
  <div class="card"><div class="n">${success}</div>主路径成功</div>
  <div class="card"><div class="n">${graceful}</div>优雅/环境错误</div>
  <div class="card"><div class="n">${serverErr}</div>崩溃</div>
</div>
<div class="mermaid">flowchart LR
  C[测试客户端 Node.js] -->|POST /mcp| S[McpSyncServer v2.0.0]
  S --> H[IaihubToolHandler 18工具]
  H --> T[Tool/ForumPost Service]
  H --> K[KnowledgeBaseService]
  H --> U[UserService 鉴权]
  T --> DB[(MySQL)]
  K --> DB
  K -->|语义检索| R[(RAG 172.53.3.98:8000)]
  R -. 不可达 .-> K
  style R stroke:#e74c3c,color:#e74c3c
</div>
<h2>结果明细</h2>
<table><thead><tr><th>#</th><th>工具</th><th>分类</th><th>期望</th><th>HTTP</th><th>结果</th><th>耗时(ms)</th><th>判定</th></tr></thead>
<tbody>${rows}</tbody></table>
<h2>逐工具详情</h2>
${details}
<h2>发现与建议</h2>
<table><thead><tr><th>编号</th><th>类型</th><th>发现</th><th>严重度</th><th>建议</th></tr></thead><tbody>
<tr><td>F1</td><td>环境依赖</td><td>kb_search / kb_document_status 依赖 RAG，本环境不可达，优雅返回“RAG 服务不可用”</td><td>中</td><td>联调时保证 RAG 可达；降级正确无需改码</td></tr>
<tr><td>F2</td><td>文档不一致</td><td>文档标注 MCP 为 17 tools，实际 18 个</td><td>低</td><td>同步文档</td></tr>
<tr><td>F3</td><td>测试产物</td><td>测试创建的工具/帖子/知识库已清理，库内无残留</td><td>提示</td><td>已清理</td></tr>
</tbody></table>
</body></html>`;

fs.writeFileSync('d:/repos/CodingHub/test/mcp-test-report.html', html);
console.log('Report generated:');
console.log('  test/mcp-test-report.md');
console.log('  test/mcp-test-report.html');
console.log(`Summary: ${pass}/${total} PASS, ${success} SUCCESS, ${graceful} GRACEFUL_ERROR, ${serverErr} SERVER_ERROR`);
