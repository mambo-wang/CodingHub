# Tasks

## Impact Analysis Status

- 状态：已生成 / 已跳过
- 跳过原因：`<如全部源码文件为新增，请在此说明>`
- 如已生成：参考 `impact-analysis.md` 的调用图、受影响测试、回归建议和设计修正建议。

---

## A. Atomic TDD Task List

<!-- 每个 task 只能是一个 TDD 阶段 -->
<!-- 必须使用 checkbox 格式 -->

### [Feature name]

- [ ] RED: ...
- [ ] GREEN: ...（引用对应 RED）
- [ ] REFACTOR: ...（可选）

---

## B. UI Implementation Tasks【如有 UI 改动】

<!-- 基于 design-system.md（如有）、全局设计系统和 ui-preview.html（如有） -->

### UI: [Component/Page Name]

- [ ] 实现 [组件名]——基于设计系统规范，参考 ui-preview.html（如有）
- [ ] 验证 [组件名]——检查视觉、交互状态、响应式和可访问性是否符合设计规范

---

## C. Browser Test【如有 UI 改动】

> 开发任务完成后，手动加载 `/openspec-browser-test` skill 或使用 opencli browser 执行浏览器测试。
> **测试依据**: `specs`、`design.md`、`ui-preview.html`（如有）

### Test: [Scenario]

- [ ] TC-XXX: [测试名称]——覆盖 [场景]，执行后记录 PASS / FAIL