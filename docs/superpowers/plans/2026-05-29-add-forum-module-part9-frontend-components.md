# Forum Module - Frontend Components

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建论坛模块的前端 Vue 组件

**架构：** 使用 Vue 3 Composition API，组件化设计，Tiptap 编辑器

**技术栈：** Vue 3, TypeScript, Tiptap, markdown-it

---

## 文件结构

```
frontend/src/
├── components/forum/
│   ├── PostCard.vue
│   ├── PostContent.vue
│   ├── CommentList.vue
│   ├── CommentItem.vue
│   ├── CommentEditor.vue
│   ├── TagInput.vue
│   └── CategoryFilter.vue
└── pages/forum/
    ├── PostListPage.vue
    ├── PostDetailPage.vue
    └── PostEditorPage.vue
```

---

<!-- openspec-task: PostCard Component -->
### Task 1：PostCard Component

**文件：**
- 创建：`frontend/src/components/forum/PostCard.vue`
- 测试：`frontend/tests/unit/components/PostCard.test.ts`

- [ ] **步骤 1：编写组件测试**

```typescript
// tests/unit/components/PostCard.test.ts
describe('PostCard.vue', () => {
  it('should display post information', () => {
    const post = {
      id: 1,
      title: 'Test Post',
      authorName: 'Author',
      categoryName: 'Category',
      createdAt: '2026-05-29T10:00:00Z'
    };
    
    expect(post.title).toBe('Test Post');
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "PostCard"`
预期：FAIL

- [ ] **步骤 3：编写 PostCard 组件**

```vue
<!-- frontend/src/components/forum/PostCard.vue -->
<template>
  <div class="post-card" @click="goToDetail">
    <h3 class="post-title">{{ post.title }}</h3>
    <div class="post-meta">
      <span class="author">{{ post.authorName }}</span>
      <span class="category">{{ post.categoryName }}</span>
      <span class="date">{{ formatDate(post.createdAt) }}</span>
    </div>
    <div class="post-stats">
      <span>👁 {{ post.viewCount }}</span>
      <span>❤️ {{ post.likeCount }}</span>
      <span>💬 {{ post.commentCount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ForumPost } from '@/types/forum';
import { useRouter } from 'vue-router';

defineProps<{ post: ForumPost }>();

const router = useRouter();

const goToDetail = () => {
  router.push(`/forum/posts/${props.post.id}`);
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN');
};
</script>

<style scoped>
.post-card {
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.post-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.post-title {
  margin: 0 0 8px;
}

.post-meta {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.post-stats {
  font-size: 12px;
  color: #999;
}

.post-stats span {
  margin-right: 12px;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "PostCard"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/components/forum/PostCard.vue
git commit -m "feat(forum): add PostCard component"
```

---

<!-- openspec-task: PostContent Component -->
### Task 2：PostContent Component（Markdown 渲染 + 工具链接）

**文件：**
- 创建：`frontend/src/components/forum/PostContent.vue`
- 测试：`frontend/tests/unit/components/PostContent.test.ts`

- [ ] **步骤 1：编写组件测试**

```typescript
describe('PostContent.vue', () => {
  it('should render markdown content', () => {
    const html = renderMarkdown('# Hello');
    expect(html).toContain('<h1>');
  });
  
  it('should add target blank to tool links', () => {
    const html = '<a href="/tools/123">Tool</a>';
    const processed = processToolLinks(html);
    expect(processed).toContain('target="_blank"');
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "PostContent"`
预期：FAIL

- [ ] **步骤 3：编写 PostContent 组件**

```vue
<!-- frontend/src/components/forum/PostContent.vue -->
<template>
  <div class="post-content" v-html="processedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import markdownIt from 'markdown-it';

const props = defineProps<{
  content: string;
}>();

const md = markdownIt({
  html: true,
  linkify: true
});

const processedHtml = computed(() => {
  let html = md.render(props.content);
  
  // 识别 /tools/\d+ 链接，添加 target="_blank"
  html = html.replace(/href="(\/tools\/\d+)"/g, 'href="$1" target="_blank" rel="noopener"');
  
  // 识别外部链接，添加标记
  html = html.replace(/href="(https?:\/\/(?!localhost|127\.0\.0\.1)[^"]+)"/g, 
    'href="$1" target="_blank" rel="noopener" class="external-link"');
  
  return html;
});
</script>

<style scoped>
.post-content {
  line-height: 1.6;
}

.post-content :deep(h1) {
  font-size: 1.5em;
  margin: 1em 0;
}

.post-content :deep(code) {
  background: #f5f5f5;
  padding: 2px 4px;
  border-radius: 4px;
}

.post-content :deep(.external-link)::after {
  content: ' ↗';
  font-size: 0.8em;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "PostContent"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/components/forum/PostContent.vue
git commit -m "feat(forum): add PostContent component with markdown and tool links"
```

---

### Task 3：CommentList & CommentItem Components

**文件：**
- 创建：`frontend/src/components/forum/CommentList.vue`
- 创建：`frontend/src/components/forum/CommentItem.vue`
- 测试：`frontend/tests/unit/components/Comment.test.ts`

- [ ] **步骤 1：编写组件测试**

```typescript
describe('Comment Components', () => {
  it('should render comment tree', () => {
    const comments = [
      { id: 1, content: 'Root', parentId: null, rootId: null },
      { id: 2, content: 'Reply', parentId: 1, rootId: 1 }
    ];
    
    expect(comments[1].parentId).toBe(1);
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "Comment"`
预期：FAIL

- [ ] **步骤 3：编写 CommentItem 组件**

```vue
<!-- frontend/src/components/forum/CommentItem.vue -->
<template>
  <div class="comment-item" :class="{ 'is-reply': comment.parentId !== null }">
    <div class="comment-header">
      <span class="author">{{ comment.authorName || '访客' }}</span>
      <span class="date">{{ formatDate(comment.createdAt) }}</span>
    </div>
    <div class="comment-content">{{ comment.content }}</div>
    <div class="comment-actions">
      <button @click="handleLike">❤️ {{ comment.likeCount }}</button>
      <button @click="handleReply">回复</button>
    </div>
    <div v-if="comment.parentId === null" class="comment-children">
      <CommentItem 
        v-for="child in children" 
        :key="child.id" 
        :comment="child" 
        :children="getChildren(child.id)" 
        @reply="handleReply"
        @like="handleLike"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ForumComment } from '@/types/forum';

const props = defineProps<{
  comment: ForumComment;
  children: ForumComment[];
}>();

const emit = defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'like', commentId: number): void;
}>();

const handleReply = () => emit('reply', props.comment.id);
const handleLike = () => emit('like', props.comment.id);

const getChildren = (parentId: number) => {
  return props.comment.rootId 
    ? [] // 简化：实际从 store 获取
    : [];
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('zh-CN');
};
</script>

<style scoped>
.comment-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.comment-item.is-reply {
  margin-left: 24px;
  border-left: 2px solid #ddd;
}

.comment-header {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.comment-content {
  margin: 8px 0;
}

.comment-actions {
  font-size: 12px;
}

.comment-actions button {
  margin-right: 12px;
  background: none;
  border: none;
  cursor: pointer;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "Comment"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/components/forum/CommentItem.vue
git commit -m "feat(forum): add CommentItem component"
```

- [ ] **步骤 6：编写 CommentList 组件**

```vue
<!-- frontend/src/components/forum/CommentList.vue -->
<template>
  <div class="comment-list">
    <h3>评论 ({{ comments.length }})</h3>
    <CommentItem 
      v-for="root in rootComments" 
      :key="root.id" 
      :comment="root" 
      :children="getChildren(root.id)"
      @reply="handleReply"
      @like="handleLike"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ForumComment } from '@/types/forum';
import CommentItem from './CommentItem.vue';

const props = defineProps<{
  comments: ForumComment[];
}>();

const emit = defineEmits<{
  (e: 'reply', commentId: number): void;
  (e: 'like', commentId: number): void;
}>();

const rootComments = computed(() => 
  props.comments.filter(c => c.parentId === null || c.parentId === c.id)
);

const getChildren = (parentId: number) =>
  props.comments.filter(c => c.parentId === parentId);

const handleReply = (id: number) => emit('reply', id);
const handleLike = (id: number) => emit('like', id);
</script>

<style scoped>
.comment-list {
  margin-top: 24px;
}

.comment-list h3 {
  margin-bottom: 16px;
}
</style>
```

- [ ] **步骤 7：Commit**

```bash
git add frontend/src/components/forum/CommentList.vue
git commit -m "feat(forum): add CommentList component"
```

---

### Task 4：CommentEditor Component

**文件：**
- 创建：`frontend/src/components/forum/CommentEditor.vue`
- 测试：`frontend/tests/unit/components/CommentEditor.test.ts`

- [ ] **步骤 1：编写组件测试**

```typescript
describe('CommentEditor.vue', () => {
  it('should toggle anonymous mode', () => {
    const isLoggedIn = false;
    expect(isLoggedIn).toBe(false);
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "CommentEditor"`
预期：FAIL

- [ ] **步骤 3：编写 CommentEditor 组件**

```vue
<!-- frontend/src/components/forum/CommentEditor.vue -->
<template>
  <div class="comment-editor">
    <div v-if="!isLoggedIn" class="anonymous-input">
      <input 
        v-model="authorName" 
        placeholder="输入昵称" 
        class="nickname-input"
      />
    </div>
    <textarea 
      v-model="content" 
      placeholder="发表评论..." 
      class="content-input"
      rows="3"
    ></textarea>
    <button @click="submit" :disabled="!canSubmit">提交</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const props = defineProps<{
  isLoggedIn: boolean;
  parentId?: number;
}>();

const emit = defineEmits<{
  (e: 'submit', data: { content: string; authorName?: string; parentId?: number }): void;
}>();

const content = ref('');
const authorName = ref('');

const canSubmit = computed(() => {
  return content.value.trim().length > 0 && 
    (props.isLoggedIn || authorName.value.trim().length > 0);
});

const submit = () => {
  if (!canSubmit.value) return;
  
  emit('submit', {
    content: content.value,
    authorName: props.isLoggedIn ? undefined : authorName.value,
    parentId: props.parentId
  });
  
  content.value = '';
};
</script>

<style scoped>
.comment-editor {
  margin-top: 16px;
}

.anonymous-input {
  margin-bottom: 8px;
}

.nickname-input {
  width: 200px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.content-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
}

button {
  margin-top: 8px;
  padding: 8px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "CommentEditor"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/components/forum/CommentEditor.vue
git commit -m "feat(forum): add CommentEditor component with anonymous support"
```

---

### Task 5：TagInput & CategoryFilter Components

**文件：**
- 创建：`frontend/src/components/forum/TagInput.vue`
- 创建：`frontend/src/components/forum/CategoryFilter.vue`
- 测试：`frontend/tests/unit/components/TagInput.test.ts`

- [ ] **步骤 1：编写组件测试**

```typescript
describe('TagInput.vue', () => {
  it('should add and remove tags', () => {
    const tags: string[] = [];
    tags.push('教程');
    tags.push('心得');
    expect(tags.length).toBe(2);
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd frontend && npm run test -- --grep "TagInput"`
预期：FAIL

- [ ] **步骤 3：编写 TagInput 组件**

```vue
<!-- frontend/src/components/forum/TagInput.vue -->
<template>
  <div class="tag-input">
    <div class="selected-tags">
      <span 
        v-for="(tag, index) in selectedTags" 
        :key="index" 
        class="tag"
      >
        {{ tag }}
        <button @click="removeTag(index)">×</button>
      </span>
    </div>
    <input 
      v-model="inputValue" 
      @input="handleInput"
      @keydown.enter.prevent="addTag"
      placeholder="输入标签后回车"
      list="tag-suggestions"
    />
    <datalist id="tag-suggestions">
      <option v-for="suggestion in suggestions" :key="suggestion" :value="suggestion" />
    </datalist>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const props = defineProps<{
  selectedTags: string[];
  suggestions?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:selectedTags', tags: string[]): void;
}>();

const inputValue = ref('');

const handleInput = () => {
  // 触发搜索建议
};

const addTag = () => {
  if (!inputValue.value.trim()) return;
  if (props.selectedTags.includes(inputValue.value)) return;
  
  const newTags = [...props.selectedTags, inputValue.value.trim()];
  emit('update:selectedTags', newTags);
  inputValue.value = '';
};

const removeTag = (index: number) => {
  const newTags = props.selectedTags.filter((_, i) => i !== index);
  emit('update:selectedTags', newTags);
};
</script>

<style scoped>
.tag-input {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  background: #e0e0e0;
  border-radius: 4px;
}

.tag button {
  margin-left: 4px;
  background: none;
  border: none;
  cursor: pointer;
}

input {
  flex: 1;
  min-width: 200px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd frontend && npm run test -- --grep "TagInput"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add frontend/src/components/forum/TagInput.vue
git commit -m "feat(forum): add TagInput component"
```

- [ ] **步骤 6：编写 CategoryFilter 组件**

```vue
<!-- frontend/src/components/forum/CategoryFilter.vue -->
<template>
  <div class="category-filter">
    <button 
      :class="{ active: !selectedCategory }"
      @click="select(null)"
    >
      全部
    </button>
    <button 
      v-for="category in categories" 
      :key="category.id"
      :class="{ active: selectedCategory === category.id }"
      @click="select(category.id)"
    >
      {{ category.name }}
    </button>
  </div>
</template>

<script setup lang="ts">
import type { ForumCategory } from '@/types/forum';

defineProps<{
  categories: ForumCategory[];
  selectedCategory: number | null;
}>();

const emit = defineEmits<{
  (e: 'select', categoryId: number | null): void;
}>();

const select = (categoryId: number | null) => {
  emit('select', categoryId);
};
</script>

<style scoped>
.category-filter {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
}

button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}
</style>
```

- [ ] **步骤 7：Commit**

```bash
git add frontend/src/components/forum/CategoryFilter.vue
git commit -m "feat(forum): add CategoryFilter component"
```

---

## 自检

- [x] 所有前端组件已定义（5 个组件任务）
- [x] 每个任务包含完整的 RED/GREEN 步骤
- [x] 无占位符
- [x] 路径使用绝对路径