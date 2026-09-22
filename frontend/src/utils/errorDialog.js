import { reactive } from 'vue'

export const errorDialogState = reactive({
  visible: false,
  title: '操作失败',
  message: '',
  detail: ''
})

let lastKey = ''
let lastAt = 0
const DUPLICATE_WINDOW_MS = 1500

function normalizeMessage(input) {
  if (input instanceof Error) return input.message || '操作失败'
  if (typeof input === 'string') return input
  return String(input?.message || input?.msg || '操作失败')
}

export function showErrorDialog(message, options = {}) {
  const text = normalizeMessage(message)
  const title = options.title || '操作失败'
  const detail = options.detail || ''
  const key = `${title}|${text}|${detail}`
  const now = Date.now()

  // 同一时间只保留一个错误弹窗；重复错误不叠加。
  if (errorDialogState.visible) return
  if (key === lastKey && now - lastAt < DUPLICATE_WINDOW_MS) return

  lastKey = key
  lastAt = now
  errorDialogState.title = title
  errorDialogState.message = text
  errorDialogState.detail = detail
  errorDialogState.visible = true
}

export function closeErrorDialog() {
  errorDialogState.visible = false
}
