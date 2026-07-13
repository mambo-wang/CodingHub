# 常见陷阱 (Gotchas)

> CodingHub 操作中容易踩坑的地方，操作前建议快速浏览。

1. **文件上传走 REST，不走 MCP**: `h3_coding_hub_tool_file_upload` 只返回端点信息，实际用 curl 或 `$CHUB tool-file-upload`
2. **下载链接是相对路径**: `h3_coding_hub_tool_download` 返回的 URL 需拼接 `{baseUrl}` 前缀；直接用 `$CHUB tool-download` 更省事
3. **MCP 端点无需 JWT**: `/sse` 和 `/mcp/**` 已 permitAll，不要传 Authorization
4. **chub CLI 自动处理 token**: Agent 不要手动登录或拼接 `Authorization` 头
5. **文件上传端点无需 JWT**: `POST /api/v1/tools/{toolId}/files` 已 permitAll
6. **modify 的 partial update**: 只更新传入的字段；version 不传自动递增
7. **skill 多文件才需压缩**: 只有 SKILL.md 时直接上传原文
8. **知识库文档上传也走 REST**: `h3_coding_hub_kb_upload_document` 只返回端点信息
9. **上传后异步处理**: 文档经历 UPLOADING → CONVERTING → CHUNKING → EMBEDDING → READY，必须等全部 READY 后再检索
10. **带图片文档必须预处理**: 含截图/图表的 PDF/Word/PPT 需先用 markitdown-mcp 预处理
11. **kb_search 默认值**: `rerank=true`, `expandContext=1`，一般无需修改
12. **Python 的 requests 依赖**: `chub.py` 依赖 `requests` 库，初始化脚本的 `python -c "import requests"` 会自动检测；若缺失先 `pip install requests`。Node 版本无需额外安装
