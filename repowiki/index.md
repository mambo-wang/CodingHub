# 项目文档索引

> 自动生成于 2026-07-05T18:05:19+08:00 | 本文件由系统自动维护

## 模块文档

| 文档 | 说明 |
|------|------|
| [overview](overview.md) | CodingHub（ai-tool-square）是一个面向 AI 工具分享与技术交流的综合性平台，由 Java 后端、Vue 前端和 Python RAG 知识库引擎三大子系统协同构成，提供 AI 工具管理、社区论坛、微课视频、智能知识库 |
| [RAG服务](RAG服务.md) | RAG（Retrieval-Augmented Generation）服务是 CodingHub 的智能知识库引擎，以 Python 独立实现，为平台提供文档导入、智能分块、向量存储与语义检索能力。该服务采用双协议架构——同时作为 MCP  |
| [RAG服务_RAG核心](RAG服务_RAG核心.md) | RAG（Retrieval-Augmented Generation）服务是 CodingHub 的知识库核心模块，提供文档导入、智能分块、向量存储与语义检索能力。该模块以 Python 实现，同时作为 MCP 服务器和 REST API  |
| [前端](前端.md) | CodingHub 前端基于 **Vue 3.4 / TypeScript 5.4 / Vite 5.2** 构建，采用组件化开发模式，为平台提供工具展示、论坛交流、微课视频、知识库管理、留言反馈及概览统计等完整功能。前端通过 Axios  |
| [前端_服务与状态](前端_服务与状态.md) | 本模块是 CodingHub 前端应用的核心数据层，涵盖了所有与后端 API 通信的**服务层（Services）**、基于 Pinia 的**状态管理（Stores）**、可复用的**组合式函数（Composables）**、全局**类型 |
| [后端](后端.md) | CodingHub 后端基于 Java 17 / Spring Boot 3.2.5 构建，采用经典的 Controller - Service - Repository - Model 四层架构，运行端口 8082。后端共包含 22 个控 |
| [后端/MCP工具](后端_MCP工具.md) | MCP工具模块是 CodingHub 平台的 AI 代理接口层，基于 Model Context Protocol (MCP) 2.0.0 标准实现。该模块将平台的工具管理、社区论坛、知识库等核心能力封装为 18 个 MCP 工具，供 AI |
| [后端_基础设施](后端_基础设施.md) | 本模块涵盖 CodingHub 后端应用的基础设施层，包括应用启动入口、文件存储配置、统一 API 响应封装、全局异常处理体系、跨领域数据传输对象（DTO）以及工具类。这些组件构成了整个后端系统的底层骨架，为各业务模块（工具、论坛、微课、知 |
| [后端_工具核心](后端_工具核心.md) | 本模块是 CodingHub 平台的核心业务模块，负责 AI 工具的完整生命周期管理，包括工具的创建、查询、更新、删除，工具分类管理，以及工具附件文件的上传、下载和清理功能。模块遵循 Spring Boot 标准四层架构（Controlle |
| [后端_微课](后端_微课.md) | 微课模块是 CodingHub 平台的核心内容模块之一，提供视频上传、流式播放、弹幕互动、封面管理、热度排行等完整的视频管理能力。该模块采用标准四层架构（Controller → Service → Repository → Model）， |
| [后端_标签与通知](后端_标签与通知.md) | 本模块涵盖 CodingHub 后端的两大基础能力：**统一标签系统**和**站内通知系统**。标签模块为工具（[Tool](../backend/src/main/java/com/iaihub/toolbox/model/Tool.ja |
| [后端_留言反馈](后端_留言反馈.md) | 留言反馈模块是 CodingHub 平台的用户意见收集与管理系统。它允许已登录用户和匿名用户提交留言反馈（建议、Bug 报告、表扬等），管理员可以查看留言并进行回复或删除。模块采用经典的 Spring Boot 三层架构（Controlle |
| [后端_知识库](后端_知识库.md) | 知识库模块是 CodingHub 平台中用于管理 RAG（检索增强生成）知识库的核心后端模块。它提供了知识库的完整生命周期管理，包括创建、查询、更新、删除以及基于语义的搜索功能。该模块作为 Java 后端与 Python RAG 微服务之间 |
| [后端_管理概览](后端_管理概览.md) | 管理概览模块是 CodingHub 平台的后台管理与数据统计中心，由两个核心控制器组成：**[AdminController](../backend/src/main/java/com/iaihub/toolbox/controller/A |
| [后端_论坛](后端_论坛.md) | 论坛模块是 CodingHub 平台的核心社区功能之一，为用户提供帖子发布、分类浏览、标签管理等社区交流能力。该模块采用标准的 Spring Boot 四层架构（Controller - Service - Repository - Mod |
| [后端互动系统](后端_互动系统.md) | 互动系统是 CodingHub 平台的核心社交功能模块，提供统一的点赞（Like）、评论（Comment）和收藏（Favorite）三大互动能力。该模块采用"统一目标"设计模式，通过 `TargetType` 枚举将互动行为抽象为通用操作， |
| [认证与安全](后端_认证与安全.md) | 认证与安全模块是 CodingHub 平台的核心基础设施，负责用户身份认证、权限管理、JWT 令牌处理以及系统安全防护。该模块实现了完整的用户生命周期管理，包括注册、登录、令牌刷新、个人资料管理以及管理员审批流程。 |

## 知识笔记

| 标题 | 类型 | 日期 | 文件 |
|------|------|------|------|
