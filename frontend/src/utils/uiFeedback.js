import {ElMessage as ElementMessage} from 'element-plus'
import {showErrorDialog} from './errorDialog.js'

const withDefaults = (message, options = {}) => ({
  message,
  duration: 1400,
  grouping: true,
  ...options
})

export const ElMessage = {
  success: (message, options) => ElementMessage.success(withDefaults(message, options)),
  warning: (message, options) => ElementMessage.warning(withDefaults(message, options)),
  info: (message, options) => ElementMessage.info(withDefaults(message, options)),
  error: (message, options = {}) => showErrorDialog(message, options)
}
