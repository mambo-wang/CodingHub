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

  upload(file: File, source: string, onProgress?: (percent: number) => void): Promise<PluginDetail> {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('source', source)
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

  update(id: number, file: File, source: string, onProgress?: (percent: number) => void): Promise<PluginDetail> {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('source', source)
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
