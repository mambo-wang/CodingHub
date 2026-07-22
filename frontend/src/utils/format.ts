/**
 * 数字格式化工具
 * 用于工具卡片统计行等处的紧凑数字展示
 */

/**
 * 格式化计数数字：
 * - ≥10000 → `x.x万`（保留一位小数，整除时省略 .0）
 * - ≥1000 → `x.xk`（保留一位小数，整除时省略 .0）
 * - 否则原值
 *
 * @example
 * formatCount(207)     // "207"
 * formatCount(1200)    // "1.2k"
 * formatCount(165000)  // "16.5万"
 * formatCount(10000)   // "1万"
 */
export function formatCount(n: number | undefined | null): string {
  const value = n ?? 0
  if (value >= 10000) {
    return trimZero((value / 10000).toFixed(1)) + '万'
  }
  if (value >= 1000) {
    return trimZero((value / 1000).toFixed(1)) + 'k'
  }
  return String(value)
}

function trimZero(s: string): string {
  return s.endsWith('.0') ? s.slice(0, -2) : s
}
