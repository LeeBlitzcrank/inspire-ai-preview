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
