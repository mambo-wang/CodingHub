# 热榜页面优化 - Plan 1: Tool 实体基础字段

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 Tool 实体添加 viewCount、likeCount、commentCount、score 字段和 updateScore 方法

**架构：** 在 Tool.java 实体中添加统计字段（Integer 类型，默认 0）和 score 字段（BigDecimal 类型，默认 0），以及 updateScore() 方法计算 score = viewCount*1 + likeCount*3 + commentCount*5

**技术栈：** Java 17, Spring Boot 3.2.5, JPA, Gradle

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/
├── model/Tool.java                    # 修改：添加统计字段和 updateScore 方法
├── model/forum/ForumPost.java         # 修改：添加 score 字段和 updateScore 方法
└── test/java/com/iaihub/toolbox/
    └── model/ToolTest.java            # 新增：Tool 实体测试
```

---

## Task 1: Tool 新增 viewCount 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java
package com.iaihub.toolbox.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolTest {

    @Test
    void tool_hasDefaultViewCountOfZero() {
        Tool tool = Tool.builder().name("Test Tool").build();
        assertEquals(0, tool.getViewCount());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getViewCount()"

- [ ] **步骤 3：添加 viewCount 字段**

```java
// backend/src/main/java/com/iaihub/toolbox/model/Tool.java
@Column(name = "view_count")
@Builder.Default
private Integer viewCount = 0;

public Integer getViewCount() { return viewCount; }
public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java
git commit -m "feat: add viewCount field to Tool entity"
```

---

## Task 2: Tool 新增 likeCount 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void tool_hasDefaultLikeCountOfZero() {
    Tool tool = Tool.builder().name("Test Tool").build();
    assertEquals(0, tool.getLikeCount());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getLikeCount()"

- [ ] **步骤 3：添加 likeCount 字段**

```java
@Column(name = "like_count")
@Builder.Default
private Integer likeCount = 0;

public Integer getLikeCount() { return likeCount; }
public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java
git commit -m "feat: add likeCount field to Tool entity"
```

---

## Task 3: Tool 新增 commentCount 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void tool_hasDefaultCommentCountOfZero() {
    Tool tool = Tool.builder().name("Test Tool").build();
    assertEquals(0, tool.getCommentCount());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getCommentCount()"

- [ ] **步骤 3：添加 commentCount 字段**

```java
@Column(name = "comment_count")
@Builder.Default
private Integer commentCount = 0;

public Integer getCommentCount() { return commentCount; }
public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java
git commit -m "feat: add commentCount field to Tool entity"
```

---

## Task 4: Tool 新增 score 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void tool_hasDefaultScoreOfZero() {
    Tool tool = Tool.builder().name("Test Tool").build();
    assertEquals(BigDecimal.ZERO, tool.getScore());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getScore()"

- [ ] **步骤 3：添加 score 字段**

```java
import java.math.BigDecimal;

@Column(name = "score", precision = 10, scale = 2)
@Builder.Default
private BigDecimal score = BigDecimal.ZERO;

public BigDecimal getScore() { return score; }
public void setScore(BigDecimal score) { this.score = score; }
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java
git commit -m "feat: add score field to Tool entity"
```

---

## Task 5: Tool 新增 updateScore 方法

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void tool_updateScore_calculatesCorrectly() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.setViewCount(10);
    tool.setLikeCount(5);
    tool.setCommentCount(2);

    tool.updateScore();

    // score = viewCount * 1 + likeCount * 3 + commentCount * 5
    // score = 10 * 1 + 5 * 3 + 2 * 5 = 10 + 15 + 10 = 35
    assertEquals(new BigDecimal("35"), tool.getScore());
}

@Test
void tool_updateScore_withZeroValues() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.updateScore();
    assertEquals(BigDecimal.ZERO, tool.getScore());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method updateScore()"

- [ ] **步骤 3：添加 updateScore 方法**

```java
public void updateScore() {
    this.score = BigDecimal.valueOf(this.viewCount)
        .multiply(BigDecimal.valueOf(1))
        .add(BigDecimal.valueOf(this.likeCount).multiply(BigDecimal.valueOf(3)))
        .add(BigDecimal.valueOf(this.commentCount).multiply(BigDecimal.valueOf(5)));
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java
git commit -m "feat: add updateScore method to Tool entity"
```

---

## Task 6: Tool 新增 increment/decrement 方法

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Tool.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/ToolTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void tool_incrementViewCount_increasesViewCountAndScore() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.setViewCount(0);
    tool.setLikeCount(0);
    tool.setCommentCount(0);

    tool.incrementViewCount();

    assertEquals(1, tool.getViewCount());
    assertEquals(new BigDecimal("1"), tool.getScore()); // viewCount * 1 = 1
}

@Test
void tool_incrementLikeCount_increasesLikeCountAndScore() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.setViewCount(0);
    tool.setLikeCount(0);
    tool.setCommentCount(0);

    tool.incrementLikeCount();

    assertEquals(1, tool.getLikeCount());
    assertEquals(new BigDecimal("3"), tool.getScore()); // likeCount * 3 = 3
}

@Test
void tool_incrementCommentCount_increasesCommentCountAndScore() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.setViewCount(0);
    tool.setLikeCount(0);
    tool.setCommentCount(0);

    tool.incrementCommentCount();

    assertEquals(1, tool.getCommentCount());
    assertEquals(new BigDecimal("5"), tool.getScore()); // commentCount * 5 = 5
}

@Test
void tool_decrementLikeCount_decreasesLikeCountAndScore() {
    Tool tool = Tool.builder().name("Test Tool").build();
    tool.setViewCount(0);
    tool.setLikeCount(3);
    tool.setCommentCount(0);
    tool.updateScore();

    tool.decrementLikeCount();

    assertEquals(2, tool.getLikeCount());
    assertEquals(new BigDecimal("6"), tool.getScore()); // 2 * 3 = 6
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method incrementViewCount()"

- [ ] **步骤 3：添加 increment/decrement 方法**

```java
public void incrementViewCount() {
    this.viewCount++;
    updateScore();
}

public void decrementViewCount() {
    if (this.viewCount > 0) this.viewCount--;
    updateScore();
}

public void incrementLikeCount() {
    this.likeCount++;
    updateScore();
}

public void decrementLikeCount() {
    if (this.likeCount > 0) this.likeCount--;
    updateScore();
}

public void incrementCommentCount() {
    this.commentCount++;
    updateScore();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/Tool.java
git commit -m "feat: add increment/decrement methods to Tool entity"
```