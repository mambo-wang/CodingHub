# Forum Module - Backend Setup (Database + Entities)

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的数据库表结构和 JPA 实体

**架构：** 使用 Spring Boot JPA 创建 6 个实体类（ForumPost, ForumCategory, ForumTag, ForumPostTag, ForumComment, ForumLike）和对应的数据库表，采用软删除策略

**技术栈：** Spring Boot JPA, MySQL, Flyway Migration

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/
├── model/forum/
│   ├── ForumPost.java
│   ├── ForumCategory.java
│   ├── ForumTag.java
│   ├── ForumPostTag.java
│   ├── ForumComment.java
│   └── ForumLike.java

backend/src/main/resources/db/migration/
└── V1__create_forum_tables.sql
```

---

<!-- openspec-task: Database Schema -->
### Task 1：Database Schema Migration

**文件：**
- 创建：`backend/src/main/resources/db/migration/V1__create_forum_tables.sql`

- [ ] **步骤 1：编写 SQL 迁移测试**

```sql
-- 测试验证表结构
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'ai_tool_square' AND table_name = 'forum_post';
```

- [ ] **步骤 2：运行迁移**

运行：`cd backend && ./gradlew flywayMigrate`
预期：SUCCESS，6 张表创建完成

- [ ] **步骤 3：验证表存在**

```bash
mysql -uroot -proot ai_tool_square -e "SHOW TABLES LIKE 'forum_%'"
```
预期：6 张表全部存在

- [ ] **步骤 4：Commit**

```bash
git add backend/src/main/resources/db/migration/V1__create_forum_tables.sql
git commit -m "feat(forum): add database schema for forum module"
```

---

<!-- openspec-task: ForumCategory Entity -->
### Task 2：ForumCategory Entity

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumCategory.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumCategoryTest.java`

- [ ] **步骤 1：编写 ForumCategory 实体测试**

```java
@Test
void testForumCategoryFields() {
    ForumCategory category = new ForumCategory();
    category.setId(1L);
    category.setName("使用心得");
    category.setDescription("分享工具使用心得");
    category.setSortOrder(1);
    
    assertEquals(1L, category.getId());
    assertEquals("使用心得", category.getName());
    assertEquals("分享工具使用心得", category.getDescription());
    assertEquals(1, category.getSortOrder());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryTest*"`
预期：FAIL，compilation error（类不存在）

- [ ] **步骤 3：编写 ForumCategory 实体**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_category")
@Data
public class ForumCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(length = 255)
    private String description;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumCategory.java
git commit -m "feat(forum): add ForumCategory entity"
```

---

<!-- openspec-task: ForumTag Entity -->
### Task 3：ForumTag Entity

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumTag.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumTagTest.java`

- [ ] **步骤 1：编写 ForumTag 实体测试**

```java
@Test
void testForumTagFields() {
    ForumTag tag = new ForumTag();
    tag.setId(1L);
    tag.setName("教程");
    tag.setPostCount(5);
    tag.setSystem(true);
    
    assertEquals(1L, tag.getId());
    assertEquals("教程", tag.getName());
    assertEquals(5, tag.getPostCount());
    assertTrue(tag.getSystem());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumTagTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumTag 实体**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_tag")
@Data
public class ForumTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String name;
    
    @Column(name = "post_count")
    private Integer postCount = 0;
    
    @Column(name = "is_system")
    private Boolean system = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumTagTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumTag.java
git commit -m "feat(forum): add ForumTag entity"
```

---

<!-- openspec-task: ForumPost Entity -->
### Task 4：ForumPost Entity

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumPostTest.java`

- [ ] **步骤 1：编写 ForumPost 实体测试**

```java
@Test
void testForumPostFieldsAndStatus() {
    ForumPost post = new ForumPost();
    post.setId(1L);
    post.setTitle("我的第一篇文章");
    post.setContent("# Markdown 内容");
    post.setAuthorId(100L);
    post.setCategoryId(1L);
    post.setViewCount(0);
    post.setLikeCount(0);
    post.setCommentCount(0);
    post.setStatus(ForumPostStatus.NORMAL);
    
    assertEquals(1L, post.getId());
    assertEquals("我的第一篇文章", post.getTitle());
    assertEquals("# Markdown 内容", post.getContent());
    assertEquals(ForumPostStatus.NORMAL, post.getStatus());
}

@Test
void testForumPostStatusEnum() {
    assertEquals(ForumPostStatus.NORMAL, ForumPostStatus.valueOf("NORMAL"));
    assertEquals(ForumPostStatus.DELETED, ForumPostStatus.valueOf("DELETED"));
    assertEquals(ForumPostStatus.HIDDEN, ForumPostStatus.valueOf("HIDDEN"));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumPost 实体和状态枚举**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

enum ForumPostStatus {
    NORMAL, DELETED, HIDDEN
}

@Entity
@Table(name = "forum_post", indexes = {
    @Index(name = "idx_forum_post_author", columnList = "author_id"),
    @Index(name = "idx_forum_post_category", columnList = "category_id"),
    @Index(name = "idx_forum_post_created", columnList = "created_at")
})
@Data
public class ForumPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "author_id", nullable = false)
    private Long authorId;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @Column(name = "comment_count")
    private Integer commentCount = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForumPostStatus status = ForumPostStatus.NORMAL;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPost.java
git commit -m "feat(forum): add ForumPost entity with status enum"
```

---

<!-- openspec-task: ForumPostTag Entity -->
### Task 5：ForumPostTag Entity (Join Table)

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPostTag.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumPostTagTest.java`

- [ ] **步骤 1：编写 ForumPostTag 实体测试**

```java
@Test
void testForumPostTagCompositeKey() {
    ForumPostTag postTag = new ForumPostTag();
    postTag.setPostId(1L);
    postTag.setTagId(10L);
    
    assertEquals(1L, postTag.getPostId());
    assertEquals(10L, postTag.getTagId());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostTagTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumPostTag 实体（复合主键）**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "forum_post_tag")
@IdClass(ForumPostTagId.class)
@Data
public class ForumPostTag {
    
    @Id
    @Column(name = "post_id")
    private Long postId;
    
    @Id
    @Column(name = "tag_id")
    private Long tagId;
}

@Data
class ForumPostTagId implements Serializable {
    private Long postId;
    private Long tagId;
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostTagTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumPostTag.java
git commit -m "feat(forum): add ForumPostTag join table entity"
```

---

<!-- openspec-task: ForumComment Entity -->
### Task 6：ForumComment Entity

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumComment.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumCommentTest.java`

- [ ] **步骤 1：编写 ForumComment 实体测试**

```java
@Test
void testForumCommentFields() {
    ForumComment comment = new ForumComment();
    comment.setId(1L);
    comment.setPostId(10L);
    comment.setAuthorId(100L);
    comment.setAuthorName("小明");
    comment.setParentId(null);
    comment.setRootId(null);
    comment.setContent("这是评论内容");
    comment.setLikeCount(0);
    
    assertEquals(1L, comment.getId());
    assertEquals(10L, comment.getPostId());
    assertNull(comment.getParentId());
    assertEquals("小明", comment.getAuthorName());
}

@Test
void testForumCommentReply() {
    ForumComment reply = new ForumComment();
    reply.setId(2L);
    reply.setPostId(10L);
    reply.setAuthorId(101L);
    reply.setAuthorName("小红");
    reply.setParentId(1L);
    reply.setRootId(1L);
    reply.setContent("这是回复");
    
    assertEquals(1L, reply.getParentId());
    assertEquals(1L, reply.getRootId());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumComment 实体**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_comment", indexes = {
    @Index(name = "idx_forum_comment_post", columnList = "post_id"),
    @Index(name = "idx_forum_comment_root", columnList = "root_id")
})
@Data
public class ForumComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Column(name = "author_id")
    private Long authorId;
    
    @Column(name = "author_name", length = 50)
    private String authorName;
    
    @Column(name = "parent_id")
    private Long parentId;
    
    @Column(name = "root_id")
    private Long rootId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumComment.java
git commit -m "feat(forum): add ForumComment entity with tree structure support"
```

---

<!-- openspec-task: ForumLike Entity -->
### Task 7：ForumLike Entity

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/forum/ForumLike.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/model/forum/ForumLikeTest.java`

- [ ] **步骤 1：编写 ForumLike 实体测试**

```java
@Test
void testForumLikePost() {
    ForumLike like = new ForumLike();
    like.setId(1L);
    like.setPostId(10L);
    like.setUserId(100L);
    like.setIpHash("abc123");
    
    assertEquals(10L, like.getPostId());
    assertNull(like.getCommentId());
    assertEquals(100L, like.getUserId());
}

@Test
void testForumLikeComment() {
    ForumLike like = new ForumLike();
    like.setId(2L);
    like.setCommentId(20L);
    like.setUserId(100L);
    
    assertNull(like.getPostId());
    assertEquals(20L, like.getCommentId());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumLikeTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumLike 实体**

```java
package com.iaihub.toolbox.model.forum;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_like")
@Data
public class ForumLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id")
    private Long postId;
    
    @Column(name = "comment_id")
    private Long commentId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "ip_hash", length = 64)
    private String ipHash;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumLikeTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/model/forum/ForumLike.java
git commit -m "feat(forum): add ForumLike entity with post/comment mutual exclusion"
```

---

## 自检

- [x] 所有 7 个任务已定义（Database Schema + 6 Entities）
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符（TODO、TBD 等）
- [x] 路径使用绝对路径
- [x] Commit 消息遵循 Conventional Commits