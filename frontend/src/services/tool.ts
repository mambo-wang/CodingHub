import api from './api';
import type { ToolDetailDTO } from '@/types/tool';

export interface ToolDetailVO extends ToolDetailDTO {
  viewCount: number;
  likeCount: number;
  commentCount: number;
  score: number;
  isLiked?: boolean;
}

export async function getToolDetail(id: number): Promise<ToolDetailVO> {
  const response = await api.get<{code: number; message: string; data: ToolDetailVO}>(`/tools/${id}`);
  return response.data.data;
}

export async function getTool(id: number): Promise<ToolDetailVO> {
  const response = await api.get<ToolDetailVO>(`/tools/${id}`);
  return response.data;
}

export async function pinTool(id: number): Promise<void> {
  await api.post(`/tools/${id}/pin`);
}

export async function unpinTool(id: number): Promise<void> {
  await api.delete(`/tools/${id}/pin`);
}

export async function getHotTop5(): Promise<number[]> {
  const response = await api.get<{code: number; message: string; data: number[]}>('/tools/hot-top5');
  return response.data.data;
}
