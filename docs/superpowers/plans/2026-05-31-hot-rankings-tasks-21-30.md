# 热榜页面优化 - Plan 4: 测试 + 迁移

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 完成浏览器测试任务和数据库迁移

**技术栈：** Java 17, Spring Boot 3.2.5, Vue 3, MySQL

---

## 文件结构

```
backend/src/main/resources/db/migration/
└── V2__add_tool_stats_and_score.sql    # 新增：工具统计字段 + score

frontend/src/
├── pages/DetailPage.vue               # 修改：添加点赞、评论功能
├── components/
│   ├── ToolLikeButton.vue            # 新增：点赞按钮组件
│   ├── ToolCommentList.vue            # 新增：评论列表组件
│   └── ToolCommentEditor.vue          # 新增：评论编辑器组件
└── services/tool.ts                  # 新增/修改：点赞、评论 API
```

---

## Task 21: DetailPage 工具详情页改造

**文件：**
- 修改：`frontend/src/pages/DetailPage.vue`

- [ ] **实现说明**

1. 显示 viewCount、likeCount、commentCount
2. 添加点赞按钮（带登录验证）
3. 添加评论区域（带登录验证）

```vue
<template>
  <div class="tool-detail">
    <div class="stats-bar">
      <span>👁 {{ viewCount }}</span>
      <ToolLikeButton :tool-id="toolId" :count="likeCount" />
      <span>💬 {{ commentCount }}</span>
    </div>
    <ToolCommentEditor :tool-id="toolId" @submitted="onCommentSubmitted" />
    <ToolCommentList :tool-id="toolId" />
  </div>
</template>
```

- [ ] **验证：** 页面显示统计数，点赞和评论功能正常

---

## Task 22: ToolLikeButton 点赞按钮组件

**文件：**
- 创建：`frontend/src/components/ToolLikeButton.vue`

- [ ] **实现说明**

点赞按钮组件，支持：
- 显示点赞数
- 点击后调用 API 点赞/取消点赞
- 登录状态判断

```vue
<template>
  <button :class="['like-btn', { 'liked': isLiked }]" @click="handleClick">
    <ThumbsUp :size="18" />
    <span>{{ count }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { likeTool, unlikeTool, getLikeStatus } from '@/services/tool';

const props = defineProps<{
  toolId: number;
  count: number;
}>();

const authStore = useAuthStore();
const isLiked = ref(false);

const handleClick = async () => {
  if (!authStore.isLoggedIn) {
    // 提示登录
    return;
  }
  if (isLiked.value) {
    await unlikeTool(props.toolId);
    isLiked.value = false;
  } else {
    await likeTool(props.toolId);
    isLiked.value = true;
  }
};

// 页面加载时检查点赞状态
onMounted(async () => {
  if (authStore.isLoggedIn) {
    isLiked.value = await getLikeStatus(props.toolId);
  }
});
</script>
```

- [ ] **验证：** 点赞按钮状态正确切换

---

## Task 23: ToolCommentList 评论列表组件

**文件：**
- 创建：`frontend/src/components/ToolCommentList.vue`

- [ ] **实现说明**

显示工具的评论列表：
- 获取并展示评论
- 支持分页

```vue
<template>
  <div class="comment-list">
    <div v-for="comment in comments" :key="comment.id" class="comment-item">
      <div class="comment-header">
        <span class="username">{{ comment.username }}</span>
        <span class="date">{{ comment.createdAt }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { getComments } from '@/services/tool';

const props = defineProps<{ toolId: number }>();
const comments = ref([]);

onMounted(async () => {
  comments.value = await getComments(props.toolId);
});
</script>
```

- [ ] **验证：** 评论列表正确显示

---

## Task 24: ToolCommentEditor 评论编辑器组件

**文件：**
- 创建：`frontend/src/components/ToolCommentEditor.vue`

- [ ] **实现说明**

评论输入和提交组件：

```vue
<template>
  <div class="comment-editor">
    <textarea v-model="content" placeholder="发表评论..." />
    <button @click="submit" :disabled="!content.trim()">发送</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { postComment } from '@/services/tool';

const props = defineProps<{ toolId: number }>();
const emit = defineEmits<{ (e: 'submitted'): void }>();
const content = ref('');

const submit = async () => {
  if (!content.value.trim()) return;
  await postComment(props.toolId, content.value);
  content.value = '';
  emit('submitted');
};
</script>
```

- [ ] **验证：** 评论提交成功，评论列表刷新

---

## Task 25: ToolService API 调用

**文件：**
- 创建/修改：`frontend/src/services/tool.ts`

- [ ] **实现说明**

添加点赞、评论 API 调用：

```typescript
import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

export async function likeTool(toolId: number): Promise<void> {
  await api.post(`/tools/${toolId}/like`);
}

export async function unlikeTool(toolId: number): Promise<void> {
  await api.delete(`/tools/${toolId}/like`);
}

export async function getLikeStatus(toolId: number): Promise<boolean> {
  const response = await api.get(`/tools/${toolId}/like-status`);
  return response.data;
}

export async function getComments(toolId: number): Promise<Comment[]> {
  const response = await api.get(`/tools/${toolId}/comments`);
  return response.data;
}

export async function postComment(toolId: number, content: string): Promise<void> {
  await api.post(`/tools/${toolId}/comments`, { content });
}
```

- [ ] **验证：** API 调用正常

---

## Task 26: 数据库迁移 - 为 tool 表添加字段

**文件：**
- 创建：`backend/src/main/resources/db/migration/V2__add_tool_stats_and_score.sql`

- [ ] **执行 SQL**

```sql
-- 为 tool 表添加统计字段
ALTER TABLE tool ADD COLUMN view_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN like_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN comment_count INT DEFAULT 0;
ALTER TABLE tool ADD COLUMN score DECIMAL(10,2) DEFAULT 0;

-- 为 score 字段添加索引
CREATE INDEX idx_tool_score ON tool(score DESC);
```

- [ ] **验证：** 数据库表结构正确

---

## Task 27: 数据库迁移 - 为 forum_post 表添加 score

- [ ] **执行 SQL**

```sql
-- 为 forum_post 表添加 score 字段
ALTER TABLE forum_post ADD COLUMN score DECIMAL(10,2) DEFAULT 0;

-- 为 score 字段添加索引
CREATE INDEX idx_forum_post_score ON forum_post(score DESC);

-- 初始化已有数据的 score
UPDATE forum_post SET score = (COALESCE(view_count, 0) * 1 + COALESCE(like_count, 0) * 3 + COALESCE(comment_count, 0) * 5);
```

- [ ] **验证：** 数据正确更新

---

## Task 28: 数据库迁移 - 创建 tool_like 表

- [ ] **执行 SQL**

```sql
-- 新增 tool_like 表
CREATE TABLE tool_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tool_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_tool_like_tool_user UNIQUE (tool_id, user_id),
    CONSTRAINT fk_tool_like_tool FOREIGN KEY (tool_id) REFERENCES tool(id),
    CONSTRAINT fk_tool_like_user FOREIGN KEY (user_id) REFERENCES user(id)
);
```

- [ ] **验证：** 表创建成功

---

## Task 29: ToolController 点赞 API

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/ToolController.java`

- [ ] **实现说明**

添加点赞/取消点赞/状态查询 API：

```java
@PostMapping("/{id}/like")
public ResponseEntity<Void> likeTool(@PathVariable Long id, @AuthenticationPrincipal User user) {
    toolService.likeTool(id, user.getId());
    return ResponseEntity.ok().build();
}

@DeleteMapping("/{id}/like")
public ResponseEntity<Void> unlikeTool(@PathVariable Long id, @AuthenticationPrincipal User user) {
    toolService.unlikeTool(id, user.getId());
    return ResponseEntity.ok().build();
}

@GetMapping("/{id}/like-status")
public ResponseEntity<Boolean> getLikeStatus(@PathVariable Long id, @AuthenticationPrincipal User user) {
    boolean isLiked = toolService.isLikedByUser(id, user.getId());
    return ResponseEntity.ok(isLiked);
}
```

- [ ] **验证：** API 响应正常

---

## Task 30: 工具评论功能

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- Controller 添加评论 API

- [ ] **实现说明**

添加评论功能：评论成功后更新 commentCount 和 score

```java
public void addComment(Long toolId, String content) {
    Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
        .orElseThrow(() -> new ResourceNotFoundException("Tool not found"));

    // 保存评论
    ToolComment comment = ToolComment.builder()
        .toolId(toolId)
        .userId(currentUserId)
        .content(content)
        .build();
    toolCommentRepository.save(comment);

    // 更新工具评论数
    tool.incrementCommentCount();
    toolRepository.save(tool);
}
```

- [ ] **验证：** 评论成功，commentCount + 1

---

## 浏览器测试说明

> 开发任务完成后，手动加载 `/openspec-browser-test` skill 执行浏览器测试。

测试用例：

| TC | 测试名称 | 验证内容 |
|----|---------|----------|
| TC-001 | 页面加载测试 | 访问 /overview，确认页面显示"热榜"标题 |
| TC-002 | 工具热榜显示测试 | 确认工具按 score 排序，点击条目跳转工具详情 |
| TC-003 | 帖子热榜显示测试 | 确认帖子按 score 排序，点击条目跳转帖子详情 |
| TC-004 | Tab 分类过滤测试 | 点击分类 Tab，只显示该分类下的条目 |
| TC-005 | 统计卡片数据测试 | 确认用户、帖子、工具数量正确显示 |
| TC-006 | 响应式布局测试 | 在不同屏幕宽度下检查布局 |
| TC-007 | 工具详情页加载 | 访问 /tools/1，确认显示统计数 |
| TC-008 | 点赞功能测试 | 登录后点击点赞按钮，确认 likeCount + 1 |
| TC-009 | 取消点赞测试 | 再次点击点赞按钮，确认 likeCount - 1 |
| TC-010 | 评论功能测试 | 提交评论后，确认评论显示且 commentCount + 1 |
| TC-011 | 未登录提示测试 | 未登录时点击点赞或评论，确认提示登录 |