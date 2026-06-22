import axios from 'axios';
import type { StatsDto, ToolRankDto, PostRankDto, VideoRankDto } from '@/types/overview';

const api = axios.create({
  baseURL: '/api',
});

export async function fetchStats(): Promise<StatsDto> {
  const response = await api.get('/overview/stats');
  return response.data;
}

export async function fetchToolRanks(): Promise<ToolRankDto[]> {
  const response = await api.get('/overview/tool-ranks');
  return response.data;
}

export async function fetchPostRanks(): Promise<PostRankDto[]> {
  const response = await api.get('/overview/post-ranks');
  return response.data;
}

export async function fetchVideoRanks(): Promise<VideoRankDto[]> {
  const response = await api.get('/overview/video-ranks');
  return response.data;
}