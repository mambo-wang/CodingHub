# 概览页面 (Overview Page) 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现概览页面，展示工具热榜、帖子热榜、平台统计数据，采用 Tab 页切换和双列布局

**架构：** 后端 REST API 返回统计数据和热榜数据（TOP 5/类别），前端 Vue 3 组件渲染 Tab 切换的热榜列表

**技术栈：** Java 17 / Spring Boot 3.2.5, Vue 3 / TypeScript / Vite, MySQL 8.x

---

## 文件结构

### Backend
```
backend/src/main/java/com/iaihub/toolbox/
├── controller/OverviewController.java
├── service/OverviewService.java + OverviewServiceImpl.java
├── dto/StatsDto.java + ToolRankDto.java + PostRankDto.java
```

### Frontend
```
frontend/src/
├── pages/OverviewPage.vue
├── components/StatsCard.vue + RankItem.vue + ToolRankList.vue + PostRankList.vue
├── services/overview.ts
├── types/overview.ts
```

---

## Backend 实现任务

<!-- openspec-task: A.1 -->
### Task 1: OverviewController Stats API TDD

**文件：** 创建 `backend/src/test/java/com/iaihub/toolbox/controller/OverviewControllerTest.java`

- [ ] **步骤 1：编写失败测试**

```java
@Test
void getStats_returnsUserCountPostCountToolCount() throws Exception {
    mockMvc.perform(get("/api/overview/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userCount").exists())
        .andExpect(jsonPath("$.postCount").exists())
        .andExpect(jsonPath("$.toolCount").exists());
}
```

- [ ] **步骤 2：运行测试验证失败**

```bash
./gradlew test --tests "*OverviewControllerTest*" -v
```
预期：FAIL，编译错误 "OverviewController 类不存在"

- [ ] **步骤 3：创建 StatsDto.java**

```java
package com.iaihub.toolbox.dto;

public class StatsDto {
    private Long userCount;
    private Long postCount;
    private Long toolCount;

    public StatsDto(Long userCount, Long postCount, Long toolCount) {
        this.userCount = userCount;
        this.postCount = postCount;
        this.toolCount = toolCount;
    }

    public Long getUserCount() { return userCount; }
    public Long getPostCount() { return postCount; }
    public Long getToolCount() { return toolCount; }
}
```

- [ ] **步骤 4：创建 OverviewController.java**

```java
@RestController
@RequestMapping("/api/overview")
public class OverviewController {
    @GetMapping("/stats")
    public StatsDto getStats() {
        return new StatsDto(0L, 0L, 0L);
    }
}
```

- [ ] **步骤 5：运行测试验证通过**

```bash
./gradlew test --tests "*OverviewControllerTest*" -v
```
预期：PASS

- [ ] **步骤 6：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/StatsDto.java \
        backend/src/main/java/com/iaihub/toolbox/controller/OverviewController.java \
        backend/src/test/java/com/iaihub/toolbox/controller/OverviewControllerTest.java
git commit -m "feat: add overview stats API endpoint"
```

---

<!-- openspec-task: A.2 -->
### Task 2: OverviewService 查询数据库

**文件：** 创建 `OverviewService.java` + `OverviewServiceImpl.java`

- [ ] **步骤 1：创建 Service 接口**

```java
package com.iaihub.toolbox.service;

import com.iaihub.toolbox.dto.StatsDto;

public interface OverviewService {
    StatsDto getStats();
}
```

- [ ] **步骤 2：创建 Service 实现**

```java
@Service
public class OverviewServiceImpl implements OverviewService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ToolRepository toolRepository;

    public OverviewServiceImpl(UserRepository userRepository,
                               PostRepository postRepository,
                               ToolRepository toolRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.toolRepository = toolRepository;
    }

    @Override
    public StatsDto getStats() {
        long userCount = userRepository.count();
        long postCount = postRepository.count();
        long toolCount = toolRepository.count();
        return new StatsDto(userCount, postCount, toolCount);
    }
}
```

- [ ] **步骤 3：修改 Controller 使用 Service**

```java
@RestController
@RequestMapping("/api/overview")
public class OverviewController {
    private final OverviewService overviewService;
    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/stats")
    public StatsDto getStats() {
        return overviewService.getStats();
    }
}
```

- [ ] **步骤 4：运行测试**

```bash
./gradlew test --tests "*OverviewControllerTest*" -v
```
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/service/OverviewService.java \
        backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java
git commit -m "feat: add OverviewService with database queries"
```

---

<!-- openspec-task: A.3 -->
### Task 3: 重构 - 确保返回值不为 null

**文件：** 创建 `OverviewServiceTest.java`

- [ ] **步骤 1：创建 null 安全性测试**

```java
@Test
void getStats_neverReturnsNull() {
    UserRepository userRepo = mock(UserRepository.class);
    when(userRepo.count()).thenReturn(0L);
    PostRepository postRepo = mock(PostRepository.class);
    when(postRepo.count()).thenReturn(0L);
    ToolRepository toolRepo = mock(ToolRepository.class);
    when(toolRepo.count()).thenReturn(0L);

    OverviewServiceImpl service = new OverviewServiceImpl(userRepo, postRepo, toolRepo);
    StatsDto result = service.getStats();

    assertNotNull(result);
    assertNotNull(result.getUserCount());
    assertNotNull(result.getPostCount());
    assertNotNull(result.getToolCount());
}
```

- [ ] **步骤 2：运行测试**

```bash
./gradlew test --tests "*OverviewServiceTest*" -v
```
预期：PASS

- [ ] **步骤 3：Commit**

```bash
git add backend/src/test/java/com/iaihub/toolbox/service/OverviewServiceTest.java
git commit -m "test: add OverviewService null-safety test"
```

---

<!-- openspec-task: A.4 -->
### Task 4: Tool Ranks API 测试与实现

**文件：** 创建 `ToolRankDto.java`，修改 `OverviewController.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void getToolRanks_returnsGroupedToolList() throws Exception {
    mockMvc.perform(get("/api/overview/tool-ranks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
}
```

- [ ] **步骤 2：创建 ToolRankDto**

```java
public class ToolRankDto {
    private String category;
    private String toolName;
    private Long hotScore;

    public ToolRankDto(String category, String toolName, Long hotScore) {
        this.category = category;
        this.toolName = toolName;
        this.hotScore = hotScore;
    }

    public String getCategory() { return category; }
    public String getToolName() { return toolName; }
    public Long getHotScore() { return hotScore; }
}
```

- [ ] **步骤 3：在 Service 接口添加方法**

```java
List<ToolRankDto> getToolRanks();
```

- [ ] **步骤 4：运行测试**

```bash
./gradlew test --tests "*OverviewControllerTest*" -v
```
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/ToolRankDto.java
git commit -m "feat: add ToolRankDto and tool-ranks endpoint stub"
```

---

<!-- openspec-task: A.5 -->
### Task 5: 实现 Tool Ranks 查询逻辑

**文件：** 修改 `OverviewServiceImpl.java`

- [ ] **步骤 1：批量查询所有工具并按类别分组**

```java
@Override
public List<ToolRankDto> getToolRanks() {
    List<ToolRankDto> result = new ArrayList<>();
    List<Category> categories = categoryRepository.findAll();

    // 批量查询所有工具
    Map<Long, List<Tool>> toolsByCategory = toolRepository
        .findAllWithCategory()
        .stream()
        .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

    for (Category category : categories) {
        List<Tool> tools = toolsByCategory.getOrDefault(category.getId(), Collections.emptyList());
        tools.stream()
            .sorted(Comparator.comparing(Tool::getHotScore).reversed())
            .limit(5)
            .forEach(t -> result.add(new ToolRankDto(category.getName(), t.getName(), t.getHotScore())));
    }
    return result;
}
```

- [ ] **步骤 2：在 ToolRepository 添加批量查询方法**

```java
@Query("SELECT t FROM Tool t JOIN FETCH t.category")
List<Tool> findAllWithCategory();
```

- [ ] **步骤 3：运行测试**

```bash
./gradlew test --tests "*Overview*" -v
```
预期：PASS

- [ ] **步骤 4：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/ToolRepository.java \
        backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java
git commit -m "feat: implement tool ranks query with batch loading"
```

---

<!-- openspec-task: A.6 -->
### Task 6: Post Ranks API 测试与实现

**文件：** 创建 `PostRankDto.java`，修改 `OverviewServiceImpl.java`

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void getPostRanks_returnsGroupedPostList() throws Exception {
    mockMvc.perform(get("/api/overview/post-ranks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
}
```

- [ ] **步骤 2：创建 PostRankDto**

```java
public class PostRankDto {
    private String category;
    private String postTitle;
    private Long commentCount;

    public PostRankDto(String category, String postTitle, Long commentCount) {
        this.category = category;
        this.postTitle = postTitle;
        this.commentCount = commentCount;
    }

    public String getCategory() { return category; }
    public String getPostTitle() { return postTitle; }
    public Long getCommentCount() { return commentCount; }
}
```

- [ ] **步骤 3：在 Service 接口添加方法**

```java
List<PostRankDto> getPostRanks();
```

- [ ] **步骤 4：运行测试**

```bash
./gradlew test --tests "*OverviewControllerTest*" -v
```
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/dto/PostRankDto.java
git commit -m "feat: add PostRankDto and post-ranks endpoint stub"
```

---

<!-- openspec-task: A.7 -->
### Task 7: 实现 Post Ranks 查询逻辑

**文件：** 修改 `OverviewServiceImpl.java`

- [ ] **步骤 1：实现批量查询逻辑**

```java
@Override
public List<PostRankDto> getPostRanks() {
    List<PostRankDto> result = new ArrayList<>();
    List<Category> categories = categoryRepository.findAll();

    Map<Long, List<Post>> postsByCategory = postRepository
        .findAllWithCategory()
        .stream()
        .collect(Collectors.groupingBy(p -> p.getCategory().getId()));

    for (Category category : categories) {
        List<Post> posts = postsByCategory.getOrDefault(category.getId(), Collections.emptyList());
        posts.stream()
            .sorted(Comparator.comparing(p -> p.getComments().size()).reversed())
            .limit(5)
            .forEach(p -> result.add(new PostRankDto(category.getName(), p.getTitle(), (long) p.getComments().size())));
    }
    return result;
}
```

- [ ] **步骤 2：在 PostRepository 添加批量查询方法**

```java
@Query("SELECT p FROM Post p JOIN FETCH p.category LEFT JOIN FETCH p.comments")
List<Post> findAllWithCategory();
```

- [ ] **步骤 3：运行测试**

```bash
./gradlew test --tests "*Overview*" -v
```
预期：PASS

- [ ] **步骤 4：Commit**

```bash
git add backend/src/main/java/com/iaihub/toolbox/repository/PostRepository.java \
        backend/src/main/java/com/iaihub/toolbox/service/OverviewServiceImpl.java
git commit -m "feat: implement post ranks query with batch loading"
```

---

## Frontend 实现任务

<!-- openspec-task: B.1 -->
### Task 8: 创建 Overview Types

**文件：** 创建 `frontend/src/types/overview.ts`

- [ ] **步骤 1：创建类型定义**

```typescript
export interface StatsDto {
  userCount: number;
  postCount: number;
  toolCount: number;
}

export interface ToolRankDto {
  id: number;
  category: string;
  toolName: string;
  hotScore: number;
}

export interface PostRankDto {
  id: number;
  category: string;
  postTitle: string;
  commentCount: number;
}
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/types/overview.ts
git commit -m "feat: add overview TypeScript types"
```

---

<!-- openspec-task: B.2 -->
### Task 9: 创建 OverviewService

**文件：** 创建 `frontend/src/services/overview.ts`

- [ ] **步骤 1：创建 API 服务**

```typescript
import axios from 'axios';
import type { StatsDto, ToolRankDto, PostRankDto } from '@/types/overview';

const api = axios.create({ baseURL: '/api' });

export async function fetchStats(): Promise<StatsDto> {
  const response = await api.get('/overview/stats');
  return response.data;
}

export async function fetchToolRanks(): Promise<ToolRankDto[]> {
  const response = await api.get('/overview/tool-ranks');
  return response.data;
}

export async function fetchPostRanks(): Promise<PostRankDto[]> {
  const response = await api.get('/overview/post-ranks');
  return response.data;
}
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/services/overview.ts
git commit -m "feat: add overview API service"
```

---

<!-- openspec-task: B.3 -->
### Task 10: 创建 StatsCard.vue

**文件：** 创建 `frontend/src/components/StatsCard.vue`

- [ ] **步骤 1：创建组件**

```vue
<template>
  <div class="stats-card glass-card">
    <div class="flex items-center gap-3">
      <div class="icon-wrapper" :style="iconStyle">
        <component :is="iconComponent" :size="20" />
      </div>
      <div>
        <p class="text-xs" style="color: var(--color-muted);">{{ label }}</p>
        <p class="font-code text-2xl font-bold">{{ formattedValue }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Users, MessageSquare, Wrench } from '@lucide/vue';

const props = defineProps<{
  label: string;
  value: number;
  icon: 'users' | 'message-square' | 'wrench';
}>();

const iconComponent = computed(() => {
  const icons = { users: Users, 'message-square': MessageSquare, wrench: Wrench };
  return icons[props.icon];
});

const iconStyle = computed(() => {
  const styles = {
    users: { background: 'rgba(0,255,255,0.1)', border: '1px solid rgba(0,255,255,0.2)', color: '#00FFFF' },
    'message-square': { background: 'rgba(255,0,255,0.1)', border: '1px solid rgba(255,0,255,0.2)', color: '#FF00FF' },
    wrench: { background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.2)', color: '#22C55E' }
  };
  return styles[props.icon];
});

const formattedValue = computed(() => props.value.toLocaleString('zh-CN'));
</script>

<style scoped>
.stats-card { position: relative; padding: 16px; overflow: hidden; }
.stats-card::before { content: ''; position: absolute; inset: -100%; background: radial-gradient(circle, rgba(0,255,255,0.08) 0%, transparent 50%); opacity: 0; transition: opacity 400ms ease; pointer-events: none; }
.stats-card:hover::before { opacity: 1; }
.glass-card { background: rgba(15, 23, 42, 0.9); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; transition: all 200ms ease; }
.glass-card:hover { border-color: rgba(0, 255, 255, 0.3); box-shadow: 0 0 30px rgba(0, 255, 255, 0.15); }
.icon-wrapper { padding: 8px; border-radius: 8px; }
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/components/StatsCard.vue
git commit -m "feat: add StatsCard.vue component"
```

---

<!-- openspec-task: B.4 -->
### Task 11: 创建 RankItem.vue

**文件：** 创建 `frontend/src/components/RankItem.vue`

- [ ] **步骤 1：创建组件**

```vue
<template>
  <div class="rank-item" @click="$emit('click')">
    <span class="rank-badge" :class="{ 'top-3': rank <= 3 }">{{ rank }}</span>
    <span class="flex-1 text-sm truncate">{{ title }}</span>
    <span class="text-xs font-code" style="color: var(--color-muted);">{{ count }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ rank: number; title: string; count: number; }>();
defineEmits<{ (e: 'click'): void; }>();
</script>

<style scoped>
.rank-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: 8px; transition: all 150ms ease; cursor: pointer; }
.rank-item:hover { background: #1E293B; }
.rank-badge { min-width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #00FFFF, #FF00FF); color: #0F172A; font-family: 'Fira Code', monospace; font-size: 11px; font-weight: 700; border-radius: 6px; }
.rank-badge.top-3 { background: linear-gradient(135deg, #FBBF24, #22C55E); }
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/components/RankItem.vue
git commit -m "feat: add RankItem.vue component"
```

---

<!-- openspec-task: B.5 -->
### Task 12: 创建 ToolRankList.vue

**文件：** 创建 `frontend/src/components/ToolRankList.vue`

- [ ] **步骤 1：创建组件**

```vue
<template>
  <div class="tool-rank-list">
    <div class="tab-container">
      <button v-for="cat in ['全部', ...categories]" :key="cat"
        :class="['tab-btn', { active: selectedCategory === (cat === '全部' ? null : cat) }]"
        @click="$emit('select', cat === '全部' ? null : cat)">{{ cat }}</button>
    </div>
    <div class="rank-panel">
      <div v-if="loading" class="loading-state">
        <div v-for="i in 5" :key="i" class="skeleton h-10 w-full mb-2"></div>
      </div>
      <div v-else-if="items.length === 0" class="empty-state"><p>暂无数据</p></div>
      <div v-else class="rank-items">
        <RankItem v-for="(item, index) in items" :key="item.id" :rank="index + 1"
          :title="item.toolName" :count="item.hotScore" @click="handleClick(item)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import RankItem from './RankItem.vue';
import type { ToolRankDto } from '@/types/overview';

const props = defineProps<{ categories: string[]; selectedCategory: string | null; items: ToolRankDto[]; loading?: boolean; }>();
const emit = defineEmits<{ (e: 'select', category: string | null): void; }>();
const handleClick = (item: ToolRankDto) => { /* 导航到详情页 */ };
</script>

<style scoped>
.tab-btn { display: flex; align-items: center; gap: 6px; padding: 8px 14px; border: 1.5px solid #1E293B; border-radius: 20px; background: rgba(15, 23, 42, 0.9); color: #94A3B8; font-size: 13px; font-weight: 500; cursor: pointer; transition: all 0.2s ease; }
.tab-btn:hover { border-color: #00FFFF; color: #00FFFF; }
.tab-btn.active { background: linear-gradient(135deg, #00FFFF, #FF00FF); border-color: transparent; color: #0F172A; font-weight: 600; }
.rank-panel { background: rgba(15, 23, 42, 0.9); border: 1px solid rgba(255, 255, 255, 0.08); border-top: none; border-radius: 0 0 16px 16px; padding: 12px; }
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/components/ToolRankList.vue
git commit -m "feat: add ToolRankList.vue with tab switching"
```

---

<!-- openspec-task: B.6 -->
### Task 13: 创建 PostRankList.vue

**文件：** 创建 `frontend/src/components/PostRankList.vue`

- [ ] **步骤 1：创建组件**（结构与 ToolRankList.vue 类似）

```vue
<template>
  <div class="post-rank-list">
    <div class="tab-container">
      <button v-for="cat in ['全部', ...categories]" :key="cat"
        :class="['tab-btn', { active: selectedCategory === (cat === '全部' ? null : cat) }]"
        @click="$emit('select', cat === '全部' ? null : cat)">{{ cat }}</button>
    </div>
    <div class="rank-panel">
      <div v-if="loading" class="loading-state">
        <div v-for="i in 5" :key="i" class="skeleton h-10 w-full mb-2"></div>
      </div>
      <div v-else-if="items.length === 0" class="empty-state"><p>暂无数据</p></div>
      <div v-else class="rank-items">
        <RankItem v-for="(item, index) in items" :key="item.id" :rank="index + 1"
          :title="item.postTitle" :count="item.commentCount" @click="handleClick(item)" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import RankItem from './RankItem.vue';
import type { PostRankDto } from '@/types/overview';

const props = defineProps<{ categories: string[]; selectedCategory: string | null; items: PostRankDto[]; loading?: boolean; }>();
const emit = defineEmits<{ (e: 'select', category: string | null): void; }>();
const handleClick = (item: PostRankDto) => { /* 导航到详情页 */ };
</script>

<style scoped>
/* 与 ToolRankList.vue 相同样式 */
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/components/PostRankList.vue
git commit -m "feat: add PostRankList.vue with tab switching"
```

---

<!-- openspec-task: B.7 -->
### Task 14: 创建 OverviewPage.vue

**文件：** 创建 `frontend/src/pages/OverviewPage.vue`

- [ ] **步骤 1：创建主页面组件**

```vue
<template>
  <div class="overview-page">
    <header class="page-header">
      <h1 class="font-code text-xl font-bold" style="color: #00FFFF;">数据概览</h1>
      <p class="text-sm mt-1" style="color: var(--color-muted);">Platform Overview</p>
    </header>

    <section class="stats-grid">
      <StatsCard label="用户" :value="stats.userCount" icon="users" />
      <StatsCard label="帖子" :value="stats.postCount" icon="message-square" />
      <StatsCard label="工具" :value="stats.toolCount" icon="wrench" />
    </section>

    <section class="main-content">
      <div class="rank-section">
        <h2 class="font-code text-base font-semibold flex items-center gap-2 mb-3">
          <Flame :size="16" style="color: #00FFFF;" /> 工具热榜
        </h2>
        <ToolRankList :categories="toolCategories" :selectedCategory="selectedToolCategory" :items="toolItems"
          @select="selectedToolCategory = $event" />
      </div>
      <div class="rank-section">
        <h2 class="font-code text-base font-semibold flex items-center gap-2 mb-3">
          <MessageCircle :size="16" style="color: #FF00FF;" /> 帖子热榜
        </h2>
        <PostRankList :categories="postCategories" :selectedCategory="selectedPostCategory" :items="postItems"
          @select="selectedPostCategory = $event" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Flame, MessageCircle } from '@lucide/vue';
import StatsCard from '@/components/StatsCard.vue';
import ToolRankList from '@/components/ToolRankList.vue';
import PostRankList from '@/components/PostRankList.vue';
import { fetchStats, fetchToolRanks, fetchPostRanks } from '@/services/overview';
import type { StatsDto, ToolRankDto, PostRankDto } from '@/types/overview';

const stats = ref<StatsDto>({ userCount: 0, postCount: 0, toolCount: 0 });
const toolCategories = ref<string[]>([]);
const postCategories = ref<string[]>([]);
const toolItems = ref<ToolRankDto[]>([]);
const postItems = ref<PostRankDto[]>([]);
const selectedToolCategory = ref<string | null>(null);
const selectedPostCategory = ref<string | null>(null);

onMounted(async () => {
  const [statsData, toolData, postData] = await Promise.all([
    fetchStats(), fetchToolRanks(), fetchPostRanks()
  ]);
  stats.value = statsData;
  toolItems.value = toolData;
  postItems.value = postData;
  toolCategories.value = [...new Set(toolData.map(t => t.category))];
  postCategories.value = [...new Set(postData.map(p => p.category))];
});
</script>

<style scoped>
.overview-page { padding: 16px; max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
.main-content { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.rank-section { background: rgba(15, 23, 42, 0.9); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 16px; }
@media (max-width: 768px) {
  .stats-grid { grid-template-columns: 1fr; }
  .main-content { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/pages/OverviewPage.vue
git commit -m "feat: add OverviewPage.vue main component"
```

---

<!-- openspec-task: B.8 -->
### Task 15: 路由配置

**文件：** 修改 `frontend/src/router/index.ts`

- [ ] **步骤 1：添加路由**

```typescript
{
  path: '/overview',
  name: 'Overview',
  component: () => import('@/pages/OverviewPage.vue')
}
```

- [ ] **步骤 2：Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "feat: add /overview route to router"
```

---

## Plan Summary

**Total tasks:** 15 (7 Backend + 8 Frontend)

**Mapping coverage:** All OpenSpec tasks covered ✓

- A.1-A.7: Backend TDD tasks (Stats API, Tool Ranks, Post Ranks)
- B.1-B.8: Frontend tasks (Types, Service, Components, Page, Router)

**Next step:** Run `/opsx:executing-plans overview-page` to start implementation.