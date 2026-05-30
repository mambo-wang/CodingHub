import axios from 'axios';
import { useAuthStore } from '@/stores/auth';
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

// 创建独立的 axios 实例，自动携带 auth token
const forumApi = axios.create({
  baseURL: BASE_URL,
  timeout: 60000
});

// 使用 auth store 的请求拦截器来添加 token
forumApi.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore();
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const forumService = {
  // Posts
  async getPostList(params?: {
    category?: number;
    tag?: number;
    keyword?: string;
    page?: number;
    size?: number;
  }): Promise<PageResponse<ForumPost>> {
    const response = await forumApi.get('/posts', { params });
    return response.data;
  },

  async getMyPosts(): Promise<PageResponse<ForumPost>> {
    const response = await forumApi.get('/posts/my');
    return response.data;
  },

  async getPostById(id: number): Promise<ForumPost> {
    const response = await forumApi.get(`/posts/${id}`);
    return response.data;
  },

  async createPost(data: ForumPostCreateRequest): Promise<ForumPost> {
    const response = await forumApi.post('/posts', data);
    return response.data;
  },

  async updatePost(id: number, data: ForumPostCreateRequest): Promise<ForumPost> {
    const response = await forumApi.put(`/posts/${id}`, data);
    return response.data;
  },

  async deletePost(id: number): Promise<void> {
    await forumApi.delete(`/posts/${id}`);
  },

  // Categories
  async getCategories(): Promise<ForumCategory[]> {
    const response = await forumApi.get('/categories');
    return response.data;
  },

  // Tags
  async getTags(): Promise<ForumTag[]> {
    const response = await forumApi.get('/tags');
    return response.data;
  },

  async getHotTags(): Promise<ForumTag[]> {
    const response = await forumApi.get('/tags/hot');
    return response.data;
  },

  async createTag(name: string, isSystem: boolean = false): Promise<ForumTag> {
    const response = await forumApi.post('/tags', { name, isSystem });
    return response.data;
  },

  // Comments
  async getComments(postId: number): Promise<ForumComment[]> {
    const response = await forumApi.get(`/posts/${postId}/comments`);
    return response.data;
  },

  async createComment(postId: number, data: ForumCommentCreateRequest): Promise<ForumComment> {
    const response = await forumApi.post(`/posts/${postId}/comments`, data);
    return response.data;
  },

  async deleteComment(commentId: number): Promise<void> {
    await forumApi.delete(`/comments/${commentId}`);
  },

  // Likes
  async like(data: ForumLikeRequest): Promise<void> {
    await forumApi.post('/likes', data);
  },

  async unlike(data: ForumLikeRequest): Promise<void> {
    await forumApi.delete('/likes', { data });
  }
};

export default forumService;