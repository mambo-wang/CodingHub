# Tasks: CodingHub

**Input**: Design documents from `/specs/001-ai-tool-square/`

**Prerequisites**: plan.md (✅), spec.md (✅), research.md (✅), data-model.md (✅), contracts/api-contracts.md (✅), quickstart.md (✅)

---

## Phase 1: Setup (Project Initialization)

**Purpose**: 创建项目基础结构，前后端项目初始化

- [X] T001 Create project root directory structure `ai-tool-square/` per plan.md
- [X] T002 Initialize Spring Boot backend with Gradle in `backend/` directory
- [X] T003 [P] Configure backend `build.gradle` with dependencies: Spring Boot 3.2+, Spring Security, Spring Data JPA, MySQL Connector, jjwt, commons-text, flexmark
- [X] T004 [P] Configure backend `application.yml` with database, JWT, and logging settings
- [X] T005 Initialize Vue 3 + TypeScript frontend with Vite in `frontend/` directory
- [X] T006 [P] Configure frontend `package.json` with dependencies: Vue 3.4+, Vite 5+, Pinia, Vue Router 4, Axios, Element Plus, markdown-it, highlight.js
- [X] T007 [P] Configure TypeScript `tsconfig.json` with strict mode enabled
- [X] T008 [P] Configure Vite `vite.config.ts` with proxy settings for API backend
- [X] T009 Create `.env` file in frontend with `VITE_API_BASE_URL=http://localhost:8082/api/v1`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 核心基础设施，必须在所有用户故事之前完成

**⚠️ CRITICAL**: Phase 2 未完成前，所有用户故事无法开始

### Backend Foundation

- [X] T010 [P] Create MySQL database `ai_tool_square` with utf8mb4 charset
- [X] T011 [P] Create JPA entities: User, Tool, Category in `backend/src/main/java/com/iaihub/toolbox/model/`
- [X] T012 [P] Create JPA repositories with Spring Data JPA in `backend/src/main/java/com/iaihub/toolbox/repository/`
- [X] T013 Create global exception handling in `backend/src/main/java/com/iaihub/toolbox/exception/`
- [X] T014 Create DTOs for API requests/responses in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T015 Create utility classes: XSS sanitization in `backend/src/main/java/com/iaihub/toolbox/util/`
- [X] T016 Create JWT utility class in `backend/src/main/java/com/iaihub/toolbox/util/JwtUtil.java`
- [X] T017 [P] Configure Spring Security with JWT filter, CORS, and public/private path rules in `backend/src/main/java/com/iaihub/toolbox/config/`
- [X] T018 Create REST API base structure with ApiResponse wrapper in `backend/src/main/java/com/iaihub/toolbox/config/`
- [X] T019 Pre-seed Category data (Skill, MCP, API, Prompt, 其他) on application startup

### Frontend Foundation

- [X] T020 [P] Create frontend project structure: assets/, components/, pages/, stores/, services/, router/, utils/
- [X] T021 [P] Create API service layer with Axios in `frontend/src/services/api.ts`
- [X] T022 [P] Create Pinia auth store in `frontend/src/stores/auth.ts` for JWT token management
- [X] T023 Create Vue Router configuration in `frontend/src/router/index.ts` with public and protected routes
- [X] T024 Create global styles and Element Plus theme setup in `frontend/src/assets/`
- [X] T025 Create App.vue with layout structure (header, main content, footer)

---

## Phase 3: User Story 1 - 浏览工具广场首页 (Priority: P1) 🎯 MVP

**Goal**: 访客可在首页浏览工具列表，支持分类筛选和关键词搜索

**Independent Test**: 清空浏览器缓存后直接访问首页，验证不登录情况下浏览工具列表、搜索和筛选功能

### Implementation for User Story 1

- [X] T026 [P] [US1] Create ToolSummaryDTO and PageResponse in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T027 [P] [US1] Implement ToolService with list/filter/search logic in `backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- [X] T028 [US1] Implement CategoryService in `backend/src/main/java/com/iaihub/toolbox/service/CategoryService.java`
- [X] T029 [US1] Implement GET `/api/v1/categories` endpoint in `backend/src/main/java/com/iaihub/toolbox/controller/CategoryController.java`
- [X] T030 [US1] Implement GET `/api/v1/tools` endpoint (public, with pagination/filter/search) in `backend/src/main/java/com/iaihub/toolbox/controller/ToolController.java`
- [X] T031 [P] [US1] Create ToolCard component in `frontend/src/components/ToolCard.vue`
- [X] T032 [P] [US1] Create CategoryFilter component in `frontend/src/components/CategoryFilter.vue`
- [X] T033 [US1] Create SearchBar component in `frontend/src/components/SearchBar.vue`
- [X] T034 [US1] Create HomePage in `frontend/src/pages/HomePage.vue` integrating ToolCard, CategoryFilter, SearchBar
- [X] T035 [US1] Add loading states and Toast notifications for API errors in frontend
- [X] T036 [US1] Add frontend routing for home page in `frontend/src/router/index.ts`

**Checkpoint**: User Story 1 完成 - 首页可浏览、筛选、搜索工具列表

---

## Phase 4: User Story 2 - 查看工具详情 (Priority: P1)

**Goal**: 用户点击工具卡片进入详情页，查看完整 Markdown 渲染内容和工具元信息

**Independent Test**: 点击任意工具卡片，能看到完整的 Markdown 渲染内容和上传者信息

### Implementation for User Story 2

- [X] T037 [P] [US2] Create ToolDetailDTO in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T038 [US2] Add getToolById method in ToolService with XSS sanitization for content
- [X] T039 [US2] Implement GET `/api/v1/tools/{id}` endpoint in ToolController
- [X] T040 [P] [US2] Create MarkdownRenderer component in `frontend/src/components/MarkdownRenderer.vue` using markdown-it + highlight.js
- [X] T041 [P] [US2] Create ToolDetailCard component in `frontend/src/components/ToolDetailCard.vue`
- [X] T042 [US2] Create DetailPage in `frontend/src/pages/DetailPage.vue` integrating MarkdownRenderer and ToolDetailCard
- [X] T043 [US2] Add route for detail page `/tools/:id` in `frontend/src/router/index.ts`
- [X] T044 [US2] Handle 404 case when tool not found with friendly error page

**Checkpoint**: User Story 2 完成 - 可查看工具详情和 Markdown 渲染内容

---

## Phase 5: User Story 3 - 上传新工具 (Priority: P1)

**Goal**: 登录用户可填写表单上传新工具，包含名称、分类、Markdown 介绍

**Independent Test**: 登录后填写表单提交，可在列表和详情页看到新上传的工具

### Implementation for User Story 3

- [X] T045 [P] [US3] Create CreateToolRequest DTO in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T046 [US3] Add createTool method in ToolService with validation and duplicate check
- [X] T047 [US3] Implement POST `/api/v1/tools` endpoint (protected) in ToolController
- [X] T048 [P] [US3] Create ToolForm component in `frontend/src/components/ToolForm.vue` with validation
- [X] T049 [P] [US3] Create MarkdownEditor component in `frontend/src/components/MarkdownEditor.vue`
- [X] T050 [US3] Create UploadPage in `frontend/src/pages/UploadPage.vue` integrating ToolForm
- [X] T051 [US3] Add route `/tools/upload` (protected) in `frontend/src/router/index.ts`
- [X] T052 [US3] Add auth guard to redirect unauthenticated users to login
- [X] T053 [US3] Show success toast and redirect to home after upload success

**Checkpoint**: User Story 3 完成 - 登录用户可上传工具

---

## Phase 6: User Story 4 - 用户认证与登录 (Priority: P2)

**Goal**: 支持邮箱密码注册和登录，区分登录用户和访客权限

**Independent Test**: 访客无法访问上传表单但可浏览；注册登录后可访问上传功能

### Implementation for User Story 4

- [X] T054 [P] [US4] Create RegisterRequest, LoginRequest, LoginResponse DTOs in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T055 [P] [US4] Create RefreshResponse DTO in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T056 [US4] Implement UserService with register, login, password hashing (BCrypt)
- [X] T057 [US4] Implement AuthService with JWT token generation and validation
- [X] T058 [US4] Implement POST `/api/v1/auth/register` endpoint in AuthController
- [X] T059 [US4] Implement POST `/api/v1/auth/login` endpoint in AuthController
- [X] T060 [US4] Implement POST `/api/v1/auth/refresh` endpoint in AuthController
- [X] T061 [US4] Store refresh token and implement token revocation logic
- [X] T062 [P] [US4] Create LoginPage in `frontend/src/pages/LoginPage.vue`
- [X] T063 [P] [US4] Create RegisterPage in `frontend/src/pages/RegisterPage.vue`
- [X] T064 [US4] Add routes `/login` and `/register` in frontend router
- [X] T065 [US4] Update auth store to handle login/register/refresh/logout actions
- [X] T066 [US4] Add JWT access token to API service Authorization header
- [X] T067 [US4] Handle 401 responses with token refresh or redirect to login

**Checkpoint**: User Story 4 完成 - 支持注册登录，权限控制正常

---

## Phase 7: User Story 5 - 管理我上传的工具 (Priority: P3)

**Goal**: 登录用户可在个人中心查看、编辑、删除自己上传的工具

**Independent Test**: 登录后访问个人中心，可看到自己上传的工具列表并进行编辑删除

### Implementation for User Story 5

- [X] T068 [P] [US5] Create UpdateToolRequest DTO in `backend/src/main/java/com/iaihub/toolbox/dto/`
- [X] T069 [US5] Add updateTool and deleteTool methods in ToolService with ownership validation
- [X] T070 [US5] Implement PUT `/api/v1/tools/{id}` endpoint in ToolController
- [X] T071 [US5] Implement DELETE `/api/v1/tools/{id}` endpoint (soft delete) in ToolController
- [X] T072 [US5] Implement GET `/api/v1/users/me/tools` endpoint in ToolController
- [X] T073 [US5] Implement GET `/api/v1/users/me` endpoint in UserController
- [X] T074 [P] [US5] Create MyToolsPage in `frontend/src/pages/MyToolsPage.vue` with tool list
- [X] T075 [P] [US5] Create EditToolPage in `frontend/src/pages/EditToolPage.vue` with pre-filled form
- [X] T076 [US5] Create ConfirmDialog component in `frontend/src/components/ConfirmDialog.vue`
- [X] T077 [US5] Add routes `/me/tools` and `/me/tools/:id/edit` in frontend router
- [X] T078 [US5] Add "我的工具" link in header navigation for logged-in users
- [X] T079 [US5] Show confirm dialog before delete and refresh list after success

**Checkpoint**: User Story 5 完成 - 用户可管理自己的工具

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 全局改进和收尾工作

- [X] T080 [P] Add XSS sanitization validation on all user inputs in backend
- [X] T081 [P] Add input validation (username, password, tool name) with proper error messages
- [X] T082 Add 404 error page in `frontend/src/pages/NotFoundPage.vue`
- [X] T083 Add loading skeletons during data fetch in frontend components
- [X] T084 Add responsive design for mobile/tablet in frontend
- [X] T085 Add keyboard navigation support and accessibility attributes
- [X] T086 Optimize images and add lazy loading for tool cards
- [X] T087 Verify SC-001 to SC-007 performance criteria
- [X] T088 Run quickstart.md verification steps
- [X] T089 Update README.md with setup instructions

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 无依赖，可立即开始
- **Phase 2 (Foundational)**: 依赖 Phase 1 完成 — **阻塞所有用户故事**
- **Phase 3-7 (User Stories)**: 全部依赖 Phase 2 完成
- **Phase 8 (Polish)**: 依赖所有用户故事完成

### User Story Dependencies

| 用户故事 | 依赖 | 可独立测试 |
|---------|------|-----------|
| US1 (首页浏览) | Phase 2 | ✅ 是 |
| US2 (详情页) | Phase 2 | ✅ 是 |
| US3 (上传工具) | Phase 2 | ✅ 是 |
| US4 (认证登录) | Phase 2 | ✅ 是 |
| US5 (管理工具) | Phase 2 | ✅ 是 |

---

## Parallel Opportunities

### Phase 1 并行任务
- T003 + T004 (backend build config)
- T006 + T007 + T008 (frontend config)
- T009 可与上述并行

### Phase 2 并行任务
- T010 + T011 + T012 (database + entities + repositories)
- T014 + T015 (DTOs + util)
- T016 可与上述部分并行
- T017 + T018 (Security + REST base)
- T020 + T021 + T022 (frontend structure + API + auth store)
- T023 + T024 + T025 (router + styles + App)

### Within User Stories 并行任务
- US1: T026 + T027 可并行; T031 + T032 + T033 可并行
- US2: T037 + T038 可并行; T040 + T041 可并行
- US4: T054 + T055 可并行; T062 + T063 可并行
- US5: T068 + T069 可并行; T074 + T075 + T076 可并行

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. ✅ 完成 Phase 1: Setup
2. ✅ 完成 Phase 2: Foundational (CRITICAL - 阻塞所有故事)
3. ➡️ 完成 Phase 3: User Story 1
4. **停止并验证**: 独立测试用户故事 1
5. 部署/演示 (如果就绪)

### Incremental Delivery

1. Phase 1 + Phase 2 → 基础就绪
2. Phase 3 (US1) → 测试 → 部署/演示 (**MVP!**)
3. Phase 4 (US2) → 测试 → 部署/演示
4. Phase 5 (US3) → 测试 → 部署/演示
5. Phase 6 (US4) → 测试 → 部署/演示
6. Phase 7 (US5) → 测试 → 部署/演示
7. Phase 8 (Polish) → 最终发布

---

## Summary

- **Total Tasks**: 89
- **User Stories**: 5
  - US1: 浏览工具广场首页 (P1) 🎯 MVP
  - US2: 查看工具详情 (P1)
  - US3: 上传新工具 (P1)
  - US4: 用户认证与登录 (P2)
  - US5: 管理我上传的工具 (P3)
- **Parallel Opportunities**: 18+ 可并行任务组
- **MVP Scope**: US1 (Phase 1 + Phase 2 + Phase 3)
- **Independent Test Criteria**: 每个用户故事可独立测试
