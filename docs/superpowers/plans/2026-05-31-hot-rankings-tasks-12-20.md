# 热榜页面优化 - Plan 3: DTO + 热榜排序 + UI

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现 DTO 添加 id/score 字段，热榜按 score 排序，OverviewPage 和 ToolRankList/PostRankList UI 优化

**架构：** 修改 DTO 添加 id 和 score 字段；OverviewServiceImpl 按 score 排序；前端 OverviewPage 简化装饰、ToolRankList/PostRankList 添加点击跳转

**技术栈：** Java 17, Spring Boot 3.2.5, Vue 3, TypeScript, Vite

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/
├── dto/
│   ├── ToolRankDto.java              # 修改：添加 id、score 字段
│   └── PostRankDto.java              # 修改：添加 id、score 字段
├── service/
│   └── OverviewServiceImpl.java       # 修改：按 score 排序

frontend/src/
├── types/overview.ts                 # 修改：添加 id、score 字段
├── pages/OverviewPage.vue            # 修改：标题改为"热榜"，简化装饰
├── components/
│   ├── ToolRankList.vue             # 修改：添加点击跳转
│   ├── PostRankList.vue              # 修改：添加点击跳转
│   └── StatsCard.vue                # 修改：移除过度动画
```

---

## Task 12: ToolRankDto 添加 id 和 score 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/dto/ToolRankDto.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/ToolRankDtoTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.iaihub.toolbox.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ToolRankDtoTest {

    @Test
    void toolRankDto_hasIdField() {
        ToolRankDto dto = new ToolRankDto(1L, "AI", "ChatGPT", new BigDecimal("100"));
        assertEquals(1L, dto.getId());
    }

    @Test
    void toolRankDto_hasScoreField() {
        ToolRankDto dto = new ToolRankDto(1L, "AI", "ChatGPT", new BigDecimal("100"));
        assertEquals(new BigDecimal("100"), dto.getScore());
    }

    @Test
    void toolRankDto_constructorAcceptsAllFields() {
        ToolRankDto dto = new ToolRankDto(1L, "AI", "ChatGPT", new BigDecimal("100"));
        assertEquals("AI", dto.getCategory());
        assertEquals("ChatGPT", dto.getToolName());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolRankDtoTest*" -v`
预期：FAIL，编译错误 "constructor ToolRankDto(long, String, String, BigDecimal) not found"

- [ ] **步骤 3：修改 ToolRankDto 添加字段**

```java
// backend/src/main/java/com/iaihub/toolbox/dto/ToolRankDto.java
package com.iaihub.toolbox.dto;

import java.math.BigDecimal;

public class ToolRankDto {
    private Long id;
    private String category;
    private String toolName;
    private BigDecimal score;

    public ToolRankDto(Long id, String category, String toolName, BigDecimal score) {
        this.id = id;
        this.category = category;
        this.toolName = toolName;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolRankDtoTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/ToolRankDto.java
git commit -m "feat: add id and score fields to ToolRankDto"
```

---

## Task 13: PostRankDto 添加 id 和 score 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/dto/PostRankDto.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/PostRankDtoTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void postRankDto_hasIdAndScoreFields() {
    PostRankDto dto = new PostRankDto(1L, "Tech", "Post Title", new BigDecimal("50"));
    assertEquals(1L, dto.getId());
    assertEquals(new BigDecimal("50"), dto.getScore());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*PostRankDtoTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getId()"

- [ ] **步骤 3：修改 PostRankDto 添加字段**

```java
// backend/src/main/java/com/iaihub/toolbox/dto/PostRankDto.java
package com.iaihub.toolbox.dto;

import java.math.BigDecimal;

public class PostRankDto {
    private Long id;
    private String category;
    private String postTitle;
    private BigDecimal score;

    public PostRankDto(Long id, String category, String postTitle, BigDecimal score) {
        this.id = id;
        this.category = category;
        this.postTitle = postTitle;
        this.score = score;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*PostRankDtoTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/PostRankDto.java
git commit -m "feat: add id and score fields to PostRankDto"
```

---

## Task 14: 工具热榜按 score 排序

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/OverviewServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void getToolRanks_returnsSortedByScoreDescending() {
    // Setup: Create tools with different scores
    Tool tool1 = Tool.builder().name("Tool1").category(category).build();
    tool1.setScore(new BigDecimal("50"));

    Tool tool2 = Tool.builder().name("Tool2").category(category).build();
    tool2.setScore(new BigDecimal("100"));

    when(toolRepository.findAllWithCategory()).thenReturn(Arrays.asList(tool1, tool2));

    List<ToolRankDto> result = overviewService.getToolRanks();

    assertEquals("Tool2", result.get(0).getToolName()); // 100 > 50
    assertEquals("Tool1", result.get(1).getToolName());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*OverviewServiceTest*" -v`
预期：FAIL，测试断言失败 "expected Tool2 but was Tool1"

- [ ] **步骤 3：修改 getToolRanks 按 score 排序**

```java
// backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java

@Override
public List<ToolRankDto> getToolRanks() {
    return toolRepository.findAllWithCategory().stream()
        .sorted(Comparator.comparing(Tool::getScore).reversed())
        .limit(10)
        .map(tool -> new ToolRankDto(
            tool.getId(),
            tool.getCategory().getName(),
            tool.getName(),
            tool.getScore()
        ))
        .collect(Collectors.toList());
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*OverviewServiceTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java
git commit -m "feat: sort tool ranks by score descending"
```

---

## Task 15: 帖子热榜按 score 排序

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/OverviewServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void getPostRanks_returnsSortedByScoreDescending() {
    // Setup: Create posts with different scores
    ForumPost post1 = ForumPost.builder().title("Post1").authorId(1L).categoryId(1L).build();
    post1.setScore(new BigDecimal("30"));

    ForumPost post2 = ForumPost.builder().title("Post2").authorId(1L).categoryId(1L).build();
    post2.setScore(new BigDecimal("60"));

    when(forumPostRepository.findAllWithCategory()).thenReturn(Arrays.asList(post1, post2));

    List<PostRankDto> result = overviewService.getPostRanks();

    assertEquals("Post2", result.get(0).getPostTitle()); // 60 > 30
    assertEquals("Post1", result.get(1).getPostTitle());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*OverviewServiceTest*" -v`
预期：FAIL，测试断言失败

- [ ] **步骤 3：修改 getPostRanks 按 score 排序**

```java
@Override
public List<PostRankDto> getPostRanks() {
    return forumPostRepository.findAllWithCategory().stream()
        .sorted(Comparator.comparing(ForumPost::getScore).reversed())
        .limit(10)
        .map(post -> new PostRankDto(
            post.getId(),
            forumCategoryRepository.findById(post.getCategoryId()).map(ForumCategory::getName).orElse("Unknown"),
            post.getTitle(),
            post.getScore()
        ))
        .collect(Collectors.toList());
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*OverviewServiceTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java
git commit -m "feat: sort post ranks by score descending"
```

---

## Task 16: OverviewPage 主页面优化

**文件：**
- 修改：`frontend/src/pages/OverviewPage.vue`

- [ ] **实现说明**

1. 标题改为"热榜"
2. 移除扫描线动画和动态网格背景
3. 保持简洁布局

```vue
<template>
  <div class="overview-page">
    <div class="page-container">
      <header class="page-header">
        <h1 class="title">热榜</h1>
        <p class="subtitle">Hot Rankings</p>
      </header>
      <!-- Stats Grid -->
      <section class="stats-grid">
        <StatsCard label="用户总数" :value="stats.userCount" icon="users" />
        <StatsCard label="帖子总数" :value="stats.postCount" icon="message-square" />
        <StatsCard label="工具总数" :value="stats.toolCount" icon="wrench" />
      </section>
      <!-- Rankings -->
      <section class="main-content">
        <ToolRankList ... />
        <PostRankList ... />
      </section>
    </div>
  </div>
</template>
```

- [ ] **验证：** 检查页面标题是否显示为"热榜"，布局是否简洁

---

## Task 17: ToolRankList 组件添加点击跳转

**文件：**
- 修改：`frontend/src/components/ToolRankList.vue`

- [ ] **实现说明**

添加点击跳转功能：点击条目时跳转到 `/tools/${item.id}`

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();

const handleClick = (item: ToolRankDto) => {
  router.push(`/tools/${item.id}`);
};
</script>

<template>
  <div class="rank-item" @click="handleClick(item)">
    <!-- rank badge, title, score badge -->
  </div>
</template>
```

- [ ] **验证：** 点击工具条目是否能正确跳转到工具详情页

---

## Task 18: PostRankList 组件添加点击跳转

**文件：**
- 修改：`frontend/src/components/PostRankList.vue`

- [ ] **实现说明**

添加点击跳转功能：点击条目时跳转到 `/forum/posts/${item.id}`

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router';
const router = useRouter();

const handleClick = (item: PostRankDto) => {
  router.push(`/forum/posts/${item.id}`);
};
</script>
```

- [ ] **验证：** 点击帖子条目是否能正确跳转到帖子详情页

---

## Task 19: StatsCard 组件优化

**文件：**
- 修改：`frontend/src/components/StatsCard.vue`

- [ ] **实现说明**

移除过度动画，保持简洁 hover 效果：
- 移除扫描线动画
- 保留简洁的 hover 效果（border-color 变化）
- 保留数字滚动动画（保留）

- [ ] **验证：** 检查 hover 效果是否平滑，动画是否简洁

---

## Task 20: 前端类型更新

**文件：**
- 修改：`frontend/src/types/overview.ts`

- [ ] **实现说明**

添加 `id` 和 `score` 字段到 ToolRankDto 和 PostRankDto 接口

```typescript
export interface ToolRankDto {
  id: number;
  category: string;
  toolName: string;
  score: number;
}

export interface PostRankDto {
  id: number;
  category: string;
  postTitle: string;
  score: number;
}
```

- [ ] **验证：** 类型定义与后端 DTO 一致