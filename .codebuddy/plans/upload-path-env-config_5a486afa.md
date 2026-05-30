---
name: upload-path-env-config
overview: 将工具文件存放路径改为可通过环境变量配置，默认使用用户家目录下的 aifiles 目录
todos:
  - id: modify-upload-config
    content: 修改 UploadConfig.java 支持环境变量配置，默认使用 user.home/aifiles，并在初始化时创建目录
    status: completed
  - id: update-application-yml
    content: 更新 application.yml 添加环境变量配置说明注释
    status: completed
    dependencies:
      - modify-upload-config
  - id: update-environment-json
    content: 更新 environment.json 添加 AIHUB_FILE_BASE_DIR 环境变量说明
    status: completed
    dependencies:
      - modify-upload-config
---

## 用户需求

工具文件的存放路径改为可配置，配置方式为使用环境变量，默认动态获取并存放到当前用户家目录的 aifiles 目录下，如果目录不存在则自动创建。

## 核心功能

1. 文件存储路径通过环境变量配置（环境变量名：`AIHUB_FILE_BASE_DIR`）
2. 默认路径为用户家目录下的 `aifiles` 目录（`${user.home}/aifiles`）
3. 应用启动时自动创建目录（如不存在）

## 技术方案

### 实现策略

在 `UploadConfig` 中使用 Spring 的 `@Value` 注解配合环境变量实现配置，同时在应用启动时通过 `ApplicationRunner` 或 `@PostConstruct` 确保目录存在。

### 关键技术决策

1. **环境变量优先级**：环境变量 `AIHUB_FILE_BASE_DIR` > 默认值 `user.home/aifiles`
2. **目录初始化时机**：使用 `@PostConstruct` 在 `UploadConfig` 初始化后创建目录
3. **异常处理**：如果目录创建失败，记录日志并抛出明确的异常信息

### 修改文件

1. `UploadConfig.java` - 添加环境变量支持和目录初始化逻辑
2. `application.yml` - 更新默认值说明
3. `environment.json` - 添加环境变量配置说明