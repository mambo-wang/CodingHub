import axios from 'axios';
import type { ToolDetailDTO } from '@/types/tool';

const api = axios.create({ baseURL: '/api/v1/tools' });

export interface ToolDetailVO extends ToolDetailDTO {
  viewCount: number;
  likeCount: number;
  commentCount: number;
  score: number;
  isLiked?: boolean;
}

export interface Comment {
  id: number;
  content: string;
  username: string;
  createdAt: string;
}

export async function getToolDetail(id: number): Promise<ToolDetailVO> {
  const response = await api.get<{code: number; message: string; data: ToolDetailVO}>(`/${id}`);
  return response.data.data;
}

export async function likeTool(id: number): Promise<void> {
  await api.post(`/${id}/like`);
}

export async function unlikeTool(id: number): Promise<void> {
  await api.delete(`/${id}/like`);
}

export async function getLikeStatus(id: number): Promise<boolean> {
  const response = await api.get<{code: number; message: string; data: boolean}>(`/${id}/like-status`);
  return response.data.data;
}

export async function getComments(id: number): Promise<Comment[]> {
  const response = await api.get<{code: number; message: string; data: Comment[]}>(`/${id}/comments`);
  return response.data.data || [];
}

export async function addComment(id: number, content: string): Promise<Comment> {
  const response = await api.post<{code: number; message: string; data: Comment}>(`/${id}/comments`, { content });
  return response.data.data;
}

export async function getTool(id: number): Promise<ToolDetailVO> {
  const response = await api.get<ToolDetailVO>(`/${id}`);
  return response.data;
}