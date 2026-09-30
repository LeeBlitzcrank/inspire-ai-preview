/**
 * 文件：frontend/src/utils/uiFeedback.js
 * 所属模块：前端通用工具和基础能力
 * 主要职责：前端工具模块，提供可复用基础函数
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
