---
title: 软删除策略
type: concept
---

# 软删除策略

## 定义

平台对内容实体采用软删除（逻辑删除）而非物理删除，通过 `status = DELETED` 标记实现数据可恢复性和审计追踪。

## 适用范围

| 实体 | 状态字段 | 删除行为 |
|------|---------|----------|
| [Tool](../entities/Tool.md) | ToolStatus.DELETED | 标记删除，列表不展示 |
| [ForumPost](../entities/ForumPost.md) | PostStatus.DELETED | 标记删除 + 级联评论标记 |
| [Video](../entities/Video.md) | VideoStatus.DELETED | 标记删除 |
| [FeedbackMessage](../entities/FeedbackMessage.md) | FeedbackStatus.DELETED | 标记删除 |

## 实现规则

1. **查询过滤**: 所有列表/详情接口默认过滤 `status != DELETED`
2. **级联处理**: 帖子删除时，其下评论同步标记为 DELETED
3. **权限控制**: 删除操作需 `isOwner || isAdmin`（[[rbac-permission]]）
4. **不可恢复**: 当前版本不提供恢复接口（预留）
5. **关联清理**: 删除时清理相关互动记录（[unified-interaction](unified-interaction.md)）

## 代码模式

```java
// 实体状态枚举
public enum ToolStatus { ACTIVE, DELETED }

// 服务层删除
public void deleteTool(Long id, Long userId) {
    Tool tool = toolRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Tool not found"));
    if (!tool.getAuthor().getId().equals(userId) && !isAdmin(userId)) {
        throw new ForbiddenException("No permission");
    }
    tool.setStatus(ToolStatus.DELETED);
    toolRepository.save(tool);
}
```

## 关联页面

[Tool](../entities/Tool.md) · [ForumPost](../entities/ForumPost.md) · [Video](../entities/Video.md) · [[rbac-permission]]

## 设计决策来源

- forum-post-deletion (2026-05-25)
