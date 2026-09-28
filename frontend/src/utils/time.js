export function formatRelativeTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000)
  if (diffMin < 5) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const month = date.getMonth() + 1
  const day = date.getDate()
  if (date.getFullYear() === new Date().getFullYear()) return `${month}月${day}日`
  return `${date.getFullYear()}年${month}月${day}日`
}

export function formatCommentTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const diffMin = Math.floor((now.getTime() - date.getTime()) / 60000)
  if (diffMin >= 0 && diffMin < 5) return '刚刚'
  if (diffMin >= 0 && diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour >= 0 && diffHour < 24 && date.toDateString() === now.toDateString()) {
    return `${diffHour}小时前`
  }
  const month = date.getMonth() + 1
  const day = date.getDate()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (date.getFullYear() === now.getFullYear()) return `${month}月${day}日 ${time}`
  return `${date.getFullYear()}年${month}月${day}日 ${time}`
}
