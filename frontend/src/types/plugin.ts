import type { Tag } from '@/types'

export interface PluginSummary {
  id: number
  name: string
  description: string
  version: string
  logoUrl: string | null
  source: string
  likeCount: number
  commentCount: number
  viewCount: number
  favoriteCount: number
  pinned: boolean
  tags: Tag[]
  score: number
  authorId: number
  authorUsername: string
  authorNickname: string | null
  createdAt: string
}

export interface ComponentSummary {
  skills: string[]
  agents: string[]
  commands: string[]
  hooks: string[]
  mcpServers: boolean
  lspServers: boolean
  hasBin: boolean
  hasSettings: boolean
}

export interface PluginDetail extends PluginSummary {
  components: ComponentSummary[]
  pluginJson: Record<string, unknown>
  sourceZipName: string | null
}

export interface PluginQuery {
  keyword?: string
  page?: number
  size?: number
  sort?: 'new' | 'hot'
}
