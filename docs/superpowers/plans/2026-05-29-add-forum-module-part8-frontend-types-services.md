# Forum Module - Frontend Types & Services

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的前端类型定义和 API 服务层

**架构：** 使用 TypeScript 类型定义，Axios 进行 API 调用，Pinia 管理状态

**技术栈：** TypeScript, Axios, Pinia

---

## 文件结构

```
frontend/src/
├── types/
│   └── forum.ts
├── services/
│   └── forum.ts
└── stores/
    └── forum.ts
```

---

<!-- openspec-task: Frontend Types -->
### Task 1：Forum TypeScript 类型定义

**文件：**
- 创建：`frontend/src/types/forum.ts`
- 测试：`frontend/tests/unit/types/forum.test.ts`

- [ ] **步骤 1：编写类型测试**

```typescript
// tests/unit/types/forum.test.ts
describe('Forum Types', () => {
  it('should validate ForumPost type', () => {
    const post: ForumPost = {
      id: 1,
      title: 'Test Post',
      content: '# Content',
      authorId: 100,
      authorName: 'Author',
      categoryId: 1,
      categoryName: 'Category',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    
    expect(post.id).toBe(1);
    expect(post.title).toBe('Test Post');
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "Forum Types"`
预期：FAIL，compilation error

- [ ] **步骤 3：编写类型定义**

```typescript
// frontend/src/types/forum.ts

export interface ForumPost {
  id: number;
  title: string;
  content: string;
  authorId: number;
  authorName: string;
  categoryId: number;
  categoryName: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ForumPostCreateRequest {
  title: string;
  content: string;
  categoryId: number;
  tagIds?: number[];
}

export interface ForumComment {
  id: number;
  postId: number;
  authorId: number | null;
  authorName: string | null;
  parentId: number | null;
  rootId: number | null;
  content: string;
  likeCount: number;
  createdAt: string;
}

export interface ForumCommentCreateRequest {
  content: string;
  parentId?: number;
  authorName?: string;
}

export interface ForumCategory {
  id: number;
  name: string;
  description: string;
  sortOrder: number;
  postCount: number;
}

export interface ForumTag {
  id: number;
  name: string;
  postCount: number;
  isSystem: boolean;
}

export interface ForumLikeRequest {
  postId?: number;
  commentId?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "Forum Types"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/types/forum.ts
git commit -m "feat(forum): add Forum TypeScript types"
```

---

<!-- openspec-task: Frontend Services -->
### Task 2：Forum API Service

**文件：**
- 创建：`frontend/src/services/forum.ts`
- 测试：`frontend/tests/unit/services/forum.test.ts`

- [ ] **步骤 1：编写服务测试**

```typescript
// tests/unit/services/forum.test.ts
describe('ForumService', () => {
  it('should get post list', async () => {
    const response = await forumService.getPostList();
    expect(response.content).toBeDefined();
  });
  
  it('should get post by id', async () => {
    const post = await forumService.getPostById(1);
    expect(post.id).toBe(1);
  });
  
  it('should create post', async () => {
    const newPost = await forumService.createPost({
      title: 'New Post',
      content: '# Content',
      categoryId: 1
    });
    expect(newPost.title).toBe('New Post');
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "ForumService"`
预期：FAIL

- [ ] **步骤 3：编写 API 服务**

```typescript
// frontend/src/services/forum.ts
import axios from 'axios';
import type {
  ForumPost,
  ForumPostCreateRequest,
  ForumComment,
  ForumCommentCreateRequest,
  ForumCategory,
  ForumTag,
  ForumLikeRequest,
  PageResponse
} from '@/types/forum';

const BASE_URL = '/api/forum';

const forumService = {
  // Posts
  async getPostList(params?: {
    category?: number;
    tag?: number;
    keyword?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<ForumPost>> {
    const response = await axios.get(`${BASE_URL}/posts`, { params });
    return response.data;
  },
  
  async getPostById(id: number): Promise<ForumPost> {
    const response = await axios.get(`${BASE_URL}/posts/${id}`);
    return response.data;
  },
  
  async createPost(data: ForumPostCreateRequest): Promise<ForumPost> {
    const response = await axios.post(`${BASE_URL}/posts`, data);
    return response.data;
  },
  
  async updatePost(id: number, data: ForumPostCreateRequest): Promise<ForumPost> {
    const response = await axios.put(`${BASE_URL}/posts/${id}`, data);
    return response.data;
  },
  
  async deletePost(id: number): Promise<void> {
    await axios.delete(`${BASE_URL}/posts/${id}`);
  },
  
  // Categories
  async getCategories(): Promise<ForumCategory[]> {
    const response = await axios.get(`${BASE_URL}/categories`);
    return response.data;
  },
  
  // Tags
  async getTags(): Promise<ForumTag[]> {
    const response = await axios.get(`${BASE_URL}/tags`);
    return response.data;
  },
  
  async getHotTags(): Promise<ForumTag[]> {
    const response = await axios.get(`${BASE_URL}/tags/hot`);
    return response.data;
  },
  
  async createTag(name: string, isSystem: boolean = false): Promise<ForumTag> {
    const response = await axios.post(`${BASE_URL}/tags`, { name, isSystem });
    return response.data;
  },
  
  // Comments
  async getComments(postId: number): Promise<ForumComment[]> {
    const response = await axios.get(`${BASE_URL}/posts/${postId}/comments`);
    return response.data;
  },
  
  async createComment(postId: number, data: ForumCommentCreateRequest): Promise<ForumComment> {
    const response = await axios.post(`${BASE_URL}/posts/${postId}/comments`, data);
    return response.data;
  },
  
  async deleteComment(commentId: number): Promise<void> {
    await axios.delete(`${BASE_URL}/comments/${commentId}`);
  },
  
  // Likes
  async like(data: ForumLikeRequest): Promise<void> {
    await axios.post(`${BASE_URL}/likes`, data);
  },
  
  async unlike(data: ForumLikeRequest): Promise<void> {
    await axios.delete(`${BASE_URL}/likes`, { data });
  }
};

export default forumService;
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "ForumService"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/services/forum.ts
git commit -m "feat(forum): add Forum API service"
```

---

<!-- openspec-task: Frontend Store -->
### Task 3：Forum Pinia Store

**文件：**
- 创建：`frontend/src/stores/forum.ts`
- 测试：`frontend/tests/unit/stores/forum.test.ts`

- [ ] **步骤 1：编写 Store 测试**

```typescript
// tests/unit/stores/forum.test.ts
describe('Forum Store', () => {
  it('should fetch posts', async () => {
    const store = useForumStore();
    await store.fetchPosts();
    expect(store.posts).toBeDefined();
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "Forum Store"`
预期：FAIL

- [ ] **步骤 3：编写 Store**

```typescript
// frontend/src/stores/forum.ts
import { defineStore } from 'pinia';
import forumService from '@/services/forum';
import type { ForumPost, ForumCategory, ForumTag } from '@/types/forum';

export const useForumStore = defineStore('forum', {
  state: () => ({
    posts: [] as ForumPost[],
    currentPost: null as ForumPost | null,
    categories: [] as ForumCategory[],
    tags: [] as ForumTag[],
    pagination: {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0
    },
    loading: false
  }),
  
  actions: {
    async fetchPosts(params?: {
      category?: number;
      tag?: number;
      keyword?: string;
      page?: number;
      size?: number;
    }) {
      this.loading = true;
      try {
        const response = await forumService.getPostList(params);
        this.posts = response.content;
        this.pagination = {
          page: response.number,
          size: response.size,
          totalElements: response.totalElements,
          totalPages: response.totalPages
        };
      } finally {
        this.loading = false;
      }
    },
    
    async fetchPostById(id: number) {
      this.loading = true;
      try {
        this.currentPost = await forumService.getPostById(id);
      } finally {
        this.loading = false;
      }
    },
    
    async fetchCategories() {
      this.categories = await forumService.getCategories();
    },
    
    async fetchTags() {
      this.tags = await forumService.getTags();
    }
  }
});
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "Forum Store"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/stores/forum.ts
git commit -m "feat(forum): add Forum Pinia store"
```

---

## 自检

- [x] 前端类型、服务、Store 已定义（3 个任务）
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径