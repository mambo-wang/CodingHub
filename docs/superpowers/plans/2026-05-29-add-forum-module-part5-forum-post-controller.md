# Forum Module - Controller Layer (Part 1: ForumPostController)

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的帖子相关 API 端点

**架构：** 使用 Spring MVC @RestController，JWT 认证通过 SecurityConfig 配置

**技术栈：** Spring MVC, @RestController, @RequestMapping, Pageable

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/controller/forum/
└── ForumPostController.java
```

---

<!-- openspec-task: ForumPostController -->
### Task 1：ForumPostController - GET 帖子列表

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumPostControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testGetPostList() throws Exception {
    mockMvc.perform(get("/api/forum/posts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").exists());
}

@Test
void testGetPostListWithPagination() throws Exception {
    mockMvc.perform(get("/api/forum/posts")
            .param("page", "0")
            .param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(5));
}

@Test
void testGetPostListWithCategoryFilter() throws Exception {
    mockMvc.perform(get("/api/forum/posts")
            .param("category", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].categoryId").value(1));
}

@Test
void testGetPostListWithKeywordSearch() throws Exception {
    mockMvc.perform(get("/api/forum/posts")
            .param("keyword", "Spring"))
        .andExpect(status().isOk());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumPostController（GET 列表）**

```java
package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumPostDTO;
import com.iaihub.toolbox.service.forum.ForumPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
public class ForumPostController {
    
    private final ForumPostService postService;
    
    @GetMapping
    public ResponseEntity<Page<ForumPostDTO>> getPostList(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long tag,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ForumPostDTO> posts = postService.getPostList(category, tag, keyword, pageable);
        
        return ResponseEntity.ok(posts);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java
git commit -m "feat(forum): add GET /api/forum/posts endpoint"
```

---

### Task 2：ForumPostController - GET 帖子详情

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumPostControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testGetPostById() throws Exception {
    mockMvc.perform(get("/api/forum/posts/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.content").exists());
}

@Test
void testGetPostNotFound() throws Exception {
    mockMvc.perform(get("/api/forum/posts/999"))
        .andExpect(status().isNotFound());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 GET 详情方法**

```java
@GetMapping("/{id}")
public ResponseEntity<ForumPostDTO> getPostById(@PathVariable Long id) {
    ForumPostDTO post = postService.getPostById(id);
    return ResponseEntity.ok(post);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java
git commit -m "feat(forum): add GET /api/forum/posts/{id} endpoint"
```

---

### Task 3：ForumPostController - POST 创建帖子

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumPostControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testCreatePost() throws Exception {
    String token = obtainJwtToken("testuser", "password");
    
    mockMvc.perform(post("/api/forum/posts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "新帖子",
                    "content": "# 内容",
                    "categoryId": 1,
                    "tagIds": [1, 2]
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("新帖子"));
}

@Test
void testCreatePostWithoutAuth() throws Exception {
    mockMvc.perform(post("/api/forum/posts")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "新帖子",
                    "content": "# 内容",
                    "categoryId": 1
                }
                """))
        .andExpect(status().isUnauthorized());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 POST 创建方法**

```java
@PostMapping
public ResponseEntity<ForumPostDTO> createPost(
        @AuthenticationPrincipal UserDetails user,
        @RequestBody @Valid ForumPostCreateRequest request) {
    
    Long authorId = Long.parseLong(user.getUsername());
    ForumPostDTO created = postService.createPost(
        authorId, request.title(), request.content(),
        request.categoryId(), request.tagIds()
    );
    
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java
git commit -m "feat(forum): add POST /api/forum/posts endpoint"
```

---

### Task 4：ForumPostController - PUT 更新帖子

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumPostControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testUpdatePost() throws Exception {
    String token = obtainJwtToken("author", "password");
    
    mockMvc.perform(put("/api/forum/posts/1")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "更新标题",
                    "content": "更新内容",
                    "categoryId": 1,
                    "tagIds": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("更新标题"));
}

@Test
void testUpdatePostByNonAuthor() throws Exception {
    String token = obtainJwtToken("otheruser", "password");
    
    mockMvc.perform(put("/api/forum/posts/1")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "更新标题",
                    "content": "更新内容",
                    "categoryId": 1,
                    "tagIds": []
                }
                """))
        .andExpect(status().isForbidden());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 PUT 更新方法**

```java
@PutMapping("/{id}")
public ResponseEntity<ForumPostDTO> updatePost(
        @AuthenticationPrincipal UserDetails user,
        @PathVariable Long id,
        @RequestBody @Valid ForumPostCreateRequest request) {
    
    Long userId = Long.parseLong(user.getUsername());
    ForumPostDTO updated = postService.updatePost(
        id, userId, request.title(), request.content(),
        request.categoryId(), request.tagIds()
    );
    
    return ResponseEntity.ok(updated);
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java
git commit -m "feat(forum): add PUT /api/forum/posts/{id} endpoint"
```

---

### Task 5：ForumPostController - DELETE 删除帖子

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumPostControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testDeletePost() throws Exception {
    String token = obtainJwtToken("author", "password");
    
    mockMvc.perform(delete("/api/forum/posts/1")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
}

@Test
void testDeletePostByNonAuthor() throws Exception {
    String token = obtainJwtToken("otheruser", "password");
    
    mockMvc.perform(delete("/api/forum/posts/1")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 DELETE 方法**

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> deletePost(
        @AuthenticationPrincipal UserDetails user,
        @PathVariable Long id) {
    
    Long userId = Long.parseLong(user.getUsername());
    postService.deletePost(id, userId);
    
    return ResponseEntity.noContent();
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumPostControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumPostController.java
git commit -m "feat(forum): add DELETE /api/forum/posts/{id} endpoint"
```

---

## 自检

- [x] ForumPostController 完整 CRUD 已定义（5 个任务）
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径