export interface StatsDto {
  userCount: number;
  postCount: number;
  toolCount: number;
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