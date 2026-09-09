### 2026-09-09 12:42

## 2026-09-09 论坛 md/html 支持 — grill 决策树全部收敛（10 问）

需求：论坛支持导入/编辑 Markdown 与 HTML 格式，MCP 同步支持。

已定决策：
1. **存储**：`forum_post` 新增 `contentFormat` 枚举列（MARKDOWN|HTML），默认 MARKDOWN，存量数据按 MARKDOWN 迁移 → 是「存储格式」，不是「来源格式」
2. **编辑**：源码编辑 + 预览，MD/HTML 共用现有 textarea，HTML 原帖无损保存
3. **安全**：**彻底不做消毒**，任何人可发 HTML 原文。已知代价：refreshToken 存 localStorage → 存储型 XSS = 全站账号接管；且 markdown-it 的 `html:true` 使该洞**现在就已存在**。ADR 候选
4. **导入通道**：服务端 multipart 导入接口 `POST /api/forum/posts/import`，服务端解析文件直接建帖
5. **MCP 导入**：新增 `h3_coding_hub_post_import`，沿用 `h3_coding_hub_tool_file_upload` 惯例——只回传 REST multipart 接口信息，由客户端自行 HTTP 上传
6. **MCP 工具集**：全套——`post_create` 加 contentFormat、`post_get` 返回 contentFormat、新增 `post_import`、新增 `post_update`、新增 `post_list`
7. **列表摘要**：前端按 contentFormat 分支，HTML 帖用 DOMParser 取 textContent；MD 帖沿用 PostCard 现有正则（后端不生成 excerpt）
8. **导入入参**：title 自动推导（HTML 取 <title>/<h1>，MD 取首个 #，退化用文件名）+ 表单可覆盖；categoryId/visibility 同理
9. **post_list 粒度**：只返回元数据（id/title/分类/统计），不含正文，看正文走 `post_get`
10. **术语**：保留「导入」，与知识库「上传文档」按**留存性**区分——导入=解析后即丢弃文件，上传=文件落盘留存

实现假设（未逐条确认）：扩展名白名单 .md/.markdown/.html/.htm，UTF-8，10MB 上限，contentFormat 参数可覆盖扩展名推断。

待办：写 CONTEXT.md 论坛章节 + docs/adr 不消毒 ADR（待用户确认）。

### 2026-09-09 12:44

## 2026-09-09 文档已落盘

- `CONTEXT.md` 新增「论坛（Forum）」章节：帖子 / 正文格式 / 导入 / 上传 四条术语（导入与上传按**留存性**区分）
- `docs/adr/0006-forum-content-no-sanitization.md`：记录「彻底不消毒」决策、四条被否决的替代方案、以及「现有 XssSanitizer 是 escapeHtml4，不可用于论坛正文」这一坑

下一步可开工：Flyway V10 加 contentFormat 列 → ForumPost 实体/DTO/Service → 导入接口 → MCP 五工具 → 前端编辑器+预览+PostCard 摘要。

### 2026-09-09 12:58

## 2026-09-09 第一轮实现完成

已改文件：
- `V14__add_forum_post_content_format.sql` — forum_post 加 content_format（默认 MARKDOWN）
- `model/forum/ContentFormat.java`（新增枚举）· `ForumPost` 加字段 · `ForumPostCreateRequest`/`ForumPostDTO` 加 contentFormat
- `ForumPostService`：parseFormat / importPost（含 title 推导、扩展名判定、UTF-8 读取）+ 抽出 findAccessiblePost（新增 getPostForEdit，避免 MCP 更新时误增浏览量）
- `ForumPostController`：POST /api/forum/posts/import（multipart）
- MCP：`post_create` 加 contentFormat、`post_get` 返回 contentFormat、新增 `post_list`/`post_update`/`post_import`；工具数 23→24→**27**（原注释 23 本就是过期的）
- 前端：types/services 加字段与 importPost；PostContent 支持 HTML 直通；PostCard 摘要按格式分支（DOMParser）；编辑器加格式切换+预览+导入按钮（导入仅新帖页，编辑页隐藏，避免“另发一篇”误解）

验证：后端 gradlew compileJava 通过；前端 vue-tsc --noEmit 通过。**未做**：迁移实际执行、端到端冒烟（需起服务）。

下一步建议：跑 `make db` 应用 V14 后，手工冒烟 创建 MD/HTML 帖 → 导入 .md/.html → MCP post_list/post_update/post_import。

### 2026-09-09 13:09

## 2026-09-09 端到端测试完成（重启后端 + 实测）

**环境**：MySQL（3306 在监听，5432 未监听，默认 profile=mysql）；ddl-auto=update 自动建列，Flyway 日志未出现。后端 8082 / 前端 5173 已重启并确认可访问。

**REST 全绿**：登录→建 HTML 帖(HTML)/MD 帖(MARKDOWN)/不带格式帖(默认 MARKDOWN)→读取→更新，均符合预期；导入 .md/.html（标题自动推导、title/contentFormat 显式覆盖、中文标题）全通过；非法扩展名 400、未认证 401。

**MCP 全绿**（Streamable HTTP /mcp，26 个工具，6 个帖子工具）：post_list（仅元数据+contentFormat）、post_get、post_import（只回传接口信息）、post_create（建出 HTML 帖 id=33）、post_update（未传字段保持原值）。

**E2E 揪出的两个真问题（已修）**
1. **UTF-8 BOM 未剥离**（真 bug）：带 BOM 的 .md 首行 `# 标题` 因 `^` 锚在行首而匹配失败，标题退化为文件名，且 BOM 污染正文。已在 `readAsUtf8` 剥离 `\uFEFF`。Windows 记事本存的 md 普遍带 BOM，必踩。
2. MCP 工具数注释被我改错：原注释“23 个工具”是**正确的**（23+新增3=26），我上一轮误改成 27，已回滚为 26。

**测试工具本身的坑**：Windows `curl.exe -F` 按控制台代码页发 GBK，中文表单字段会乱码（服务端按 UTF-8 解析，所以是工具问题）。改用 .NET HttpClient 显式 UTF-8 后正常。

**遗留**：测试帖 id 24~33 留在库里未清理（含 HTML 帖可供肉眼看渲染效果），等用户确认后清理。

### 2026-09-09 13:34

## 2026-09-09 收尾：清理 + 固化脚本

- 测试帖 id 24~33 已全部软删（204）
- 新增 `scripts/e2e-forum.ps1`：一键「杀进程 → gradlew bootRun → 等就绪 → REST 全链路 → MCP 全链路 → 自动清理测试帖 → PASS/FAIL 汇总」，退出码 0/1。支持 `-SkipRestart` / `-KeepData` / `-BaseUrl` / `-RepoRoot`。**完整跑一次 27/27 全绿**（含重启）。

脚本里踩到并已修的 3 个坑（都已写进脚本注释）：
1. Spring 打印 `Started ... in` 后 DispatcherServlet 还要 ~15s 才就绪，只看日志会撞连接失败 → 必须轮询真实 HTTP 端点做就绪探针
2. 脚本含中文必须存 **UTF-8 with BOM**，否则 PowerShell 5.1 按 GBK 解析直接 ParserError
3. .NET `StartsWith('\uFEFF')` 默认走文化敏感比较，U+FEFF 是零权重字符会被忽略，对任意字符串都返回 true → 判 BOM 必须用 `StringComparison.Ordinal`（与服务端那个 BOM bug 同根）
4. Windows `curl.exe -F` 发 GBK，中文表单字段乱码 → 脚本统一用 .NET HttpClient 显式 UTF-8

状态：功能完成 + E2E 全绿；未提交 git（等用户确认）。
