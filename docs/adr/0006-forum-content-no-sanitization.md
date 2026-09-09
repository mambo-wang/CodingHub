# 论坛正文不做 HTML 消毒

论坛帖子正文（Markdown 与 HTML 两种格式）在写入与渲染时**均不做任何 XSS 消毒**，任何登录用户都可发布含任意 HTML 的帖子。

我们明确接受由此带来的存储型 XSS 风险：`accessToken` 与 `refreshToken` 存放于 `localStorage`（`frontend/src/stores/auth.ts`），因此一篇恶意帖子足以窃取浏览者的 refreshToken（7 天有效）并接管其账号。

关键在于**该风险在本决定之前就已存在**：`PostContent.vue` 的 markdown-it 开启了 `html: true` 且渲染结果未经 sanitize 直接走 `v-html`，同样的 payload 今天就能贴进 Markdown 帖子。本 ADR 只是拒绝在"支持 HTML 格式"这个新功能上顺手修补它——既不引入 jsoup / DOMPurify 依赖，也不加 CSP 兜底，也不把 HTML 格式限制给管理员。

## Considered Options

- **后端 jsoup 白名单入库消毒** —— 与"HTML 原文无损保存、可无损再编辑"（见 `CONTEXT.md` 正文格式）直接冲突，且会清洗掉 Markdown 代码块中的 HTML 示例。
- **前端 DOMPurify 渲染时消毒** —— 保住原文，但消毒规则前后端不共享，MCP 返回的原文对第三方客户端依然有毒。
- **加 CSP 响应头兜底** —— `script-src` 不给 `unsafe-inline` 即可让 `onerror` 失效且不损失原文，是性价比最高的方案，仍被否决。
- **限制 HTML 格式仅管理员可用** —— 把风险押在少数账号上，且 Markdown 内联 HTML 这个既有洞不受影响。

## Consequences

- 项目现成的 `XssSanitizer.sanitize()` 是 `escapeHtml4()`（全量转义 HTML），与"存储 HTML"不兼容，**不可**用于论坛正文——它是被有意绕过的，不是漏用。
- 若日后反悔，最低成本路径是加 CSP 响应头：它不改动任何正文内容，也不影响已入库数据。
