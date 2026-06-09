# Implementation Plan: CodingHub

**Branch**: `001-ai-tool-square` | **Date**: 2026-05-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-ai-tool-square/spec.md`

## Summary

CodingHub是一个帮助用户发现、分享和管理 AI 工具（Skill、MCP、API、Prompt 等）的社区平台。核心功能包括：工具浏览与搜索、工具详情展示（Markdown 渲染）、用户注册登录、工具上传管理。

技术方案：前后端分离架构，前端使用 Vue3 + TypeScript（frontend-design 技能辅助 UI 设计），后端使用 Spring Boot + Gradle，数据库使用 MySQL。RESTful API 提供数据交互通道。

## Technical Context

**Language/Version**: Java 17+, Kotlin (optional for Spring Boot), Vue 3.4+, TypeScript 5.3+

**Primary Dependencies**:
- Backend: Spring Boot 3.2+, Spring Security (JWT), Spring Data JPA, MySQL Connector/J 8.0, jjwt (JWT), commons-text (XSS sanitization), flexmark (Markdown parsing)
- Frontend: Vue 3.4+, Vite 5+, Pinia (state), Vue Router 4, Axios, @vueuse/core, markdown-it, highlight.js, TailwindCSS (or Element Plus per constitution)
- Testing: JUnit 5, Mockito, SpringBootTest, Vue Test Utils, Playwright

**Storage**: MySQL 8.0 (host: localhost, port: 3306, user: root, password: root, database: ai_tool_square)

**Testing**: JUnit 5 + Mockito (backend unit), Vue Test Utils (frontend unit), SpringBootTest (integration), Playwright (E2E)

**Target Platform**: Web browser (Chrome/Firefox/Safari latest 2 versions), backend on Linux/macOS server

**Project Type**: Web application (frontend + backend API)

**Performance Goals**:
- 首页加载 < 3s (SC-001)
- API 读操作 p95 < 200ms, 写操作 p95 < 500ms (per constitution)
- 搜索响应 < 1s (SC-006)
- 支持 100 并发用户 (SC-007)

**Constraints**: 单体架构前后端同服务器部署；无文件上传；无第三方登录

**Scale/Scope**: MVP 阶段，预计 < 1000 工具，< 10000 注册用户

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 宪章条款 | 检查项 | 状态 | 说明 |
|---------|--------|------|------|
| I. Code Quality — Type Safety | 前后端均使用强类型语言/框架（TypeScript strict, Java 17） | ✅ PASS | Vue3 + TypeScript strict mode, Spring Boot + JPA 实体注解 |
| I. Code Quality — Error Handling | 后端方法不返回 null，使用 Optional 或抛异常 | ✅ PASS | JPA findById 返回 Optional，Service 层使用异常体系 |
| II. Testing Standards — TDD | 新功能测试先行，R-G-R 循环 | ⚠️ NOTE | 实现阶段遵循，本次规划不涉及测试代码 |
| II. Testing Standards — Coverage | 新代码覆盖 >= 80%，关键路径 100% | ⚠️ NOTE | 实现阶段质量门控 |
| II. Testing Standards — Test Categories | 单元/契约/集成三类测试 | ✅ PASS | JUnit5(unit), SpringBootTest(integration), REST contract |
| III. UX Consistency — Feedback | 所有操作有视觉反馈 | ✅ PASS | 加载状态、Toast 提示、错误消息设计 |
| III. UX Consistency — a11y | 键盘可访问，WCAG 2.1 AA | ⚠️ NOTE | 实现阶段验证，MVP 可基础达标 |
| IV. Performance — Response Time | API p95 读 < 200ms，写 < 500ms | ✅ PASS | 在 Constraints 中声明 |
| V. Observability — Structured Logging | 结构化日志（JSON） | ✅ PASS | Spring Boot + Logback JSON encoder |
| V. Observability — Tracing | 相关性 ID | ⚠️ NOTE | 单体架构暂不需分布式追踪，可预留 X-Request-ID |
| VI. Simplicity — YAGNI | 不做预见性功能 | ✅ PASS | MVP 仅实现 spec 中定义的 5 个用户故事 |
| Security — Input Validation | 所有外部输入验证消毒 | ✅ PASS | XSS 防护：commons-text StringEscapeUtils，SQL 注入：JPA 参数化查询 |
| Security — Auth | JWT with short expiry | ✅ PASS | Access token 15min + Refresh token 7d |

**Gate Result**: ✅ ALL GATES PASS — 项目技术方案符合宪法要求，可以继续

## Project Structure

### Documentation (this feature)

```text
specs/001-ai-tool-square/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── api-contracts.md # REST API contract definitions
├── user-journeys.md     # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
ai-tool-square/          # 项目根目录 (或直接用仓库根)
├── backend/             # Spring Boot 后端
│   ├── src/main/java/com/iaihub/toolbox/
│   │   ├── ToolSquareApplication.java
│   │   ├── config/          # Security, CORS, Web config
│   │   ├── controller/      # REST Controllers
│   │   ├── service/        # Business logic
│   │   ├── repository/     # JPA Repositories
│   │   ├── model/          # JPA Entities
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Global exception handling
│   │   └── util/           # XSS sanitization, JWT utils
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/   # Flyway migrations (optional)
│   ├── src/test/java/      # Unit + Integration tests
│   └── build.gradle
├── frontend/             # Vue 3 前端
│   ├── src/
│   │   ├── assets/         # 静态资源
│   │   ├── components/     # 可复用组件
│   │   ├── pages/          # 页面组件
│   │   ├── stores/         # Pinia stores
│   │   ├── services/       # API 调用层
│   │   ├── router/         # Vue Router
│   │   ├── utils/          # 工具函数
│   │   └── App.vue
│   ├── public/
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
└── README.md
```

**Structure Decision**: Web application (Option 2) with separate backend/frontend directories. Backend uses standard Spring Boot package layout. Frontend uses Vue 3 + Vite feature-based structure. No mobile targets in scope.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Phase 0: Research Notes

All unknowns already resolved from user requirements:

- **Tech stack (Vue3 + Spring Boot + Gradle + MySQL)**: explicitly specified by user, no alternatives needed
- **UI reference (skillhub.cn)**: 已确认参考站点，frontend-design 技能将辅助实现
- **Database**: MySQL 已安装，账号密码 root/root
- **Authentication**: JWT (而非 session) per constitution and spec assumption
- **Markdown rendering**: flexmark (backend) + markdown-it (frontend) — both well-supported, no NEEDS CLARIFICATION