export interface StatsDto {
  userCount: number;
  postCount: number;
  toolCount: number;
  videoCount: number;
}

export interface ToolRankDto {
  id: number;
  category: string;
  toolName: string;
  score: number;
}

export interface PostRankDto {
  id: number;
  category: string;
  postTitle: string;
  score: number;
}

export interface VideoRankDto {
  id: number;
  videoTitle: string;
  viewCount: number;
  likeCount: number;
}