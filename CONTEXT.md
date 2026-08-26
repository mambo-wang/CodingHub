# CodingHub Context

CodingHub 是 AI 工具市场平台：既有面向 AI 助手的工具广场，即将新增面向 CodeBuddy 的插件市场。本文件收录平台领域术语。

## 插件市场（Plugin Marketplace）

**插件（Plugin）**:
一个遵循 CodeBuddy 插件规范的软件包，其根目录含 `.codebuddy-plugin/plugin.json` 清单，可携带 `.mcp.json`、`skills/`、`agents/`、`hooks/` 等组件。
_Avoid_: 工具、扩展、附件

**插件市场（Plugin Marketplace）**:
CodingHub 提供的开放市场，用户可自行创建并发布 CodeBuddy 插件；后端动态生成 CodeBuddy 可消费的 `marketplace.json`。
_Avoid_: 插件商店、插件广场

**上传托管（Upload-hosted）**:
插件交付形态：用户上传插件源码 zip。服务端临时解压做 B 档校验并生成插件结构摘要（components），校验后删除临时目录；持久存储仅保留 zip 原件（作为 CodeBuddy HTTP source 的下载源）与库中的结构摘要（供详情页展示）。
_Avoid_: GitHub 托管、Git source、保留解压目录

**结构摘要（Components）**:
上传校验时从插件 zip 解析出的组件清单（skills 列表、MCP 服务器名、agents、hooks 数、bin 可执行文件等），存库供插件详情页展示，是"详情页展示插件包含啥"的数据源。
_Avoid_: 实时读文件系统

**marketplace.json**:
CodeBuddy 插件市场清单文件，按 CodeBuddy 官方 schema 动态生成（`name` / `owner` / `plugins[]`），是 CodeBuddy `/plugin marketplace add` 的消费对象。每次请求从插件表实时聚合，不缓存。

**市场公开性（Public Marketplace）**:
市场消费端（marketplace.json 与 zip 下载）匿名公开——CodeBuddy CLI 的 marketplace add 不带认证参数，是机制约束；鉴权仅在上传/管理侧。插件 `.mcp.json` 指向的 MCP 端点鉴权由插件作者自行处理。
_Avoid_: 私有市场、带鉴权的下载端点

**覆盖式更新（Overwrite Update）**:
插件版本更新方式：重新上传 zip 覆盖 `source.zip`。唯一版本源是 zip 内 `plugin.json.version`，上传时解析入库、marketplace.json 引用同一值；version 未变化则拒绝覆盖。不做多版本管理。
_Avoid_: 版本历史、回滚、CodingHub 自管 version

**独立插件市场页（Plugin Market Page）**:
插件市场的前端形态：独立的 `/plugin-market` 列表/详情/上传页面，复用工具广场通用组件；不与工具广场混排。

**历史分类清理（Category Cleanup）**:
删除工具广场中由 "API" 展示替换而来的"插件"分类（Category ID=3），其下既有工具迁移到"其他"分类；后端 `toDTO` 替换逻辑与前端"插件" logo 映射一并移除。

**统一互动接入（Unified Interaction）**:
插件复用平台统一互动基础设施（点赞/评论/收藏）：`TargetType` 枚举新增 `PLUGIN` 值，三个 Service（Like/Comment/Favorite）补 switch 分支，Plugin 实体带 likeCount/commentCount/viewCount/score 字段；接评分联动（`score = viewCount×1 + likeCount×3 + commentCount×5`，对齐工具广场），支持匿名点赞（IP hash），点赞通知自动打通。
_Avoid_: 新建独立互动表、独立互动接口

## 工具广场（Tool Market）

**工具广场（Tool Market）**:
CodingHub 现有模块：AI 工具包的发布、浏览、评分与互动。用户口语称"工具广场"，正式文档与代码中称"工具市场"。
_Avoid_: 插件市场（二者是不同的领域概念）

**工具（Tool）**:
工具广场中的一件 AI 工具包，含名称、分类、README、附件文件，带评分/点赞/评论等互动数据。
_Avoid_: 插件
