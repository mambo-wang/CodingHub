import logoSkill from '@/assets/logos/skill.png'
import logoMcp from '@/assets/logos/mcp.png'
import logoPlugin from '@/assets/logos/plugin.png'
import logoPrompt from '@/assets/logos/prompt.png'
import logoRule from '@/assets/logos/rule.png'
import logoOther from '@/assets/logos/other.png'

/**
 * 分类默认 logo 映射。
 * 当后台未返回工具自身 logo（logoUrl 为空）时，前端按分类名渲染本地默认图。
 * 未匹配到的分类统一回退到「其他」。
 */
const CATEGORY_LOGOS: Record<string, string> = {
  Skill: logoSkill,
  MCP: logoMcp,
  插件: logoPlugin,
  Prompt: logoPrompt,
  Rule: logoRule,
  其他: logoOther,
}

export function getDefaultLogo(categoryName?: string | null): string {
  if (!categoryName) return logoOther
  return CATEGORY_LOGOS[categoryName] ?? logoOther
}
