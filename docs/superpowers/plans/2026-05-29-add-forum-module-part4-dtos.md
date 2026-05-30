# Forum Module - DTO Layer

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的数据传输对象（DTO），解耦 API 响应和内部实体

**架构：** 使用 Java Record 或 Lombok @Data 风格，保持 POJO 纯净

**技术栈：** Java Record, Lombok

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/dto/forum/
├── ForumPostDTO.java
├── ForumPostCreateRequest.java
├── ForumCommentDTO.java
├── ForumCommentCreateRequest.java
├── ForumLikeRequest.java
├── ForumCategoryDTO.java
└── ForumTagDTO.java
```

---

### Task 1：ForumCategoryDTO 和 ForumTagDTO

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCategoryDTO.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumTagDTO.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/forum/DTOTest.java`

- [ ] **步骤 1：编写 DTO 测试**

```java
@Test
void testForumCategoryDTO() {
    ForumCategoryDTO dto = new ForumCategoryDTO(1L, "使用心得", "描述", 1, 10);
    assertEquals(1L, dto.id());
    assertEquals("使用心得", dto.name());
    assertEquals(10, dto.postCount());
}

@Test
void testForumTagDTO() {
    ForumTagDTO dto = new ForumTagDTO(1L, "教程", 5, false);
    assertEquals(1L, dto.id());
    assertEquals("教程", dto.name());
    assertEquals(5, dto.postCount());
    assertFalse(dto.system());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*DTOTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 DTO 类**

```java
// ForumCategoryDTO.java
package com.iaihub.toolbox.dto.forum;

public record ForumCategoryDTO(
    Long id,
    String name,
    String description,
    Integer sortOrder,
    Integer postCount
) {}

// ForumTagDTO.java
package com.iaihub.toolbox.dto.forum;

public record ForumTagDTO(
    Long id,
    String name,
    Integer postCount,
    Boolean system
) {}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*DTOTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCategoryDTO.java
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumTagDTO.java
git commit -m "feat(forum): add ForumCategoryDTO and ForumTagDTO"
```

---

### Task 2：ForumPostDTO 和 ForumPostCreateRequest

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumPostDTO.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumPostCreateRequest.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/forum/ForumPostDTOTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testForumPostDTO() {
    ForumPostDTO dto = new ForumPostDTO(
        1L, "标题", "# 内容", 100L, "作者",
        1L, "分类", 0, 0, 0,
        LocalDateTime.now(), LocalDateTime.now()
    );
    
    assertEquals(1L, dto.id());
    assertEquals("标题", dto.title());
    assertEquals("作者", dto.authorName());
}

@Test
void testForumPostCreateRequest() {
    ForumPostCreateRequest req = new ForumPostCreateRequest(
        "标题", "# 内容", 1L, List.of(1L, 2L)
    );
    
    assertEquals("标题", req.title());
    assertEquals(1L, req.categoryId());
    assertEquals(2, req.tagIds().size());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostDTOTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 DTO 类**

```java
// ForumPostDTO.java
package com.iaihub.toolbox.dto.forum;

import java.time.LocalDateTime;

public record ForumPostDTO(
    Long id,
    String title,
    String content,
    Long authorId,
    String authorName,
    Long categoryId,
    String categoryName,
    Integer viewCount,
    Integer likeCount,
    Integer commentCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// ForumPostCreateRequest.java
package com.iaihub.toolbox.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ForumPostCreateRequest(
    @NotBlank String title,
    @NotBlank String content,
    @NotNull Long categoryId,
    List<Long> tagIds
) {}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostDTOTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumPostDTO.java
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumPostCreateRequest.java
git commit -m "feat(forum): add ForumPostDTO and ForumPostCreateRequest"
```

---

### Task 3：ForumCommentDTO 和 ForumCommentCreateRequest

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCommentDTO.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCommentCreateRequest.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/forum/ForumCommentDTOTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testForumCommentDTO() {
    ForumCommentDTO dto = new ForumCommentDTO(
        1L, 10L, 100L, "小明", 5L, 5L,
        "评论内容", 0, LocalDateTime.now()
    );
    
    assertEquals(1L, dto.id());
    assertEquals(10L, dto.postId());
    assertEquals("小明", dto.authorName());
    assertEquals(5L, dto.parentId());
}

@Test
void testForumCommentCreateRequest() {
    ForumCommentCreateRequest req = new ForumCommentCreateRequest(
        "评论内容", 5L, "访客"
    );
    
    assertEquals("评论内容", req.content());
    assertEquals(5L, req.parentId());
    assertEquals("访客", req.authorName());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentDTOTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 DTO 类**

```java
// ForumCommentDTO.java
package com.iaihub.toolbox.dto.forum;

import java.time.LocalDateTime;

public record ForumCommentDTO(
    Long id,
    Long postId,
    Long authorId,
    String authorName,
    Long parentId,
    Long rootId,
    String content,
    Integer likeCount,
    LocalDateTime createdAt
) {}

// ForumCommentCreateRequest.java
package com.iaihub.toolbox.dto.forum;

import jakarta.validation.constraints.NotBlank;

public record ForumCommentCreateRequest(
    @NotBlank String content,
    Long parentId,
    String authorName
) {}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentDTOTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCommentDTO.java
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumCommentCreateRequest.java
git commit -m "feat(forum): add ForumCommentDTO and ForumCommentCreateRequest"
```

---

### Task 4：ForumLikeRequest

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumLikeRequest.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/dto/forum/ForumLikeRequestTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testForumLikeRequestForPost() {
    ForumLikeRequest req = new ForumLikeRequest(1L, null);
    assertEquals(1L, req.postId());
    assertNull(req.commentId());
}

@Test
void testForumLikeRequestForComment() {
    ForumLikeRequest req = new ForumLikeRequest(null, 5L);
    assertNull(req.postId());
    assertEquals(5L, req.commentId());
}

@Test
void testMutualExclusion() {
    assertThrows(IllegalArgumentException.class, () -> {
        new ForumLikeRequest(1L, 5L); // 两者同时存在
    });
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumLikeRequestTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 DTO 类**

```java
package com.iaihub.toolbox.dto.forum;

public record ForumLikeRequest(
    Long postId,
    Long commentId
) {
    public ForumLikeRequest {
        if (postId == null && commentId == null) {
            throw new IllegalArgumentException("postId 和 commentId 至少需要一个");
        }
        if (postId != null && commentId != null) {
            throw new IllegalArgumentException("postId 和 commentId 只能选一个");
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumLikeRequestTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/forum/ForumLikeRequest.java
git commit -m "feat(forum): add ForumLikeRequest with mutual exclusion"
```

---

## 自检

- [x] 所有 DTO 已定义（7 个）
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径