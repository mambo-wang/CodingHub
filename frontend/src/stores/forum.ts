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
    },

    async fetchMyPosts() {
      this.loading = true;
      try {
        const response = await forumService.getMyPosts();
        this.posts = response.content || [];
        this.pagination = {
          page: 0,
          size: this.pagination.size,
          totalElements: this.posts.length,
          totalPages: 1
        };
      } finally {
        this.loading = false;
      }
    }
  }
});