import {onBeforeUnmount, onMounted} from 'vue'

export function useVisibilityRefresh(onVisible) {
  const handler = () => {
    if (!document.hidden) onVisible()
  }

  onMounted(() => document.addEventListener('visibilitychange', handler))
  onBeforeUnmount(() => document.removeEventListener('visibilitychange', handler))
}
