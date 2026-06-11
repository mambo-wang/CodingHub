// CodingHub MCP 客户端 - 列出可用工具
const http = require('http');

const HOST = '127.0.0.1';
const PORT = 8082;
const SSE_PATH = '/sse';
const MSG_PATH = '/mcp/message';

// 工具列表缓存（id -> resolver）
const pendingRequests = new Map();
let sseBuf = '';

function postJson(sessionId, body) {
  return new Promise((resolve, reject) => {
    const data = Buffer.from(JSON.stringify(body), 'utf8');
    const req = http.request({
      host: HOST, port: PORT,
      path: `${MSG_PATH}?sessionId=${sessionId}`,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length,
        'Accept': 'application/json, text/event-stream',
      },
      timeout: 10000,
    }, (res) => {
      let chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8');
        if (text && text.trim().length > 0) {
          console.log(`[POST-sync] status=${res.statusCode} body=${text}`);
        } else {
          console.log(`[POST-sync] status=${res.statusCode} (empty body, awaiting SSE)`);
        }
        resolve();
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

function openSse(sessionResolver) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      host: HOST, port: PORT,
      path: SSE_PATH,
      method: 'GET',
      headers: { 'Accept': 'text/event-stream', 'Cache-Control': 'no-cache' },
    }, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`SSE status ${res.statusCode}`));
        return;
      }
      console.log(`[SSE] connected, status=${res.statusCode}`);
      res.setEncoding('utf8');
      res.on('data', (chunk) => {
        sseBuf += chunk;
        // 按 \n\n 切分事件
        let idx;
        while ((idx = sseBuf.indexOf('\n\n')) !== -1) {
          const raw = sseBuf.slice(0, idx);
          sseBuf = sseBuf.slice(idx + 2);
          parseSseEvent(raw);
        }
      });
      res.on('end', () => console.log('[SSE] closed'));
      res.on('error', (e) => console.log(`[SSE] error: ${e.message}`));
    });
    req.on('error', reject);
    req.end();

    function parseSseEvent(raw) {
      const lines = raw.split('\n');
      let event = 'message';
      let dataLines = [];
      for (const line of lines) {
        if (line.startsWith('event:')) event = line.slice(6).trim();
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
      }
      const data = dataLines.join('\n');
      console.log(`[SSE] event=${event} data=${data.length > 200 ? data.slice(0,200)+'...' : data}`);

      if (event === 'endpoint') {
        const m = data.match(/sessionId=([a-f0-9-]+)/);
        if (m) {
          console.log(`[SSE] sessionId=${m[1]}`);
          sessionResolver.resolve(m[1]);
        }
      } else if (event === 'message' && data) {
        try {
          const msg = JSON.parse(data);
          if (msg.id != null && pendingRequests.has(msg.id)) {
            pendingRequests.get(msg.id)(msg);
            pendingRequests.delete(msg.id);
          }
        } catch (e) {
          console.log(`[SSE] JSON parse error: ${e.message}`);
        }
      }
    }
  });
}

function waitForSessionId() {
  return new Promise((resolve) => {
    openSse({ resolve });
  });
}

function sendAndWait(sessionId, payload, label) {
  return new Promise((resolve, reject) => {
    if (payload.id != null) {
      const timeout = setTimeout(() => {
        pendingRequests.delete(payload.id);
        reject(new Error(`${label} timeout`));
      }, 8000);
      pendingRequests.set(payload.id, (msg) => {
        clearTimeout(timeout);
        resolve(msg);
      });
    }
    postJson(sessionId, payload).catch((e) => {
      if (payload.id != null) {
        pendingRequests.delete(payload.id);
        clearTimeout(timeout);
      }
      reject(e);
    });
  });
}

(async () => {
  console.log('=== CodingHub MCP 客户端 ===');
  const sessionId = await waitForSessionId();

  // initialize
  const initResult = await sendAndWait(sessionId, {
    jsonrpc: '2.0', id: 1, method: 'initialize',
    params: { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'codebuddy-cli', version: '1.0' } }
  }, 'initialize');
  console.log('\n>>> initialize 响应：');
  console.log(JSON.stringify(initResult, null, 2));

  // notifications/initialized (no id, no response expected)
  await postJson(sessionId, { jsonrpc: '2.0', method: 'notifications/initialized' });

  // tools/list
  const listResult = await sendAndWait(sessionId, {
    jsonrpc: '2.0', id: 2, method: 'tools/list', params: {}
  }, 'tools/list');
  console.log('\n>>> tools/list 响应：');
  console.log(JSON.stringify(listResult, null, 2));

  // 退出
  setTimeout(() => process.exit(0), 500);
})().catch((e) => {
  console.error('错误:', e);
  process.exit(1);
});
