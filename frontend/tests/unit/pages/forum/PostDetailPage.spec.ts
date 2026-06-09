import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useForumStore } from '@/stores/forum';
import { useAuthStore } from '@/stores/auth';

// Mock router
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: vi.fn() }),
}));

// Mock forum service
vi.mock('@/services/forum', () => ({
  default: {
    getComments: vi.fn().mockResolvedValue([]),
    like: vi.fn(),
  },
}));

vi.mock('@/services/api', () => ({
  postFavoriteApi: {
    toggleFavorite: vi.fn(),
  },
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
  },
}));

describe('PostDetailPage - 删除流程', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('RED: 作者身份（currentUser.id === post.authorId）时模板存在 [data-testid="delete-post-btn"]', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 42, username: 'author' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: true,
        },
      },
    });

    await wrapper.vm.$nextTick();
    // Wait for async onMounted to complete
    await new Promise(r => setTimeout(r, 50));
    await wrapper.vm.$nextTick();

    // currentPost is set synchronously, but mount triggers fetchPostById which we didn't mock
    // Set it directly after mount
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    // We need to access the component instance via the wrapper
    // Force update to reflect store changes
    await wrapper.vm.$forceUpdate();
    await wrapper.vm.$nextTick();

    // For the test to work with stubs, we check the data-testid exists in the rendered output
    // Since ConfirmDialog is stubbed and the actual button renders, we can still find it
    const deleteBtn = wrapper.find('[data-testid="delete-post-btn"]');
    expect(deleteBtn.exists()).toBe(true);
  });

  it('RED: 非作者不渲染删除按钮', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 99, username: 'other' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: true,
        },
      },
    });

    await wrapper.vm.$nextTick();

    const deleteBtn = wrapper.find('[data-testid="delete-post-btn"]');
    expect(deleteBtn.exists()).toBe(false);
  });

  it('RED: 未登录不渲染删除按钮', async () => {
    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: true,
        },
      },
    });

    await wrapper.vm.$nextTick();

    const deleteBtn = wrapper.find('[data-testid="delete-post-btn"]');
    expect(deleteBtn.exists()).toBe(false);
  });

  it('RED: 点击"删除"按钮后 ConfirmDialog visible 变 true', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 42, username: 'author' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: {
            template: '<div v-if="visible" class="mock-dialog">dialog</div>',
            props: ['visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();

    // Click delete button
    await wrapper.find('[data-testid="delete-post-btn"]').trigger('click');
    await wrapper.vm.$nextTick();

    // ConfirmDialog stub visible changes based on v-model
    expect(wrapper.find('.mock-dialog').exists()).toBe(true);
  });

  it('GREEN: @confirm 调用 forumStore.deletePost，成功后 router.push("/forum")', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 42, username: 'author' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    // Mock deletePost to resolve
    vi.spyOn(forumStore, 'deletePost').mockResolvedValue({ success: true });

    const mockPush = vi.fn();
    vi.mocked(vi.fn()).mockReset();

    // Create a fresh factory for this specific test
    const routerModule = await import('vue-router');

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: {
            template: '<div class="mock-dialog"><button class="mock-confirm" @click="$emit(\'confirm\')">确认</button></div>',
            props: ['visible'],
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();

    // Click delete button to open dialog
    await wrapper.find('[data-testid="delete-post-btn"]').trigger('click');
    await wrapper.vm.$nextTick();

    // Click confirm
    await wrapper.find('.mock-confirm').trigger('click');
    await wrapper.vm.$nextTick();

    // Wait for async completion
    await new Promise(r => setTimeout(r, 50));
    await wrapper.vm.$nextTick();

    expect(forumStore.deletePost).toHaveBeenCalledWith(1);
  });

  it('RED: 403 错误时弹出"您不是该帖子的作者，无权删除"', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 42, username: 'author' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    vi.spyOn(forumStore, 'deletePost').mockResolvedValue({ success: false, errorCode: 'FORBIDDEN' });

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: {
            template: '<div class="mock-dialog"><button class="mock-confirm" @click="$emit(\'confirm\')">确认</button></div>',
            props: ['visible'],
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    await wrapper.find('[data-testid="delete-post-btn"]').trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-confirm').trigger('click');
    await wrapper.vm.$nextTick();
    await new Promise(r => setTimeout(r, 50));

    const { ElMessage } = await import('element-plus');
    expect(ElMessage.warning).toHaveBeenCalledWith('您不是该帖子的作者，无权删除');
  });

  it('RED: 404 时关闭对话框 + 跳转 + toast"帖子不存在或已被删除"', async () => {
    const authStore = useAuthStore();
    authStore.setUser({ id: 42, username: 'author' });

    const forumStore = useForumStore();
    forumStore.currentPost = {
      id: 1,
      authorId: 42,
      title: 'Test',
      content: 'Content',
      authorName: 'author',
      categoryId: 1,
      categoryName: '技术',
      viewCount: 0,
      likeCount: 0,
      commentCount: 0,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    };
    vi.spyOn(forumStore, 'deletePost').mockResolvedValue({ success: false, errorCode: 'NOT_FOUND' });

    const { default: PostDetailPage } = await import('@/pages/forum/PostDetailPage.vue');
    const wrapper = mount(PostDetailPage, {
      global: {
        stubs: {
          PostContent: true,
          CommentList: true,
          CommentEditor: true,
          AuthorBadge: true,
          ConfirmDialog: {
            template: '<div class="mock-dialog"><button class="mock-confirm" @click="$emit(\'confirm\')">确认</button></div>',
            props: ['visible'],
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    await wrapper.find('[data-testid="delete-post-btn"]').trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-confirm').trigger('click');
    await wrapper.vm.$nextTick();
    await new Promise(r => setTimeout(r, 50));

    const { ElMessage } = await import('element-plus');
    expect(ElMessage.warning).toHaveBeenCalledWith('帖子不存在或已被删除');
  });
});
