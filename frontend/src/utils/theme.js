/**
 * 主题切换：由个人页根节点挂 data-theme，具体色值全在 styles/tokens.css 里。
 * 目前三套：mint（薄荷绿，默认）/ orange（暖橙）/ morandi（莫兰迪灰）
 */
import { ref } from 'vue'

const STORAGE_KEY = 'inspire_theme'
export const THEMES = [
  // mint 就是项目原本的配色，作为默认主题
  { key: 'mint', label: '薄荷绿' },
  { key: 'orange', label: '暖橙' },
  { key: 'morandi', label: '莫兰迪灰' }
]

export const currentTheme = ref('mint')

/**
 * 只更新状态 + 持久化，**不往 <html> 写属性**。
 * 主题靠「个人页根节点上的 :data-theme」生效（见 Personal.vue），
 * 这样其他页面始终用 :root 的薄荷绿，不受影响。
 */
export function applyTheme(key) {
  const valid = THEMES.some(t => t.key === key) ? key : 'mint'
  currentTheme.value = valid
  try { localStorage.setItem(STORAGE_KEY, valid) } catch (e) { /* 忽略隐私模式 */ }
}

export function initTheme() {
  let saved = 'mint'
  try { saved = localStorage.getItem(STORAGE_KEY) || 'mint' } catch (e) { /* 忽略 */ }
  currentTheme.value = THEMES.some(t => t.key === saved) ? saved : 'mint'
}
