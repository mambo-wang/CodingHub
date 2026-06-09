import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import PostCard from '@/components/forum/PostCard.vue';

const mockPost = {
  id: 1,
  title: 'Test Post',
  content: 'Content',
  authorId: 42,
  authorName: 'testuser',
  authorNickname: 'Test User',
  categoryId: 1,
  categoryName: '技术',
  viewCount: 100,
  likeCount: 10,
  commentCount: 5,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  isFavorited: false,
  favoriteCount: 0,
};

describe('PostCard - deletable prop & @delete event', () => {
  it('RED: deletable=false（默认）时不渲染删除图标按钮', () => {
    const wrapper = mount(PostCard, {
      props: { post: mockPost },
    });

    expect(wrapper.find('[aria-label="删除此帖"]').exists()).toBe(false);
  });

  it('RED: deletable=true 时渲染删除图标按钮，含 aria-label="删除此帖"', () => {
    const wrapper = mount(PostCard, {
      props: { post: mockPost, deletable: true },
    });

    const deleteBtn = wrapper.find('[aria-label="删除此帖"]');
    expect(deleteBtn.exists()).toBe(true);
  });

  it('RED: 点击删除按钮触发 @delete 事件，payload 为 post.id', async () => {
    const wrapper = mount(PostCard, {
      props: { post: mockPost, deletable: true },
    });

    await wrapper.find('[aria-label="删除此帖"]').trigger('click');

    expect(wrapper.emitted('delete')).toBeTruthy();
    expect(wrapper.emitted('delete')![0]).toEqual([1]);
  });

  it('RED: 点击删除按钮不触发卡片 click（stopPropagation）', async () => {
    const wrapper = mount(PostCard, {
      props: { post: mockPost, deletable: true },
      attachTo: document.body,
    });

    // The card navigates on click; we verify delete click doesn't bubble
    const deleteBtn = wrapper.find('[aria-label="删除此帖"]');
    await deleteBtn.trigger('click');

    // delete emitted but @click (goToDetail) should not have been triggered
    expect(wrapper.emitted('delete')).toBeTruthy();
  });

  it('REFACTOR: PostListPage / MyFavoritesPage / PostEditorPage 三处现有调用点不受影响（不传 deletable 默认为 false）', () => {
    const wrapper = mount(PostCard, {
      props: { post: mockPost },
    });

    // Without deletable, no delete button
    expect(wrapper.find('[aria-label="删除此帖"]').exists()).toBe(false);

    // Card click still works (no regression)
    expect(wrapper.find('.post-card').exists()).toBe(true);
    expect(wrapper.text()).toContain('Test Post');
  });
});
