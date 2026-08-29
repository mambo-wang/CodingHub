import api from './api'
import type { PluginDetail, PluginQuery, PluginSummary } from '@/types/plugin'
import type { PageResponse } from '@/types'
import type { AxiosProgressEvent } from 'axios'
import { ElMessage } from 'element-plus'

export const pluginApi = {
  list(params: PluginQuery): Promise<PageResponse<PluginSummary>> {
    return api.get('/plugins', { params }).then(res => res.data.data as PageResponse<PluginSummary>)
  },

  getDetail(id: number): Promise<PluginDetail> {
    return api.get(`/plugins/${id}`).then(res => res.data.data as PluginDetail)
  },

  upload(
    file: File,
    source?: string,
    logoUrl?: string | null,
    onProgress?: (percent: number) => void,
    tagIds?: number[]
  ): Promise<PluginDetail> {
    const fd = new FormData()
    fd.append('file', file)
    if (source && source.trim()) fd.append('source', source.trim())
    if (logoUrl !== undefined) fd.append('logoUrl', logoUrl ?? '')
    if (tagIds) tagIds.forEach(id => fd.append('tagIds', String(id)))
    return api.post('/plugins', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000,
      onUploadProgress: (e: AxiosProgressEvent) => {
        if (e.total && onProgress) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      }
    }).then(res => res.data.data as PluginDetail)
  },

  update(
    id: number,
    file: File,
    source?: string,
    logoUrl?: string | null,
    onProgress?: (percent: number) => void,
    tagIds?: number[]
  ): Promise<PluginDetail> {
    const fd = new FormData()
    fd.append('file', file)
    if (source && source.trim()) fd.append('source', source.trim())
    if (logoUrl !== undefined) fd.append('logoUrl', logoUrl ?? '')
    if (tagIds) tagIds.forEach(id => fd.append('tagIds', String(id)))
    return api.put(`/plugins/${id}`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000,
      onUploadProgress: (e: AxiosProgressEvent) => {
        if (e.total && onProgress) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      }
    }).then(res => res.data.data as PluginDetail)
  },

  getHotTop5(): Promise<number[]> {
    return api.get('/plugins/hot-top5').then(res => res.data.data as number[])
  },

  pin(id: number): Promise<void> {
    return api.post(`/plugins/${id}/pin`).then(() => undefined)
  },

  unpin(id: number): Promise<void> {
    return api.delete(`/plugins/${id}/pin`).then(() => undefined)
  },

  remove(id: number): Promise<void> {
    return api.delete(`/plugins/${id}`).then(res => res.data as void)
  },

  download(id: number): Promise<void> {
    return api.get(`/plugins/${id}/download`, { responseType: 'blob' }).then(response => {
      const blob = new Blob([response.data], { type: 'application/zip' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `plugin-${id}.zip`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }).catch(() => {
      ElMessage.error('插件包下载失败')
    })
  }
}
