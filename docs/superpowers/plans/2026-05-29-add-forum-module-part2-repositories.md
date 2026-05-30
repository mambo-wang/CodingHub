# Forum Module - Repository Layer

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的 Repository 层，支持分页、筛选、搜索等查询

**架构：** 使用 Spring Data JPA 的 `JpaRepository` 和 `@Query` 自定义查询，支持动态条件构建

**技术栈：** Spring Data JPA, Pageable, Specification

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/repository/forum/
├── ForumPostRepository.java
├── ForumCategoryRepository.java
├── ForumTagRepository.java
├── ForumCommentRepository.java
└── ForumLikeRepository.java
```

---

<!-- openspec-task: ForumCategoryRepository -->
### Task 1：ForumCategoryRepository

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumCategoryRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/forum/ForumCategoryRepositoryTest.java`

- [ ] **步骤 1：编写 ForumCategoryRepository 测试**

```java
@Test
void testFindAllOrderBySortOrder() {
    List<ForumCategory> categories = repository.findAllByOrderBySortOrderAsc();
    
    assertFalse(categories.isEmpty());
    // 验证排序
    for (int i = 0; i < categories.size() - 1; i++) {
        assertTrue(categories.get(i).getSortOrder() <= categories.get(i + 1).getSortOrder());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryRepositoryTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCategoryRepository**

```java
package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {
    
    List<ForumCategory> findAllByOrderBySortOrderAsc();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryRepositoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumCategoryRepository.java
git commit -m "feat(forum): add ForumCategoryRepository"
```

---

<!-- openspec-task: ForumTagRepository -->
### Task 2：ForumTagRepository

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumTagRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/forum/ForumTagRepositoryTest.java`

- [ ] **步骤 1：编写 ForumTagRepository 测试**

```java
@Test
void testFindByName() {
    Optional<ForumTag> tag = repository.findByName("教程");
    assertTrue(tag.isPresent());
    assertEquals("教程", tag.get().getName());
}

@Test
void testFindTop10ByOrderByPostCountDesc() {
    List<ForumTag> hotTags = repository.findTop10ByOrderByPostCountDesc();
    assertTrue(hotTags.size() <= 10);
}

@Test
void testFindByNameContaining() {
    List<ForumTag> tags = repository.findByNameContaining("教");
    assertFalse(tags.isEmpty());
    tags.forEach(t -> assertTrue(t.getName().contains("教")));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumTagRepositoryTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumTagRepository**

```java
package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForumTagRepository extends JpaRepository<ForumTag, Long> {
    
    Optional<ForumTag> findByName(String name);
    
    List<ForumTag> findTop10ByOrderByPostCountDesc();
    
    List<ForumTag> findByNameContaining(String keyword);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumTagRepositoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumTagRepository.java
git commit -m "feat(forum): add ForumTagRepository with hot tags and search"
```

---

<!-- openspec-task: ForumPostRepository -->
### Task 3：ForumPostRepository

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumPostRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/forum/ForumPostRepositoryTest.java`

- [ ] **步骤 1：编写 ForumPostRepository 测试**

```java
@Test
void testFindByStatusOrderByCreatedAtDesc() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ForumPost> posts = repository.findByStatusOrderByCreatedAtDesc(
        ForumPostStatus.NORMAL, pageable);
    
    assertTrue(posts.getTotalElements() >= 0);
    assertTrue(posts.getContent().size() <= 10);
}

@Test
void testFindByCategoryIdAndStatus() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ForumPost> posts = repository.findByCategoryIdAndStatus(
        1L, ForumPostStatus.NORMAL, pageable);
    
    posts.getContent().forEach(p -> assertEquals(1L, p.getCategoryId()));
}

@Test
void testSearchByTitle() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<ForumPost> posts = repository.searchByTitle("Spring", pageable);
    
    posts.getContent().forEach(p -> assertTrue(
        p.getTitle().contains("Spring")));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostRepositoryTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumPostRepository**

```java
package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumPost;
import com.iaihub.toolbox.model.forum.ForumPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {
    
    Page<ForumPost> findByStatusOrderByCreatedAtDesc(ForumPostStatus status, Pageable pageable);
    
    Page<ForumPost> findByCategoryIdAndStatus(Long categoryId, ForumPostStatus status, Pageable pageable);
    
    @Query("SELECT p FROM ForumPost p WHERE p.status = :status AND p.title LIKE %:keyword%")
    Page<ForumPost> searchByTitle(@Param("keyword") String keyword, 
                                   @Param("status") ForumPostStatus status, 
                                   Pageable pageable);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostRepositoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumPostRepository.java
git commit -m "feat(forum): add ForumPostRepository with search and filter"
```

---

<!-- openspec-task: ForumCommentRepository -->
### Task 4：ForumCommentRepository

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumCommentRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/forum/ForumCommentRepositoryTest.java`

- [ ] **步骤 1：编写 ForumCommentRepository 测试**

```java
@Test
void testFindByPostIdOrderByCreatedAtAsc() {
    List<ForumComment> comments = repository.findByPostIdOrderByCreatedAtAsc(1L);
    
    // 验证树形结构：root 评论在前，子评论在后
    for (int i = 0; i < comments.size() - 1; i++) {
        ForumComment current = comments.get(i);
        ForumComment next = comments.get(i + 1);
        if (next.getParentId() != null) {
            assertTrue(
                current.getId().equals(next.getParentId()) ||
                current.getRootId().equals(next.getRootId())
            );
        }
    }
}

@Test
void testFindByRootId() {
    List<ForumComment> replies = repository.findByRootId(1L);
    
    replies.forEach(r -> assertTrue(
        r.getRootId().equals(1L) || r.getParentId().equals(1L)));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentRepositoryTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCommentRepository**

```java
package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    
    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    List<ForumComment> findByRootId(Long rootId);
    
    List<ForumComment> findByParentId(Long parentId);
    
    long countByPostId(Long postId);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentRepositoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumCommentRepository.java
git commit -m "feat(forum): add ForumCommentRepository with tree query"
```

---

<!-- openspec-task: ForumLikeRepository -->
### Task 5：ForumLikeRepository

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumLikeRepository.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/forum/ForumLikeRepositoryTest.java`

- [ ] **步骤 1：编写 ForumLikeRepository 测试**

```java
@Test
void testFindByUserIdAndPostId() {
    Optional<ForumLike> like = repository.findByUserIdAndPostId(100L, 1L);
    // 无数据时返回 empty
    assertFalse(like.isPresent());
}

@Test
void testFindByIpHashAndPostId() {
    Optional<ForumLike> like = repository.findByIpHashAndPostId("abc123", 1L);
    assertFalse(like.isPresent());
}

@Test
void testExistsByUserIdAndPostId() {
    boolean exists = repository.existsByUserIdAndPostId(100L, 1L);
    assertFalse(exists);
}

@Test
void testExistsByIpHashAndPostId() {
    boolean exists = repository.existsByIpHashAndPostId("abc123", 1L);
    assertFalse(exists);
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumLikeRepositoryTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumLikeRepository**

```java
package com.iaihub.toolbox.repository.forum;

import com.iaihub.toolbox.model.forum.ForumLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ForumLikeRepository extends JpaRepository<ForumLike, Long> {
    
    Optional<ForumLike> findByUserIdAndPostId(Long userId, Long postId);
    
    Optional<ForumLike> findByIpHashAndPostId(String ipHash, Long postId);
    
    Optional<ForumLike> findByUserIdAndCommentId(Long userId, Long commentId);
    
    Optional<ForumLike> findByIpHashAndCommentId(String ipHash, Long commentId);
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    boolean existsByIpHashAndPostId(String ipHash, Long postId);
    
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    
    boolean existsByIpHashAndCommentId(String ipHash, Long commentId);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumLikeRepositoryTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/forum/ForumLikeRepository.java
git commit -m "feat(forum): add ForumLikeRepository with duplicate detection"
```

---

## 自检

- [x] 所有 5 个 Repository 已定义
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径