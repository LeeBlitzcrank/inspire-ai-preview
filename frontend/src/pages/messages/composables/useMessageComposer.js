/**
 * 文件：frontend/src/pages/messages/composables/useMessageComposer.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getMyInspires, uploadFile} from '@/api/inspire.js'
import {recallMessage, sendMessage} from '@/api/message.js'
import {INPUT_LIMITS, textLength} from '@/utils/validation.js'

export function useMessageComposer({
  activeConversation,
  myId,
  messages,
  loadMessages,
  loadConversations,
  scrollMessagesToBottom
}) {
  const sending = ref(false)
  const imageInput = ref(null)
  const inspirePickerOpen = ref(false)
  const inspirePickerLoading = ref(false)
  const myInspireList = ref([])

  const resolveOtherId = (conversation) => {
    if (!conversation) return null
    const mine = String(myId.value || '')
    const user1 = conversation.user1Id != null ? String(conversation.user1Id) : ''
    const user2 = conversation.user2Id != null ? String(conversation.user2Id) : ''
    if (user1 && user2) {
      if (!mine) return null
      return user1 === mine ? user2 : user1
    }
    return conversation.targetUserId || conversation.otherUserId || null
  }

  const sendMsg = async (inputMsg) => {
    const content = inputMsg.value.trim()
    if (!content || sending.value) return
    if (textLength(content) > INPUT_LIMITS.messageMax) {
      ElMessage.warning(`消息不能超过 ${INPUT_LIMITS.messageMax} 个字符`)
      return
    }
    const otherId = resolveOtherId(activeConversation.value)
    if (!otherId) {
      ElMessage.error('会话信息不完整，请从左侧会话列表重新进入')
      return
    }

    sending.value = true
    try {
      await sendMessage(otherId, content)
      inputMsg.value = ''
      messages.value = await loadMessages(activeConversation.value.id)
      await scrollMessagesToBottom()
      loadConversations()
    } catch (e) {
      ElMessage.error('发送失败')
    } finally {
      sending.value = false
    }
  }

  const sendImage = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file || sending.value) return
    const otherId = resolveOtherId(activeConversation.value)
    if (!otherId) return
    sending.value = true
    try {
      const fd = new FormData()
      fd.append('file', file)
      const upload = await uploadFile(fd)
      if (upload.code !== 200 || !upload.data?.url) throw new Error(upload.msg || '上传失败')
      await sendMessage(
        otherId,
        upload.data.url,
        'image',
        JSON.stringify({width: upload.data.width, height: upload.data.height})
      )
      messages.value = await loadMessages(activeConversation.value.id)
      await scrollMessagesToBottom()
      loadConversations()
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '图片发送失败')
    } finally {
      sending.value = false
    }
  }

  const openInspirePicker = async () => {
    inspirePickerOpen.value = true
    inspirePickerLoading.value = true
    try {
      const res = await getMyInspires(1, 30)
      myInspireList.value = res.data?.records || []
    } catch (e) {
      myInspireList.value = []
    } finally {
      inspirePickerLoading.value = false
    }
  }

  const sendInspireCard = async (item) => {
    const otherId = resolveOtherId(activeConversation.value)
    if (!otherId || sending.value) return
    sending.value = true
    try {
      const extra = JSON.stringify({id: item.id, title: item.title, img: item.img, tag: item.tag})
      await sendMessage(otherId, item.title || '灵感卡片', 'inspire', extra)
      inspirePickerOpen.value = false
      messages.value = await loadMessages(activeConversation.value.id)
      await scrollMessagesToBottom()
      loadConversations()
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '发送失败')
    } finally {
      sending.value = false
    }
  }

  const recallMsg = async (message) => {
    try {
      await recallMessage(activeConversation.value.id, message.id)
      messages.value = await loadMessages(activeConversation.value.id)
      await loadConversations()
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '撤回失败')
    }
  }

  return {
    sending,
    imageInput,
    inspirePickerOpen,
    inspirePickerLoading,
    myInspireList,
    sendMsg,
    sendImage,
    openInspirePicker,
    sendInspireCard,
    recallMsg
  }
}
