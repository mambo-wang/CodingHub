# 插件市场增强（收藏数 + 标签 + 置顶/热门角标）实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 插件市场卡片展示收藏数并计入热度分；插件接入统一标签系统（上传/编辑维护、卡片+详情展示）；管理员可置顶插件，列表卡片展示置顶/热门角标。

**架构：** 完全对齐工具广场既有模式——`Plugin` 实体加 `favoriteCount`/`pinned` 冗余字段，repository 层 `@Modifying` 原子更新计数并同步 `score ±4`；标签走 `TagType.PLUGIN` + `plugin_tag` 关联表 + `TagService`；热门角标由新端点 `GET /api/v1/plugins/hot-top5` 驱动。前端复用 `TagSelector`/`TagBadge` 与 HomePage 的 badge 样式。

**技术栈：** Java 17 / Spring Boot 3.2.5 / Spring Data JPA / JUnit5+Mockito；Vue 3.4 / TS 5.4 / @lucide/vue / Element Plus。

**规格：** `docs/superpowers/specs/2026-08-29-plugin-favorite-count-design.md`

**环境备忘（本机）：**
- 后端启动：`export JAVA_HOME="/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home"`，profile 用 `--args='--spring.profiles.active=postgresql'`（MySQL 本机不可用）。
- 后端测试：`cd backend && ./gradlew test --tests "<类名>"`（首次运行含编译约 1-2 分钟）。
- 前端验证：`cd frontend && npx vue-tsc --noEmit`（存量有 8 个与本改动无关的类型错误，只需保证不新增）。
- **项目 git 规则：每次 commit 前必须暂停，向人工确认后再提交。**

**文件结构（创建/修改全景）：**

| 文件 | 操作 | 职责 |
|---|---|---|
| `backend/.../model/Plugin.java` | 修改 | +favoriteCount/pinned 字段与实体方法，updateScore 纳入收藏×4 |
| `backend/.../repository/PluginRepository.java` | 修改 | +原子收藏计数、pin/unpin、hot-top5、hot 排序改 pinned 优先 |
| `backend/.../repository/UnifiedFavoriteRepository.java` | 修改 | +按 PLUGIN 分组计数查询（回填用） |
| `backend/.../service/UnifiedFavoriteService.java` | 修改 | toggleFavorite 增 PLUGIN 分支 |
| `backend/.../dto/plugin/PluginSummaryDTO.java` | 修改 | +favoriteCount/pinned/tags |
| `backend/.../service/plugin/PluginService.java` | 修改 | 映射新字段；upload/update 维护 tagIds；pin/unpin/hotTop5 方法 |
| `backend/.../controller/plugin/PluginController.java` | 修改 | +pin/unpin/hot-top5 端点，upload/update 接 tagIds |
| `backend/.../model/tag/TagType.java` | 修改 | +PLUGIN |
| `backend/.../model/tag/PluginTag.java` | 创建 | plugin_tag 关联实体（对齐 ToolTag） |
| `backend/.../repository/tag/PluginTagRepository.java` | 创建 | 关联查询/删除 |
| `backend/.../config/PluginFavoriteBackfillRunner.java` | 创建 | 启动幂等回填收藏数（独立 Runner，不动 DataInitializer 的 early-return 逻辑） |
| `backend/src/test/.../service/UnifiedFavoriteServiceTest.java` | 修改 | PLUGIN toggle 测试 |
| `backend/src/test/.../service/plugin/PluginServiceTest.java` | 创建 | updateScore/pin/tags/回填测试 |
| `frontend/src/types/plugin.ts` | 修改 | +3 字段 |
| `frontend/src/services/plugin.ts` | 修改 | hotTop5/pin/unpin；upload/update 附 tagIds |
| `frontend/src/pages/plugin/PluginMarketPage.vue` | 修改 | 收藏数、角标、标签、置顶按钮 |
| `frontend/src/pages/plugin/PluginUploadPage.vue` | 修改 | TagSelector 接入 |
| `frontend/src/pages/plugin/PluginEditPage.vue` | 修改 | TagSelector 接入 |
| `frontend/src/pages/plugin/PluginDetailPage.vue` | 修改 | TagBadge 展示 + 管理员置顶按钮（同页顺手，含在卡片角标需求内） |

---

### 任务 1：收藏计数字段与 score 联动（后端）

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Plugin.java`（85~131 行区域）
- 修改：`backend/src/main/java/com/iaihub/toolbox/repository/PluginRepository.java`（60 行后）
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/UnifiedFavoriteService.java:76-86`
- 修改：`backend/src/main/java/com/iaihub/toolbox/dto/plugin/PluginSummaryDTO.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/plugin/PluginService.java:739-790`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/UnifiedFavoriteServiceTest.java`

- [x] **步骤 1.1：编写失败的测试**

在 `UnifiedFavoriteServiceTest.java` 中新增（沿用该文件已有的 `favoriteService`/`pluginRepository` mock 与 setUp 风格；若无 plugin 测试夹具则按下面完整新增）：

```java
private Plugin testPlugin;

@Test
void toggleFavorite_plugin_incrementsFavoriteCountAndScore4() {
    testPlugin = Plugin.builder()
            .id(10L).name("demo-plugin").version("1.0.0")
            .author(testUser).status(Plugin.Status.NORMAL)
            .viewCount(10).likeCount(2).commentCount(1).favoriteCount(0)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    testPlugin.updateScore(); // score = 10 + 6 + 5 = 21
    when(pluginRepository.findByIdAndStatusNormal(10L)).thenReturn(Optional.of(testPlugin));
    when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "PLUGIN", 10L))
            .thenReturn(Optional.empty());

    InteractionResponse resp = favoriteService.toggleFavorite("PLUGIN", 10L, 100L);

    assertTrue(resp.isFavorited());
    verify(favoriteRepository).save(any(UnifiedFavorite.class));
    verify(pluginRepository).incrementFavoriteCount(10L);
}

@Test
void toggleFavorite_plugin_unfavorite_decrementsFavoriteCount() {
    testPlugin = Plugin.builder().id(10L).name("demo-plugin").version("1.0.0")
            .author(testUser).status(Plugin.Status.NORMAL)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    when(pluginRepository.findByIdAndStatusNormal(10L)).thenReturn(Optional.of(testPlugin));
    when(favoriteRepository.findByUserIdAndTargetTypeAndTargetId(100L, "PLUGIN", 10L))
            .thenReturn(Optional.of(UnifiedFavorite.builder()
                    .userId(100L).targetType("PLUGIN").targetId(10L).build()));

    favoriteService.toggleFavorite("PLUGIN", 10L, 100L);

    verify(pluginRepository).decrementFavoriteCount(10L);
}

@Test
void pluginUpdateScore_includesFavoriteWeight4() {
    Plugin p = Plugin.builder().viewCount(1).likeCount(1).commentCount(1).favoriteCount(3).build();
    p.updateScore();
    // 1×1 + 1×3 + 3×4 + 1×5 = 21
    assertEquals(0, new java.math.BigDecimal("21").compareTo(p.getScore()));
}
```

注意：`validateTargetExists` 对 PLUGIN 走 `pluginRepository.findByIdAndStatusNormal`（UnifiedFavoriteService 第 254 行），mock 按此打桩。`InteractionResponse` 的 favorited 字段 getter 以实际类为准（先读 `dto/InteractionResponse.java` 确认 `isFavorited()`/`getFavorited()`）。

- [x] **步骤 1.2：运行测试验证失败**

运行：`cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.UnifiedFavoriteServiceTest" 2>&1 | tail -20`
预期：COMPILATION ERROR（`favoriteCount` 字段与 `incrementFavoriteCount` 方法不存在）——编译失败即"红"。

- [x] **步骤 1.3：实现 Plugin 实体字段**

`Plugin.java` 在 `commentCount` 字段后新增：

```java
@Column(name = "favorite_count")
@Builder.Default
private Integer favoriteCount = 0;
```

`updateScore()` 整方法替换（注释同步改）：

```java
// 综合热度分：score = viewCount×1 + likeCount×3 + favoriteCount×4 + commentCount×5（对齐工具广场，插件无下载量）
public void updateScore() {
    int view = this.viewCount != null ? this.viewCount : 0;
    int like = this.likeCount != null ? this.likeCount : 0;
    int favorite = this.favoriteCount != null ? this.favoriteCount : 0;
    int comment = this.commentCount != null ? this.commentCount : 0;
    this.score = BigDecimal.valueOf(view)
        .add(BigDecimal.valueOf(like).multiply(BigDecimal.valueOf(3)))
        .add(BigDecimal.valueOf(favorite).multiply(BigDecimal.valueOf(4)))
        .add(BigDecimal.valueOf(comment).multiply(BigDecimal.valueOf(5)));
}
```

仿照 `incrementLikeCount/decrementLikeCount` 新增实体方法：

```java
public void incrementFavoriteCount() {
    this.favoriteCount = (this.favoriteCount == null ? 0 : this.favoriteCount) + 1;
    updateScore();
}

public void decrementFavoriteCount() {
    this.favoriteCount = this.favoriteCount == null ? 0 : this.favoriteCount;
    if (this.favoriteCount > 0) this.favoriteCount--;
    updateScore();
}
```

- [x] **步骤 1.4：实现 PluginRepository 原子更新**

在 `decrementLikeCount` 声明后新增（写法对齐 ToolRepository 165~179 行）：

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Transactional
@Query("UPDATE Plugin p SET p.favoriteCount = COALESCE(p.favoriteCount, 0) + 1, " +
       "p.score = COALESCE(p.score, 0) + 4 " +
       "WHERE p.id = :id AND p.status = 'NORMAL'")
int incrementFavoriteCount(@Param("id") Long id);

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Transactional
@Query("UPDATE Plugin p SET p.favoriteCount = CASE WHEN COALESCE(p.favoriteCount, 0) > 0 THEN p.favoriteCount - 1 ELSE 0 END, " +
       "p.score = CASE WHEN COALESCE(p.score, 0) >= 4 THEN p.score - 4 ELSE p.score END " +
       "WHERE p.id = :id AND p.status = 'NORMAL'")
int decrementFavoriteCount(@Param("id") Long id);
```

- [x] **步骤 1.5：toggleFavorite 接 PLUGIN 分支**

`UnifiedFavoriteService.java:76-86` 计数块改为：

```java
// Update denormalized counters and hot score atomically (avoids updatedAt refresh)
if (targetType == TargetType.TOOL) {
    if (favorited) { toolRepository.incrementFavoriteCount(targetId); }
    else { toolRepository.decrementFavoriteCount(targetId); }
} else if (targetType == TargetType.PLUGIN) {
    if (favorited) { pluginRepository.incrementFavoriteCount(targetId); }
    else { pluginRepository.decrementFavoriteCount(targetId); }
}
```

（`pluginRepository` 已是该服务构造依赖，无需新注入。）

- [x] **步骤 1.6：DTO 与映射**

`PluginSummaryDTO.java` 在 `private Integer likeCount;` 后加 `private Integer favoriteCount;`。
`PluginService.java` 的 `toSummaryDTO()` 在 `.likeCount(...)` 后加 `.favoriteCount(plugin.getFavoriteCount())`；`copySummary()` 加 `to.setFavoriteCount(from.getFavoriteCount());`。

- [x] **步骤 1.7：运行测试验证通过**

运行：`cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.UnifiedFavoriteServiceTest" 2>&1 | tail -10`
预期：BUILD SUCCESSFUL，新用例全绿；存量用例不破。

- [x] **步骤 1.8：人工确认后 Commit**

向用户确认后：`git add backend/src/main/java/com/iaihub/toolbox/model/Plugin.java backend/src/main/java/com/iaihub/toolbox/repository/PluginRepository.java backend/src/main/java/com/iaihub/toolbox/service/UnifiedFavoriteService.java backend/src/main/java/com/iaihub/toolbox/dto/plugin/PluginSummaryDTO.java backend/src/main/java/com/iaihub/toolbox/service/plugin/PluginService.java backend/src/test/java/com/iaihub/toolbox/service/UnifiedFavoriteServiceTest.java && git commit -m "feat(plugin): 收藏计数冗余字段与热度分联动（对齐工具广场模式）"`

---

### 任务 2：置顶与 hot-top5（后端）

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/Plugin.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/repository/PluginRepository.java:19-30`
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/plugin/PluginService.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/plugin/PluginController.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/dto/plugin/PluginSummaryDTO.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/plugin/PluginServiceTest.java`（创建）

- [x] **步骤 2.1：编写失败的测试（pin/unpin/hotTop5 行为）**

创建 `backend/src/test/java/com/iaihub/toolbox/service/plugin/PluginServiceTest.java`（Mockito 风格，`@InjectMocks PluginService` + `@Mock PluginRepository pluginRepository`；若 PluginService 构造依赖过多导致 @InjectMocks 不便，就只 new 需要的 mock 构造——以实际类构造器为准，先读 PluginService 字段声明）。

```java
@ExtendWith(MockitoExtension.class)
class PluginServiceTest {
    @Mock PluginRepository pluginRepository;
    // 其余依赖用 @Mock 补齐（以 PluginService 实际构造参数为准）

    @Test
    void pinPlugin_callsRepositoryPinById() {
        when(pluginRepository.findById(1L)).thenReturn(Optional.of(
                Plugin.builder().id(1L).name("p").version("1.0.0").build()));
        pluginService.pinPlugin(1L);
        verify(pluginRepository).pinById(1L);
    }

    @Test
    void unpinPlugin_callsRepositoryUnpinById() {
        when(pluginRepository.findById(1L)).thenReturn(Optional.of(
                Plugin.builder().id(1L).name("p").version("1.0.0").build()));
        pluginService.unpinPlugin(1L);
        verify(pluginRepository).unpinById(1L);
    }

    @Test
    void pin_missingPlugin_throws() {
        when(pluginRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> pluginService.pinPlugin(99L));
    }
}
```

- [x] **步骤 2.2：运行验证失败**

`cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.plugin.PluginServiceTest"`
预期：COMPILATION ERROR（`pinned`/`pinById`/`pinPlugin` 不存在）。

- [x] **步骤 2.3：实现实体与仓库**

`Plugin.java` 在 `score` 字段后加：

```java
@Column(nullable = false)
@Builder.Default
private Boolean pinned = false;
```

（注意：`nullable=false` + ddl-auto update 对已有行需默认值——Hibernate 加列不回填，MySQL/PG 行为一致会失败或留 NULL。规避：列不加 `nullable=false`，声明 `@Builder.Default private Boolean pinned = false;` 即可，读取处用 `Boolean.TRUE.equals(pinned)` 兜 null。）

`PluginRepository.java` 新增（对齐 ToolRepository 112~121 行）：

```java
// 置顶/取消置顶
@Modifying
@Transactional
@Query("UPDATE Plugin p SET p.pinned = true WHERE p.id = :id")
int pinById(@Param("id") Long id);

@Modifying
@Transactional
@Query("UPDATE Plugin p SET p.pinned = false WHERE p.id = :id")
int unpinById(@Param("id") Long id);

// 热度 Top5（仅返回有互动数据的插件，避免新插件被标记为热门）
@Query("SELECT p.id FROM Plugin p WHERE p.status = 'NORMAL' AND p.score > 0 ORDER BY p.score DESC")
List<Long> findTop5ByStatusOrderByScoreDesc(Pageable pageable);
```

`findByFiltersOrderByHot` 的 ORDER BY 改为：`ORDER BY p.pinned DESC, p.score DESC`（`findByFilters` 最新排序不动）。
⚠️ Hibernate JPQL 对 NULL 的 DESC 排序默认把 NULL 排最前（方言相关）——新列存量行为 NULL 会被顶到最前。回填：在本任务一并执行一次性 JPQL `UPDATE Plugin p SET p.pinned = false WHERE p.pinned IS NULL`（放 `PluginFavoriteBackfillRunner`，见任务 4，那里统一处理）。

- [x] **步骤 2.4：实现 Service 与 Controller**

`PluginService.java` 新增（放 pinTool 风格方法，`import org.springframework.data.domain.PageRequest`）：

```java
public void pinPlugin(Long id) {
    pluginRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("插件不存在"));
    pluginRepository.pinById(id);
}

public void unpinPlugin(Long id) {
    pluginRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("插件不存在"));
    pluginRepository.unpinById(id);
}

public List<Long> getHotTop5() {
    return pluginRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
}
```

`PluginController.java` 新增端点（`import org.springframework.security.access.prepost.PreAuthorize;`）：

```java
@PostMapping("/{id}/pin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public ResponseEntity<ApiResponse<Void>> pinPlugin(@PathVariable Long id) {
    pluginService.pinPlugin(id);
    return ResponseEntity.ok(ApiResponse.success("置顶成功", null));
}

@DeleteMapping("/{id}/pin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public ResponseEntity<ApiResponse<Void>> unpinPlugin(@PathVariable Long id) {
    pluginService.unpinPlugin(id);
    return ResponseEntity.ok(ApiResponse.success("取消置顶成功", null));
}

@GetMapping("/hot-top5")
public ResponseEntity<ApiResponse<List<Long>>> getHotTop5() {
    return ResponseEntity.ok(ApiResponse.success(pluginService.getHotTop5()));
}
```

Security 无需改：GET `/api/v1/plugins/hot-top5` 已被 `{id}` permitAll 匹配（SecurityConfig 第 68 行），POST/DELETE pin 走 `/api/v1/plugins/**` authenticated（第 114 行）。`PluginSummaryDTO` 加 `private Boolean pinned;`，`toSummaryDTO()`/`copySummary()` 补 `plugin.getPinned()`。

- [x] **步骤 2.5：运行测试验证通过**

`cd backend && ./gradlew test --tests "com.iaihub.toolbox.service.plugin.PluginServiceTest" 2>&1 | tail -5` → 全绿。

- [x] **步骤 2.6：人工确认后 Commit**（`feat(plugin): 支持管理员置顶与热度Top5接口`）

---

### 任务 3：插件接入统一标签（后端）

**文件：**
- 修改：`backend/src/main/java/com/iaihub/toolbox/model/tag/TagType.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/model/tag/PluginTag.java`
- 创建：`backend/src/main/java/com/iaihub/toolbox/repository/tag/PluginTagRepository.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/service/plugin/PluginService.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/controller/plugin/PluginController.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/dto/plugin/PluginSummaryDTO.java`
- 测试：`backend/src/test/java/com/iaihub/toolbox/service/plugin/PluginServiceTest.java`（追加）

- [x] **步骤 3.1：编写失败的测试（标签维护行为）**

```java
@Test
void upload_withTagIds_savesPluginTagAssociations() {
    // given：mock zip 解析链路成本高，直接测公开的标签维护方法
    // （见步骤 3.4 提取的 replacePluginTags）
}
```

由于 `upload()` 涉及 zip/文件系统的重依赖，**把标签关联逻辑提取为独立方法单独测**：

```java
@Test
void replaceTags_rebuildsAssociationsAndUsageCounts() {
    when(pluginTagRepository.findByPluginId(5L)).thenReturn(List.of(new PluginTag(5L, 20L)));
    Tag oldTag = Tag.builder().id(20L).name("old").tagType(TagType.PLUGIN).usageCount(1).build();
    Tag newTag = Tag.builder().id(21L).name("new").tagType(TagType.PLUGIN).usageCount(0).build();
    when(tagRepository.findById(20L)).thenReturn(Optional.of(oldTag));
    when(tagRepository.findById(21L)).thenReturn(Optional.of(newTag));

    pluginService.replaceTags(5L, List.of(21L));

    verify(pluginTagRepository).deleteByPluginId(5L);
    verify(pluginTagRepository).save(new PluginTag(5L, 21L));
    assertEquals(0, oldTag.getUsageCount()); // decrement 旧标签
    assertEquals(1, newTag.getUsageCount()); // increment 新标签
}
```

- [x] **步骤 3.2：运行验证失败**（`TagType.PLUGIN`、`PluginTag`、`replaceTags` 不存在 → 编译失败）

- [x] **步骤 3.3：实现标签类型与关联表**

`TagType.java` 枚举加 `PLUGIN`（注意检查该枚举是否有配套白名单/校验需要同步，读 `TagController.getTagsByType` 的 `type` 参数校验逻辑——`TagService` 第 29 行用 `TagType.valueOf` 天然支持新值）。

创建 `model/tag/PluginTag.java`（照抄 ToolTag，把 tool 换成 plugin）：

```java
@Entity
@Table(name = "plugin_tag")
@IdClass(PluginTag.PluginTagId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginTag {
    @Id @Column(name = "plugin_id") private Long pluginId;
    @Id @Column(name = "tag_id") private Long tagId;

    @Data
    public static class PluginTagId implements Serializable {
        private Long pluginId;
        private Long tagId;
    }
}
```

创建 `repository/tag/PluginTagRepository.java`（对齐 ToolTagRepository）：

```java
@Repository
public interface PluginTagRepository extends JpaRepository<PluginTag, PluginTag.PluginTagId> {
    List<PluginTag> findByPluginId(Long pluginId);
    void deleteByPluginId(Long pluginId);

    @Query("SELECT pt.pluginId FROM PluginTag pt WHERE pt.tagId = :tagId")
    List<Long> findPluginIdsByTagId(@Param("tagId") Long tagId);
}
```

- [x] **步骤 3.4：Service 标签逻辑**

`PluginService` 注入 `PluginTagRepository pluginTagRepository`、`TagRepository tagRepository`（import 对齐 ToolService 用法），新增：

```java
/** 重建插件标签关联（先减旧标签 usage，再挂新标签并加 usage）。传 null 视为清空。 */
@Transactional
public void replaceTags(Long pluginId, List<Long> tagIds) {
    List<PluginTag> oldTags = pluginTagRepository.findByPluginId(pluginId);
    for (PluginTag pt : oldTags) {
        tagRepository.findById(pt.getTagId()).ifPresent(Tag::decrementUsage);
    }
    pluginTagRepository.deleteByPluginId(pluginId);
    if (tagIds == null) return;
    for (Long tagId : tagIds) {
        pluginTagRepository.save(new PluginTag(pluginId, tagId));
        tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
    }
}
```

（`deleteByPluginId` 派生删除在 `@Transactional` 下需先 `flush`？ToolService 同写法已在生产使用，照抄即可；如测试报 flush 问题，在 delete 后加 `pluginTagRepository.flush();`。）

`upload(...)`/`update(...)`/`createDraft(...)` 方法签名各加 `List<Long> tagIds` 参数（update：`tagIds != null` 才调 `replaceTags`，对齐 ToolService"请求带字段才替换"语义；upload/draft：非空即 `replaceTags`）。`toSummaryDTO` 装配 tags：

```java
List<Tag> tags = pluginTagRepository.findByPluginId(plugin.getId()).stream()
        .map(pt -> tagRepository.findById(pt.getTagId()).orElse(null))
        .filter(Objects::nonNull)
        .toList();
// builder 链加：.pinned(plugin.getPinned()).tags(tags.stream().map(tagService::toDTO).toList())
```

（`PluginService` 注入 `TagService tagService` 复用 `toDTO`。列表 12 条×2 查询的 N+1 在本规模可接受，与 Tool 现状一致，不做过度优化。）`PluginSummaryDTO` 加 `private List<com.iaihub.toolbox.dto.tag.TagDTO> tags;` 与 `private Boolean pinned;`（pinned 在任务 2 已加则跳过），`copySummary()` 补两行。

- [x] **步骤 3.5：Controller 接收 tagIds**

`PluginController` upload/update 各加：

```java
@RequestParam(value = "tagIds", required = false) List<Long> tagIds,
```

并透传给 service。multipart 表单重复 append `tagIds` Spring 自动绑定 List。

- [x] **步骤 3.6：运行测试 + 全量后端测试**

`cd backend && ./gradlew test 2>&1 | tail -10` → 全绿。

- [x] **步骤 3.7：人工确认后 Commit**（`feat(plugin): 接入统一标签系统（PLUGIN 类型 + plugin_tag 关联）`）

---

### 任务 4：启动幂等回填（收藏数 + pinned 归一）

**文件：**
- 创建：`backend/src/main/java/com/iaihub/toolbox/config/PluginCounterBackfillRunner.java`
- 修改：`backend/src/main/java/com/iaihub/toolbox/repository/UnifiedFavoriteRepository.java`（加分组计数查询）
- 测试：`PluginServiceTest.java` 或新建 `PluginCounterBackfillRunnerTest.java`

- [x] **步骤 4.1：编写失败的测试**

```java
@Test
void backfill_syncsFavoriteCountAndScore() {
    Plugin p = Plugin.builder().id(1L).name("p").version("1.0.0")
            .status(Plugin.Status.NORMAL).viewCount(0).likeCount(0).commentCount(0).favoriteCount(0)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    p.setScore(BigDecimal.ZERO);
    when(pluginRepository.findByStatus(Plugin.Status.NORMAL)).thenReturn(List.of(p));
    when(favoriteRepository.countByTargetTypeGroupByTargetId("PLUGIN"))
            .thenReturn(List.of(new Object[]{1L, 3L}));

    runner.run();

    assertEquals(3, p.getFavoriteCount());
    assertEquals(0, new BigDecimal("12").compareTo(p.getScore())); // 3×4
    verify(pluginRepository).save(p);
}
```

- [x] **步骤 4.2：运行验证失败**（类不存在 → 编译失败）

- [x] **步骤 4.3：实现 Runner**

`UnifiedFavoriteRepository` 加：

```java
@Query("SELECT f.targetId, COUNT(f) FROM UnifiedFavorite f WHERE f.targetType = :targetType GROUP BY f.targetId")
List<Object[]> countByTargetTypeGroupByTargetId(@Param("targetType") String targetType);
```

`PluginCounterBackfillRunner`（新文件，`@Component` implements `CommandLineRunner`，注入 `PluginRepository`/`UnifiedFavoriteRepository`，`@Transactional`）：

```java
@Override
public void run(String... args) {
    Map<Long, Long> actualCounts = favoriteRepository
            .countByTargetTypeGroupByTargetId("PLUGIN").stream()
            .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    for (Plugin p : pluginRepository.findByStatus(Plugin.Status.NORMAL)) {
        int actual = actualCounts.getOrDefault(p.getId(), 0L).intValue();
        boolean dirty = false;
        if (!Objects.equals(p.getFavoriteCount(), actual)) { p.setFavoriteCount(actual); dirty = true; }
        if (p.getPinned() == null) { p.setPinned(false); dirty = true; } // 新列存量行 NULL 归一，修复排序
        if (dirty) { p.updateScore(); pluginRepository.save(p); }
    }
}
```

（`PluginRepository.findByStatus` 若无现成方法则加 `List<Plugin> findByStatus(Plugin.Status status);` 派生查询。Runner 幂等：第二次启动无差异即零写入。）

- [x] **步骤 4.4：运行测试通过 + 人工确认后 Commit**（`feat(plugin): 启动幂等回填插件收藏数与置顶标记`）

---

### 任务 5：前端类型与 API 层

**文件：**
- 修改：`frontend/src/types/plugin.ts`
- 修改：`frontend/src/services/plugin.ts`

- [x] **步骤 5.1：类型**

`PluginSummary` 接口 `likeCount` 后加：

```ts
favoriteCount: number
pinned: boolean
tags: Tag[]
```

文件顶部 `import type { Tag } from '@/types'`（若 types/index.ts 导出 Tag 的 path 不同，以 `ToolSummary` 引用的同款 import 为准）。

- [x] **步骤 5.2：API**

`pluginApi` 内加：

```ts
getHotTop5(): Promise<number[]> {
  return api.get('/plugins/hot-top5').then(res => res.data.data as number[])
},
pin(id: number): Promise<void> {
  return api.post(`/plugins/${id}/pin`).then(() => undefined)
},
unpin(id: number): Promise<void> {
  return api.delete(`/plugins/${id}/pin`).then(() => undefined)
},
```

`upload(...)`/`update(...)` 参数末尾加 `tagIds?: number[]`，FormData 组装处加：

```ts
if (tagIds) tagIds.forEach(id => fd.append('tagIds', String(id)))
```

- [x] **步骤 5.3：类型检查**

`cd frontend && npx vue-tsc --noEmit 2>&1 | grep -c "error TS"` → 数量不超过存量 8（页面用到新字段的报错在任务 6/7 消化，若因 PluginSummary 新必填字段导致既有 mock 报错，把新字段声明为 `favoriteCount: number; pinned: boolean; tags: Tag[]` 后同步修 mock）。
⚠️ 后端未重启前接口无新字段，前端渲染处一律 `p.favoriteCount ?? 0`、`p.tags || []` 兜底。

---

### 任务 6：插件市场页（收藏数 + 角标 + 标签 + 置顶按钮）

**文件：**
- 修改：`frontend/src/pages/plugin/PluginMarketPage.vue`

- [x] **步骤 6.1：脚本**

imports 增加：`Bookmark, ArrowUp, Flame, Pin, PinOff` 加入第 4 行 `@lucide/vue` 导入；`import TagBadge from '@/components/common/TagBadge.vue'`。

状态与方法（放 `const plugins = ref(...)` 附近）：

```ts
const hotTop5Ids = ref<Set<number>>(new Set())
const pinLoadingId = ref<number | null>(null)

const loadHotTop5 = async () => {
  try { hotTop5Ids.value = new Set(await pluginApi.getHotTop5()) } catch { /* 静默降级 */ }
}

const handlePinPlugin = async (p: PluginSummary) => {
  if (pinLoadingId.value === p.id) return
  pinLoadingId.value = p.id
  try {
    if (p.pinned) { await pluginApi.unpin(p.id) } else { await pluginApi.pin(p.id) }
    p.pinned = !p.pinned
  } catch { ElMessage.error('操作失败') } finally { pinLoadingId.value = null }
}
```

`onMounted(load)` 改为 `onMounted(() => { load(); loadHotTop5() })`；`changeSort` 与 `search` 后也刷新 `loadHotTop5()` 可不做（榜单不随查询变化）。

- [x] **步骤 6.2：模板**

名称行（`plugin-name-row`）内 `version-tag` 之后加角标：

```html
<span v-if="p.pinned" class="badge-pill badge-pinned">
  <ArrowUp :size="12" aria-hidden="true" />
  <span>置顶</span>
</span>
<span v-if="hotTop5Ids.has(p.id)" class="badge-pill badge-hot">
  <Flame :size="12" aria-hidden="true" />
  <span>热门</span>
</span>
```

stats 行 Heart 的 `</span>` 后插入：

```html
<span class="stat">
  <Bookmark :size="14" aria-hidden="true" />
  {{ fmtCount(p.favoriteCount ?? 0) }}
</span>
```

`plugin-desc` 之后加标签行：

```html
<div v-if="p.tags && p.tags.length" class="plugin-tags">
  <TagBadge v-for="t in p.tags.slice(0, 3)" :key="t.id" :tag="t" />
</div>
```

管理员置顶按钮：卡片根元素内右上角（参考 HomePage `btn-icon-pin-tool` 放法）：

```html
<button
  v-if="authStore.isAdmin"
  class="btn-icon-pin"
  :aria-label="p.pinned ? '取消置顶' : '置顶'"
  :disabled="pinLoadingId === p.id"
  @click.stop="handlePinPlugin(p)"
>
  <PinOff v-if="p.pinned" :size="14" />
  <Pin v-else :size="14" />
</button>
```

- [x] **步骤 6.3：样式**

scoped style 补 `.badge-pill/.badge-pinned/.badge-hot/.btn-icon-pin/.plugin-tags`——从 `HomePage.vue`/`ToolCard` 对应样式复制（数值色用现有 CSS 变量；先 grep 定位 HomePage 里 `.badge-pinned`/`.badge-hot` 块，原样搬运保持视觉一致）。

- [x] **步骤 6.4：类型检查**

`cd frontend && npx vue-tsc --noEmit 2>&1 | grep "PluginMarketPage" ` → 无新增报错。

---

### 任务 7：上传/编辑/详情页标签接入

**文件：**
- 修改：`frontend/src/pages/plugin/PluginUploadPage.vue`
- 修改：`frontend/src/pages/plugin/PluginEditPage.vue`
- 修改：`frontend/src/pages/plugin/PluginDetailPage.vue`

- [x] **步骤 7.1：上传页**

`import TagSelector from '@/components/common/TagSelector.vue'`；`const selectedTags = ref<Tag[]>([])`；表单区 logo/source 字段后加 `<TagSelector v-model="selectedTags" tagType="PLUGIN" />`；提交调用改 `pluginApi.upload(file, source, logoUrl, onProgress, selectedTags.value.map(t => t.id))`（参数顺序对齐任务 5.2 签名）。

- [x] **步骤 7.2：编辑页**

同上传页接入 TagSelector；页面加载详情后回填：`selectedTags.value = detail.tags ?? []`；提交传 tagIds。

- [x] **步骤 7.3：详情页**

标题区下方加：

```html
<div v-if="plugin.tags && plugin.tags.length" class="plugin-tags">
  <TagBadge v-for="t in plugin.tags" :key="t.id" :tag="t" />
</div>
```

管理员置顶按钮与 stats 收藏数（Bookmark + `favoriteCount`）同步加到详情页头部信息行（与列表页同款样式）。

- [x] **步骤 7.4：类型检查**

`cd frontend && npx vue-tsc --noEmit 2>&1 | grep -E "plugin/" ` → 无报错。人工确认后 Commit（前后端合并为规格约定的两个 commit 时，此处先只提交任务 5-7：`feat(plugin): 市场页收藏/置顶/热门展示与标签接入`）。

---

### 任务 8：端到端验证（运行中服务实测）

- [x] **步骤 8.1：重启后端加载新 schema/代码**

```bash
pkill -f "gradlew bootRun"; export JAVA_HOME="/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home"
cd backend && ./gradlew bootRun --args='--spring.profiles.active=postgresql' &
```

等待 `Started ToolSquareApplication`（约 10~45s）。确认 PG `plugin` 表新列与 `plugin_tag` 表已由 Hibernate 生成：`psql ... -c "\d plugin"`。

- [x] **步骤 8.2：API 级验证**

```bash
TOKEN=$(登录 admin 取 token)
# 1. 收藏：对某插件 POST /api/v1/interactions/favorite（具体路径以 UnifiedInteractionController 为准）toggle 两次，
#    断言 favoriteCount 0→1→0 且 score ±4；
# 2. GET /api/v1/plugins/hot-top5 → 返回 ≤5 个 ID；
# 3. POST /api/v1/plugins/{id}/pin 用 admin token → 200；USER token → 403；
# 4. sort=hot 列表：置顶插件排首位；
# 5. 上传/编辑带 tagIds 后 GET 详情 tags 正确、Tag usageCount 变化正确。
```

- [x] **步骤 8.3：浏览器实测**

http://localhost:5173/plugins ：卡片显示 🔖 收藏数；置顶/热门角标出现；标签徽章渲染；管理员 Pin 按钮点击后角标与排序实时变化；上传/编辑页 TagSelector 可选标签并持久化。

- [x] **步骤 8.4：`make lint` + 后端全量测试收尾**，向用户汇报验证证据并确认可归档。

## 自检记录

- 规格覆盖度：收藏（任务1）✓ 计入score（任务1）✓ 回填（任务4）✓ 标签（任务3/7）✓ 置顶（任务2/6）✓ hot-top5（任务2/6）✓ 卡片角标（任务6）✓ 测试计划（各任务TDD + 8）✓
- 类型一致性：`favoriteCount/pinned/tags` 命名贯穿 DTO/entity/TS 一致；`replaceTags`、`pinPlugin/unpinPlugin/getHotTop5` 命名任务内引用一致。
- 实现与规格的两处微偏差（已论证）：回填写入独立 `PluginCounterBackfillRunner` 而非 DataInitializer（其 early-return 会跳过回填）；`pinned` 列不加 `nullable=false`（ddl-auto 加列不回填存量行）。
