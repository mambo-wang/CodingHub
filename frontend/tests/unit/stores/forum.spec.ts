import { describe, it, expect, vi, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useForumStore } from '@/stores/forum';
import forumService from '@/services/forum';

vi.mock('@/services/forum', () => ({
  default: {
    deletePost: vi.fn(),
  },
}));

describe('forum store - deletePost', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('RED: deletePost(1) 在 service resolve 时返回 { success: true } 并从 posts 数组移除 id=1', async () => {
    const store = useForumStore();
    store.posts = [
      { id: 1, title: 'Post 1' } as any,
      { id: 2, title: 'Post 2' } as any,
    ];
    vi.mocked(forumService.deletePost).mockResolvedValueOnce();

    const result = await store.deletePost(1);

    expect(result).toEqual({ success: true });
    expect(store.posts).toHaveLength(1);
    expect(store.posts[0].id).toBe(2);
  });

  it('RED: deletePost(1) 在 service reject (status 401) 时返回 { success: false, errorCode: "AUTH" }', async () => {
    const store = useForumStore();
    const error = { response: { status: 401 } };
    vi.mocked(forumService.deletePost).mockRejectedValueOnce(error);

    const result = await store.deletePost(1);

    expect(result).toEqual({ success: false, errorCode: 'AUTH' });
  });

  it('RED: deletePost(1) 在 status 403 时返回 { success: false, errorCode: "FORBIDDEN" }', async () => {
    const store = useForumStore();
    const error = { response: { status: 403 } };
    vi.mocked(forumService.deletePost).mockRejectedValueOnce(error);

    const result = await store.deletePost(1);

    expect(result).toEqual({ success: false, errorCode: 'FORBIDDEN' });
  });

  it('RED: deletePost(1) 在 status 404 时返回 { success: false, errorCode: "NOT_FOUND" }', async () => {
    const store = useForumStore();
    const error = { response: { status: 404 } };
    vi.mocked(forumService.deletePost).mockRejectedValueOnce(error);

    const result = await store.deletePost(1);

    expect(result).toEqual({ success: false, errorCode: 'NOT_FOUND' });
  });

  it('RED: deletePost(1) 在网络错误（无 response）时返回 { success: false, errorCode: "UNKNOWN" }', async () => {
    const store = useForumStore();
    const error = new Error('Network Error');
    vi.mocked(forumService.deletePost).mockRejectedValueOnce(error);

    const result = await store.deletePost(1);

    expect(result).toEqual({ success: false, errorCode: 'UNKNOWN' });
  });

  it('REFACTOR: _mapErrorToCode 错误码映射覆盖全部分支', () => {
    const store = useForumStore();

    expect(store._mapErrorToCode({ response: { status: 401 } })).toBe('AUTH');
    expect(store._mapErrorToCode({ response: { status: 403 } })).toBe('FORBIDDEN');
    expect(store._mapErrorToCode({ response: { status: 404 } })).toBe('NOT_FOUND');
    expect(store._mapErrorToCode({ response: { status: 500 } })).toBe('UNKNOWN');
    expect(store._mapErrorToCode({})).toBe('UNKNOWN');
    expect(store._mapErrorToCode(new Error())).toBe('UNKNOWN');
  });
});
