# 热榜页面优化 - Plan 2: ForumPost + ToolLike + Service

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为 ForumPost 添加 score 字段和 updateScore 方法，创建 ToolLike 实体和 ToolService 点赞功能

**架构：** ForumPost.java 添加 score 字段；新建 ToolLike.java 实体和 ToolLikeRepository；ToolService 添加 likeTool/unlikeTool/isLikedByUser 方法

**技术栈：** Java 17, Spring Boot 3.2.5, JPA, Gradle

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/
├── model/forum/
│   ├── ForumPost.java                    # 修改：添加 score 字段
│   └── ToolLike.java                    # 新增：点赞实体
├── repository/
│   ├── ToolLikeRepository.java           # 新增：点赞数据访问
│   └── ForumPostRepository.java          # 修改：添加 findByIdWithRelations
└── service/
    ├── ToolService.java                 # 修改：添加点赞方法
    └── forum/ForumPostService.java       # 修改：添加 updateScore
```

---

## Task 7: ForumPost 新增 score 字段

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumPostTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.iaihub.toolbox.model.forum;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ForumPostTest {

    @Test
    void forumPost_hasDefaultScoreOfZero() {
        ForumPost post = ForumPost.builder().title("Test").content("Content").authorId(1L).categoryId(1L).build();
        assertEquals(BigDecimal.ZERO, post.getScore());
    }

    @Test
    void forumPost_updateScore_calculatesCorrectly() {
        ForumPost post = ForumPost.builder().title("Test").content("Content").authorId(1L).categoryId(1L).build();
        post.setViewCount(10);
        post.setLikeCount(5);
        post.setCommentCount(2);

        post.updateScore();

        // score = viewCount * 1 + likeCount * 3 + commentCount * 5 = 10 + 15 + 10 = 35
        assertEquals(new BigDecimal("35"), post.getScore());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method getScore()"

- [ ] **步骤 3：添加 score 字段和 updateScore 方法**

```java
// backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java

@Column(name = "score", precision = 10, scale = 2)
@Builder.Default
private BigDecimal score = BigDecimal.ZERO;

public BigDecimal getScore() { return score; }
public void setScore(BigDecimal score) { this.score = score; }

public void updateScore() {
    this.score = BigDecimal.valueOf(this.viewCount)
        .multiply(BigDecimal.valueOf(1))
        .add(BigDecimal.valueOf(this.likeCount).multiply(BigDecimal.valueOf(3)))
        .add(BigDecimal.valueOf(this.commentCount).multiply(BigDecimal.valueOf(5)));
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java
git commit -m "feat: add score field and updateScore method to ForumPost"
```

---

## Task 8: ToolLike 实体创建

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/ToolLike.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/ToolLikeRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/ToolLikeRepositoryTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolLike;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class ToolLikeRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ToolLikeRepository toolLikeRepository;

    @Test
    void toolLikeRepository_canSaveAndFind() {
        ToolLike like = ToolLike.builder()
            .toolId(1L)
            .userId(1L)
            .build();

        ToolLike saved = toolLikeRepository.save(like);

        assertNotNull(saved.getId());
        assertEquals(1L, saved.getToolId());
        assertEquals(1L, saved.getUserId());
    }

    @Test
    void toolLikeRepository_existsByToolIdAndUserId() {
        ToolLike like = ToolLike.builder()
            .toolId(1L)
            .userId(1L)
            .build();
        toolLikeRepository.save(like);

        boolean exists = toolLikeRepository.existsByToolIdAndUserId(1L, 1L);

        assertTrue(exists);
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolLikeRepositoryTest*" -v`
预期：FAIL，编译错误 "cannot find class ToolLike"

- [ ] **步骤 3：创建 ToolLike 实体和 Repository**

```java
// backend/src/main/java/com/iaihub/toolbox/model/ToolLike.java
package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tool_like", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tool_like_tool_user", columnNames = {"tool_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

```java
// backend/src/main/java/com/iaihub/toolbox/repository/ToolLikeRepository.java
package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.ToolLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ToolLikeRepository extends JpaRepository<ToolLike, Long> {

    boolean existsByToolIdAndUserId(Long toolId, Long userId);

    Optional<ToolLike> findByToolIdAndUserId(Long toolId, Long userId);

    void deleteByToolIdAndUserId(Long toolId, Long userId);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolLikeRepositoryTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/ToolLike.java backend/src/main/java/com/iaihub/toolbox/repository/ToolLikeRepository.java
git commit -m "feat: add ToolLike entity and ToolLikeRepository"
```

---

## Task 9: 工具点赞功能

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/ToolServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
// backend/src/test/java/com/iaihub/toolbox/service/ToolServiceTest.java
@Test
void likeTool_increasesLikeCountAndScore() {
    // Setup: Create tool with id=1
    Tool tool = Tool.builder().name("Test").build();
    when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));
    when(toolLikeRepository.existsByToolIdAndUserId(1L, 1L)).thenReturn(false);

    // Execute
    toolService.likeTool(1L, 1L);

    // Verify
    assertEquals(1, tool.getLikeCount());
    verify(toolRepository).save(tool);
    verify(toolLikeRepository).save(any(ToolLike.class));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method likeTool"

- [ ] **步骤 3：实现 likeTool 方法**

```java
// backend/src/main/java/com/iaihub/toolbox/service/ToolService.java

public void likeTool(Long toolId, Long userId) {
    Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
        .orElseThrow(() -> new ResourceNotFoundException("Tool not found"));

    // 检查是否已点赞
    if (toolLikeRepository.existsByToolIdAndUserId(toolId, userId)) {
        return; // 已点赞，直接返回
    }

    // 保存点赞记录
    ToolLike like = ToolLike.builder()
        .toolId(toolId)
        .userId(userId)
        .build();
    toolLikeRepository.save(like);

    // 更新工具统计
    tool.incrementLikeCount();
    toolRepository.save(tool);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/ToolService.java
git commit -m "feat: add likeTool method to ToolService"
```

---

## Task 10: 工具取消点赞功能

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/ToolServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void unlikeTool_decreasesLikeCountAndScore() {
    Tool tool = Tool.builder().name("Test").build();
    tool.setLikeCount(1);
    tool.updateScore();

    when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));
    when(toolLikeRepository.findByToolIdAndUserId(1L, 1L))
        .thenReturn(Optional.of(ToolLike.builder().id(1L).toolId(1L).userId(1L).build()));

    toolService.unlikeTool(1L, 1L);

    assertEquals(0, tool.getLikeCount());
    verify(toolRepository).save(tool);
    verify(toolLikeRepository).delete(any(ToolLike.class));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method unlikeTool"

- [ ] **步骤 3：实现 unlikeTool 方法**

```java
public void unlikeTool(Long toolId, Long userId) {
    Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
        .orElseThrow(() -> new ResourceNotFoundException("Tool not found"));

    Optional<ToolLike> like = toolLikeRepository.findByToolIdAndUserId(toolId, userId);
    if (like.isPresent()) {
        toolLikeRepository.delete(like.get());
        tool.decrementLikeCount();
        toolRepository.save(tool);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/ToolService.java
git commit -m "feat: add unlikeTool method to ToolService"
```

---

## Task 11: 工具详情页浏览量更新

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/ToolService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/ToolServiceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void incrementViewCount_increasesViewCountAndScore() {
    Tool tool = Tool.builder().name("Test").build();
    tool.setViewCount(0);
    tool.setLikeCount(0);
    tool.setCommentCount(0);

    when(toolRepository.findByIdAndStatusNormal(1L)).thenReturn(Optional.of(tool));

    toolService.incrementViewCount(1L);

    assertEquals(1, tool.getViewCount());
    verify(toolRepository).save(tool);
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：FAIL，编译错误 "cannot find symbol - method incrementViewCount"

- [ ] **步骤 3：实现 incrementViewCount 方法**

```java
public void incrementViewCount(Long toolId) {
    Tool tool = toolRepository.findByIdAndStatusNormal(toolId)
        .orElseThrow(() -> new ResourceNotFoundException("Tool not found"));

    tool.incrementViewCount();
    toolRepository.save(tool);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ToolServiceTest*" -v`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/ToolService.java
git commit -m "feat: add incrementViewCount method to ToolService"
```