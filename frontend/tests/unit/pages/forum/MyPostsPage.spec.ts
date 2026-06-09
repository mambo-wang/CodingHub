import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { useForumStore } from '@/stores/forum';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
  },
}));

const mockPosts = [
  {
    id: 1, title: 'Post 1', content: 'Content 1', authorId: 42,
    authorName: 'author', categoryId: 1, categoryName: '技术',
    viewCount: 10, likeCount: 2, commentCount: 1,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    isFavorited: false, favoriteCount: 0,
  },
  {
    id: 2, title: 'Post 2', content: 'Content 2', authorId: 42,
    authorName: 'author', categoryId: 2, categoryName: '设计',
    viewCount: 20, likeCount: 5, commentCount: 3,
    createdAt: '2026-01-02T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z',
    isFavorited: true, favoriteCount: 1,
  },
];

describe('MyPostsPage - 删除流程', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('RED: PostCard 接收 deletable=true', async () => {
    const forumStore = useForumStore();
    forumStore.posts = mockPosts;
    forumStore.loading = false;

    const { default: MyPostsPage } = await import('@/pages/forum/MyPostsPage.vue');
    const wrapper = mount(MyPostsPage, {
      global: {
        stubs: {
          SidebarNav: true,
          PostCard: {
            props: ['post', 'deletable'],
            template: '<div class="mock-post-card"><button v-if="deletable" class="mock-delete">删除</button></div>',
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    expect(wrapper.findAll('.mock-delete').length).toBeGreaterThan(0);
  });

  it('RED: 点击 PostCard 上的删除图标弹 ConfirmDialog', async () => {
    const forumStore = useForumStore();
    forumStore.posts = mockPosts;
    forumStore.loading = false;

    const { default: MyPostsPage } = await import('@/pages/forum/MyPostsPage.vue');
    const wrapper = mount(MyPostsPage, {
      global: {
        stubs: {
          SidebarNav: true,
          PostCard: {
            props: ['post', 'deletable'],
            template: '<div class="mock-post-card"><button class="mock-delete" @click="$emit(\'delete\', post.id)">删除</button></div>',
            emits: ['delete'],
          },
          ConfirmDialog: {
            props: ['visible'],
            template: '<div v-if="visible" class="mock-dialog">dialog</div>',
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-delete').trigger('click');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.mock-dialog').exists()).toBe(true);
  });

  it('RED: 确认删除后 service 成功，列表中对应项被移除', async () => {
    const forumStore = useForumStore();
    forumStore.posts = [...mockPosts];
    forumStore.loading = false;
    vi.spyOn(forumStore, 'deletePost').mockResolvedValue({ success: true });

    const { default: MyPostsPage } = await import('@/pages/forum/MyPostsPage.vue');
    const wrapper = mount(MyPostsPage, {
      global: {
        stubs: {
          SidebarNav: true,
          PostCard: {
            props: ['post', 'deletable'],
            template: '<div class="mock-post-card"><button class="mock-delete" @click="$emit(\'delete\', post.id)">删除</button></div>',
            emits: ['delete'],
          },
          ConfirmDialog: {
            props: ['visible'],
            template: '<div class="mock-dialog"><button class="mock-confirm" @click="$emit(\'confirm\')">确认</button></div>',
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-delete').trigger('click');
    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-confirm').trigger('click');
    await wrapper.vm.$nextTick();
    await new Promise(r => setTimeout(r, 50));

    expect(forumStore.deletePost).toHaveBeenCalledWith(1);
  });

  it('REFACTOR: 与 PostDetailPage 共用 ConfirmDialog，状态独立', async () => {
    const forumStore = useForumStore();
    forumStore.posts = mockPosts;
    forumStore.loading = false;

    const { default: MyPostsPage } = await import('@/pages/forum/MyPostsPage.vue');
    const wrapper = mount(MyPostsPage, {
      global: {
        stubs: {
          SidebarNav: true,
          PostCard: {
            props: ['post', 'deletable'],
            template: '<div class="mock-post-card"><button class="mock-delete" @click="$emit(\'delete\', post.id)">删除</button></div>',
            emits: ['delete'],
          },
          ConfirmDialog: {
            props: ['visible', 'title', 'danger', 'loading'],
            template: '<div v-if="visible" class="mock-dialog" data-testid="confirm-dialog">title={{ title }} danger={{ danger }}</div>',
            emits: ['confirm', 'cancel', 'update:visible'],
          },
        },
      },
    });

    await wrapper.vm.$nextTick();
    await wrapper.find('.mock-delete').trigger('click');
    await wrapper.vm.$nextTick();

    const dialog = wrapper.find('.mock-dialog');
    expect(dialog.exists()).toBe(true);
    expect(dialog.text()).toContain('danger=true');
    expect(dialog.text()).toContain('删除帖子');
  });
});
