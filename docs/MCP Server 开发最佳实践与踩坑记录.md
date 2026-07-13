# MCP Server 开发最佳实践与踩坑记录

https://modelcontextprotocol.info/zh-cn/docs/best-practices/

## 一、架构设计原则

### 1.1 单一职责边界

每个 MCP Server 应围绕一个明确的能力域构建，不要做"万能 Server"。当功能膨胀时，考虑拆分成多个 Server。比如 CodeWiki 专注于代码仓库分析和 Wiki 生成，CodingHub 专注于工具广场的搜索/安装/发布，各司其职。

### 1.2 模块化分离

工具（Tools）、资源（Resources）、提示（Prompts）是 MCP 协议的三大原语，保持它们之间的解耦。Resources 应视为只读或最小可变的上下文，不要在 Resources 里实现业务逻辑。

### 1.3 Server Instructions 不可少

通过 `instructions` 字段给 LLM 写一份"使用手册"，说明 Server 能做什么、什么场景用哪个工具、有哪些约束。这直接影响 Agent 调用效果。

### 1.4 语义化版本控制

对破坏性变更严格遵循 SemVer，因为下游客户端（Host 应用）会依赖你的工具签名。

## 二、工具设计

### 2.1 基本要求

每个工具必须有唯一的 name、清晰的 description 和 inputSchema。description 不是给人看的文档，而是给 LLM 看的"调用指南"——应包含：做什么、什么场景用、输入含义、返回什么。

### 2.2 无状态 & 幂等

工具调用应接受客户端生成的请求 ID，对相同输入返回确定性结果。如果需要维护状态（如 CodeWiki 的 session），要有明确的生命周期管理（如 `close_session`）。

### 2.3 双重可读的输出

响应既要让 LLM 能解析（结构化 JSON），也要让人类能阅读（文本内容块）。如果定义了 outputSchema，必须提供符合 schema 的结构化结果，同时为了向后兼容在 text content block 里放序列化 JSON。

### 2.4 人在环路（Human-in-the-loop）

高风险操作（写文件、修改数据）应提供确认机制或试运行模式。MCP 规范明确要求 "there SHOULD always be a human in the loop"。

### 2.5 工具数量控制

工具太多会让 LLM 选择困难，描述占用的 token 也会膨胀。社区经验建议单个 Server 不超过 15-20 个工具。

## 三、错误处理

MCP 协议把错误分成两层：

* **协议层错误**：用标准 JSON-RPC 错误码，如工具不存在（MethodNotFound）、参数不合法（InvalidParams）
* **运行时错误**：在工具返回结果中设置 `isError: true`，并在 content 里给出详细错误信息

错误消息要做到：机器可读的状态码 + 人类可读的解释 + 告诉 Agent 下一步该怎么办。

## 四、安全与凭证

* **传输安全**：HTTP 传输强制 OAuth 2.1 认证，生成不可预测的会话标识符
* **凭证保护**：永远不要在工具返回结果或日志中暴露 API Key、Token
* **输入验证**：MCP 规范强制要求 validate all tool inputs、implement access controls、rate limit invocations、sanitize outputs
* **最小权限原则**：Server 只请求完成功能所必需的最小权限

## 五、性能与传输

### 5.1 传输协议选择

* **stdio**：适合本地进程模式（桌面客户端场景）
* **Streamable HTTP**：生产环境推荐，支持双向通信（旧版 SSE 已 deprecated）
* 生产环境不要用 SSE，存在连接泄漏风险（见踩坑记录）

### 5.2 大数据处理

不要在单个工具调用结果里内联 MB 级别数据，应返回资源 URI 让客户端按需拉取，或使用 file-side-channel 模式（见踩坑记录）。

### 5.3 性能优化

* 异步处理 + 超时/取消机制
* 连接池减少建连开销
* 高频数据引入缓存
* Server 启动阶段做预热（warmup），避免首次调用触发客户端超时

## 六、测试与可观测性

* **结构化日志**：记录请求延迟、状态、token 消耗，含 trace ID 便于端到端排查
* **故障注入**：注入下游延迟、部分失败、格式错误输入，验证容错能力
* **MCP Inspector**：官方调试工具，开发阶段用它测试工具输入输出

## 七、容易忽略的细节

* **工具变更通知**：运行期间工具变化时 SHOULD 发送 `notifications/tools/list_changed`
* **跨平台兼容**：路径分隔符、编码等问题要统一处理（如 Windows 反斜杠 vs 正斜杠）
* **优雅降级**：针对不同 Host 客户端能力差异，回退到基础格式

***

## 八、实战踩坑记录

### 坑 1：SSE 传输模式连接泄漏（CodingHub 项目）

**问题**：CodingHub（ai-tool-square）开发初期使用 SSE（Server-Sent Events）作为 MCP 传输模式。运行一段时间后发现 SSE 连接存在泄漏问题，导致 Tomcat 连接数被占满，服务完全无法响应新请求。

**原因**：SSE 是长连接协议，客户端断开时服务端未必能及时感知并释放连接资源。在高并发或客户端异常断连场景下，僵尸连接不断累积，最终耗尽 Tomcat 的连接池。

**解决方案**：从 SSE 迁移到 Streamable HTTP 传输模式。Streamable HTTP 支持双向通信，连接管理更可控，且已被 MCP 官方规范标记为 SSE 的替代方案。

**教训**：生产环境的 MCP Server 不要用 SSE 传输模式。SSE 在 MCP 规范中已被标记为 deprecated，官方推荐使用 Streamable HTTP。

### 坑 2：MCP 通道传输大量数据（CodeWiki-CN 项目）

**问题**：CodeWiki-CN 开发初期通过 MCP 工具调用结果直接传递大量数据（如完整的 AST 解析结果、代码组件树、依赖关系图等），导致三个严重问题：

1. LLM 上下文窗口被撑满（context overflow）
2. MCP 响应超时（timeout）
3. 整体处理速度极慢

**原因**：MCP 工具调用的结果会被注入到 LLM 的上下文窗口中。当单次返回数据量过大（如数 MB 的 JSON）时，一方面占用大量 token 导致上下文窗口不够用，另一方面序列化和传输本身就非常耗时，容易触发客户端超时。

**解决方案**：采用 file-side-channel（文件侧通道）模式——将大数据写入临时文件，MCP 工具只返回文件路径或摘要信息，Agent 按需通过其他工具读取文件内容。这样 MCP 通道只传递轻量元数据，大数据走旁路。

**教训**：MCP 是"控制通道"而非"数据通道"。大体积数据应走 file-side-channel 或返回资源 URI 让客户端按需拉取，不要把 MCP 当数据管道用。

### 坑 3：Streamable HTTP 无法直接传输二进制文件（CodingHub 项目）

**问题**：CodingHub 的 MCP Server 使用 Streamable HTTP 传输模式后，发现无法直接通过 MCP 工具调用的结果传递二进制文件（如 zip 包、PDF、Word 文档等）。MCP 协议原生只支持 `TextContent` 和 `ImageContent`，没有二进制 blob 类型，服务端也无法在工具响应中内联文件内容。

**原因**：MCP 协议定位为"控制通道"（control channel），其消息交换基于 JSON-RPC，所有 content 类型仅限于文本和图片（base64）。二进制文件传输超出了 MCP 协议的设计范围。

**解决方案**：将文件上传/下载与 MCP 解耦，采用"**MCP 返回端点信息，HTTP 执行实际传输**"的分离模式：

```
MCP 通道（控制层）:
  h3_coding_hub_tool_file_upload(toolId)
    → 返回 { uploadUrl, httpMethod, contentType, formFields, limits }
  
  h3_coding_hub_tool_download(toolId, fileId)
    → 返回 { downloadUrl, fileName, fileSize }（相对路径）
  
  h3_coding_hub_tool_files(toolId)
    → 返回文件列表（含下载路径）

HTTP 通道（数据层）:
  上传: curl -X POST {baseUrl}/api/v1/tools/{toolId}/files -F "files=@file.zip"
  下载: curl -O {baseUrl}/api/v1/tools/{toolId}/files/{fileId}/download
```

**关键实现细节**：
- `h3_coding_hub_tool_file_upload` 不执行上传，只返回 REST API 的接口描述（含完整的 curl 示例）
- `h3_coding_hub_tool_upload` description 中详细说明了 MCP 不可做文件上传的原因和替代方案
- 文件上传端点 `POST /api/v1/tools/{toolId}/files` 已 permitAll（无需 JWT），Agent 可直接调用
- 对知识库文档也是同样模式：`h3_coding_hub_kb_upload_document` 返回 RAG 服务的批量上传端点

**教训**：**MCP 是控制通道，不是数据通道**。不要试图让 MCP 承载二进制传输，而是让 MCP 告诉 Agent"数据去哪里、怎么传"，实际传输走 REST/HTTP 旁路。这个模式在工具文件上传和知识库文档上传两个场景中都验证了可行性。

***

## 九、MCP 与 SKILL 能力对等：三大原语补齐

CodingHub 的定位是**双通道架构**：MCP（优先）和 SKILL（HTTP 降级）应能完成同样的事情。初始阶段 MCP 只有工具（Tools），缺少 Prompt 和 Resource。通过以下三轮补齐，MCP 实现了与 SKILL 功能对等。

### 9.1 提示词（Prompts）— 工作流模板补齐

**背景**：SKILL 的 `SKILL.md` 中定义了 6 个核心工作流（搜索工具、安装工具、版本检查、发布工具、更新工具、论坛发帖），每个工作流包含详细的步骤说明。MCP 初始阶段没有这些工作流指引，Agent 只能自行理解工具签名来编排。

**优化**：创建 `McpPromptProvider`，注册 6 个 Prompt 模板，每个 Prompt 对应 SKILL.md 中的一个工作流：

| Prompt 名 | 对应 SKILL.md 章节 | 参数 | 返回的消息 |
|-----------|-------------------|------|-----------|
| `search-tools` | 1. 搜索与安装工具 → 搜索 | `query`（可选） | 指导调用 `tool_search` → 表格展示 |
| `install-tool` | 1. 搜索与安装工具 → 安装 | `toolName`（必填） | 6 步完整安装流程 |
| `check-versions` | 1. tools.version 规则 | 无参数 | 扫描 → 对比 → 报告 |
| `publish-tool` | 2. 发布新工具 | `skillName`（必填） | 10 步完整发布流程 |
| `update-tool` | 3. 更新已有工具 | `skillName`、`version`（可选） | 9 步完整更新流程 |
| `forum-post` | 4. 论坛发帖 | `filePath`、`title`（可选） | 5 步完整发帖流程 |

**Prompt 设计要点**：
- 每个 Prompt 返回 `USER` 角色消息，**不是**替 Agent 执行，而是告诉它"应该怎么做"
- 消息中包含具体工具名称（如 `h3_coding_hub_tool_search`）、参数映射、注意事项（如"下载链接是相对路径需拼接 URL"）
- 参数传递验证+兜底：`toolName` 为必填、`query` 为可选并自动生成合理搜索条件
- 参考了 SKILL.md 中同样的双通道流程图逻辑，确保 Prompt 和 SKILL 的工作流一致

**效果**：Agent 调用 `install-tool` Prompt 后，自动获得与 SKILL 完全相同的安装步骤指导，无需事先熟悉 CodingHub 的 API 结构。

### 9.2 资源（Resources）— 上下文数据补齐

**背景**：SKILL 可以通过 `config.json` 和工具目录结构获取上下文（如已安装的工具列表、版本号）。MCP 初始阶段没有类似的"查询上下文"机制，Agent 每次都需要先调用 `tool_search` 才能知道广场上有什么工具。

**优化**：创建 `McpResourceHandler`，注册 3 个 MCP Resource：

| 资源 URI | 类型 | MCP 特性 | 用途 |
|----------|------|---------|------|
| `codinghub://tools/catalog` | 静态资源 | 支持订阅通知 | 工具广场全量目录（最多 200 条） |
| `codinghub://tools/recent` | 静态资源 | 支持订阅通知 | 最近更新的工具（前 20 条） |
| `codinghub://tool/{id}` | **Resource Template** | 参数化 URI | 单个工具详情 |

**Resource Template 的关键作用**：`codinghub://tool/{id}` 是 MCP 协议的 Resource Template 模式——Agent 构造 URI 时填入工具 ID，Server 动态解析并返回对应数据。这为 Agent 提供了一种"无需调用工具即可获取数据"的轻量方式，特别适合 Agent 想快速预览某个工具详情的场景。

**订阅通知机制**：`McpNotificationService` 在工具发生新增/更新/删除时，自动推送 `notifications/resources/list_changed` 和 `notifications/resources/updated` 通知，让 Agent 的上下文保持新鲜。

### 9.3 工具的 description 优化（关键策略）

**背景**：MCP 工具如果不写好的 description，LLM 无法理解该在什么场景下调用。这与 SKILL 中 `SKILL.md` 的 description 字段面临同样的问题。

**优化**：借鉴 Anthropic "为模型而非人类编写"的原则，为每个工具的 description 字段注入**触发条件 + 行为指引 + 注意事项**：

```java
// 优化前：泛泛而谈
"获取工具详情"

// 优化后：包含场景、返回内容、后续步骤提示
"获取工具详情，包括完整的 markdown 文档。
返回的 data.version 可作为版本号写入本地工具目录下的 tools.version 文件"
```

```java
// 优化前：没有说明文件上传的限制
"上传文件到指定工具"

// 优化后：明确告知 MCP 不可传二进制，说明 REST 替代方案
"上传文件到指定工具。本工具告知客户端文件上传的 REST API 接口信息。
客户端应使用 HTTP Multipart POST 请求直接上传文件，无需认证。
REST API 详情：
- URL: POST {mcp_server_base_url}/api/v1/tools/{toolId}/files
- Content-Type: multipart/form-data
...（包含完整的接口说明、字段列表、限制、使用步骤）"
```

```java
// 对知识库文档上传同样处理
"MCP 协议不直接支持二进制文件传输。要上传文件到知识库，请使用 REST API。
本工具返回完整的 RAG 服务批量上传端点 URL（绝对地址，可直接使用）、
支持的文件类型和 curl 示例。
上传 URL 从配置文件实时读取 RAG 服务地址构造，无需手动拼接。"
```

**核心技巧**：description 中不仅写"这个工具做什么"，还写"为什么不能做"以及"替代方案是什么"，让 LLM 理解上下文并自动降级。


### 9.4 MCP ⇒ SKILL 能力对等清单

| 能力域 | SKILL（HTTP CLI） | MCP（优先通道） | 对等方案 |
|--------|------------------|----------------|---------|
| **搜索工具** | `$CHUB tool-search` | `h3_coding_hub_tool_search` | 直接等价 |
| **获取详情** | `$CHUB tool-get` | `h3_coding_hub_tool_get` | 直接等价 |
| **文件列表** | `$CHUB tool-files` | `h3_coding_hub_tool_files` | 直接等价 |
| **文件下载** | `$CHUB tool-download` | `h3_coding_hub_tool_download` | 直接等价（返回相对路径） |
| **创建工具** | `$CHUB tool-create` | `h3_coding_hub_tool_create` | 直接等价 |
| **修改工具** | `$CHUB tool-modify` | `h3_coding_hub_tool_modify` | 直接等价 |
| **文件上传** | `$CHUB tool-file-upload` | `h3_coding_hub_tool_file_upload` | **MCP 返回端点，HTTP 执行** |
| **文件删除** | `$CHUB tool-file-delete` | `h3_coding_hub_tool_file_delete` | 直接等价 |
| **搜索帖子** | `$CHUB post-search` | `h3_coding_hub_post_search` | 直接等价 |
| **获取帖子** | `$CHUB post-get` | `h3_coding_hub_post_get` | 直接等价 |
| **创建帖子** | `$CHUB post-create` | `h3_coding_hub_post_create` | 直接等价 |
| **知识库 CRUD** | `$CHUB kb-*` | `h3_coding_hub_kb_*` | 直接等价 |
| **知识库搜索** | `$CHUB kb-search` | `h3_coding_hub_kb_search` | 直接等价 |
| **文档上传** | `$CHUB tool-file-upload`（工具） / REST（知识库） | `h3_coding_hub_kb_upload_document` | **MCP 返回端点，HTTP 执行** |
| **文档状态** | REST | `h3_coding_hub_kb_document_status` | MCP 提供，SKILL 没有 |
| **工作流指引** | SKILL.md 逐章节描述 | 6 个 Prompt 模板 | **等效果，形式不同** |
| **上下文数据** | config.json + 文件系统 | 3 个 Resource（含订阅通知） | **Resource 更强大** |

**总结**：MCP 通过 **18 个工具 + 6 个 Prompt + 3 个 Resource** 的三元组补齐，实现了与 SKILL 完全对等的能力。关键设计哲学：

1. **MCP 做 Prompt 的事，而不是重新发明 SKILL.md** — 6 个 Prompt 模板直接复刻 SKILL.md 的工作流描述，Agent 调用 Prompt 拿到与 SKILL 一致的指导
2. **Resource 做上下文的事，而不是让 Agent 反复调用工具** — 3 个 Resource 让 Agent 无需调工具即可获取目录/详情，订阅通知保持上下文新鲜
3. **description 做兜底说明的事** — 对文件上传等 MCP 无法直接处理的场景，description 中详细说明 REST 替代方案，确保 Agent 不会卡住

---

## 十、核心哲学总结

把 MCP Server 当作一个给 LLM 用的 API 来设计：清晰的契约（Schema）、明确的边界（单一职责）、防御性编程（输入验证 + 错误处理）、以及对调用者（LLM Agent）友好的接口设计（好的 description + instructions）。

MCP 是控制通道，不是数据通道；SSE 已死，Streamable HTTP 当立。

**补充**：MCP 三大原语（Tools / Prompts / Resources）缺一不可。只提供 Tools 时 MCP 只能做"远程函数调用"，补齐 Prompts 和 Resources 后 MCP 才能与 SKILL 达到同等体验——Prompts 替代 SKILL.md 的工作流指引，Resources 替代 SKILL 的文件系统上下文。

