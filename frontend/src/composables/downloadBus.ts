import { reactive } from 'vue'

// 记录当前会话内对每个工具执行的下载次数。
// 首页在展示下载量时会叠加这个值，使下载后计数能"即时"反映；
// 首页下一次拉取数据（此时接口已返回服务端最新值）后调用 clearDownloads 清空，避免重复累加。
export const sessionDownloads = reactive<Record<number, number>>({})

export function addDownload(toolId: number): void {
  sessionDownloads[toolId] = (sessionDownloads[toolId] ?? 0) + 1
}

export function clearDownloads(): void {
  for (const key of Object.keys(sessionDownloads)) {
    delete sessionDownloads[Number(key)]
  }
}
