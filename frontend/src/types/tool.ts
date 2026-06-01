export interface ToolDetailDTO {
  id: number;
  name: string;
  categoryName: string;
  categoryIcon: string;
  content: string;
  uploaderId: number;
  uploaderUsername: string;
  createdAt: string;
  updatedAt: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  score?: number;
  isLiked?: boolean;
}

export interface ToolSummary {
  id: number;
  name: string;
  categoryName: string;
  categoryIcon: string;
  uploaderUsername: string;
  createdAt: string;
}