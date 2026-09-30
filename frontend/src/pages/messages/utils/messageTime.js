/**
 * 文件：frontend/src/pages/messages/utils/messageTime.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：前端工具模块，提供可复用基础函数
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
export function formatConversationTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) return date.toTimeString().slice(0, 5)
  const diff = (now - date) / 86400000
  if (diff < 2) return '昨天'
  if (diff < 7) {
    const days = ['日', '一', '二', '三', '四', '五', '六']
    return `周${days[date.getDay()]}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

export function formatMessageTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const hhmm = date.toTimeString().slice(0, 5)
  if (date.toDateString() === now.toDateString()) return hhmm
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return `昨天 ${hhmm}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${hhmm}`
}
