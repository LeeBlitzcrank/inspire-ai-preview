import {computed, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {autoFormatHtml} from '@/utils/autoFormat.js'

export function useCreateEditor(form) {
  // —— 富文本详情（contenteditable）：以 HTML 字符串存储，提交与回显均走 innerHTML ——
  const descRef = ref(null)
  const contentLen = computed(() => {
    const html = form.value.content || ''
    const text = html.replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ')
    return text.replace(/\s/g, '').length
  })
  const onDescInput = () => { if (descRef.value) form.value.content = descRef.value.innerHTML }
  const setContent = (html) => {
    form.value.content = html || ''
    if (descRef.value) descRef.value.innerHTML = form.value.content
  }
  const execCmd = (cmd) => { descRef.value?.focus(); document.execCommand(cmd); onDescInput() }
  const execBlock = (tag) => { descRef.value?.focus(); document.execCommand('formatBlock', false, tag); onDescInput() }
  const clearFormat = () => {
    descRef.value?.focus()
    document.execCommand('removeFormat')
    document.execCommand('formatBlock', false, 'P')
    onDescInput()
  }
  // 读取编辑器里的纯文本：按块级元素补换行，并给列表/标题补回标记，
  // 这样反复点「自动排版」也不会把已有的列表结构弄丢
  const getEditorPlainText = () => {
    const el = descRef.value
    if (!el) return ''
    const BLOCK = new Set(['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'BLOCKQUOTE'])
    let out = ''
    const walk = (node) => {
      node.childNodes.forEach(child => {
        if (child.nodeType === 3) { out += child.textContent; return }
        if (child.nodeType !== 1) return
        const tag = child.tagName
        if (tag === 'BR') { out += '\n'; return }
        const isBlock = BLOCK.has(tag)
        if (isBlock && out && !out.endsWith('\n')) out += '\n'
        if (tag === 'LI') {
          // 有序列表补 "1. "，无序列表补 "- "，让下次排版仍能识别成列表
          const parent = child.parentElement
          if (parent && parent.tagName === 'OL') {
            out += (Array.prototype.indexOf.call(parent.children, child) + 1) + '. '
          } else {
            out += '- '
          }
        }
        if (tag === 'H1' || tag === 'H2' || tag === 'H3') out += '【'
        walk(child)
        if (tag === 'H1' || tag === 'H2' || tag === 'H3') out += '】'
        if (isBlock && !out.endsWith('\n')) out += '\n'
      })
    }
    walk(el)
    return out.replace(/\n{3,}/g, '\n\n').trim()
  }

  // 自动排版：按段落 / 编号 / 清单把纯文本整理成结构化富文本
  const autoFormatContent = () => {
    const plain = getEditorPlainText() || (form.value.content || '').replace(/<[^>]*>/g, ' ')
    if (!plain.trim()) return ElMessage.warning('还没有内容可以排版')
    const before = form.value.content || ''
    const formatted = autoFormatHtml(plain)
    if (formatted === before) return ElMessage.info('这段内容已经是排版后的效果')
    setContent(formatted)
    ElMessage.success('已自动排版')
  }

  return {
    descRef,
    contentLen,
    setContent,
    onDescInput,
    execCmd,
    execBlock,
    clearFormat,
    getEditorPlainText,
    autoFormatContent
  }
}
