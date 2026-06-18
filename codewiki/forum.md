# 论坛模块（Forum）文档

## 概述

论坛模块是 IAIHub Toolbox 平台的社区交流核心，提供帖子发布、评论互动、点赞、收藏、标签管理和分类浏览等完整的社区功能。模块采用分层架构设计，包含数据模型层（Model）、数据访问层（Repository）、业务逻辑层（Service）和接口控制层（Controller），支持登录用户和匿名访客两种身份的差异化交互。

---

## 架构总览

```mermaid
graph TB
    subgraph 客户端
        FE[前端应用]
    end

    subgraph 论坛模块
        subgraph Controller层
            PC[ForumPostController<br/>帖子接口]
            CC[ForumCommentController<br/>评论接口]
            LC[ForumLikeController<br/>点赞接口]
            TC[ForumTagController<br/>标签接口]
            CATC[ForumCategoryController<br/>分类接口]
            FC[PostFavoriteController<br/>收藏接口]
        end

        subgraph Service层
            PS[ForumPostService]
            CS[ForumCommentService]
            LS[ForumLikeService]
            TS[ForumTagService]
            CATS[ForumCategoryService]
            FAVS[PostFavoriteService]
        end

        subgraph Repository层
            PR[ForumPostRepository]
            CR[ForumCommentRepository]
            LR[ForumLikeRepository]
            TR[ForumTagRepository]
            CATR[ForumCategoryRepository]
            PTR[ForumPostTagRepository]
            FAVR[PostFavoriteRepository]
        end

        subgraph Model层
            PM[ForumPost]
            CM[ForumComment]
            LM[ForumLike]
            TM[ForumTag]
            CATM[ForumCategory]
            PTM[ForumPostTag]
            FAVM[PostFavorite]
        end
    end

    subgraph 数据库
        DB[(MySQL)]
    end

    FE -->|HTTP API| PC & CC & LC & TC & CATC & FC
    PC --> PS
    CC --> CS
    LC --> LS
    TC --> TS
    CATC --> CATS
    FC --> FAVS

    PS --> PR & CATR & PTR
    CS --> CR & PR
    LS --> LR & PR & CR
    TS --> TR
    CATS --> CATR
    FAVS --> FAVR & PR

    PR --> PM
    CR --> CM
    LR --> LM
    TR --> TM
    CATR --> CATM
    PTR --> PTM
    FAVR --> FAVM

    PM & CM & LM & TM & CATM & PTM & FAVM --> DB
```

---

## 数据模型

### 实体关系图

```mermaid
erDiagram
    ForumPost ||--o{ ForumComment : "拥有评论"
    ForumPost ||--o{ ForumLike : "被点赞"
    ForumPost ||--o{ PostFavorite : "被收藏"
    ForumPost }o--|| ForumCategory : "属于分类"
    ForumPost ||--o{ ForumPostTag : "关联标签"
    ForumTag ||--o{ ForumPostTag : "被帖子引用"
    ForumComment ||--o{ ForumComment : "嵌套回复"
    ForumComment ||--o{ ForumLike : "被点赞"
    User ||--o{ ForumPost : "作者"
    User ||--o{ ForumComment : "评论者"
    User ||--o{ ForumLike : "点赞者"
    User ||--o{ PostFavorite : "收藏者"

    ForumPost {
        Long id PK
        String title "标题, 非空, 200字符"
        String content "内容, TEXT"
        Long authorId "作者ID, 非空"
        Long categoryId "分类ID, 非空"
        Integer viewCount "浏览数, 默认0"
        Integer likeCount "点赞数, 默认0"
        Integer commentCount "评论数, 默认0"
        ForumPostStatus status "状态: NORMAL/DELETED/HIDDEN"
        BigDecimal score "热度分数"
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }

    ForumComment {
        Long id PK
        Long postId "帖子ID, 非空"
        Long authorId "作者ID, 可空(匿名)"
        String authorName "作者名, 50字符"
        Long parentId "父评论ID"
        Long rootId "根评论ID"
        String content "内容, TEXT"
        Integer likeCount "点赞数, 默认0"
        LocalDateTime createdAt
        LocalDateTime updatedAt
    }

    ForumLike {
        Long id PK
        Long postId "帖子ID, 可空"
        Long commentId "评论ID, 可空"
        Long userId "用户ID, 可空(匿名)"
        String ipHash "IP哈希, 64字符"
        LocalDateTime createdAt
    }

    ForumTag {
        Long id PK
        String name "标签名, 唯一, 50字符"
        Integer postCount "帖子数, 默认0"
        Boolean isSystem "是否系统标签"
        LocalDateTime createdAt
    }

    ForumCategory {
        Long id PK
        String name "分类名, 唯一, 50字符"
        String description "描述, 255字符"
        Integer sortOrder "排序, 默认0"
        LocalDateTime createdAt
    }

    ForumPostTag {
        Long postId PK "帖子ID"
        Long tagId PK "标签ID"
    }

    PostFavorite {
        Long id PK
        Long userId "用户ID, 非空"
        Long postId "帖子ID, 非空"
        LocalDateTime createdAt
    }
```

### 帖子状态枚举（ForumPostStatus）

| 状态值 | 说明 |
|--------|------|
| `NORMAL` | 正常状态，可见可交互 |
| `DELETED` | 已删除（软删除），列表不展示 |
| `HIDDEN` | 已隐藏，列表不展示 |

### 热度评分算法

`ForumPost` 实体内置了热度评分计算方法，评分公式为：

```
score = viewCount × 1 + likeCount × 3 + commentCount × 5
```

该评分用于 [overview 模块](overview.md) 的帖子排行榜功能，按分类对帖子进行热度排名。

---

## Service 层详解

### 1. ForumPostService — 帖子服务

负责帖子的完整生命周期管理，包括创建、查询、更新、删除（软删除）。

```mermaid
flowchart LR
    subgraph 查询
        A1[getPostList] --> A2{有keyword?}
        A2 -->|是| A3[searchByTitle<br/>标题模糊搜索]
        A2 -->|否| A4{有categoryId?}
        A4 -->|是| A5[findByCategoryIdAndStatus]
        A4 -->|否| A6[findByStatusOrderByCreatedAtDesc]
        A3 & A5 & A6 --> A7[toDTO 转换]
    end

    subgraph 创建
        B1[createPost] --> B2[保存帖子实体]
        B2 --> B3{有tagIds?}
        B3 -->|是| B4[批量保存ForumPostTag]
        B3 -->|否| B5[返回DTO]
        B4 --> B5
    end

    subgraph 删除
        C1[deletePost] --> C2[校验作者权限]
        C2 --> C3[设置status=DELETED]
        C3 --> C4[保存]
    end
```

**核心方法：**

| 方法 | 说明 | 事务 |
|------|------|------|
| `getPostList(categoryId, keyword, pageable)` | 分页查询帖子列表，支持按分类筛选和标题搜索 | 否 |
| `getMyPosts(userId, pageable)` | 查询指定用户的帖子 | 否 |
| `getPostById(id)` | 获取帖子详情，**自动增加浏览数** | 否 |
| `createPost(authorId, request)` | 创建帖子并关联标签 | 是 |
| `updatePost(postId, userId, request)` | 更新帖子（仅作者可操作） | 是 |
| `deletePost(postId, userId)` | 软删除帖子（仅作者可操作） | 是 |

> **权限控制**：更新和删除操作通过比较 `post.getAuthorId()` 与当前用户 ID 实现所有权校验，不匹配时抛出 `ForbiddenException`。

### 2. ForumCommentService — 评论服务

支持评论的创建、回复（嵌套）和删除，并维护帖子的评论计数。

```mermaid
flowchart TB
    subgraph 创建评论
        D1[createComment] --> D2[校验帖子存在]
        D2 --> D3[保存评论实体]
        D3 --> D4[帖子commentCount + 1]
    end

    subgraph 创建回复
        E1[createReply] --> E2[校验父评论存在]
        E2 --> E3[设置parentId和rootId]
        E3 --> E4[保存回复实体]
    end

    subgraph 删除评论
        F1[deleteComment] --> F2[校验作者权限]
        F2 --> F3[物理删除评论]
        F3 --> F4[帖子commentCount - 1<br/>最小为0]
    end
```

**嵌套回复设计：**

评论采用两级嵌套模型，通过 `parentId` 和 `rootId` 实现：

- **`parentId`**：直接父评论的 ID
- **`rootId`**：根评论的 ID（若回复的是根评论，则 `rootId = parentId`；若回复的是子评论，则 `rootId` 继承父评论的 `rootId`）

这种设计允许前端通过 `rootId` 将同一讨论线程下的所有评论聚合展示。

> **匿名评论支持**：当用户未登录时，`authorId` 为 `null`，使用 `authorName` 作为匿名标识。已登录用户的 `authorName` 会被置为 `null`，以 `authorId` 为准。

### 3. ForumLikeService — 点赞服务

支持帖子和评论的点赞/取消点赞，同时兼容登录用户和匿名访客。

```mermaid
flowchart TB
    subgraph 点赞流程
        G1[likePost / likeComment] --> G2{用户已登录?}
        G2 -->|是| G3[按userId查重]
        G2 -->|否| G4[按ipHash查重]
        G3 --> G5{已点赞?}
        G4 --> G5
        G5 -->|是| G6[抛出BusinessException<br/>已点赞]
        G5 -->|否| G7[保存ForumLike]
        G7 --> G8[目标likeCount + 1]
    end

    subgraph 取消点赞
        H1[unlikePost] --> H2{用户已登录?}
        H2 -->|是| H3[按userId查找]
        H2 -->|否| H4[按ipHash查找]
        H3 & H4 --> H5{找到记录?}
        H5 -->|是| H6[删除记录<br/>likeCount - 1]
        H5 -->|否| H7[静默返回]
    end
```

**防重复点赞机制：**

| 身份 | 去重标识 | 说明 |
|------|----------|------|
| 登录用户 | `userId` | 每个用户对同一帖子/评论只能点赞一次 |
| 匿名访客 | `ipHash` | 同一 IP 对同一帖子/评论只能点赞一次 |

> **IP 哈希处理**：在 `ForumLikeController` 中，匿名用户的 IP 地址通过 SHA-256 哈希处理后存储，保护用户隐私的同时实现去重。

### 4. ForumTagService — 标签服务

| 方法 | 说明 |
|------|------|
| `getAllTags()` | 获取全部标签列表 |
| `getHotTags()` | 获取热门标签（按 `postCount` 降序取前 10） |
| `createTag(name, isSystem)` | 创建标签，重名时抛出 `DuplicateResourceException` |

### 5. ForumCategoryService — 分类服务

提供论坛分类的查询功能，按 `sortOrder` 升序返回所有分类。

### 6. PostFavoriteService — 收藏服务

```mermaid
flowchart LR
    I1[addFavorite] --> I2{已收藏?}
    I2 -->|是| I3[返回已有记录]
    I2 -->|否| I4[创建新收藏]
    I5[removeFavorite] --> I6{存在收藏?}
    I6 -->|是| I7[删除收藏]
    I6 -->|否| I8[返回false]
    I9[getUserFavoritePosts] --> I10[查询用户收藏列表]
    I10 --> I11[批量查询帖子实体]
```

收藏服务通过 `PostFavorite` 实体的唯一约束（`user_id` + `post_id`）保证幂等性，重复收藏不会创建多条记录。

---

## Controller 层 — API 接口

### 接口总览

```mermaid
graph LR
    subgraph 帖子接口
        P1["GET /api/forum/posts<br/>帖子列表"]
        P2["GET /api/forum/posts/my<br/>我的帖子"]
        P3["GET /api/forum/posts/{id}<br/>帖子详情"]
        P4["POST /api/forum/posts<br/>发帖"]
        P5["PUT /api/forum/posts/{id}<br/>编辑帖子"]
        P6["DELETE /api/forum/posts/{id}<br/>删除帖子"]
    end

    subgraph 评论接口
        C1["GET /api/forum/posts/{postId}/comments<br/>评论列表"]
        C2["POST /api/forum/posts/{postId}/comments<br/>发评论/回复"]
        C3["DELETE /api/forum/comments/{id}<br/>删除评论"]
    end

    subgraph 点赞接口
        L1["POST /api/forum/likes<br/>点赞"]
        L2["DELETE /api/forum/likes<br/>取消点赞"]
    end

    subgraph 标签接口
        T1["GET /api/forum/tags<br/>全部标签"]
        T2["GET /api/forum/tags/hot<br/>热门标签"]
        T3["POST /api/forum/tags<br/>创建标签"]
    end

    subgraph 分类接口
        CAT1["GET /api/forum/categories<br/>全部分类"]
    end

    subgraph 收藏接口
        F1["POST /api/v1/post-favorites/{postId}<br/>收藏"]
        F2["DELETE /api/v1/post-favorites/{postId}<br/>取消收藏"]
        F3["GET /api/v1/post-favorites<br/>收藏列表"]
        F4["GET /api/v1/post-favorites/posts<br/>收藏帖子"]
        F5["GET /api/v1/post-favorites/check/{postId}<br/>检查收藏"]
    end
```

### 接口详情

#### 帖子接口（ForumPostController）

| 方法 | 路径 | 认证 | 参数 | 说明 |
|------|------|------|------|------|
| GET | `/api/forum/posts` | 否 | `category`, `tag`, `keyword`, `page`(默认0), `size`(默认10) | 分页查询帖子列表 |
| GET | `/api/forum/posts/my` | **是** | `page`, `size` | 查询当前用户的帖子 |
| GET | `/api/forum/posts/{id}` | 否 | — | 获取帖子详情（自动+1浏览数） |
| POST | `/api/forum/posts` | **是** | `ForumPostCreateRequest` | 创建帖子 |
| PUT | `/api/forum/posts/{id}` | **是** | `ForumPostCreateRequest` | 更新帖子（仅作者） |
| DELETE | `/api/forum/posts/{id}` | **是** | — | 删除帖子（仅作者，软删除） |

#### 评论接口（ForumCommentController）

| 方法 | 路径 | 认证 | 参数 | 说明 |
|------|------|------|------|------|
| GET | `/api/forum/posts/{postId}/comments` | 否 | — | 获取帖子的全部评论 |
| POST | `/api/forum/posts/{postId}/comments` | 可选 | `ForumCommentCreateRequest` | 发表评论或回复（含 `parentId` 时为回复） |
| DELETE | `/api/forum/comments/{id}` | **是** | — | 删除评论（仅作者） |

#### 点赞接口（ForumLikeController）

| 方法 | 路径 | 认证 | 参数 | 说明 |
|------|------|------|------|------|
| POST | `/api/forum/likes` | 可选 | `ForumLikeRequest` | 点赞帖子或评论 |
| DELETE | `/api/forum/likes` | 可选 | `ForumLikeRequest` | 取消点赞帖子 |

> `ForumLikeRequest` 中 `postId` 和 `commentId` 二选一，不可同时为空或同时有值（构造时校验）。

#### 标签接口（ForumTagController）

| 方法 | 路径 | 认证 | 参数 | 说明 |
|------|------|------|------|------|
| GET | `/api/forum/tags` | 否 | — | 获取全部标签 |
| GET | `/api/forum/tags/hot` | 否 | — | 获取热门标签 Top 10 |
| POST | `/api/forum/tags` | 可选 | `TagCreateRequest{name, isSystem}` | 创建标签 |

#### 分类接口（ForumCategoryController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/forum/categories` | 否 | 获取全部分类（按 sortOrder 排序） |

#### 收藏接口（PostFavoriteController）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/v1/post-favorites/{postId}` | **是** | 收藏帖子 |
| DELETE | `/api/v1/post-favorites/{postId}` | **是** | 取消收藏 |
| GET | `/api/v1/post-favorites` | **是** | 获取用户收藏列表 |
| GET | `/api/v1/post-favorites/posts` | **是** | 获取用户收藏的帖子实体列表 |
| GET | `/api/v1/post-favorites/check/{postId}` | **是** | 检查是否已收藏 |

> **认证差异**：收藏接口通过 `HttpServletRequest` 手动解析 JWT Token 获取用户 ID（依赖 `JwtUtil`），而其他论坛接口使用 Spring Security 的 `@AuthenticationPrincipal User` 注解。详见 [authentication 模块](authentication.md)。

---

## DTO 数据传输对象

```mermaid
classDiagram
    class ForumPostCreateRequest {
        +String title
        +String content
        +Long categoryId
        +List~Long~ tagIds
    }

    class ForumPostDTO {
        +Long id
        +String title
        +String content
        +Long authorId
        +String authorName
        +String authorNickname
        +Long categoryId
        +String categoryName
        +Integer viewCount
        +Integer likeCount
        +Integer commentCount
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class ForumCommentCreateRequest {
        +String content
        +Long parentId
        +String authorName
    }

    class ForumCommentDTO {
        +Long id
        +Long postId
        +Long authorId
        +String authorName
        +String authorNickname
        +Long parentId
        +Long rootId
        +String content
        +Integer likeCount
        +LocalDateTime createdAt
    }

    class ForumLikeRequest {
        +Long postId
        +Long commentId
    }

    class ForumTagDTO {
        +Long id
        +String name
        +Integer postCount
        +Boolean isSystem
    }

    class ForumCategoryDTO {
        +Long id
        +String name
        +String description
        +Integer sortOrder
        +Integer postCount
    }
```

---

## 核心业务流程

### 发帖流程

```mermaid
sequenceDiagram
    participant U as 用户
    participant C as ForumPostController
    participant S as ForumPostService
    participant PR as ForumPostRepository
    participant PTR as ForumPostTagRepository
    participant CATR as ForumCategoryRepository
    participant UR as UserRepository

    U->>C: POST /api/forum/posts (ForumPostCreateRequest)
    C->>C: 校验 @AuthenticationPrincipal User
    C->>S: createPost(authorId, request)
    S->>S: 构建 ForumPost 实体 (status=NORMAL)
    S->>PR: save(post)
    PR-->>S: 返回保存后的 post (含id)

    alt 有 tagIds
        loop 每个 tagId
            S->>PTR: save(ForumPostTag)
        end
    end

    S->>CATR: findById(categoryId)
    CATR-->>S: 分类名称
    S->>UR: findById(authorId)
    UR-->>S: 作者用户名/昵称
    S-->>C: ForumPostDTO
    C-->>U: 201 Created + ForumPostDTO
```

### 评论与嵌套回复流程

```mermaid
sequenceDiagram
    participant U as 用户/访客
    participant C as ForumCommentController
    participant S as ForumCommentService
    participant CR as ForumCommentRepository
    participant PR as ForumPostRepository

    U->>C: POST /api/forum/posts/{postId}/comments
    C->>C: 判断是否登录 + 是否有 parentId

    alt 有 parentId（回复）
        C->>S: createReply(postId, authorId, authorName, content, parentId)
        S->>CR: findById(parentId)
        CR-->>S: 父评论
        S->>S: 设置 rootId = parent.rootId ?? parentId
        S->>CR: save(reply)
    else 无 parentId（评论）
        C->>S: createComment(postId, authorId, authorName, content)
        S->>PR: findById(postId) 校验帖子存在
        S->>CR: save(comment)
        S->>PR: 更新帖子 commentCount + 1
    end

    S-->>C: ForumCommentDTO
    C-->>U: 201 Created
```

### 点赞流程（登录用户 vs 匿名用户）

```mermaid
sequenceDiagram
    participant U as 用户/访客
    participant C as ForumLikeController
    participant S as ForumLikeService
    participant LR as ForumLikeRepository
    participant PR as ForumPostRepository

    U->>C: POST /api/forum/likes (ForumLikeRequest)

    alt 登录用户
        C->>C: userId = user.getId()
        C->>S: likePost(postId, userId, null)
        S->>LR: existsByUserIdAndPostId(userId, postId)
    else 匿名访客
        C->>C: ipHash = SHA-256(remoteAddr)
        C->>S: likePost(postId, null, ipHash)
        S->>LR: existsByIpHashAndPostId(ipHash, postId)
    end

    alt 已点赞
        LR-->>S: true
        S-->>C: 抛出 BusinessException("已点赞")
        C-->>U: 400 错误
    else 未点赞
        LR-->>S: false
        S->>LR: save(ForumLike)
        S->>PR: 更新帖子 likeCount + 1
        S-->>C: 成功
        C-->>U: 201 Created
    end
```

---

## 跨模块依赖

```mermaid
graph LR
    subgraph forum[论坛模块]
        FP[ForumPost]
        FR[ForumPostRepository]
        FCR[ForumCategoryRepository]
    end

    subgraph auth[authentication 模块]
        U[User 实体]
        UR[UserRepository]
        JWT[JwtUtil]
    end

    subgraph mcp[mcp-server 模块]
        MS[McpSearchService]
        PSR[PostSearchResult]
    end

    subgraph ov[overview 模块]
        OS[OverviewServiceImpl]
        PRD[PostRankDto]
    end

    FP -.->|authorId 引用| U
    FR -->|查询作者信息| UR
    JWT -.->|Token解析| UR

    MS -->|搜索帖子| FR
    MS -->|返回结果| PSR

    OS -->|统计帖子数| FR
    OS -->|帖子排行榜| FR
    OS -->|分类信息| FCR
    OS -->|热度评分| FP
    OS -->|返回排名| PRD
```

### 依赖说明

| 依赖方向 | 说明 | 参考文档 |
|----------|------|----------|
| forum → authentication | 帖子和评论通过 `authorId` 关联 `User` 实体；收藏接口使用 `JwtUtil` 解析 Token | [authentication.md](authentication.md) |
| mcp-server → forum | `McpSearchService` 调用 `ForumPostRepository` 搜索帖子，返回 `PostSearchResult` | [mcp-server.md](mcp-server.md) |
| overview → forum | `OverviewServiceImpl` 统计帖子总数、按分类生成帖子热度排行榜（使用 `ForumPost.score`） | [overview.md](overview.md) |

---

## 数据库索引设计

| 表名 | 索引名 | 索引列 | 用途 |
|------|--------|--------|------|
| `forum_post` | `idx_forum_post_author` | `author_id` | 加速按作者查询帖子 |
| `forum_post` | `idx_forum_post_category` | `category_id` | 加速按分类筛选帖子 |
| `forum_post` | `idx_forum_post_created` | `created_at` | 加速按时间排序查询 |
| `forum_comment` | `idx_forum_comment_post` | `post_id` | 加速按帖子查询评论 |
| `forum_comment` | `idx_forum_comment_root` | `root_id` | 加速按根评论聚合回复线程 |
| `post_favorites` | 唯一约束 | `user_id, post_id` | 防止重复收藏 |

---

## 前端类型映射

论坛模块的前端类型定义在 `frontend/src/types/forum.ts` 中，与后端 DTO 保持一一对应：

| 后端 DTO | 前端接口 | 差异说明 |
|----------|----------|----------|
| `ForumPostDTO` | `ForumPost` | 前端额外包含 `authorAvatarUrl`、`isFavorited`、`favoriteCount` 字段 |
| `ForumCategoryDTO` | `ForumCategory` | 完全一致 |
| `ForumTagDTO` | `ForumTag` | 完全一致 |
| `ForumPostCreateRequest` | `ForumPostCreateRequest` | 完全一致 |
| `ForumCommentCreateRequest` | `ForumCommentCreateRequest` | 完全一致 |
| `ForumLikeRequest` | `ForumLikeRequest` | 完全一致 |
| `ForumCommentDTO` | `ForumComment` | 完全一致 |
| Spring `Page<T>` | `PageResponse<T>` | 字段映射：`content`/`totalElements`/`totalPages`/`size`/`number` |

---

## 设计要点总结

1. **软删除策略**：帖子删除采用状态标记（`DELETED`）而非物理删除，保留数据可追溯性；评论删除为物理删除。
2. **双身份支持**：点赞和评论功能同时支持登录用户（`userId`）和匿名访客（`ipHash`/`authorName`），降低参与门槛。
3. **计数冗余**：帖子的 `viewCount`、`likeCount`、`commentCount` 作为冗余字段存储，避免实时聚合查询，但需在相关操作中同步维护。
4. **嵌套评论模型**：通过 `parentId` + `rootId` 两级引用实现评论线程，支持无限深度的回复展示。
5. **热度评分**：帖子内置 `score` 字段和 `updateScore()` 方法，采用加权公式（浏览×1 + 点赞×3 + 评论×5）量化热度，供排行榜使用。
6. **标签多对多**：通过 `ForumPostTag` 关联表实现帖子与标签的多对多关系，使用复合主键（`postId` + `tagId`）。
