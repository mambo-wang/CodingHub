---
name: openspec-browser-test
description: OpenSpec工作流完成后手动触发，使用opencli-browser做UI端到端自动化测试
---
# openspec-browser-test

> **重要**: 这是一个**手动触发**的 skill，用于在开发任务和单元测试全部完成后，对 UI 进行端到端浏览器自动化测试验证。

## 何时使用

在以下条件满足后执行：
- ✅ 所有 TDD 任务已完成（RED/GREEN/REFACTOR）
- ✅ UI 实现任务已完成
- ✅ 单元测试全部通过
- ✅ 开发服务器已启动 (`npm run dev`)

## 前置条件检查

```bash
opencli doctor
```

必须确保 `doctor` 通过，否则浏览器测试无法执行。

## 测试用例编写规范

基于 specs/**/*.md 中的 GIVEN/WHEN/THEN 场景转换为测试用例：

```bash
# 示例：测试概览页面统计卡片加载
opencli browser overview open "http://localhost:5173/overview"
opencli browser overview wait selector ".glass-card" --timeout 15000
opencli browser overview find --css ".stat-card"
opencli browser overview get text ".stat-value"
# 验证数值 > 0
```

## 常用命令速查

```bash
# 打开页面
opencli browser <session> open "<url>"

# 页面快照（带 [N] 索引）
opencli browser <session> state

# 查找元素
opencli browser <session> find --css "<selector>"

# 点击/悬停
opencli browser <session> click <target>
opencli browser <session> hover <target>

# 输入/填充
opencli browser <session> type <target> "<text>"
opencli browser <session> fill <target> "<text>"

# 获取值
opencli browser <session> get text <target>
opencli browser <session> get value <target>

# 等待
opencli browser <session> wait selector "<selector>" --timeout <ms>
opencli browser <session> wait text "<text>" --timeout <ms>

# 截图
opencli browser <session> screenshot [<path>]

# 网络请求
opencli browser <session> network

# 关闭会话
opencli browser <session> close
```

## 与 opencli-browser Skill 的关系

本 skill 是 opencli-browser 在 OpenSpec 工作流中的具体应用：
- **opencli-browser** — 通用浏览器驱动技能，详细命令参考
- **openspec-browser-test** — OpenSpec 专用的测试执行指南和模板

详细命令和规则请参阅 `opencli-browser` skill。

## 输出产物

browser-test artifact 应包含：
- 测试环境信息
- 每个测试用例的执行结果（✅ PASS / ❌ FAIL）
- 失败用例的截图证据
- 测试总结（X/Y Passed）

## 示例测试报告

```
## Test Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| TC-001: 页面加载 | ✅ PASS | StatCard 正确渲染 |
| TC-002: 数据验证 | ✅ PASS | 用户总数 12,847 |
| TC-003: Tab 切换 | ❌ FAIL | 分类 Tab 切换后数据未更新 |
| TC-004: 响应式 | ✅ PASS | 375px/768px/1024px 均正常 |

**Overall:** 3/4 Passed
```

## 特别的要求
遇到问题一定要自动修复，不要停下来问我
修复问题后自动重启服务验证问题是否修复
