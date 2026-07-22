export interface ToolDetailDTO {
  id: number;
  name: string;
  categoryName: string;
  categoryIcon: string;
  logoUrl?: string | null;
  content: string;
  uploaderId: number;
  uploaderUsername: string;
  createdAt: string;
  updatedAt: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  favoriteCount?: number;
  downloadCount?: number;
  score?: number;
  isLiked?: boolean;
}

export interface ToolSummary {
  id: number;
  name: string;
  categoryName: string;
  categoryIcon: string;
  uploaderUsername: string;
  score?: number;
  pinned?: boolean;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  createdAt: string;
}