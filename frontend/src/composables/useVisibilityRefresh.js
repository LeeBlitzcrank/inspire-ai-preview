/**
 * 文件：frontend/src/composables/useVisibilityRefresh.js
 * 所属模块：跨页面复用的组合式逻辑
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {onBeforeUnmount, onMounted} from 'vue'

export function useVisibilityRefresh(onVisible) {
  const handler = () => {
    if (!document.hidden) onVisible()
  }

  onMounted(() => document.addEventListener('visibilitychange', handler))
  onBeforeUnmount(() => document.removeEventListener('visibilitychange', handler))
}
