<!--
  文件：frontend/src/pages/Messages.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="messages-page scheme-a" :class="{ 'chat-mode': activeConversation }">
    <template v-if="!activeConversation">
      <header class="app-header">
        <button class="icon-button" type="button" aria-label="返回" @click="goBack">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
        </button>
        <div class="header-copy">
          <div class="app-title">私信</div>
          <div class="app-sub">{{ conversations.length }} 个会话 · {{ unreadTotal }} 条未读</div>
        </div>
        <button
          class="icon-button action-danger"
          type="button"
          aria-label="清空聊天"
          :disabled="!conversations.length"
          @click="askClearAll"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="m19 6-1 14H6L5 6" />
            <path d="M10 11v5" />
            <path d="M14 11v5" />
          </svg>
        </button>
      </header>

      <div class="list-tools">
        <label class="search-wrap">
          <svg class="icon search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.6-3.6" />
          </svg>
          <input v-model="searchText" data-message-search placeholder="搜索昵称或消息" autocomplete="off">
          <button
            v-if="searchText"
            class="search-clear"
            type="button"
            aria-label="清空搜索"
            @click="searchText = ''"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </label>
        <div class="segment" role="tablist" aria-label="会话筛选">
          <button
            type="button"
            :class="{ on: filter === 'all' }"
            @click="filter = 'all'"
          >
            全部
          </button>
          <button
            type="button"
            :class="{ on: filter === 'unread' }"
            @click="filter = 'unread'"
          >
            未读{{ unreadTotal ? ` ${unreadTotal}` : '' }}
          </button>
        </div>
      </div>

      <div class="list-scroll">
        <AppState
          :state="conversationState"
          :rows="4"
          empty-icon="💬"
          empty-text="还没有会话"
          error-text="会话加载失败"
          @retry="loadConversations"
        >
          <template v-if="filteredUnreadConversations.length">
            <div class="list-label">未读消息 · {{ filteredUnreadConversations.length }}</div>
            <div
              v-for="c in filteredUnreadConversations"
              :key="`unread-${c.id}`"
              class="conv-row"
              role="button"
              tabindex="0"
              @click="openConversation(c)"
              @keydown.enter.prevent="openConversation(c)"
            >
              <div class="avatar" :style="avatarStyle(c)">
                {{ getOtherName(c)[0] }}
              </div>
              <div class="conv-main">
                <div class="conv-name-row">
                  <span class="conv-name truncate">{{ getOtherName(c) }}</span>
                  <span class="conv-time">{{ formatTime(c.lastTime) }}</span>
                </div>
                <div class="conv-preview truncate">{{ c.lastContent || '暂无消息' }}</div>
              </div>
              <span class="unread">{{ unreadText(getUnread(c)) }}</span>
            </div>
          </template>

          <template v-if="filter === 'all' && filteredRegularConversations.length">
            <div class="list-label">全部会话</div>
            <div
              v-for="c in filteredRegularConversations"
              :key="`all-${c.id}`"
              class="conv-row"
              role="button"
              tabindex="0"
              @click="openConversation(c)"
              @keydown.enter.prevent="openConversation(c)"
            >
              <div class="avatar" :style="avatarStyle(c)">
                {{ getOtherName(c)[0] }}
              </div>
              <div class="conv-main">
                <div class="conv-name-row">
                  <span class="conv-name truncate">{{ getOtherName(c) }}</span>
                  <span class="conv-time">{{ formatTime(c.lastTime) }}</span>
                </div>
                <div class="conv-preview truncate">{{ c.lastContent || '暂无消息' }}</div>
              </div>
            </div>
          </template>

          <div v-if="!filteredConversations.length" class="empty-list">
            <b>{{ searchText ? '没有匹配的会话' : filter === 'unread' ? '未读消息已全部处理完' : '还没有会话' }}</b>
            <span>{{ searchText ? '换个关键词试试' : filter === 'unread' ? '所有会话都已读' : '发起私信后会显示在这里' }}</span>
          </div>
        </AppState>
      </div>
    </template>

    <template v-else>
      <header class="chat-head">
        <button class="icon-button" type="button" aria-label="返回会话列表" @click="backToList">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
        </button>
        <div class="avatar sm" :style="avatarStyle(activeConversation)">
          {{ getOtherName(activeConversation)[0] }}
        </div>
        <div class="chat-header-copy">
          <div class="chat-title truncate">{{ getOtherName(activeConversation) }}</div>
          <div class="chat-status truncate">{{ chatSubtitle }}</div>
        </div>
        <button
          class="icon-button action-danger"
          type="button"
          aria-label="删除会话"
          @click="askDelete(activeConversation)"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="m19 6-1 14H6L5 6" />
            <path d="M10 11v5" />
            <path d="M14 11v5" />
          </svg>
        </button>
      </header>

      <div ref="msgBox" class="chat-scroll">
        <AppState
          :state="messageState"
          :rows="4"
          empty-icon="💬"
          empty-text="暂无消息"
          error-text="消息加载失败"
          @retry="reloadMessages"
        >
          <div class="day-label">{{ chatDayLabel }}</div>
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-row"
            :class="{ me: isMine(msg) }"
          >
            <div
              class="avatar message-avatar"
              :style="isMine(msg) ? { background: '#49645c' } : avatarStyle(activeConversation)"
            >
              {{ isMine(msg) ? myFirstChar : getOtherName(activeConversation)[0] }}
            </div>
            <div class="msg-stack">
              <div v-if="msg.recalledAt" class="bubble recalled">
                {{ isMine(msg) ? '你撤回了一条消息' : '对方撤回了一条消息' }}
              </div>
              <div v-else-if="msg.type === 'image'" class="bubble image-bubble">
                <img :src="msg.content" alt="私信图片" @load="scrollMessagesToBottom" @click="previewImage(msg.content)">
              </div>
              <div v-else-if="msg.type === 'inspire'" class="bubble inspire-bubble" @click="openInspireCard(msg)">
                <img v-if="inspireExtra(msg).img" :src="thumbOf(inspireExtra(msg).img, 220)" alt="" @load="scrollMessagesToBottom">
                <span>
                  <small>灵感卡片</small>
                  <b>{{ inspireExtra(msg).title || msg.content || '查看灵感' }}</b>
                </span>
              </div>
              <div v-else class="bubble">{{ msg.content }}</div>
              <div class="msg-meta">
                <span>{{ formatTimeDetail(msg.createTime) }}</span>
                <span v-if="isMine(msg) && !msg.recalledAt">· {{ msg.isRead ? '已读' : '未读' }}</span>
                <button v-if="isMine(msg) && !msg.recalledAt" type="button" @click="recallMsg(msg)">撤回</button>
              </div>
            </div>
          </div>
        </AppState>
      </div>

      <div class="composer">
        <input ref="imageInput" class="message-file-input" type="file" accept="image/*" @change="sendImage">
        <button class="composer-tool" type="button" title="发送图片" @click="imageInput?.click()">🖼</button>
        <button class="composer-tool" type="button" title="发送灵感卡片" @click="openInspirePicker">🔖</button>
        <input
          v-model="inputMsg"
          data-message-input
          placeholder="输入消息，回车发送"
          autocomplete="off"
          @keydown.enter.exact.prevent="sendMsg"
        >
        <button
          class="send-button"
          type="button"
          :disabled="!inputMsg.trim() || sending"
          @click="sendMsg"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m22 2-7 20-4-9-9-4Z" />
            <path d="M22 2 11 13" />
          </svg>
          {{ sending ? '发送中' : '发送' }}
        </button>
      </div>
    </template>

    <el-dialog v-model="inspirePickerOpen" title="发送灵感卡片" width="90%" append-to-body>
      <div class="inspire-picker">
        <div v-if="inspirePickerLoading" class="inspire-picker-tip">加载中…</div>
        <div v-else-if="!myInspireList.length" class="inspire-picker-tip">暂无已发布的灵感</div>
        <button
          v-for="item in myInspireList"
          :key="item.id"
          type="button"
          class="inspire-pick-row"
          @click="sendInspireCard(item)"
        >
          <img v-if="item.img" :src="thumbOf(item.img, 180)" alt="">
          <span>
            <b>{{ item.title || '无标题' }}</b>
            <small>{{ item.tag || '灵感' }}</small>
          </span>
        </button>
      </div>
    </el-dialog>

    <div v-if="modal" class="modal-mask" @click.self="closeModal">
      <div class="sheet" role="dialog" aria-modal="true">
        <h3>{{ modal.type === 'delete' ? '删除这个会话？' : '清空全部聊天？' }}</h3>
        <p>
          {{ modal.type === 'delete'
            ? `将删除与「${getOtherName(modal.conversation)}」的会话和消息。`
            : '将删除所有会话和全部消息，该操作不可恢复。' }}
        </p>
        <div class="sheet-actions">
          <button class="ghost" type="button" :disabled="modalBusy" @click="closeModal">取消</button>
          <button class="danger" type="button" :disabled="modalBusy" @click="confirmModal">
            {{ modalBusy ? '处理中' : modal.type === 'delete' ? '删除' : '全部清空' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getAccessToken} from '@/utils/tokenStorage.js'
import {thumbOf} from '@/utils/media.js'
import {
  deleteAllConversations,
  deleteConversation,
  getConversations,
  getMessages,
  markMessageRead
} from '@/api/message.js'
import {useMessageRealtime} from './messages/composables/useMessageRealtime.js'
import {useMessageComposer} from './messages/composables/useMessageComposer.js'
import {
  formatConversationTime as formatTime,
  formatMessageTime as formatTimeDetail
} from './messages/utils/messageTime.js'

const route = useRoute()
const router = useRouter()

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg,#4f9b83,#1f7560)',
  'linear-gradient(135deg,#5b7fd6,#3956ae)',
  'linear-gradient(135deg,#d48c61,#b9653f)',
  'linear-gradient(135deg,#8d75bd,#6d55a3)',
  'linear-gradient(135deg,#5f9ea0,#367b7e)',
  'linear-gradient(135deg,#c66f8d,#9c4364)'
]

const myId = computed(() => {
  const tokens = [getAccessToken(), sessionStorage.getItem('token')]
  for (const token of tokens) {
    if (!token) continue
    try {
      const payload = JSON.parse(atob(String(token).split('.')[1]))
      if (payload?.sub) return String(payload.sub)
    } catch (e) {
      // 当前 token 解析失败时继续尝试兜底来源。
    }
  }
  return String(sessionStorage.getItem('userId') || '')
})

const myFirstChar = computed(() => (sessionStorage.getItem('userNickname') || '我')[0])
const conversations = ref([])
const conversationLoading = ref(false)
const conversationError = ref('')
const activeConversation = ref(null)
const messages = ref([])
const messageLoading = ref(false)
const messageError = ref('')
const inputMsg = ref('')
const searchText = ref('')
const filter = ref('all')
const modal = ref(null)
const modalBusy = ref(false)
const msgBox = ref(null)
const conversationState = computed(() => {
  if (conversationLoading.value && !conversations.value.length) return 'loading'
  if (conversationError.value && !conversations.value.length) return 'error'
  return conversations.value.length ? 'ready' : 'empty'
})

const messageState = computed(() => {
  if (messageLoading.value && !messages.value.length) return 'loading'
  if (messageError.value && !messages.value.length) return 'error'
  return messages.value.length ? 'ready' : 'empty'
})

const unreadTotal = computed(() => (
  conversations.value.reduce((total, conversation) => total + getUnread(conversation), 0)
))

const filteredConversations = computed(() => {
  const keyword = searchText.value.trim().toLowerCase()
  return conversations.value.filter(conversation => {
    const unread = getUnread(conversation)
    if (filter.value === 'unread' && unread <= 0) return false
    if (!keyword) return true
    const name = getOtherName(conversation)
    return [name, conversation.lastContent]
      .some(value => String(value || '').toLowerCase().includes(keyword))
  })
})

const filteredUnreadConversations = computed(() => (
  filteredConversations.value.filter(conversation => getUnread(conversation) > 0)
))

const filteredRegularConversations = computed(() => (
  filteredConversations.value.filter(conversation => getUnread(conversation) <= 0)
))

const chatSubtitle = computed(() => {
  if (!activeConversation.value) return ''
  return '消息会实时同步'
})

const chatDayLabel = computed(() => {
  const first = messages.value[0]
  if (!first?.createTime) return '今天'
  const date = new Date(first.createTime)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return `${date.getMonth() + 1}月${date.getDate()}日`
})

const getOtherName = (conversation) => {
  if (!conversation) return '用户'
  if (conversation.targetNickname) return conversation.targetNickname
  const otherId = String(conversation.user1Id) === String(myId.value)
    ? conversation.user2Id
    : conversation.user1Id
  return otherId ? `用户${String(otherId).slice(-4)}` : '用户'
}

const getUnread = (conversation) => {
  if (!conversation) return 0
  const value = String(conversation.user1Id) === String(myId.value)
    ? conversation.unreadUser1
    : conversation.unreadUser2
  return Number(value || 0)
}

const unreadText = (count) => count > 99 ? '99+' : String(count)

const avatarStyle = (conversation) => {
  const seed = String(getOtherName(conversation) || conversation?.id || '')
  let hash = 0
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) >>> 0
  return { background: AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.length] }
}

const isMine = (message) => String(message.fromUserId) === String(myId.value)
const extraOf = (message) => {
  if (!message?.extraJson) return {}
  try {
    return typeof message.extraJson === 'string' ? JSON.parse(message.extraJson) : message.extraJson
  } catch (e) {
    return {}
  }
}
const inspireExtra = (message) => extraOf(message)
const previewImage = (url) => window.open(url, '_blank', 'noopener,noreferrer')
const openInspireCard = (message) => {
  const id = inspireExtra(message).id
  if (id) router.push(`/detail/${id}`)
}

const scrollMessagesToBottom = async () => {
  await nextTick()
  await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

const loadConversations = async () => {
  if (!conversations.value.length) conversationLoading.value = true
  conversationError.value = ''
  try {
    const res = await getConversations()
    conversations.value = res.data || []
    if (activeConversation.value) {
      const latest = conversations.value.find(item => String(item.id) === String(activeConversation.value.id))
      if (latest) activeConversation.value = latest
    }
  } catch (e) {
    conversationError.value = e?.message || 'load conversations failed'
  } finally {
    conversationLoading.value = false
  }
}

const loadMessages = async (conversationId) => {
  const res = await getMessages(conversationId)
  return (res.data || []).reverse()
}

const openConversation = async (conversation) => {
  activeConversation.value = conversation
  messageLoading.value = true
  messageError.value = ''
  try {
    await markMessageRead(conversation.id)
    messages.value = await loadMessages(conversation.id)
    await scrollMessagesToBottom()
    await loadConversations()
  } catch (e) {
    messages.value = []
    messageError.value = e?.message || 'load messages failed'
  } finally {
    messageLoading.value = false
  }
}

const reloadMessages = async () => {
  if (activeConversation.value) await openConversation(activeConversation.value)
}

const backToList = () => {
  activeConversation.value = null
  messages.value = []
  messageError.value = ''
  loadConversations()
}

const goBack = () => {
  if (route.query.showList === '1') router.push('/')
  else router.back()
}

const askDelete = (conversation) => {
  modal.value = { type: 'delete', conversation }
}

const askClearAll = () => {
  if (!conversations.value.length) return
  modal.value = { type: 'clear' }
}

const closeModal = () => {
  if (modalBusy.value) return
  modal.value = null
}

const confirmModal = async () => {
  if (!modal.value || modalBusy.value) return
  modalBusy.value = true
  const current = modal.value
  try {
    if (current.type === 'delete') {
      await deleteConversation(current.conversation.id)
      ElMessage.success('已删除')
      if (String(activeConversation.value?.id) === String(current.conversation.id)) {
        activeConversation.value = null
        messages.value = []
        messageError.value = ''
      }
      await loadConversations()
    } else {
      await deleteAllConversations()
      ElMessage.success('已清空')
      conversations.value = []
      activeConversation.value = null
      messages.value = []
    }
    modal.value = null
  } catch (e) {
    ElMessage.error(current.type === 'delete' ? '删除失败' : '清空失败')
  } finally {
    modalBusy.value = false
  }
}

const composer = useMessageComposer({
  activeConversation,
  myId,
  messages,
  loadMessages,
  loadConversations,
  scrollMessagesToBottom
})
const {
  sending,
  imageInput,
  inspirePickerOpen,
  inspirePickerLoading,
  myInspireList,
  sendImage,
  openInspirePicker,
  sendInspireCard,
  recallMsg
} = composer
const sendMsg = () => composer.sendMsg(inputMsg)

useMessageRealtime({
  activeConversation,
  messages,
  loadConversations,
  loadMessages,
  scrollMessagesToBottom
})

onMounted(async () => {
  await loadConversations()
  if (route.query.convId && route.query.showList !== '1') {
    const found = conversations.value.find(item => String(item.id) === String(route.query.convId))
    await openConversation(found || { id: route.query.convId })
  }

})
</script>

<style scoped src="./messages/styles/messages.css"></style>
