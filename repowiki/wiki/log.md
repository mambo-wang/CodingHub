# 操作日志

> 按日期倒序分组的操作记录，由系统自动维护（OKF v0.2 §9 格式）


## 2026-08-26
* **ingest_note**: 添加笔记: JPA @PreUpdate 会在任意 save() 时刷新 updatedAt，读改写式计数更新会污染更新时间
* **ingest_note**: 添加笔记: 内容实体计数更新应改用 repository 层原子 SQL（@Modifying）而非读改写 + save()
* **ingest_note**: 添加笔记: Spring Boot 3 应使用 jakarta.servlet 命名空间，勿误用 jgit 包下的 HttpServletRequest

## 2026-08-08
* **analyze_repo**: 分析仓库 CodingHub，1183 个组件
* **write_doc_file**: 创建 backend-core.md
* **write_doc_file**: 创建 backend-forum.md
* **write_doc_file**: 创建 backend-video.md
* **write_doc_file**: 创建 backend-kb.md
* **write_doc_file**: 创建 backend-feedback.md
* **write_doc_file**: 创建 backend-tag.md
* **write_doc_file**: 创建 backend-overview.md
* **write_doc_file**: 创建 backend-mcp.md
* **write_doc_file**: 创建 backend-infra.md
* **write_doc_file**: 创建 frontend-pages.md
* **write_doc_file**: 创建 frontend-components.md
* **write_doc_file**: 创建 frontend-services.md
* **write_doc_file**: 创建 frontend-stores.md
* **write_doc_file**: 创建 rag.md
* **write_doc_file**: 创建 backend.md
* **write_doc_file**: 创建 frontend.md
* **write_doc_file**: 创建 codinghub.md
* **lint_wiki**: 检查完成: 64 个问题
* **close_session**: 会话关闭
