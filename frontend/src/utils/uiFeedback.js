import { ElMessage as ElementMessage } from 'element-plus'
import { showErrorDialog } from './errorDialog.js'

export const ElMessage = {
  success: (...args) => ElementMessage.success(...args),
  warning: (...args) => ElementMessage.warning(...args),
  info: (...args) => ElementMessage.info(...args),
  error: (message, options = {}) => showErrorDialog(message, options)
}
