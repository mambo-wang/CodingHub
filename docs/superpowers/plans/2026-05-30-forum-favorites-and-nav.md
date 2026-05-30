# 论坛收藏与导航功能实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 为论坛模块添加收藏功能、优化顶部菜单栏、添加左侧导航栏

**架构：** 后端新增 PostFavorite 实体和 API，前端在帖子卡片/详情页添加收藏按钮，在论坛列表页添加左侧导航栏（我的帖子/我的收藏），优化顶部菜单栏移除上传工具按钮

**技术栈：** Java 17 + Spring Boot 3.2.5（后端），Vue 3 + TypeScript + Vitest（前端）

---

## 文件结构

### Backend（Java 17 + Spring Boot）
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/PostFavorite.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/PostFavoriteRepository.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/PostFavoriteService.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/PostFavoriteController.java`
- 创建：`backend/src/test/java/com/iaihub/toolbox/repository/PostFavoriteRepositoryTest.java`
- 创建：`backend/src/test/java/com/iaihub/toolbox/service/PostFavoriteServiceTest.java`
- 创建：`backend/src/test/java/com/iaihub/toolbox/controller/PostFavoriteControllerTest.java`

### Frontend（Vue 3 + TypeScript）
- 修改：`frontend/src/components/AppHeader.vue`
- 修改：`frontend/src/components/forum/PostCard.vue`
- 修改：`frontend/src/components/forum/PostDetailPage.vue`
- 修改：`frontend/src/pages/forum/PostListPage.vue`
- 修改：`frontend/src/services/api.ts`
- 创建：`frontend/src/pages/forum/MyPostsPage.vue`
- 创建：`frontend/src/pages/forum/MyFavoritesPage.vue`
- 修改：`frontend/src/router/index.ts`
- 创建：`frontend/src/components/forum/PostCard.spec.ts`
- 创建：`frontend/src/pages/forum/PostDetailPage.spec.ts`
- 创建：`frontend/src/pages/forum/PostListPage.spec.ts`
- 创建：`frontend/src/pages/forum/MyPostsPage.spec.ts`
- 创建：`frontend/src/pages/forum/MyFavoritesPage.spec.ts`
- 创建：`frontend/src/components/AppHeader.spec.ts`
- 创建：`frontend/src/router.spec.ts`
- 创建：`frontend/src/services/api.spec.ts`

---

<!-- openspec-task: RED: 编写 PostFavoriteRepositoryTest -->
### 任务 1：PostFavorite 实体与 Repository（TDD）

**文件：**
- 测试：`backend/src/test/java/com/iaihub/toolbox/repository/PostFavoriteRepositoryTest.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/PostFavorite.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/PostFavoriteRepository.java`

- [x] **RED: 编写失败的测试**

```java
package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.PostFavorite;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostFavoriteRepositoryTest {

    @Autowired
    private PostFavoriteRepository repository;

    @Test
    void testSaveFavorite() {
        PostFavorite favorite = new PostFavorite();
        favorite.setUserId(1L);
        favorite.setPostId(100L);
        PostFavorite saved = repository.save(favorite);
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getUserId());
        assertEquals(100L, saved.getPostId());
    }

    @Test
    void testUniqueIndexConstraint() {
        PostFavorite favorite1 = new PostFavorite();
        favorite1.setUserId(1L);
        favorite1.setPostId(100L);
        repository.save(favorite1);

        PostFavorite favorite2 = new PostFavorite();
        favorite2.setUserId(1L);
        favorite2.setPostId(100L);
        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(favorite2);
            repository.flush();
        });
    }

    @Test
    void testFindByUserIdAndPostId() {
        PostFavorite favorite = new PostFavorite();
        favorite.setUserId(1L);
        favorite.setPostId(100L);
        repository.save(favorite);

        Optional<PostFavorite> found = repository.findByUserIdAndPostId(1L, 100L);
        assertTrue(found.isPresent());
    }

    @Test
    void testFindByUserId() {
        PostFavorite fav1 = new PostFavorite();
        fav1.setUserId(1L);
        fav1.setPostId(100L);
        repository.save(fav1);

        PostFavorite fav2 = new PostFavorite();
        fav2.setUserId(1L);
        fav2.setPostId(200L);
        repository.save(fav2);

        var favorites = repository.findByUserId(1L);
        assertEquals(2, favorites.size());
    }

    @Test
    void testDeleteByUserIdAndPostId() {
        PostFavorite favorite = new PostFavorite();
        favorite.setUserId(1L);
        favorite.setPostId(100L);
        repository.save(favorite);

        repository.deleteByUserIdAndPostId(1L, 100L);
        Optional<PostFavorite> found = repository.findByUserIdAndPostId(1L, 100L);
        assertFalse(found.isPresent());
    }
}
```

- [x] **GREEN: 实现 PostFavorite 实体**

```java
package com.iaihub.toolbox.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_favorites",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [x] **GREEN: 实现 PostFavoriteRepository**

```java
package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {

    Optional<PostFavorite> findByUserIdAndPostId(Long userId, Long postId);

    List<PostFavorite> findByUserId(Long userId);

    void deleteByUserIdAndPostId(Long userId, Long postId);
}
```

- [x] **运行测试验证**

运行：`cd backend && ./gradlew test --tests "*PostFavoriteRepositoryTest" -v`
预期：PASS

- [x] **Commit**

```bash
cd backend
git add src/test/java/com/iaihub/toolbox/repository/PostFavoriteRepositoryTest.java
git add src/main/java/com/iaihub/toolbox/model/PostFavorite.java
git add src/main/java/com/iaihub/toolbox/repository/PostFavoriteRepository.java
git commit -m "feat: add PostFavorite entity and repository with unique constraint"
```

---

<!-- openspec-task: RED: 编写 PostFavoriteServiceTest -->
### 任务 2：PostFavorite Service（TDD）

**文件：**
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/PostFavoriteServiceTest.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/service/PostFavoriteService.java`

- [x] **RED: 编写失败的测试**

```java
package com.iaihub.toolbox.service;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.repository.PostFavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostFavoriteServiceTest {

    @Mock
    private PostFavoriteRepository repository;

    @InjectMocks
    private PostFavoriteService service;

    private Long userId = 1L;
    private Long postId = 100L;

    @Test
    void testAddFavorite_Success() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        when(repository.save(any(PostFavorite.class))).thenAnswer(inv -> {
            PostFavorite fav = inv.getArgument(0);
            fav.setId(1L);
            return fav;
        });

        PostFavorite result = service.addFavorite(userId, postId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(repository).save(any(PostFavorite.class));
    }

    @Test
    void testAddFavorite_AlreadyFavorited() {
        PostFavorite existing = new PostFavorite();
        existing.setId(1L);
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(existing));

        PostFavorite result = service.addFavorite(userId, postId);
        assertEquals(existing.getId(), result.getId());
        verify(repository, never()).save(any(PostFavorite.class));
    }

    @Test
    void testRemoveFavorite_Success() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(new PostFavorite()));
        doNothing().when(repository).deleteByUserIdAndPostId(userId, postId);

        boolean result = service.removeFavorite(userId, postId);
        assertTrue(result);
        verify(repository).deleteByUserIdAndPostId(userId, postId);
    }

    @Test
    void testRemoveFavorite_NotFound() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        boolean result = service.removeFavorite(userId, postId);
        assertFalse(result);
    }

    @Test
    void testGetUserFavorites() {
        PostFavorite fav1 = new PostFavorite();
        fav1.setId(1L);
        PostFavorite fav2 = new PostFavorite();
        fav2.setId(2L);
        when(repository.findByUserId(userId)).thenReturn(Arrays.asList(fav1, fav2));

        List<PostFavorite> result = service.getUserFavorites(userId);
        assertEquals(2, result.size());
    }

    @Test
    void testIsFavorited() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(new PostFavorite()));
        assertTrue(service.isFavorited(userId, postId));
    }

    @Test
    void testIsNotFavorited() {
        when(repository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        assertFalse(service.isFavorited(userId, postId));
    }
}
```

- [x] **GREEN: 实现 PostFavoriteService**

```java
package com.iaihub.toolbox.service;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.repository.PostFavoriteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostFavoriteService {

    private final PostFavoriteRepository repository;

    public PostFavoriteService(PostFavoriteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PostFavorite addFavorite(Long userId, Long postId) {
        Optional<PostFavorite> existing = repository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            return existing.get();
        }
        PostFavorite favorite = new PostFavorite();
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        return repository.save(favorite);
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long postId) {
        Optional<PostFavorite> existing = repository.findByUserIdAndPostId(userId, postId);
        if (existing.isEmpty()) {
            return false;
        }
        repository.deleteByUserIdAndPostId(userId, postId);
        return true;
    }

    public List<PostFavorite> getUserFavorites(Long userId) {
        return repository.findByUserId(userId);
    }

    public boolean isFavorited(Long userId, Long postId) {
        return repository.findByUserIdAndPostId(userId, postId).isPresent();
    }
}
```

- [x] **运行测试验证**

运行：`cd backend && ./gradlew test --tests "*PostFavoriteServiceTest" -v`
预期：PASS

- [x] **Commit**

```bash
cd backend
git add src/test/java/com/iaihub/toolbox/service/PostFavoriteServiceTest.java
git add src/main/java/com/iaihub/toolbox/service/PostFavoriteService.java
git commit -m "feat: add PostFavoriteService with business logic"
```

---

<!-- openspec-task: RED: 编写 PostFavoriteControllerTest -->
### 任务 3：PostFavorite Controller（TDD）

**文件：**
- 测试：`backend/src/test/java/com/iaihub/toolbox/controller/PostFavoriteControllerTest.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/controller/PostFavoriteController.java`

- [ ] **RED: 编写失败的测试**

```java
package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.security.JwtUtil;
import com.iaihub.toolbox.service.PostFavoriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostFavoriteController.class)
class PostFavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostFavoriteService service;

    @MockBean
    private JwtUtil jwtUtil;

    private String token = "test-token";
    private Long userId = 1L;
    private Long postId = 100L;

    @BeforeEach
    void setUp() {
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(jwtUtil.validateToken(token, userId)).thenReturn(true);
    }

    @Test
    void testAddFavorite() throws Exception {
        PostFavorite favorite = new PostFavorite();
        favorite.setId(1L);
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        when(service.addFavorite(userId, postId)).thenReturn(favorite);

        mockMvc.perform(post("/api/post-favorites/{postId}", postId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRemoveFavorite() throws Exception {
        when(service.removeFavorite(userId, postId)).thenReturn(true);

        mockMvc.perform(delete("/api/post-favorites/{postId}", postId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testGetUserFavorites() throws Exception {
        PostFavorite fav1 = new PostFavorite();
        fav1.setId(1L);
        PostFavorite fav2 = new PostFavorite();
        fav2.setId(2L);
        when(service.getUserFavorites(userId)).thenReturn(Arrays.asList(fav1, fav2));

        mockMvc.perform(get("/api/post-favorites")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testCheckFavorite() throws Exception {
        when(service.isFavorited(userId, postId)).thenReturn(true);

        mockMvc.perform(get("/api/post-favorites/check/{postId}", postId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
```

- [ ] **GREEN: 实现 PostFavoriteController**

```java
package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.model.PostFavorite;
import com.iaihub.toolbox.security.JwtUtil;
import com.iaihub.toolbox.service.PostFavoriteService;
import com.iaihub.toolbox.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/post-favorites")
public class PostFavoriteController {

    private final PostFavoriteService service;
    private final JwtUtil jwtUtil;

    public PostFavoriteController(PostFavoriteService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/{postId}")
    public ResponseEntity<Result<PostFavorite>> addFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Result.success(service.addFavorite(userId, postId)));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Result<Boolean>> removeFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Result.success(service.removeFavorite(userId, postId)));
    }

    @GetMapping
    public ResponseEntity<Result<List<PostFavorite>>> getUserFavorites(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Result.success(service.getUserFavorites(userId)));
    }

    @GetMapping("/check/{postId}")
    public ResponseEntity<Result<Boolean>> checkFavorite(
            @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Result.success(service.isFavorited(userId, postId)));
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractUserId(token);
    }
}
```

- [ ] **运行测试验证**

运行：`cd backend && ./gradlew test --tests "*PostFavoriteControllerTest" -v`
预期：PASS

- [ ] **Commit**

```bash
cd backend
git add src/test/java/com/iaihub/toolbox/controller/PostFavoriteControllerTest.java
git add src/main/java/com/iaihub/toolbox/controller/PostFavoriteController.java
git commit -m "feat: add PostFavoriteController with REST API endpoints"
```

---

<!-- openspec-task: RED: 编写 PostCard.spec.ts -->
### 任务 4：PostCard 收藏按钮（TDD）

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PostCard from './PostCard.vue'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', name: 'Home', component: { template: '<div></div>' } }]
})

describe('PostCard', () => {
  it('renders favorite button', () => {
    const post = { id: 1, title: 'Test', author: { username: 'u' }, likeCount: 0, commentCount: 0 }
    const wrapper = mount(PostCard, { props: { post }, global: { plugins: [router] } })
    expect(wrapper.find('.favorite-btn').exists()).toBe(true)
  })
})
```

- [ ] **GREEN: 修改 PostCard.vue 添加收藏按钮**

（查看现有 PostCard.vue 实现后添加收藏按钮）

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/components/forum/PostCard.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/components/forum/PostCard.spec.ts
git add src/components/forum/PostCard.vue
git commit -m "feat: add favorite button to PostCard"
```

---

<!-- openspec-task: RED: 编写 PostDetailPage.spec.ts -->
### 任务 5：PostDetailPage 收藏按钮（TDD）

**文件：**
- 测试：`frontend/src/pages/forum/PostDetailPage.spec.ts`
- 修改：`frontend/src/pages/forum/PostDetailPage.vue`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PostDetailPage from './PostDetailPage.vue'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/forum', name: 'Forum', component: { template: '<div></div>' } }]
})

describe('PostDetailPage', () => {
  it('renders favorite button', () => {
    const wrapper = mount(PostDetailPage, { global: { plugins: [router] } })
    expect(wrapper.find('.favorite-btn').exists()).toBe(true)
  })
})
```

- [ ] **GREEN: 修改 PostDetailPage.vue 添加收藏按钮**

（查看现有 PostDetailPage.vue 实现后添加收藏按钮）

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/pages/forum/PostDetailPage.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/pages/forum/PostDetailPage.spec.ts
git add src/pages/forum/PostDetailPage.vue
git commit -m "feat: add favorite button to PostDetailPage"
```

---

<!-- openspec-task: RED: 编写 PostListPage.spec.ts -->
### 任务 6：PostListPage 左侧导航栏（TDD）

**文件：**
- 测试：`frontend/src/pages/forum/PostListPage.spec.ts`
- 修改：`frontend/src/pages/forum/PostListPage.vue`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PostListPage from './PostListPage.vue'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/forum', name: 'ForumList', component: PostListPage },
    { path: '/forum/my-posts', name: 'MyPosts', component: { template: '<div></div>' } },
    { path: '/forum/my-favorites', name: 'MyFavorites', component: { template: '<div></div>' } }
  ]
})

describe('PostListPage', () => {
  it('renders left sidebar navigation', () => {
    const wrapper = mount(PostListPage, { global: { plugins: [router] } })
    expect(wrapper.find('.sidebar-nav').exists()).toBe(true)
  })
})
```

- [ ] **GREEN: 修改 PostListPage.vue 添加左侧导航栏**

（查看现有 PostListPage.vue 实现后添加左侧导航栏）

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/pages/forum/PostListPage.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/pages/forum/PostListPage.spec.ts
git add src/pages/forum/PostListPage.vue
git commit -m "feat: add left sidebar navigation to PostListPage"
```

---

<!-- openspec-task: RED: 编写 MyPostsPage.spec.ts -->
### 任务 7：MyPostsPage 我的帖子页面（TDD）

**文件：**
- 测试：`frontend/src/pages/forum/MyPostsPage.spec.ts`
- 创建：`frontend/src/pages/forum/MyPostsPage.vue`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MyPostsPage from './MyPostsPage.vue'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

vi.mock('@/services/api', () => ({
  getMyPosts: vi.fn().mockResolvedValue({ code: 200, data: [] })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/forum/my-posts', name: 'MyPosts', component: MyPostsPage }]
})

describe('MyPostsPage', () => {
  it('renders posts list', async () => {
    const wrapper = mount(MyPostsPage, { global: { plugins: [router] } })
    await vi.waitFor(() => { expect(wrapper.find('.post-list').exists()).toBe(true) })
  })
})
```

- [ ] **GREEN: 实现 MyPostsPage.vue**

```typescript
<template>
  <div class="my-posts-page">
    <h2>我的帖子</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="posts.length === 0" class="empty">暂无帖子</div>
    <div v-else class="post-list">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyPosts } from '@/services/api'
import PostCard from '@/components/forum/PostCard.vue'

const posts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getMyPosts()
    posts.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>
```

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/pages/forum/MyPostsPage.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/pages/forum/MyPostsPage.spec.ts
git add src/pages/forum/MyPostsPage.vue
git commit -m "feat: add MyPostsPage for user's posts"
```

---

<!-- openspec-task: RED: 编写 MyFavoritesPage.spec.ts -->
### 任务 8：MyFavoritesPage 我的收藏页面（TDD）

**文件：**
- 测试：`frontend/src/pages/forum/MyFavoritesPage.spec.ts`
- 创建：`frontend/src/pages/forum/MyFavoritesPage.vue`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MyFavoritesPage from './MyFavoritesPage.vue'
import { createRouter, createWebHistory } from 'vue-router'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

vi.mock('@/services/api', () => ({
  getMyFavorites: vi.fn().mockResolvedValue({ code: 200, data: [] })
}))

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/forum/my-favorites', name: 'MyFavorites', component: MyFavoritesPage }]
})

describe('MyFavoritesPage', () => {
  it('renders favorites list', async () => {
    const wrapper = mount(MyFavoritesPage, { global: { plugins: [router] } })
    await vi.waitFor(() => { expect(wrapper.find('.post-list').exists()).toBe(true) })
  })
})
```

- [ ] **GREEN: 实现 MyFavoritesPage.vue**

```typescript
<template>
  <div class="my-favorites-page">
    <h2>我的收藏</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="favorites.length === 0" class="empty">暂无收藏</div>
    <div v-else class="post-list">
      <PostCard v-for="post in favorites" :key="post.id" :post="post" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyFavorites } from '@/services/api'
import PostCard from '@/components/forum/PostCard.vue'

const favorites = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getMyFavorites()
    favorites.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>
```

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/pages/forum/MyFavoritesPage.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/pages/forum/MyFavoritesPage.spec.ts
git add src/pages/forum/MyFavoritesPage.vue
git commit -m "feat: add MyFavoritesPage for user's favorites"
```

---

<!-- openspec-task: RED: 编写 AppHeader.spec.ts -->
### 任务 9：AppHeader 移除上传工具按钮（TDD）

**文件：**
- 测试：`frontend/src/components/AppHeader.spec.ts`
- 修改：`frontend/src/components/AppHeader.vue`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import AppHeader from './AppHeader.vue'

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isLoggedIn: true, userId: 1 })
}))

describe('AppHeader', () => {
  it('should not render upload tool button', () => {
    const wrapper = mount(AppHeader)
    expect(wrapper.find('.upload-btn').exists()).toBe(false)
  })
})
```

- [ ] **GREEN: 修改 AppHeader.vue 移除上传工具按钮**

（查看现有 AppHeader.vue 实现后移除上传工具按钮）

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/components/AppHeader.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/components/AppHeader.spec.ts
git add src/components/AppHeader.vue
git commit -m "feat: remove upload tool button from AppHeader"
```

---

<!-- openspec-task: RED: 编写 router.spec.ts -->
### 任务 10：Router 路由配置（TDD）

**文件：**
- 测试：`frontend/src/router.spec.ts`
- 修改：`frontend/src/router/index.ts`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import router from './router/index'

describe('router', () => {
  it('should have my-posts route', () => {
    const routes = router.getRoutes()
    expect(routes.some(r => r.name === 'MyPosts')).toBe(true)
  })

  it('should have my-favorites route', () => {
    const routes = router.getRoutes()
    expect(routes.some(r => r.name === 'MyFavorites')).toBe(true)
  })
})
```

- [ ] **GREEN: 修改 router/index.ts 添加新路由**

```typescript
{
  path: '/forum/my-posts',
  name: 'MyPosts',
  component: () => import('@/pages/forum/MyPostsPage.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/forum/my-favorites',
  name: 'MyFavorites',
  component: () => import('@/pages/forum/MyFavoritesPage.vue'),
  meta: { requiresAuth: true }
}
```

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/router.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/router.spec.ts
git add src/router/index.ts
git commit -m "feat: add my-posts and my-favorites routes"
```

---

<!-- openspec-task: RED: 编写 api.spec.ts -->
### 任务 11：API 服务收藏接口（TDD）

**文件：**
- 测试：`frontend/src/services/api.spec.ts`
- 修改：`frontend/src/services/api.ts`

- [ ] **RED: 编写失败的测试**

```typescript
import { describe, it, expect, vi } from 'vitest'
import { getMyPosts, getMyFavorites, toggleFavorite } from './api'

vi.mock('./api', () => ({
  getMyPosts: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getMyFavorites: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  toggleFavorite: vi.fn().mockResolvedValue({ code: 200, data: true })
}))

describe('forum API', () => {
  it('should call getMyPosts', async () => {
    await getMyPosts()
    expect(vi.mocked(getMyPosts)).toHaveBeenCalled()
  })

  it('should call getMyFavorites', async () => {
    await getMyFavorites()
    expect(vi.mocked(getMyFavorites)).toHaveBeenCalled()
  })

  it('should call toggleFavorite', async () => {
    await toggleFavorite(1)
    expect(vi.mocked(toggleFavorite)).toHaveBeenCalledWith(1)
  })
})
```

- [ ] **GREEN: 修改 services/api.ts 添加收藏 API**

```typescript
export const postFavoriteApi = {
  addFavorite: (postId: number) => api.post(`/post-favorites/${postId}`).then(res => res.data),
  removeFavorite: (postId: number) => api.delete(`/post-favorites/${postId}`).then(res => res.data),
  getMyFavorites: () => api.get('/post-favorites').then(res => res.data),
  checkFavorite: (postId: number) => api.get(`/post-favorites/check/${postId}`).then(res => res.data),
  toggleFavorite: async (postId: number) => {
    const checkRes = await postFavoriteApi.checkFavorite(postId)
    if (checkRes.data) {
      return postFavoriteApi.removeFavorite(postId)
    } else {
      return postFavoriteApi.addFavorite(postId)
    }
  }
}

export const getMyPosts = () => api.get('/posts/my').then(res => res.data)
```

- [ ] **运行测试验证**

运行：`cd frontend && npm test -- --run src/services/api.spec.ts`
预期：PASS

- [ ] **Commit**

```bash
cd frontend
git add src/services/api.spec.ts
git add src/services/api.ts
git commit -m "feat: add post favorite APIs to services"
```