# 工具附件放开任意格式 (2026-06-15)

## 变更摘要

工具广场（AI Tool Square）的工具附件上传接口 `POST /api/v1/tools/{toolId}/files` **取消扩展名白名单**。
之前仅允许 `zip / tar / gz / py / js / ts / md / txt / json / yaml / yml / toml / xml / html / css` 等 15 种后缀，
现在支持**任意扩展名**（包括无扩展名、含多点的复合名、罕见或自定义后缀）的二进制文件上传。

## 适用与不适用

- **适用**：模型权重（.pt / .bin / .safetensors / .onnx）、数据集（.csv / .parquet / .h5）、
  Jupyter Notebook（.ipynb）、Office 文档（.docx / .xlsx / .pptx）、图像资源（.png / .jpg / .svg）、
  Python wheel（.whl）、shell 脚本（.sh）、压缩包（.7z / .rar）等任意文件
- **不适用**：用户头像上传（`POST /api/v1/users/me/avatar`）**仍受白名单约束**
  （`jpg / jpeg / png / webp / gif`），与本变更互不影响

## 保留的不变量

| 约束 | 限制 |
|------|------|
| 单文件大小 | ≤ 50MB |
| 单次请求总大小 | ≤ 200MB |
| 文件名安全 | 仍使用 `StringUtils.cleanPath` 防路径穿越 |
| 所有权校验 | 仍只允许工具上传者本人操作 |
| 同名文件 | 仍执行"删旧写新"语义 |
| 下载接口 | 仍以 `attachment; filename="..."` 返回 |

## 责任划分

- **平台**：保证写入安全（路径、归属、大小）
- **上传者**：自负其责，确保附件合法、可信；下载方运行/打开附件前应自行判断风险
- **运营**：通过举报 / 审计 / 关键词审查等机制治理滥用

## 紧急回滚

`backend/src/main/resources/application.yml` 已保留 `allowed-extensions` 配置模板（注释形式）。
如需紧急回滚，编辑该文件取消注释、填入白名单列表、重启后端即可恢复旧行为：

```yaml
app:
  upload:
    allowed-extensions:
      - zip
      - tar
      - gz
      - py
      # ... 完整白名单
```

## 相关制品

- OpenSpec 变更：`openspec/changes/allow-any-tool-attachment-format/`
- 提案：`proposal.md`
- 设计：`design.md`
- 规范：`specs/tool-file-unrestricted-format/spec.md`
- 任务清单：`tasks.md`
