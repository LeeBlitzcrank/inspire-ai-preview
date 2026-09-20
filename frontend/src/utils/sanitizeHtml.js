/**
 * 富文本安全渲染工具（无第三方依赖）
 *
 * 编辑器（Create.vue）以 HTML 字符串保存「灵感详情」，详情页用 v-html 渲染。
 * 为避免历史上或被篡改的数据注入脚本，这里做白名单过滤：
 * - 仅保留基础排版标签，其余标签一律拆掉只留文字；
 * - 清空所有属性（onclick / href / style 等全丢），杜绝事件与外部资源注入；
 * - 删除 script / style / iframe 等危险节点及其内容；
 * - 纯文本（不含标签的历史数据）按段落进行转义保留换行。
 */

// 允许保留的排版标签
const ALLOWED = new Set([
  'B', 'STRONG', 'I', 'EM', 'U', 'S',
  'H1', 'H2', 'H3',
  'BLOCKQUOTE', 'UL', 'OL', 'LI',
  'HR', 'BR', 'P', 'DIV', 'SPAN'
])

// 需要整体删除（连同内部内容）的危险标签
const BLOCKED = new Set([
  'SCRIPT', 'STYLE', 'IFRAME', 'OBJECT', 'EMBED', 'LINK', 'META', 'FORM', 'INPUT', 'SVG'
])

const escapeText = (s) => String(s)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

const clean = (node) => {
  let child = node.firstChild
  while (child) {
    const next = child.nextSibling
    if (child.nodeType === 8) {
      // 注释节点直接去掉
      child.remove()
    } else if (child.nodeType === 1) {
      const tag = child.tagName
      if (BLOCKED.has(tag)) {
        child.remove()
      } else if (!ALLOWED.has(tag)) {
        // 非白名单标签：拆壳保留内部文字/子节点，再重新遍历当前层
        while (child.firstChild) node.insertBefore(child.firstChild, child)
        child.remove()
        clean(node)
        return
      } else {
        // 保留标签本身，但清空全部属性
        for (const attr of Array.from(child.attributes)) child.removeAttribute(attr.name)
        clean(child)
      }
    }
    child = next
  }
}

export function sanitizeHtml(input) {
  if (!input) return ''
  if (typeof input !== 'string') return ''

  // 不含标签：按历史纯文本处理，转义并保留换行
  if (!/[<>]/.test(input)) {
    return escapeText(input).replace(/\r?\n/g, '<br>')
  }

  if (typeof DOMParser === 'undefined') return escapeText(input)

  const doc = new DOMParser().parseFromString(input, 'text/html')
  clean(doc.body)
  return doc.body.innerHTML
}

export default sanitizeHtml
