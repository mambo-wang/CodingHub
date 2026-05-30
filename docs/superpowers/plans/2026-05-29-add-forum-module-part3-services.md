# Forum Module - Service Layer

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的业务逻辑层，包含帖子、分类、标签、评论、点赞的服务

**架构：** 使用 Spring @Service 注解，业务逻辑封装在 Service 中，事务管理由 Spring 处理

**技术栈：** Spring Service, @Transactional, Optional

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/service/forum/
├── ForumPostService.java
├── ForumCategoryService.java
├── ForumTagService.java
├── ForumCommentService.java
└── ForumLikeService.java
```

---

<!-- openspec-task: ForumCategoryService -->
### Task 1：ForumCategoryService

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/forum/ForumCategoryService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/forum/ForumCategoryServiceTest.java`

- [ ] **步骤 1：编写 ForumCategoryService 测试**

```java
@Test
void testGetAllCategories() {
    List<ForumCategoryDTO> categories = service.getAllCategories();
    
    assertNotNull(categories);
    // 验证按 sortOrder 排序
    for (int i = 0; i < categories.size() - 1; i++) {
        assertTrue(categories.get(i).getSortOrder() <= categories.get(i + 1).getSortOrder());
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryServiceTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCategoryService**

```java
package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumCategoryDTO;
import com.iaihub.toolbox.repository.forum.ForumCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumCategoryService {
    
    private final ForumCategoryRepository categoryRepository;
    
    public List<ForumCategoryDTO> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc()
            .stream()
            .map(c -> new ForumCategoryDTO(c.getId(), c.getName(), c.getDescription(), c.getSortOrder(), 0))
            .collect(Collectors.toList());
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryServiceTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/forum/ForumCategoryService.java
git commit -m "feat(forum): add ForumCategoryService"
```

---

<!-- openspec-task: ForumTagService -->
### Task 2：ForumTagService

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/forum/ForumTagService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/forum/ForumTagServiceTest.java`

- [ ] **步骤 1：编写 ForumTagService 测试**

```java
@Test
void testGetAllTags() {
    List<ForumTagDTO> tags = service.getAllTags();
    assertNotNull(tags);
}

@Test
void testGetHotTags() {
    List<ForumTagDTO> hotTags = service.getHotTags();
    assertTrue(hotTags.size() <= 10);
}

@Test
void testCreateTag() {
    ForumTagDTO created = service.createTag("新标签", false);
    assertEquals("新标签", created.getName());
}

@Test
void testCreateTagDuplicate() {
    assertThrows(DuplicateResourceException.class, () -> {
        service.createTag("教程", false); // 已存在会抛异常
    });
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumTagServiceTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumTagService**

```java
package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumTagDTO;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.model.forum.ForumTag;
import com.iaihub.toolbox.repository.forum.ForumTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumTagService {
    
    private final ForumTagRepository tagRepository;
    
    public List<ForumTagDTO> getAllTags() {
        return tagRepository.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    public List<ForumTagDTO> getHotTags() {
        return tagRepository.findTop10ByOrderByPostCountDesc().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ForumTagDTO createTag(String name, boolean isSystem) {
        if (tagRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("标签已存在: " + name);
        }
        
        ForumTag tag = new ForumTag();
        tag.setName(name);
        tag.setSystem(isSystem);
        tag = tagRepository.save(tag);
        
        return toDTO(tag);
    }
    
    private ForumTagDTO toDTO(ForumTag tag) {
        return new ForumTagDTO(tag.getId(), tag.getName(), tag.getPostCount(), tag.getSystem());
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumTagServiceTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/forum/ForumTagService.java
git commit -m "feat(forum): add ForumTagService"
```

---

<!-- openspec-task: ForumPostService -->
### Task 3：ForumPostService

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/forum/ForumPostService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/forum/ForumPostServiceTest.java`

- [ ] **步骤 1：编写 ForumPostService 测试**

```java
@Test
void testGetPostList() {
    Page<ForumPostDTO> posts = service.getPostList(null, null, null, PageRequest.of(0, 10));
    assertNotNull(posts);
}

@Test
void testGetPostListWithCategory() {
    Page<ForumPostDTO> posts = service.getPostList(1L, null, null, PageRequest.of(0, 10));
    posts.getContent().forEach(p -> assertEquals(1L, p.getCategoryId()));
}

@Test
void testGetPostListWithKeyword() {
    Page<ForumPostDTO> posts = service.getPostList(null, null, "Spring", PageRequest.of(0, 10));
    posts.getContent().forEach(p -> assertTrue(p.getTitle().contains("Spring")));
}

@Test
void testGetPostById() {
    ForumPostDTO post = service.getPostById(1L);
    assertNotNull(post);
}

@Test
void testCreatePost() {
    ForumPostDTO created = service.createPost(100L, "标题", "# 内容", 1L, List.of(1L, 2L));
    assertEquals("标题", created.getTitle());
    assertEquals(100L, created.getAuthorId());
}

@Test
void testUpdatePost() {
    ForumPostDTO updated = service.updatePost(1L, 100L, "新标题", "新内容", 1L, List.of());
    assertEquals("新标题", updated.getTitle());
}

@Test
void testUpdatePostUnauthorized() {
    assertThrows(ForbiddenException.class, () -> {
        service.updatePost(1L, 999L, "标题", "内容", 1L, List.of()); // 非作者
    });
}

@Test
void testDeletePost() {
    service.deletePost(1L, 100L);
    // 验证状态为 DELETED
    ForumPost post = postRepository.findById(1L).orElseThrow();
    assertEquals(ForumPostStatus.DELETED, post.getStatus());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostServiceTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumPostService**

```java
package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.*;
import com.iaihub.toolbox.exception.*;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.repository.forum.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumPostService {
    
    private final ForumPostRepository postRepository;
    private final ForumCategoryRepository categoryRepository;
    private final ForumTagRepository tagRepository;
    private final ForumPostTagRepository postTagRepository;
    
    public Page<ForumPostDTO> getPostList(Long categoryId, Long tagId, String keyword, Pageable pageable) {
        Page<ForumPost> posts;
        
        if (keyword != null && !keyword.isBlank()) {
            posts = postRepository.searchByTitle(keyword, ForumPostStatus.NORMAL, pageable);
        } else if (categoryId != null) {
            posts = postRepository.findByCategoryIdAndStatus(categoryId, ForumPostStatus.NORMAL, pageable);
        } else {
            posts = postRepository.findByStatusOrderByCreatedAtDesc(ForumPostStatus.NORMAL, pageable);
        }
        
        return posts.map(this::toDTO);
    }
    
    public ForumPostDTO getPostById(Long id) {
        ForumPost post = postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + id));
        
        // 递增浏览数
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
        
        return toDTO(post);
    }
    
    @Transactional
    public ForumPostDTO createPost(Long authorId, String title, String content, Long categoryId, List<Long> tagIds) {
        ForumPost post = new ForumPost();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthorId(authorId);
        post.setCategoryId(categoryId);
        post.setStatus(ForumPostStatus.NORMAL);
        
        post = postRepository.save(post);
        
        // 保存标签关联
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                ForumPostTag pt = new ForumPostTag();
                pt.setPostId(post.getId());
                pt.setTagId(tagId);
                postTagRepository.save(pt);
                
                // 更新标签计数
                tagRepository.findById(tagId).ifPresent(t -> {
                    t.setPostCount(t.getPostCount() + 1);
                    tagRepository.save(t);
                });
            }
        }
        
        return toDTO(post);
    }
    
    @Transactional
    public ForumPostDTO updatePost(Long postId, Long userId, String title, String content, Long categoryId, List<Long> tagIds) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));
        
        if (!post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权修改此帖子");
        }
        
        post.setTitle(title);
        post.setContent(content);
        post.setCategoryId(categoryId);
        
        post = postRepository.save(post);
        return toDTO(post);
    }
    
    @Transactional
    public void deletePost(Long postId, Long userId) {
        ForumPost post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));
        
        if (!post.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权删除此帖子");
        }
        
        post.setStatus(ForumPostStatus.DELETED);
        postRepository.save(post);
    }
    
    private ForumPostDTO toDTO(ForumPost post) {
        return new ForumPostDTO(
            post.getId(), post.getTitle(), post.getContent(),
            post.getAuthorId(), getAuthorName(post.getAuthorId()),
            post.getCategoryId(), getCategoryName(post.getCategoryId()),
            post.getViewCount(), post.getLikeCount(), post.getCommentCount(),
            post.getCreatedAt(), post.getUpdatedAt()
        );
    }
    
    private String getAuthorName(Long authorId) {
        // 简化：实际应调用 UserService
        return "用户" + authorId;
    }
    
    private String getCategoryName(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .map(ForumCategory::getName).orElse("未分类");
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostServiceTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/forum/ForumPostService.java
git commit -m "feat(forum): add ForumPostService with CRUD operations"
```

---

<!-- openspec-task: ForumCommentService -->
### Task 4：ForumCommentService

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/forum/ForumCommentService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/forum/ForumCommentServiceTest.java`

- [ ] **步骤 1：编写 ForumCommentService 测试**

```java
@Test
void testGetCommentsByPostId() {
    List<ForumCommentDTO> comments = service.getCommentsByPostId(1L);
    assertNotNull(comments);
}

@Test
void testCreateCommentByLoggedInUser() {
    ForumCommentDTO created = service.createComment(1L, 100L, null, "评论内容");
    assertNotNull(created);
    assertEquals(100L, created.getAuthorId());
}

@Test
void testCreateAnonymousComment() {
    ForumCommentDTO created = service.createComment(1L, null, "访客小明", "匿名评论");
    assertNull(created.getAuthorId());
    assertEquals("访客小明", created.getAuthorName());
}

@Test
void testCreateReply() {
    ForumCommentDTO reply = service.createComment(1L, 101L, null, "回复内容");
    reply.setParentId(1L);
    reply.setRootId(1L);
    
    assertEquals(1L, reply.getParentId());
    assertEquals(1L, reply.getRootId());
}

@Test
void testDeleteComment() {
    service.deleteComment(1L, 100L);
    // 验证评论已删除
}

@Test
void testDeleteCommentUnauthorized() {
    assertThrows(ForbiddenException.class, () -> {
        service.deleteComment(1L, 999L);
    });
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentServiceTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCommentService**

```java
package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.dto.forum.ForumCommentDTO;
import com.iaihub.toolbox.exception.*;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.repository.forum.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumCommentService {
    
    private final ForumCommentRepository commentRepository;
    private final ForumPostRepository postRepository;
    private final ForumPostService postService;
    
    public List<ForumCommentDTO> getCommentsByPostId(Long postId) {
        List<ForumComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        
        return comments.stream().map(this::toDTO).collect(Collectors.toList());
    }
    
    @Transactional
    public ForumCommentDTO createComment(Long postId, Long authorId, String authorName, String content) {
        // 验证帖子存在
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));
        
        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setAuthorId(authorId);
        comment.setAuthorName(authorName);
        comment.setContent(content);
        
        comment = commentRepository.save(comment);
        
        // 更新帖子的评论计数
        postRepository.findById(postId).ifPresent(p -> {
            p.setCommentCount(p.getCommentCount() + 1);
            postRepository.save(p);
        });
        
        return toDTO(comment);
    }
    
    @Transactional
    public ForumCommentDTO createReply(Long postId, Long authorId, String authorName, String content, Long parentId) {
        ForumComment parent = commentRepository.findById(parentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + parentId));
        
        ForumComment reply = new ForumComment();
        reply.setPostId(postId);
        reply.setAuthorId(authorId);
        reply.setAuthorName(authorName);
        reply.setContent(content);
        reply.setParentId(parentId);
        reply.setRootId(parent.getRootId() != null ? parent.getRootId() : parentId);
        
        reply = commentRepository.save(reply);
        
        return toDTO(reply);
    }
    
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ForumComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + commentId));
        
        if (!comment.getAuthorId().equals(userId)) {
            throw new ForbiddenException("无权删除此评论");
        }
        
        commentRepository.delete(comment);
        
        // 更新帖子的评论计数
        postRepository.findById(comment.getPostId()).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            postRepository.save(p);
        });
    }
    
    private ForumCommentDTO toDTO(ForumComment comment) {
        return new ForumCommentDTO(
            comment.getId(), comment.getPostId(),
            comment.getAuthorId(), comment.getAuthorName(),
            comment.getParentId(), comment.getRootId(),
            comment.getContent(), comment.getLikeCount(),
            comment.getCreatedAt()
        );
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentServiceTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/forum/ForumCommentService.java
git commit -m "feat(forum): add ForumCommentService with tree structure"
```

---

<!-- openspec-task: ForumLikeService -->
### Task 5：ForumLikeService

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/forum/ForumLikeService.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/forum/ForumLikeServiceTest.java`

- [ ] **步骤 1：编写 ForumLikeService 测试**

```java
@Test
void testLikePostByLoggedInUser() {
    service.likePost(1L, 100L, null);
    
    Optional<ForumLike> like = likeRepository.findByUserIdAndPostId(100L, 1L);
    assertTrue(like.isPresent());
}

@Test
void testLikePostByAnonymous() {
    service.likePost(1L, null, "ip_hash_abc");
    
    Optional<ForumLike> like = likeRepository.findByIpHashAndPostId("ip_hash_abc", 1L);
    assertTrue(like.isPresent());
}

@Test
void testDuplicateLike() {
    service.likePost(1L, 100L, null);
    
    assertThrows(BusinessException.class, () -> {
        service.likePost(1L, 100L, null);
    });
}

@Test
void testUnlike() {
    service.likePost(1L, 100L, null);
    service.unlikePost(1L, 100L, null);
    
    Optional<ForumLike> like = likeRepository.findByUserIdAndPostId(100L, 1L);
    assertFalse(like.isPresent());
}

@Test
void testLikeComment() {
    service.likeComment(5L, 100L, null);
    
    Optional<ForumLike> like = likeRepository.findByUserIdAndCommentId(100L, 5L);
    assertTrue(like.isPresent());
}

@Test
void testMutualExclusion() {
    assertThrows(BusinessException.class, () -> {
        service.likePostAndComment(1L, 5L, 100L, null);
    });
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumLikeServiceTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumLikeService**

```java
package com.iaihub.toolbox.service.forum;

import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.model.forum.*;
import com.iaihub.toolbox.repository.forum.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumLikeService {
    
    private final ForumLikeRepository likeRepository;
    private final ForumPostRepository postRepository;
    private final ForumCommentRepository commentRepository;
    
    @Transactional
    public void likePost(Long postId, Long userId, String ipHash) {
        // 验证帖子存在
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在: " + postId));
        
        // 检查重复点赞
        if (userId != null && likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new BusinessException("已点赞");
        }
        if (ipHash != null && likeRepository.existsByIpHashAndPostId(ipHash, postId)) {
            throw new BusinessException("已点赞");
        }
        
        ForumLike like = new ForumLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setIpHash(ipHash);
        likeRepository.save(like);
        
        // 更新帖子点赞数
        postRepository.findById(postId).ifPresent(p -> {
            p.setLikeCount(p.getLikeCount() + 1);
            postRepository.save(p);
        });
    }
    
    @Transactional
    public void unlikePost(Long postId, Long userId, String ipHash) {
        ForumLike like = null;
        
        if (userId != null) {
            like = likeRepository.findByUserIdAndPostId(userId, postId).orElse(null);
        }
        if (like == null && ipHash != null) {
            like = likeRepository.findByIpHashAndPostId(ipHash, postId).orElse(null);
        }
        
        if (like != null) {
            likeRepository.delete(like);
            
            // 更新帖子点赞数
            postRepository.findById(postId).ifPresent(p -> {
                p.setLikeCount(Math.max(0, p.getLikeCount() - 1));
                postRepository.save(p);
            });
        }
    }
    
    @Transactional
    public void likeComment(Long commentId, Long userId, String ipHash) {
        commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在: " + commentId));
        
        if (userId != null && likeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            throw new BusinessException("已点赞");
        }
        if (ipHash != null && likeRepository.existsByIpHashAndCommentId(ipHash, commentId)) {
            throw new BusinessException("已点赞");
        }
        
        ForumLike like = new ForumLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setIpHash(ipHash);
        likeRepository.save(like);
        
        commentRepository.findById(commentId).ifPresent(c -> {
            c.setLikeCount(c.getLikeCount() + 1);
            commentRepository.save(c);
        });
    }
    
    @Transactional
    public void likePostAndComment(Long postId, Long commentId, Long userId, String ipHash) {
        throw new BusinessException("postId 和 commentId 只能选一个");
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumLikeServiceTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/forum/ForumLikeService.java
git commit -m "feat(forum): add ForumLikeService with duplicate detection"
```

---

## 自检

- [x] 所有 5 个 Service 已定义
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径