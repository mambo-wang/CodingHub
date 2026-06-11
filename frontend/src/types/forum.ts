export interface ForumPost {
  id: number;
  title: string;
  content: string;
  authorId: number;
  authorName: string;
  authorNickname?: string;
  authorAvatarUrl?: string | null;
  categoryId: number;
  categoryName: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  isFavorited?: boolean;
  favoriteCount?: number;
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
  authorNickname?: string | null;
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