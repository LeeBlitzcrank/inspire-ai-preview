import {onBeforeUnmount, onMounted} from 'vue'
import {connectMessageStream} from '@/api/message.js'
import {useVisibilityRefresh} from '@/composables/useVisibilityRefresh.js'

export function useMessageRealtime({
  activeConversation,
  messages,
  loadConversations,
  loadMessages,
  scrollMessagesToBottom
}) {
  let fallbackTimer = null
  let fallbackBusy = false
  let streamDisconnect = null
  let streamRetryTimer = null

  const refreshMessagesSilently = async () => {
    if (!activeConversation.value) return
    try {
      const latest = await loadMessages(activeConversation.value.id)
      if (latest.length > messages.value.length) {
        messages.value = latest
        await scrollMessagesToBottom()
      }
    } catch (e) {
      // 轮询失败不打断当前阅读，下一次轮询继续。
    }
  }

  const handleRealtimeEvent = async (event, payload) => {
    if (event === 'connected') return
    if (event === 'message' || event === 'recall') {
      await loadConversations()
      if (activeConversation.value
          && String(payload?.conversationId || '') === String(activeConversation.value.id)) {
        await refreshMessagesSilently()
      }
      return
    }
    if (event === 'read' || event === 'conversation') {
      await loadConversations()
    }
  }

  const connectStream = () => {
    streamDisconnect?.()
    streamDisconnect = connectMessageStream(
      handleRealtimeEvent,
      () => {
        if (streamRetryTimer) window.clearTimeout(streamRetryTimer)
        streamRetryTimer = window.setTimeout(connectStream, 3000)
      }
    )
  }

  onMounted(() => {
    connectStream()
    fallbackTimer = window.setInterval(() => {
      if (document.visibilityState !== 'visible' || fallbackBusy) return
      fallbackBusy = true
      Promise.allSettled([refreshMessagesSilently(), loadConversations()])
        .finally(() => { fallbackBusy = false })
    }, 30000)
  })
  useVisibilityRefresh(() => {
    loadConversations()
    refreshMessagesSilently()
  })

  onBeforeUnmount(() => {
    if (fallbackTimer) window.clearInterval(fallbackTimer)
    if (streamRetryTimer) window.clearTimeout(streamRetryTimer)
    streamDisconnect?.()
  })

  return {
    refreshMessagesSilently
  }
}
