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
              <div class="bubble">{{ msg.content }}</div>
              <div class="msg-time">{{ formatTimeDetail(msg.createTime) }}</div>
            </div>
          </div>
        </AppState>
      </div>

      <div class="composer">
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
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getAccessToken} from '@/utils/tokenStorage.js'
import {
  deleteAllConversations,
  deleteConversation,
  getConversations,
  getMessages,
  markMessageRead,
  sendMessage
} from '@/api/message.js'

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

const myFirstChar = computed(() => (sessionStorage.getItem('username') || '我')[0])
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
const sending = ref(false)
const msgBox = ref(null)
let pollTimer = null
let pollBusy = false

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
    return [name, conversation.targetUsername, conversation.lastContent]
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
  if (activeConversation.value.targetUsername) return `@${activeConversation.value.targetUsername}`
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
  if (conversation.targetUsername) return conversation.targetUsername
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

const scrollMessagesToBottom = async () => {
  await nextTick()
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

const sendMsg = async () => {
  const content = inputMsg.value.trim()
  if (!content || sending.value) return
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

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) return date.toTimeString().slice(0, 5)
  const diff = (now - date) / 86400000
  if (diff < 2) return '昨天'
  if (diff < 7) {
    const days = ['日', '一', '二', '三', '四', '五', '六']
    return `周${days[date.getDay()]}`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatTimeDetail = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const hhmm = date.toTimeString().slice(0, 5)
  if (date.toDateString() === now.toDateString()) return hhmm
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return `昨天 ${hhmm}`
  return `${date.getMonth() + 1}月${date.getDate()}日 ${hhmm}`
}

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

const onVisibilityChange = () => {
  if (document.visibilityState !== 'visible') return
  loadConversations()
  refreshMessagesSilently()
}

onMounted(async () => {
  await loadConversations()
  if (route.query.convId && route.query.showList !== '1') {
    const found = conversations.value.find(item => String(item.id) === String(route.query.convId))
    await openConversation(found || { id: route.query.convId })
  }

  pollTimer = window.setInterval(() => {
    if (document.visibilityState !== 'visible' || pollBusy) return
    pollBusy = true
    Promise.allSettled([refreshMessagesSilently(), loadConversations()])
      .finally(() => { pollBusy = false })
  }, 3000)
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  if (pollTimer) window.clearInterval(pollTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.messages-page {
  --mint: #1f8a70;
  --mint-soft: #e8f7f2;
  --ink: #17201d;
  --muted: #8b9893;
  --line: #e7eeeb;
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--ink);
  background: #f7faf9;
}

.messages-page button,
.messages-page input {
  font: inherit;
}

.messages-page button {
  cursor: pointer;
}

.icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #24322e;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.icon-button:hover:not(:disabled) {
  background: rgba(23, 32, 29, 0.06);
}

.icon-button:disabled {
  opacity: 0.3;
  cursor: default;
}

.action-danger {
  color: #b85c4b;
}

.icon {
  width: 18px;
  height: 18px;
  display: block;
}

.app-header,
.chat-head {
  height: 62px;
  padding: 0 15px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  background: rgba(255, 255, 255, 0.98);
  border-bottom: 1px solid var(--line);
}

.app-header {
  padding-left: 8px;
}

.header-copy,
.chat-header-copy,
.conv-main {
  min-width: 0;
}

.header-copy {
  flex: 1;
}

.chat-header-copy {
  flex: 1;
}

.app-title {
  font-size: 18px;
  font-weight: 850;
  line-height: 1.15;
}

.app-sub {
  margin-top: 3px;
  color: var(--muted);
  font-size: 11.5px;
}

.list-tools {
  flex: 0 0 auto;
  padding: 11px 14px 0;
  background: #fff;
  border-bottom: 1px solid var(--line);
}

.search-wrap {
  height: 39px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e1eae7;
  border-radius: 12px;
  background: #fff;
  color: #83908c;
}

.search-wrap:focus-within {
  border-color: #8bc9b9;
}

.search-icon {
  flex: 0 0 auto;
}

.search-wrap input {
  width: 100%;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ink);
  font-size: 13px;
}

.search-wrap input::placeholder {
  color: #a7b1ad;
}

.search-clear {
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #eef3f1;
  color: #74817d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.search-clear .icon {
  width: 13px;
  height: 13px;
}

.segment {
  display: flex;
  gap: 4px;
  margin-top: 11px;
  padding: 3px;
  border-radius: 12px;
  background: #edf4f1;
}

.segment button {
  flex: 1;
  height: 32px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #73817c;
  font-size: 12.5px;
}

.segment button.on {
  background: #fff;
  color: var(--mint);
  font-weight: 800;
  box-shadow: 0 1px 5px rgba(23, 60, 49, 0.07);
}

.list-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 20px;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
}

.list-label {
  margin: 4px 2px 8px;
  color: #97a39f;
  font-size: 11px;
  font-weight: 800;
}

.conv-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 10px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.conv-row + .conv-row {
  margin-top: 2px;
}

.conv-row:hover,
.conv-row:focus-visible {
  outline: 0;
  background: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(31, 70, 58, 0.045);
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

.avatar.sm {
  width: 38px;
  height: 38px;
  font-size: 14px;
}

.conv-main {
  flex: 1;
  overflow: hidden;
}

.conv-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.conv-name {
  flex: 1;
  font-size: 14.5px;
  font-weight: 800;
}

.conv-time {
  flex: 0 0 auto;
  margin-left: auto;
  color: #9ba6a2;
  font-size: 11px;
}

.conv-preview {
  margin-top: 5px;
  color: #75827e;
  font-size: 12.5px;
  line-height: 1.35;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread {
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ef6654;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 10.5px;
  font-weight: 850;
}

.empty-list {
  padding: 54px 20px;
  text-align: center;
  color: #9aa6a2;
  font-size: 12.5px;
  line-height: 1.8;
}

.empty-list b {
  display: block;
  margin-bottom: 4px;
  color: #53645e;
  font-size: 15px;
}

.chat-head {
  padding: 0 12px;
  gap: 9px;
}

.chat-title {
  font-size: 14.5px;
  font-weight: 850;
}

.chat-status {
  margin-top: 3px;
  color: #7f8d88;
  font-size: 11px;
}

.chat-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 14px 18px;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  background:
    radial-gradient(circle at 10% 8%, rgba(31, 138, 112, 0.06), transparent 26%),
    #f5f9f7;
}

.day-label {
  margin: 4px 0 18px;
  text-align: center;
  color: #9aa7a2;
  font-size: 10.5px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.msg-row.me {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 31px;
  height: 31px;
  font-size: 11px;
}

.msg-stack {
  max-width: 73%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.msg-row.me .msg-stack {
  align-items: flex-end;
}

.bubble {
  padding: 10px 13px;
  border: 1px solid #e6eeeb;
  border-radius: 16px;
  background: #fff;
  color: var(--ink);
  box-shadow: 0 2px 8px rgba(31, 70, 58, 0.035);
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.msg-row.me .bubble {
  border-color: var(--mint);
  background: var(--mint);
  color: #fff;
}

.msg-time {
  margin-top: 5px;
  color: #9ca8a4;
  font-size: 10px;
}

.composer {
  flex: 0 0 auto;
  padding: 10px 11px 12px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: rgba(255, 255, 255, 0.98);
  border-top: 1px solid var(--line);
}

.composer input {
  width: 100%;
  min-width: 0;
  height: 42px;
  padding: 0 13px;
  border: 1px solid #dfe9e5;
  border-radius: 14px;
  outline: 0;
  background: #f9fbfa;
  color: var(--ink);
  font-size: 13px;
}

.composer input:focus {
  border-color: #8bc9b9;
  background: #fff;
}

.send-button {
  height: 42px;
  padding: 0 15px;
  border: 0;
  border-radius: 14px;
  background: var(--mint);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 0 0 auto;
  font-size: 12.5px;
  font-weight: 800;
}

.send-button:disabled {
  opacity: 0.45;
  cursor: default;
}

.modal-mask {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(14, 28, 24, 0.34);
}

.sheet {
  width: 100%;
  padding: 18px 18px 20px;
  border-radius: 22px 22px 0 0;
  background: #fff;
  box-shadow: 0 -18px 40px rgba(15, 35, 29, 0.18);
}

.sheet h3 {
  margin: 0 0 7px;
  font-size: 16px;
}

.sheet p {
  margin: 0;
  color: #73807c;
  font-size: 12.5px;
  line-height: 1.7;
}

.sheet-actions {
  display: flex;
  gap: 9px;
  margin-top: 17px;
}

.sheet-actions button {
  flex: 1;
  height: 42px;
  border: 0;
  border-radius: 13px;
  font-size: 13px;
  font-weight: 800;
}

.sheet-actions button:disabled {
  opacity: 0.55;
  cursor: default;
}

.sheet-actions .ghost {
  background: #eef3f1;
  color: #52625d;
}

.sheet-actions .danger {
  background: #ef6654;
  color: #fff;
}

@media screen and (max-width: 620px) {
  .search-wrap input,
  .composer input {
    font-size: 16px !important;
  }
}
</style>
