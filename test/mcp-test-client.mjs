// CodingHub MCP — comprehensive 18-tool functional test client + report generator
import fs from 'fs';

const BASE = process.env.MCP_BASE || 'http://localhost:8082/mcp';
const USER = process.env.MCP_USER || 'wangbao';
const PASS = process.env.MCP_PASS || '123456';
const OUT_DIR = 'd:/repos/CodingHub/test';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const ts = Date.now();
const stamp = () => new Date().toISOString().replace(/[:.]/g, '-');

function parseSse(text) {
  let data = null;
  for (const line of text.split('\n')) {
    const s = line.trim();
    if (s.startsWith('data:')) {
      try { data = JSON.parse(s.slice(5).trim()); } catch {}
    }
  }
  return data;
}

async function mcpPost(sessionId, body) {
  const headers = { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream' };
  if (sessionId) headers['Mcp-Session-Id'] = sessionId;
  const res = await fetch(BASE, { method: 'POST', headers, body: JSON.stringify(body) });
  const sid = res.headers.get('mcp-session-id');
  const ct = res.headers.get('content-type') || '';
  const text = await res.text();
  const data = ct.includes('text/event-stream') ? parseSse(text) : (text ? (() => { try { return JSON.parse(text); } catch { return null; } })() : null);
  return { status: res.status, sid, data, raw: text };
}

async function toolCall(sid, name, args) {
  return mcpPost(sid, { jsonrpc: '2.0', id: Date.now(), method: 'tools/call', params: { name, arguments: args } });
}

// Extract first text content from a tools/call result
function extract(res) {
  const r = res.data && res.data.result;
  if (!r) return { isError: 'NO_RESULT', text: res.raw };
  const text = (r.content || []).map((c) => c.text || '').join('');
  return { isError: r.isError === true, text };
}

const results = [];
const ctx = { createdToolId: null, createdPostId: null };

function record(name, category, expectation, argsSummary, res, extractInfo) {
  const { isError, text } = extractInfo;
  let outcome;
  if (res.status !== 200) outcome = 'SERVER_ERROR';
  else if (isError === true) outcome = 'GRACEFUL_ERROR';
  else if (isError === false) outcome = 'SUCCESS';
  else outcome = 'MALFORMED';
  results.push({
    name, category, expectation,
    args: argsSummary,
    httpStatus: res.status,
    isError: isError === true,
    outcome,
    latencyMs: res.latencyMs,
    excerpt: (text || '').slice(0, 600),
  });
  const ok = res.status === 200 && outcome !== 'MALFORMED';
  console.log(`[${ok ? 'PASS' : 'FAIL'}] ${name} | http=${res.status} outcome=${outcome} ${res.latencyMs}ms`);
  return ok;
}

async function main() {
  const t0 = Date.now();
  console.log('== MCP handshake ==');
  const init = await mcpPost(null, { jsonrpc: '2.0', id: 1, method: 'initialize', params: { protocolVersion: '2025-03-26', capabilities: {}, clientInfo: { name: 'CodingHub-MCP-Tester', version: '1.0' } } });
  if (init.status !== 200 || !init.sid) { console.error('HANDSHAKE FAILED', init.status); process.exit(2); }
  const sid = init.sid;
  console.log('session', sid, 'server', init.data?.result?.serverInfo);
  await mcpPost(sid, { jsonrpc: '2.0', method: 'notifications/initialized' });
  await sleep(150);

  const list = await toolCall(sid, 'tools/list', {}); // not a real tool; use list via initialize? We already have it.
  // Actually get tools/list properly:
  const listRes = await mcpPost(sid, { jsonrpc: '2.0', id: 2, method: 'tools/list', params: {} });
  const tools = (listRes.data?.result?.tools) || [];
  console.log('REGISTERED TOOLS:', tools.length);

  // ---------- READ / QUERY TOOLS (no auth) ----------
  let r;
  r = await toolCall(sid, 'h3_coding_hub_tool_search', { query: 'MCP', limit: 5 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_search', '工具-查询', 'success', 'query=MCP, limit=5', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_tool_get', { toolId: 4 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_get', '工具-查询', 'success', 'toolId=4', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_tool_files', { toolId: 4 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_files', '工具-查询', 'success', 'toolId=4 (有2个文件)', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_post_search', { query: '', limit: 5 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_post_search', '论坛-查询', 'success', 'query=空, limit=5', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_post_get', { postId: 1 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_post_get', '论坛-查询', 'success', 'postId=1', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_tool_download', { toolId: 4, fileId: 1 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_download', '工具-文件', 'success', 'toolId=4, fileId=1 (真实文件)', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_kb_list', { page: 0, size: 20 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_list', '知识库-查询', 'success', 'page=0, size=20', r, extract(r));

  // ---------- WRITE TOOLS (auth) ----------
  const toolName = `MCP_E2E_TOOL_${ts}`;
  r = await toolCall(sid, 'h3_coding_hub_tool_create', { name: toolName, categoryId: 2, content: 'MCP 端到端自动化测试创建的工具内容。', version: '1.0.0', description: 'MCP e2e test', tags: ['mcp-test', 'e2e'], username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  const createExt = extract(r);
  record('h3_coding_hub_tool_create', '工具-写', 'success', `name=${toolName}, categoryId=2, tags=[mcp-test,e2e]`, r, createExt);
  try { const j = JSON.parse(createExt.text); ctx.createdToolId = j.id ?? j.toolId; } catch {}

  r = await toolCall(sid, 'h3_coding_hub_post_create', { title: `MCP_E2E_POST_${ts}`, content: 'MCP 端到端自动化测试帖子内容。', categoryId: 3, username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  const postExt = extract(r);
  record('h3_coding_hub_post_create', '论坛-写', 'success', `title=MCP_E2E_POST_${ts}, categoryId=3`, r, postExt);
  try { const j = JSON.parse(postExt.text); ctx.createdPostId = j.id ?? j.postId; } catch {}

  const uploadToolId = ctx.createdToolId || 4;
  r = await toolCall(sid, 'h3_coding_hub_tool_file_upload', { toolId: uploadToolId }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_file_upload', '工具-文件', 'success', `toolId=${uploadToolId} (返回上传接口信息)`, r, extract(r));

  const modToolId = ctx.createdToolId || 4;
  r = await toolCall(sid, 'h3_coding_hub_tool_modify', { toolId: modToolId, content: 'MCP 端到端测试：更新后的工具描述。', username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_modify', '工具-写', 'success', `toolId=${modToolId}, 更新content`, r, extract(r));

  // file delete on the freshly created tool (no files) -> graceful error, no data loss
  const delToolId = ctx.createdToolId || 4;
  r = await toolCall(sid, 'h3_coding_hub_tool_file_delete', { toolId: delToolId, fileId: 999, username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_tool_file_delete', '工具-写', 'graceful-error', `toolId=${delToolId}, fileId=999 (不存在, 验证安全错误处理)`, r, extract(r));

  // ---------- KB WRITE TOOLS ----------
  r = await toolCall(sid, 'h3_coding_hub_kb_search', { kbId: 1, query: 'Spring Boot', topK: 3 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_search', '知识库-写', 'graceful-error', 'kbId=1 (知识库不存在/RAG依赖)', r, extract(r));

  const kbName = `MCP_E2E_KB_${ts}`;
  r = await toolCall(sid, 'h3_coding_hub_kb_create', { name: kbName, description: 'MCP e2e kb', chunkMode: 'structural', chunkSize: 800, chunkOverlap: 50, rerank: true, username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_create', '知识库-写', 'env-blocked', `name=${kbName} (依赖RAG服务, 当前RAG不可用)`, r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_kb_update', { kbId: 1, name: 'x', username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_update', '知识库-写', 'graceful-error', 'kbId=1 (知识库不存在)', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_kb_delete', { kbId: 1, username: USER, password: PASS }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_delete', '知识库-写', 'graceful-error', 'kbId=1 (知识库不存在)', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_kb_upload_document', { kbId: 1 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_upload_document', '知识库-写', 'graceful-error', 'kbId=1 (知识库不存在)', r, extract(r));

  r = await toolCall(sid, 'h3_coding_hub_kb_document_status', { kbId: 1 }); r.latencyMs = Date.now()-t0;
  record('h3_coding_hub_kb_document_status', '知识库-写', 'graceful-error', 'kbId=1 (知识库不存在/RAG依赖)', r, extract(r));

  const summary = {
    generatedAt: new Date().toISOString(),
    server: init.data?.result?.serverInfo,
    registeredToolCount: tools.length,
    registeredTools: tools.map((t) => t.name),
    credentials: { user: USER, passwordNote: 'via offline bcrypt verification' },
    results,
    ctx,
  };
  fs.writeFileSync(`${OUT_DIR}/mcp-test-results.json`, JSON.stringify(summary, null, 2));
  console.log('\nWrote mcp-test-results.json');
  console.log('createdToolId=', ctx.createdToolId, 'createdPostId=', ctx.createdPostId);
}

main().catch((e) => { console.error('TEST ERROR', e); process.exit(1); });
