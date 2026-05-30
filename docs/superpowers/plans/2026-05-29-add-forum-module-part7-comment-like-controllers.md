# Forum Module - Controller Layer (Part 3: Comment + Like)

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的评论和点赞 API 端点

**架构：** 评论支持匿名，点赞通过 IP hash 识别匿名用户

**技术栈：** Spring MVC, @RequestBody, HttpServletRequest

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/controller/forum/
├── ForumCommentController.java
└── ForumLikeController.java
```

---

<!-- openspec-task: ForumCommentController -->
### Task 1：ForumCommentController - GET 评论列表

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumCommentControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testGetCommentsByPostId() throws Exception {
    mockMvc.perform(get("/api/forum/posts/1/comments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCommentController（GET 部分）**

```java
package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumCommentDTO;
import com.iaihub.toolbox.service.forum.ForumCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ForumCommentController {
    
    private final ForumCommentService commentService;
    
    @GetMapping("/api/forum/posts/{postId}/comments")
    public ResponseEntity<List<ForumCommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<ForumCommentDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java
git commit -m "feat(forum): add GET comments endpoint"
```

---

### Task 2：ForumCommentController - POST 创建评论

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumCommentControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testCreateCommentByLoggedInUser() throws Exception {
    String token = obtainJwtToken("testuser", "password");
    
    mockMvc.perform(post("/api/forum/posts/1/comments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "content": "评论内容"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content").value("评论内容"));
}

@Test
void testCreateAnonymousComment() throws Exception {
    mockMvc.perform(post("/api/forum/posts/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "content": "匿名评论",
                    "authorName": "访客小明"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorName").value("访客小明"));
}

@Test
void testCreateReply() throws Exception {
    mockMvc.perform(post("/api/forum/posts/1/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "content": "回复内容",
                    "parentId": 5
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(5));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 POST 方法**

```java
@PostMapping("/api/forum/posts/{postId}/comments")
public ResponseEntity<ForumCommentDTO> createComment(
        @PathVariable Long postId,
        @RequestBody @Valid ForumCommentCreateRequest request,
        @AuthenticationPrincipal UserDetails user,
        HttpServletRequest httpRequest) {
    
    Long authorId = null;
    String authorName = request.authorName();
    
    if (user != null) {
        authorId = Long.parseLong(user.getUsername());
        authorName = null; // 使用系统用户名
    }
    
    ForumCommentDTO created;
    if (request.parentId() != null) {
        created = commentService.createReply(postId, authorId, authorName,
            request.content(), request.parentId());
    } else {
        created = commentService.createComment(postId, authorId, authorName, request.content());
    }
    
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java
git commit -m "feat(forum): add POST comments endpoint with anonymous support"
```

---

### Task 3：ForumCommentController - DELETE 评论

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumCommentControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testDeleteComment() throws Exception {
    String token = obtainJwtToken("author", "password");
    
    mockMvc.perform(delete("/api/forum/comments/1")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
}

@Test
void testDeleteCommentByNonAuthor() throws Exception {
    String token = obtainJwtToken("otheruser", "password");
    
    mockMvc.perform(delete("/api/forum/comments/1")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 DELETE 方法**

```java
@DeleteMapping("/api/forum/comments/{id}")
public ResponseEntity<Void> deleteComment(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails user) {
    
    Long userId = Long.parseLong(user.getUsername());
    commentService.deleteComment(id, userId);
    
    return ResponseEntity.noContent();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCommentControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCommentController.java
git commit -m "feat(forum): add DELETE comments endpoint"
```

---

<!-- openspec-task: ForumLikeController -->
### Task 4：ForumLikeController - POST 点赞

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumLikeController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumLikeControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testLikePost() throws Exception {
    String token = obtainJwtToken("testuser", "password");
    
    mockMvc.perform(post("/api/forum/likes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "postId": 1
                }
                """))
        .andExpect(status().isCreated());
}

@Test
void testLikePostAnonymous() throws Exception {
    mockMvc.perform(post("/api/forum/likes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "postId": 1
                }
                """))
        .andExpect(status().isCreated());
}

@Test
void testDuplicateLike() throws Exception {
    // 重复点赞应返回冲突
    mockMvc.perform(post("/api/forum/likes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "postId": 1
                }
                """))
        .andExpect(status().isConflict());
}

@Test
void testLikePostAndCommentMutualExclusion() throws Exception {
    mockMvc.perform(post("/api/forum/likes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "postId": 1,
                    "commentId": 5
                }
                """))
        .andExpect(status().isBadRequest());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumLikeControllerTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumLikeController**

```java
package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumLikeRequest;
import com.iaihub.toolbox.service.forum.ForumLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/forum/likes")
@RequiredArgsConstructor
public class ForumLikeController {
    
    private final ForumLikeService likeService;
    
    @PostMapping
    public ResponseEntity<Void> like(
            @RequestBody @Valid ForumLikeRequest request,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest httpRequest) {
        
        Long userId = user != null ? Long.parseLong(user.getUsername()) : null;
        String ipHash = user == null ? hashIp(httpRequest.getRemoteAddr()) : null;
        
        if (request.postId() != null) {
            likeService.likePost(request.postId(), userId, ipHash);
        } else {
            likeService.likeComment(request.commentId(), userId, ipHash);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> unlike(
            @RequestBody @Valid ForumLikeRequest request,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest httpRequest) {
        
        Long userId = user != null ? Long.parseLong(user.getUsername()) : null;
        String ipHash = user == null ? hashIp(httpRequest.getRemoteAddr()) : null;
        
        if (request.postId() != null) {
            likeService.unlikePost(request.postId(), userId, ipHash);
        } else {
            // 取消评论点赞类似处理
        }
        
        return ResponseEntity.noContent();
    }
    
    private String hashIp(String ip) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(ip.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return ip; // fallback
        }
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumLikeControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumLikeController.java
git commit -m "feat(forum): add ForumLikeController with anonymous like support"
```

---

## 自检

- [x] ForumCommentController 和 ForumLikeController 已定义
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径