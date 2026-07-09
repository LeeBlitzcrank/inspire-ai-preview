<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="sidebar-list" :class="{hide: activeConversation && isMobile}">
      <div class="list-top">
        <button @click="goBack" style="font-size:20px;border:none;background:none;cursor:pointer;margin-right:10px;">&larr;</button>
        私信
        <button @click="clearAllConversations" style="margin-left:auto;padding:4px 12px;border:none;border-radius:20px;background:#f56c6c22;color:#f56c6c;font-size:12px;cursor:pointer;">清空聊天</button>
      </div>
      <div class="list-scroll">
        <div v-for="c in conversations" :key="c.id" class="conv-row">
          <div class="session-item"
               :class="{active: activeConversation && activeConversation.id === c.id, swiping: swipedId === c.id}"
               :style="getSwipeStyle(c.id)"
               @click="openConversation(c)"
               @touchstart="onTouchStart($event, c.id)"
               @touchmove="onTouchMove($event, c.id)"
               @touchend="onTouchEnd()"
               @mousedown="onTouchStart($event, c.id)"
               @mousemove="onTouchMove($event, c.id)"
               @mouseup="onTouchEnd()"
               @mouseleave="onTouchEnd()">
            <div class="avatar">
              <div style="width:100%;height:100%;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;display:flex;align-items:center;justify-content:center;font-size:18px;">{{ getOtherName(c)[0] }}</div>
            </div>
            <div class="session-info">
              <div class="name-row">
                <span class="username">{{ getOtherName(c) }}</span>
                <span class="msg-time">{{ formatTime(c.lastTime) }}</span>
              </div>
              <div class="last-text">
                {{ c.lastContent }}
                <span v-if="getUnread(c) > 0" class="unread-badge">{{ getUnread(c) > 99 ? '99+' : getUnread(c) }}</span>
              </div>
            </div>
          </div>
          <div class="swipe-delete" @click="handleDelete(c)">删除</div>
        </div>
        <div v-if="conversations.length === 0" style="text-align:center;padding:60px 20px;color:#999;font-size:14px;">还没有会话</div>
      </div>
    </div>

    <!-- 右侧聊天窗口 -->
    <div class="chat-main" :class="{show: activeConversation && isMobile}">
      <template v-if="!activeConversation">
        <div class="empty-tip">请在左侧选择用户开启对话</div>
      </template>
      <template v-else>
        <div class="chat-header">
          <button @click="closeChat" style="font-size:22px;border:none;background:none;cursor:pointer;margin-right:4px;">&larr;</button>
          <div class="avatar small">
            <div style="width:100%;height:100%;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px;">{{ getOtherName(activeConversation)[0] }}</div>
          </div>
          <span class="chat-name">{{ getOtherName(activeConversation) }}</span>
        </div>
        <div class="msg-box" ref="msgBox">
          <div v-for="msg in messages" :key="msg.id"
               class="msg-item" :class="msg.fromUserId === myId ? 'msg-right' : 'msg-left'">
            <div class="msg-avatar">
              <div :style="{width:'100%',height:'100%',background:'linear-gradient(135deg,#667eea,#764ba2)',color:'#fff',display:'flex',alignItems:'center',justifyContent:'center',fontSize:'12px'}">
                {{ msg.fromUserId === myId ? myFirstChar : getOtherName(activeConversation)[0] }}
              </div>
            </div>
            <div class="bubble-wrap">
              <div class="bubble">{{ msg.content }}</div>
              <div class="msg-small-time">{{ formatTimeDetail(msg.createTime) }}</div>
            </div>
          </div>
          <div v-if="messages.length === 0" style="text-align:center;padding:60px 20px;color:#999;font-size:14px;">暂无消息</div>
        </div>
        <div class="input-area">
          <input id="inputText" v-model="inputMsg" placeholder="输入消息，回车发送" @keydown="onKeydown">
          <button id="sendBtn" @click="sendMsg" :disabled="!inputMsg.trim()">发送</button>
        </div>
      </template>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConversations, getMessages, sendMessage, markMessageRead, sendByUsername, deleteConversation, startConversation, deleteAllConversations } from '@/api/message.js'

const route = useRoute()
const router = useRouter()
const myId = computed(() => localStorage.getItem('userId') || '')
const myFirstChar = (localStorage.getItem('username') || '我')[0]
const isMobile = ref(window.innerWidth <= 768)
const conversations = ref([])
const activeConversation = ref(null)
const messages = ref([])
const inputMsg = ref('')
const msgBox = ref(null)
let pollTimer = null

const onResize = () => { isMobile.value = window.innerWidth <= 768 }

const loadConversations = async () => {
  try { const res = await getConversations(); conversations.value = res.data || [] }
  catch (e) { conversations.value = [] }
}

const getOtherName = (c) => {
  if (c.targetNickname) return c.targetNickname
  if (c.targetUsername) return c.targetUsername
  const otherId = c.user1Id === myId.value ? c.user2Id : c.user1Id
  return '用户' + String(otherId).slice(-4)
}

const getUnread = (c) => {
  return c.user1Id === myId.value ? c.unreadUser1 : c.unreadUser2
}

const openConversation = async (c) => {
  activeConversation.value = c
  try {
    await markMessageRead(c.id)
    const res = await getMessages(c.id)
    messages.value = (res.data || []).reverse()
    await nextTick()
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
    loadConversations()
  } catch (e) { messages.value = [] }
}

const closeChat = () => {
  if (route.query.showList === '1') {
    activeConversation.value = null
    loadConversations()
  } else {
    router.back()
  }
}

const sendMsg = async () => {
  if (!inputMsg.value.trim()) return
  const otherId = activeConversation.value.user1Id === myId.value
    ? activeConversation.value.user2Id : activeConversation.value.user1Id
  try {
    await sendMessage(otherId, inputMsg.value.trim())
    inputMsg.value = ''
    const res = await getMessages(activeConversation.value.id)
    messages.value = (res.data || []).reverse()
    await nextTick()
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  } catch (e) { ElMessage.error('发送失败') }
}

const onKeydown = (e) => { if (e.key === 'Enter') sendMsg() }

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t); const now = new Date()
  if (d.toDateString() === now.toDateString()) return d.toTimeString().slice(0, 5)
  const diff = (now - d) / 86400000
  if (diff < 2) return '昨天'
  if (diff < 7) { const days = ['日','一','二','三','四','五','六']; return '周' + days[d.getDay()] }
  return (d.getMonth() + 1) + '/' + d.getDate()
}

const formatTimeDetail = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toTimeString().slice(0, 5)
}




// 滑动删除
const swipedId = ref(null)
const swipeStartX = ref(0)
const swipeCurX = ref(0)

const onTouchStart = (e, convId) => {
  // 如果已有其他会话被滑出，先复位
  if (swipedId.value && swipedId.value !== convId) {
    swipedId.value = null
  }
  swipeStartX.value = e.clientX || (e.touches && e.touches[0].clientX)
  swipeCurX.value = swipeStartX.value
  swipedId.value = convId
}

const onTouchMove = (e, convId) => {
  if (swipedId.value !== convId) return
  const cx = e.clientX || (e.touches && e.touches[0].clientX)
  if (!cx) return
  swipeCurX.value = cx
}

const onTouchEnd = () => {
  const diff = swipeStartX.value - swipeCurX.value
  if (diff > 60) {
    // 左滑超过阈值，保持删除按钮显示
  } else {
    swipedId.value = null
  }
}

const getSwipeStyle = (convId) => {
  if (swipedId.value !== convId) return {}
  const diff = swipeStartX.value - swipeCurX.value
  if (diff < 0) return {}
  const offset = Math.min(diff, 100)
  return { transform: 'translateX(' + (-offset) + 'px)', transition: swipedId.value ? 'none' : 'transform 0.3s ease' }
}

const handleDelete = async (c) => {
  try {
    await ElMessageBox.confirm('确定删除与「' + getOtherName(c) + '」的会话？消息将一并删除', '提示')
    await deleteConversation(c.id)
    ElMessage.success('已删除')
    swipedId.value = null
    if (activeConversation.value && activeConversation.value.id === c.id) {
      activeConversation.value = null
    }
    loadConversations()
  } catch (e) { console.error(e) }
}


onMounted(async () => {
  window.addEventListener('resize', onResize)
  if (route.query.convId && route.query.showList !== '1') {
    // 直接进入聊天，加载会话以获取昵称
    const convRes = await getConversations()
    const convs = convRes.data || []
    const found = convs.find(c => String(c.id) === route.query.convId)
    const targetConv = found || { id: route.query.convId }
    activeConversation.value = targetConv
    try {
      const res = await getMessages(route.query.convId)
      messages.value = (res.data || []).reverse()
      await markMessageRead(route.query.convId)
      await nextTick()
      if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
    } catch (e) { console.error(e) }
  } else {
    loadConversations()
  }
  pollTimer = setInterval(() => {
    if (activeConversation.value) {
      getMessages(activeConversation.value.id).then(res => {
        const newMsgs = (res.data || []).reverse()
        if (newMsgs.length > messages.value.length) {
          messages.value = newMsgs
          if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
        }
      }).catch(e => console.error(e))
    }
    loadConversations()
  }, 3000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (pollTimer) clearInterval(pollTimer)
})


const goBack = () => {
  if (route.query.showList === '1' && activeConversation.value) {
    activeConversation.value = null
    loadConversations()
  } else if (route.query.showList === '1') {
    router.push('/')
  } else {
    router.back()
  }
}

const clearAllConversations = async () => {
  try {
    await ElMessageBox.confirm('确定清空所有聊天会话？消息将一并删除', '提示')
    await deleteAllConversations()
    ElMessage.success('已清空')
    conversations.value = []
    activeConversation.value = null
    messages.value = []
  } catch (e) { console.error(e) }
}

</script>

<style scoped>
*{ margin:0; padding:0; box-sizing:border-box; }
.chat-container{
  display:flex; height:100vh; max-width:1280px; margin:0 auto; background:#ffffff;
}
.sidebar-list{
  width:340px; border-right:1px solid #eee; display:flex; flex-direction:column; flex-shrink:0;
}
.list-top{
  padding:18px 22px; border-bottom:1px solid #eee; font-size:22px; font-weight:600; background:#fff; display:flex; align-items:center;
}
.list-scroll{ flex:1; overflow-y:auto; }
.session-item{
  display:flex; padding:15px 22px; gap:14px; cursor:pointer; transition:background 0.24s;
}
.session-item:hover{ background-color:#f8f9fc; }
.session-item.active{ background-color:#eff4ff; }
.avatar{
  width:50px; height:50px; border-radius:50%; flex-shrink:0; overflow:hidden; background:#e5e7eb;
}
.avatar.small{ width:38px; height:38px; }
.avatar img{ width:100%; height:100%; object-fit:cover; }
.session-info{ flex:1; overflow:hidden; }
.name-row{ display:flex; justify-content:space-between; margin-bottom:5px; }
.username{ font-size:15px; font-weight:500; color:#222; }
.msg-time{ font-size:12px; color:#999; }
.last-text{ font-size:13px; color:#777; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.unread-badge{ display:inline-block; margin-left:6px; background:#f56c6c; color:#fff; border-radius:10px; padding:1px 7px; font-size:11px; }

.chat-main{ flex:1; display:flex; flex-direction:column; background:#f8f8fa; }
.chat-header{
  padding:15px 22px; border-bottom:1px solid #eee; display:flex; align-items:center; gap:12px; background:#fff;
}
.chat-name{ font-size:16px; font-weight:500; }
.msg-box{ flex:1; padding:24px; overflow-y:auto; }
.msg-item{ margin-bottom:20px; display:flex; max-width:72%; }
.msg-left{ flex-direction:row; }
.msg-right{ flex-direction:row-reverse; margin-left:auto; }
.msg-avatar{
  width:36px; height:36px; border-radius:50%; flex-shrink:0; overflow:hidden;
}
.msg-left .msg-avatar{ margin-right:10px; }
.msg-right .msg-avatar{ margin-left:10px; }
.bubble-wrap{ display:flex; flex-direction:column; }
.bubble{ padding:11px 16px; font-size:14px; line-height:1.6; border-radius:18px; }
.msg-left .bubble{ background:#ffffff; border:1px solid #e8e8e8; border-top-left-radius:4px; color:#333; }
.msg-right .bubble{ background:#6366f1; color:#fff; border-top-right-radius:4px; }
.msg-small-time{ font-size:11px; margin-top:4px; color:#aaa; }
.msg-right .msg-small-time{ color:#c7c9ff; text-align:right; }
.msg-left .msg-small-time{ text-align:right; }

.input-area{
  padding:16px 22px; border-top:1px solid #eee; display:flex; gap:14px; align-items:center; background:#fff;
}
#inputText{
  flex:1; padding:13px 18px; border:1px solid #ddd; border-radius:99px; outline:none; font-size:14px; transition:border 0.2s;
}
#inputText:focus{ border-color:#6366f1; }
#sendBtn{
  padding:12px 26px; border:none; border-radius:99px; background:linear-gradient(120deg,#6366f1,#a855f7); color:white; font-size:14px; cursor:pointer; transition:all 0.25s;
}
#sendBtn:hover{ transform:translateY(-2px); box-shadow:0 4px 12px rgba(99,102,241,0.25); }
#sendBtn:disabled{ opacity:0.5; cursor:default; transform:none; box-shadow:none; }
.empty-tip{ flex:1; display:flex; align-items:center; justify-content:center; color:#999; font-size:15px; }

@media (max-width:768px){
  .sidebar-list{ width:100%; }
  .sidebar-list.hide{ display:none; }
  .chat-main{ position:fixed; top:0; left:100%; width:100%; height:100vh; transition:left 0.3s ease; background:#fff; z-index:100; }
  .chat-main.show{ left:0; }
}


.conv-row{ position:relative; overflow:hidden; }
.session-item{ position:relative; z-index:1; background:#fff; transition:transform 0s; }
.swipe-delete{ position:absolute; right:0; top:0; height:100%; width:80px;
  background:#f56c6c; color:#fff; display:flex; align-items:center; justify-content:center;
  font-size:14px; cursor:pointer; border-radius:0; z-index:0; }
</style>
