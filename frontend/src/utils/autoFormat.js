/**
 * 纯文本 → 结构化 HTML 的「自动排版」工具
 *
 * 用于把 AI 生成的整段文字（形如 “1. xxx 2. xxx” 或按行排列的清单）
 * 整理成段落 / 有序列表 / 无序列表 / 小标题，便于富文本编辑器展示。
 * 不依赖任何第三方库，输出为白名单标签（p / ol / ul / li / h2）。
 */

const escapeHtml = (s) => String(s)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')

// 行首编号：「1.」「1、」「1)」「（1）」「一、」
const RE_ORDERED = /^(?:[（(]\d{1,2}[)）]|\d{1,2}[.、)）]|[一二三四五六七八九十]{1,2}[、.])\s*(.+)$/
// 形如 "- "、"• "、"* "、"· " 的项目符号
const RE_BULLET = /^[-•*·]\s+(.+)$/

// 整段里内联的编号（“…。 2. xxx” / “…1、xxx”）先断行，且不会误伤 “3.5” 这类小数
const RE_INLINE_BREAK = /([^\n])\s*(?=(?:\d{1,2}\s*[、)）]|\d{1,2}\.(?!\d)\s*\S|[（(]\d{1,2}[)）])\s*\S)/g

const SENTENCE_END = '。！？!?；;'

// 把一段很长的正文按句子切成多个段落，避免整篇挤成一坨
const splitLongParagraph = (text, maxLen = 110) => {
  if (text.length <= maxLen) return [text]
  const sentences = []
  let cur = ''
  for (const ch of text) {
    cur += ch
    if (SENTENCE_END.includes(ch)) { sentences.push(cur); cur = '' }
  }
  if (cur) sentences.push(cur)

  const out = []
  let buf = ''
  for (const s of sentences) {
    if (buf && buf.length + s.length > maxLen) { out.push(buf); buf = '' }
    buf += s
  }
  if (buf) out.push(buf)
  return out
}

export function autoFormatHtml(text) {
  if (!text) return ''

  let raw = String(text).replace(/\r\n?/g, '\n').trim()
  if (!raw) return ''

  // 内联编号先断行，保证每个编号独占一行
  raw = raw.replace(RE_INLINE_BREAK, '$1\n')

  const blocks = []
  let listType = null
  let listItems = []

  const flushList = () => {
    if (!listType) return
    blocks.push(`<${listType}>` + listItems.map(t => `<li>${escapeHtml(t)}</li>`).join('') + `</${listType}>`)
    listType = null
    listItems = []
  }

  for (const rawLine of raw.split('\n')) {
    const line = rawLine.trim()
    if (!line) { flushList(); continue }

    const ordered = line.match(RE_ORDERED)
    if (ordered) {
      if (listType !== 'ol') { flushList(); listType = 'ol' }
      listItems.push(ordered[1].trim())
      continue
    }

    const bullet = line.match(RE_BULLET)
    if (bullet) {
      if (listType !== 'ul') { flushList(); listType = 'ul' }
      listItems.push(bullet[1].trim())
      continue
    }

    flushList()

    // 【小标题】或以「：」结尾的短句 → 二级标题
    const bracket = line.match(/^【(.+)】$/)
    if (bracket) {
      blocks.push(`<h2>${escapeHtml(bracket[1])}</h2>`)
      continue
    }
    if (/[：:]$/.test(line) && line.length <= 20) {
      blocks.push(`<h2>${escapeHtml(line)}</h2>`)
      continue
    }

    // 长正文按句子拆成多个段落
    splitLongParagraph(line).forEach(part => {
      blocks.push(`<p>${escapeHtml(part)}</p>`)
    })
  }

  flushList()
  return blocks.join('')
}

export default autoFormatHtml
