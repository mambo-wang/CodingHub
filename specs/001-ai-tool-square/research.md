# Research Notes: AI 工具广场

**Feature**: 001-ai-tool-square
**Date**: 2026-05-29

## Research Decisions

### Decision 1: Vue 3 UI Framework (Component Library)

**Choice**: Element Plus

**Rationale**:
- Vue 3 生态最成熟的组件库，与 Vue 3 Composition API 无缝集成
- 提供完整的表单验证、表底布局、列表、卡片等组件，契合工具广场场景
- 主题定制能力满足 UI 参考 skillhub.cn 的设计需求
- 活跃维护，文档完善

**Alternatives considered**:
- Naive UI: 更轻量但组件丰富度略逊
- Ant Design Vue: 功能齐全但设计风格偏 B 端企业级，与 skillhub.cn 社区风格差异较大
- TailwindCSS + Headless UI: 更灵活但需要更多自定义工作，MVP 阶段时间成本高

---

### Decision 2: Spring Boot API 架构风格

**Choice**: RESTful API + JSON

**Rationale**:
- 前后端分离架构标准选择
- Vue 3 前端通过 Axios 调用 REST API，职责清晰
- 统一响应结构 `{ code, message, data }` 便于前端处理

**Alternatives considered**:
- GraphQL: 过度设计，MVP 阶段不需要灵活的查询能力
- WebSocket: 不需要实时通信，工具上传/浏览均为请求-响应模式

---

### Decision 3: JWT 实现方案

**Choice**:jjwt library (io.jsonwebtoken:jjwt-api 0.12.x) with Access Token (15min) + Refresh Token (7d)

**Rationale**:
- Spring Security 生态成熟，jjwt 库轻量且易用
- Access Token 短时效保证安全，Refresh Token 支持续期
- Refresh Token 存数据库，支持主动失效（登出/改密码）

**Alternatives considered**:
- OAuth2 + 认证服务器: 过度复杂，MVP 不需要第三方登录
- Session-based auth: 不适合前后端分离场景，CORS 处理复杂

---

### Decision 4: XSS 防护方案

**Choice**: OWASP Java HTML Sanitizer + 后端二次渲染转义

**Rationale**:
- Spring Boot 后端使用 `org.owasp.html.Sanitizers` 对用户提交的 Markdown 内容进行消毒
- 前端使用 `markdown-it` 渲染，默认转义 HTML 标签
- 双重防护确保安全：即便 markdown-it 被绕过，后端仍有防线

**Alternatives considered**:
- 仅前端转义: 不够安全，恶意用户可直接调用后端 API
- 仅后端消毒: 前端渲染可能有漏洞

---

### Decision 5: 数据库连接池

**Choice**: HikariCP (Spring Boot 默认)

**Rationale**:
- HikariCP 是 Spring Boot 默认数据源，性能最优
- 配置简单，无需引入额外依赖
- MySQL 8.0 兼容性好

---

### Decision 6: 前端 Markdown 渲染库

**Choice**: markdown-it + highlight.js

**Rationale**:
- markdown-it 是 Vue 3 生态最流行的 Markdown 解析器
- highlight.js 自动高亮代码块
- 支持插件扩展（如目录生成、图片尺寸控制）

**Alternatives considered**:
- marked: 性能略差
- remark: AST 解析过于复杂，MVP 不需要

---

### Decision 7: 项目初始化方式

**Choice**: 前端用 Vite 手动创建（不套用脚手架），后端用 Gradle 初始化

**Rationale**:
- Vite 手动创建更干净，避免 CRA create-react-app 等旧式脚手架的依赖冗余
- 后端 Spring Boot 使用 Spring Initializr 结构，Gradle 配置简洁
- 两个项目独立初始化，无耦合

**Alternatives considered**:
- Monorepo (pnpm workspace): MVP 阶段增加复杂度，暂不需要
- 单一 Maven 多模块: 与用户指定的 Gradle 冲突

---

## Technical Notes

### MySQL Schema 注意事项
- 使用 `utf8mb4` 字符集（而非 `utf8`），完整支持 emoji 和特殊符号
- `tool.content` 字段类型为 `TEXT`，长度约 65KB，足够 5000 字符 Markdown
- `user.password` 字段使用 `varchar(255)` 存储 BCrypt hash

### Vue 3 + TypeScript 配置
- `tsconfig.json` 开启 `strict: true`，满足宪章类型安全要求
- 使用 `<script setup lang="ts">` 语法，保持类型推导

### JWT 存储
- Access Token: 存 memory，不持久化（刷新页面后需重新登录）
- Refresh Token: 存 httpOnly Cookie（防 XSS），或 localStorage（方便开发）
- MVP 阶段简化：均存 localStorage，生产环境应使用 httpOnly Cookie