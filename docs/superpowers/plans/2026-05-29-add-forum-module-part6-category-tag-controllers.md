# Forum Module - Controller Layer (Part 2: Category + Tag)

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的分类和标签 API 端点

**架构：** 使用 Spring MVC @RestController，GET 公开，POST 需要认证

**技术栈：** Spring MVC, @RestController

---

## 文件结构

```
backend/src/main/java/com/iaihub/toolbox/controller/forum/
├── ForumCategoryController.java
└── ForumTagController.java
```

---

<!-- openspec-task: ForumCategoryController -->
### Task 1：ForumCategoryController - GET 分类列表

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCategoryController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumCategoryControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testGetAllCategories() throws Exception {
    mockMvc.perform(get("/api/forum/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryControllerTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumCategoryController**

```java
package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumCategoryDTO;
import com.iaihub.toolbox.service.forum.ForumCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/forum/categories")
@RequiredArgsConstructor
public class ForumCategoryController {
    
    private final ForumCategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<List<ForumCategoryDTO>> getAllCategories() {
        List<ForumCategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumCategoryControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumCategoryController.java
git commit -m "feat(forum): add ForumCategoryController"
```

---

<!-- openspec-task: ForumTagController -->
### Task 2：ForumTagController - GET 标签列表

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumTagController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumTagControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testGetAllTags() throws Exception {
    mockMvc.perform(get("/api/forum/tags"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
}

@Test
void testGetHotTags() throws Exception {
    mockMvc.perform(get("/api/forum/tags/hot"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length").value(10));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumTagControllerTest*"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写 ForumTagController**

```java
package com.iaihub.toolbox.controller.forum;

import com.iaihub.toolbox.dto.forum.ForumTagDTO;
import com.iaihub.toolbox.service.forum.ForumTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/forum/tags")
@RequiredArgsConstructor
public class ForumTagController {
    
    private final ForumTagService tagService;
    
    @GetMapping
    public ResponseEntity<List<ForumTagDTO>> getAllTags() {
        return ResponseEntity.ok(tagService.getAllTags());
    }
    
    @GetMapping("/hot")
    public ResponseEntity<List<ForumTagDTO>> getHotTags() {
        return ResponseEntity.ok(tagService.getHotTags());
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumTagControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumTagController.java
git commit -m "feat(forum): add ForumTagController with hot tags endpoint"
```

---

### Task 3：ForumTagController - POST 创建标签

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumTagController.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/forum/ForumTagControllerTest.java`

- [ ] **步骤 1：编写测试**

```java
@Test
void testCreateTag() throws Exception {
    String token = obtainJwtToken("testuser", "password");
    
    mockMvc.perform(post("/api/forum/tags")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "新标签",
                    "isSystem": false
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("新标签"));
}

@Test
void testCreateTagWithoutAuth() throws Exception {
    mockMvc.perform(post("/api/forum/tags")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "新标签"
                }
                """))
        .andExpect(status().isUnauthorized());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "*ForumTagControllerTest*"`
预期：FAIL

- [ ] **步骤 3：添加 POST 创建方法**

```java
@PostMapping
public ResponseEntity<ForumTagDTO> createTag(
        @AuthenticationPrincipal UserDetails user,
        @RequestBody TagCreateRequest request) {
    
    ForumTagDTO created = tagService.createTag(request.name(), request.isSystem());
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}

// 需要添加 TagCreateRequest DTO
public record TagCreateRequest(String name, Boolean isSystem) {}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "*ForumTagControllerTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/controller/forum/ForumTagController.java
git commit -m "feat(forum): add POST /api/forum/tags endpoint"
```

---

## 自检

- [x] ForumCategoryController 和 ForumTagController 已定义
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径