# Quickstart: 多文件上传功能开发

**Date**: 2026-05-29
**Feature**: 工具上传功能优化 - 多文件支持

## 开发环境准备

### 前置条件

- JDK 17+
- Node.js 20+
- MySQL 8.0+
- 已配置 `backend/src/main/resources/application.yml` 中的数据库连接

### 本地启动

**Backend**:
```bash
cd backend
./gradlew bootRun
```

**Frontend**:
```bash
cd frontend
npm install
npm run dev
```

## 关键文件变更

### Backend

| 文件 | 操作 | 说明 |
|------|------|------|
| `controller/ToolFileController.java` | 新增 | 文件上传/下载/删除 API |
| `service/ToolFileService.java` | 新增 | 文件存储核心逻辑 |
| `model/ToolFile.java` | 新增 | 文件元数据实体 |
| `repository/ToolFileRepository.java` | 新增 | 文件数据访问 |
| `service/ToolService.java` | 修改 | 删除工具时清理文件 |
| `application.yml` | 修改 | 添加文件上传配置 |

### Frontend

| 文件 | 操作 | 说明 |
|------|------|------|
| `pages/UploadPage.vue` | 修改 | 添加文件上传组件 |
| `services/api.ts` | 修改 | 添加文件上传接口方法 |
| `types/index.ts` | 修改 | 添加相关 TypeScript 类型 |

## 测试要点

### 后端测试场景

1. **上传成功**: 上传 3 个有效文件，验证文件存储路径正确
2. **单文件大小超限**: 上传 60MB 文件，验证返回 400 错误
3. **文件类型不支持**: 上传 .exe 文件，验证返回 400 错误
4. **README 保存**: 上传文件时附带 readme 内容，验证 readme.md 生成
5. **删除工具**: 删除工具后，验证物理文件同步删除

### 前端测试场景

1. **多文件选择**: 选择 5 个文件，验证文件列表正确显示
2. **移除文件**: 点击移除按钮，验证文件从列表移除
3. **上传进度**: 上传时验证进度条正确显示
4. **错误提示**: 上传失败时验证错误信息正确显示

## 常见问题

### Q: 文件上传后 404

检查 `uploads/` 目录是否创建，Spring Boot 可能无法自动创建。

**Solution**:
```java
@PostConstruct
public void init() {
    Path uploadDir = Paths.get(uploadBase);
    if (!Files.exists(uploadDir)) {
        Files.createDirectories(uploadDir);
    }
}
```

### Q: 文件类型判断不准确

浏览器发送的 `Content-Type` 可能不准确，应使用文件扩展名判断。

### Q: 大文件上传超时

检查 `application.yml` 中的超时配置:
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 200MB
```
