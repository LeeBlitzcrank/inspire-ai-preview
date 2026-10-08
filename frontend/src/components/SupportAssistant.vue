<!--
  文件：frontend/src/components/SupportAssistant.vue
  所属模块：可复用 Vue 组件
  主要职责：项目客服 RAG 浮动问答窗口
  INSPIRE_FILE_HEADER
-->
<template>
  <div ref="rootRef" class="support-root">
    <transition name="support-pop">
      <section v-if="open" ref="panelRef" class="support-panel" :style="panelStyle">
        <header class="support-head">
          <div>
            <b>AI客服</b>
            <span>{{ loading ? '正在查询项目文档…' : '在线 · 项目使用与技术答疑' }}</span>
          </div>
          <div class="support-head-actions">
            <button type="button" class="support-ticket-entry" @click="openTickets">工单</button>
            <button type="button" aria-label="关闭客服" @click="open = false">×</button>
          </div>
        </header>

        <div ref="scrollRef" class="support-messages">
          <article
            v-for="(message, index) in renderedMessages"
            :key="index"
            class="support-message"
            :class="message.role"
          >
            <span class="message-avatar">{{ message.role === 'assistant' ? '灵' : '我' }}</span>
            <div class="message-body">
              <div class="message-bubble">
                <template v-for="(block, blockIndex) in message.blocks" :key="blockIndex">
                  <p v-if="block.type === 'paragraph'">{{ block.text }}</p>
                  <ol v-else-if="block.type === 'ordered'">
                    <li v-for="(item, itemIndex) in block.items" :key="itemIndex">{{ item }}</li>
                  </ol>
                  <ul v-else>
                    <li v-for="(item, itemIndex) in block.items" :key="itemIndex">{{ item }}</li>
                  </ul>
                </template>
              </div>
              <div v-if="message.sources?.length" class="support-sources">
                <span>参考文档</span>
                <button
                  v-for="(source, sourceIndex) in message.sources"
                  :key="sourceIndex"
                  type="button"
                  :title="source.filePath"
                  @click="copySource(source)"
                >
                  {{ sourceIndex + 1 }}. {{ source.title }} · {{ source.section }}
                </button>
              </div>
              <button
                v-if="message.action === 'handoff'"
                type="button"
                class="message-action"
                @click="openHandoff"
              >打开转人工服务</button>
            </div>
          </article>
          <article v-if="loading" class="support-message assistant">
            <div class="message-bubble loading-bubble">
              <i></i><i></i><i></i>
            </div>
          </article>
        </div>

        <div v-if="messages.length <= 1" class="support-prompts">
          <button
            v-for="item in prompts"
            :key="item"
            type="button"
            @click="submit(item)"
          >{{ item }}</button>
        </div>

        <div v-if="handoffOpen" class="support-sheet">
          <div class="sheet-head">
            <b>转人工服务</b>
            <button type="button" @click="handoffOpen = false">×</button>
          </div>
          <p>补充问题并上传报错截图或视频，提交后会生成人工工单。</p>
          <textarea
            v-model="handoffForm.issue"
            maxlength="1000"
            rows="3"
            placeholder="请描述具体问题、操作步骤和报错现象"
          ></textarea>
          <input
            v-model="handoffForm.contact"
            maxlength="200"
            placeholder="联系方式（邮箱/手机号，可留空）"
          />
          <div class="support-upload-row">
            <label class="support-upload-btn">
              <input
                type="file"
                accept="image/*,video/mp4,video/webm,video/quicktime"
                multiple
                @change="selectHandoffFiles"
              />
              上传截图/视频
            </label>
            <span>{{ handoffFiles.length }}/5</span>
          </div>
          <div v-if="handoffFiles.length" class="support-file-list">
            <div v-for="(file, index) in handoffFiles" :key="`${file.name}-${index}`">
              <span>{{ file.name }}</span>
              <button type="button" @click="handoffFiles.splice(index, 1)">移除</button>
            </div>
          </div>
          <div class="sheet-actions">
            <button type="button" class="ghost" @click="handoffOpen = false">取消</button>
            <button type="button" class="primary" :disabled="handoffSubmitting" @click="submitHandoff">
              {{ handoffSubmitting ? handoffStatus : '提交人工服务' }}
            </button>
          </div>
        </div>

        <div v-if="ticketsOpen" class="support-sheet ticket-sheet">
          <div class="sheet-head">
            <b>{{ selectedTicket ? '工单详情' : '我的工单' }}</b>
            <button type="button" @click="closeTickets">×</button>
          </div>
          <template v-if="!selectedTicket">
            <div v-if="ticketLoading" class="ticket-empty">正在刷新工单…</div>
            <div v-else-if="!tickets.length" class="ticket-empty">暂时没有提交过人工工单</div>
            <button
              v-for="ticket in tickets"
              :key="ticket.ticketNo"
              type="button"
              class="ticket-row"
              @click="selectedTicket = ticket"
            >
              <span>
                <b>{{ ticket.ticketNo }}</b>
                <small>{{ ticket.issue }}</small>
              </span>
              <em :class="ticket.status">{{ statusText(ticket.status) }}</em>
            </button>
          </template>
          <template v-else>
            <div class="ticket-meta">
              <b>{{ selectedTicket.ticketNo }}</b>
              <span :class="selectedTicket.status">{{ statusText(selectedTicket.status) }}</span>
            </div>
            <div class="ticket-thread">
              <div
                v-for="(message, index) in selectedTicket.messages || []"
                :key="index"
                class="ticket-message"
                :class="message.senderType"
              >
                <small>{{ message.senderType === 'ADMIN' ? '人工客服' : '我' }} · {{ message.createTime }}</small>
                <p>{{ message.content }}</p>
              </div>
            </div>
            <div v-if="selectedTicket.attachments?.length" class="ticket-attachments">
              <span>附件</span>
              <a
                v-for="(file, index) in selectedTicket.attachments"
                :key="index"
                :href="file.url"
                target="_blank"
                rel="noreferrer"
              >{{ file.originalName || `附件${index + 1}` }}</a>
            </div>
            <div class="sheet-actions">
              <button type="button" class="ghost" @click="selectedTicket = null">返回列表</button>
              <button type="button" class="primary" @click="refreshSelectedTicket">刷新回复</button>
            </div>
          </template>
        </div>

        <form class="support-composer" @submit.prevent="submit()">
          <textarea
            v-model="input"
            maxlength="500"
            rows="2"
            placeholder="例如：怎么发布一篇灵感？"
            @keydown.enter.exact.prevent="submit()"
          ></textarea>
          <button type="submit" :disabled="loading || !input.trim()">发送</button>
        </form>
      </section>
    </transition>

    <button
      v-if="!open"
      class="support-launcher"
      :class="{ dragging }"
      :style="launcherStyle"
      type="button"
      aria-label="打开AI客服"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="onPointerCancel"
      @keydown.enter.prevent="openPanel"
      @keydown.space.prevent="openPanel"
    >
      <span class="launcher-avatar">
        AI
        <i></i>
      </span>
      <span class="launcher-copy">
        <b>AI客服</b>
        <small>在线 · 可转人工</small>
      </span>
    </button>
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {askSupport, createSupportHandoff, getSupportTicket, uploadSupportAttachment} from '@/api/support.js'

const open = ref(false)
const input = ref('')
const loading = ref(false)
const scrollRef = ref(null)
const rootRef = ref(null)
const panelRef = ref(null)
const supportX = ref(0)
const supportY = ref(0)
const panelLeft = ref(0)
const panelTop = ref(0)
const dragging = ref(false)
const userPositioned = ref(false)
const handoffOpen = ref(false)
const handoffSubmitting = ref(false)
const handoffStatus = ref('提交中…')
const handoffFiles = ref([])
const handoffForm = ref({issue: '', contact: ''})
const ticketsOpen = ref(false)
const ticketLoading = ref(false)
const tickets = ref([])
const selectedTicket = ref(null)
const TICKET_STORAGE_KEY = 'inspire_support_tickets'
const LAUNCHER_POSITION_KEY = 'inspire_support_launcher_position'
const LAUNCHER_SIZE = 54
const EDGE_GAP = 12
const messages = ref([
  {
    role: 'assistant',
    text: '你好，我是 AI 客服。项目使用、功能和实现问题都可以问我。输入“人工”可以转人工服务。'
  }
])

const prompts = [
  '怎么发布一篇灵感？',
  '怎么绑定手机号？',
  '世界种子怎么玩？',
  '怎么收藏到不同文件夹？',
  '输入“人工”转人工'
]

const isHumanRequest = (query) => {
  const text = String(query || '').replace(/\s+/g, '').toLowerCase()
  return ['人工', '真人', '转人工', '人工客服', '找客服', '联系客服', 'humanagent', 'liveagent']
    .some(keyword => text.includes(keyword))
}

const statusText = (status) => ({
  PENDING: '待处理',
  PROCESSING: '处理中',
  REPLIED: '已回复',
  CLOSED: '已关闭'
}[status] || status || '待处理')

const loadStoredTickets = () => {
  try {
    const value = JSON.parse(localStorage.getItem(TICKET_STORAGE_KEY) || '[]')
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

const saveStoredTickets = (list) => {
  localStorage.setItem(TICKET_STORAGE_KEY, JSON.stringify(list.slice(0, 20)))
}

const launcherStyle = computed(() => ({
  left: `${supportX.value}px`,
  top: `${supportY.value}px`
}))

const panelStyle = computed(() => ({
  left: `${panelLeft.value}px`,
  top: `${panelTop.value}px`
}))

const clamp = (value, min, max) => Math.max(min, Math.min(value, max))

const deviceBounds = () => {
  const width = rootRef.value?.clientWidth || window.innerWidth
  const height = rootRef.value?.clientHeight || window.innerHeight
  return {left: 0, top: 0, width, height, right: width, bottom: height}
}

const clampLauncherPosition = (x, y) => {
  const bounds = deviceBounds()
  return {
    x: clamp(x, bounds.left + EDGE_GAP, bounds.right - LAUNCHER_SIZE - EDGE_GAP),
    y: clamp(y, bounds.top + EDGE_GAP, bounds.bottom - LAUNCHER_SIZE - EDGE_GAP)
  }
}

const setDefaultLauncherPosition = () => {
  const bounds = deviceBounds()
  const position = clampLauncherPosition(
    bounds.right - LAUNCHER_SIZE - EDGE_GAP,
    bounds.bottom - LAUNCHER_SIZE - 88
  )
  supportX.value = position.x
  supportY.value = position.y
}

const loadStoredLauncherPosition = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(LAUNCHER_POSITION_KEY) || 'null')
    if (Number.isFinite(saved?.x) && Number.isFinite(saved?.y)) {
      const position = clampLauncherPosition(saved.x, saved.y)
      supportX.value = position.x
      supportY.value = position.y
      userPositioned.value = true
      return true
    }
  } catch {
    // ignore invalid stored position
  }
  return false
}

const saveLauncherPosition = () => {
  localStorage.setItem(LAUNCHER_POSITION_KEY, JSON.stringify({
    x: supportX.value,
    y: supportY.value
  }))
}

const syncLauncherPosition = () => {
  if (userPositioned.value) {
    const position = clampLauncherPosition(supportX.value, supportY.value)
    supportX.value = position.x
    supportY.value = position.y
  } else {
    setDefaultLauncherPosition()
  }
  if (open.value) nextTick(positionPanel)
}

const positionPanel = () => {
  const panel = panelRef.value
  if (!panel) return
  const bounds = deviceBounds()
  const panelWidth = panel.offsetWidth
  const panelHeight = panel.offsetHeight
  let left = supportX.value + LAUNCHER_SIZE - panelWidth
  let top = supportY.value - panelHeight - 8
  if (top < bounds.top + EDGE_GAP) {
    top = supportY.value + LAUNCHER_SIZE + 8
  }
  left = clamp(left, bounds.left + EDGE_GAP, bounds.right - panelWidth - EDGE_GAP)
  top = clamp(top, bounds.top + EDGE_GAP, bounds.bottom - panelHeight - EDGE_GAP)
  panelLeft.value = left
  panelTop.value = top
}

const openPanel = async () => {
  open.value = true
  await nextTick()
  positionPanel()
  await scrollToBottom()
}

let dragState = null
let movedDuringDrag = false

const onPointerDown = (event) => {
  if (event.button !== undefined && event.button !== 0) return
  dragState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    originX: supportX.value,
    originY: supportY.value
  }
  movedDuringDrag = false
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

const onPointerMove = (event) => {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (!movedDuringDrag && Math.hypot(deltaX, deltaY) < 5) return
  movedDuringDrag = true
  dragging.value = true
  const rootWidth = rootRef.value?.clientWidth || 1
  const visualWidth = rootRef.value?.getBoundingClientRect().width || rootWidth
  const scale = visualWidth / rootWidth
  const position = clampLauncherPosition(
    dragState.originX + deltaX / scale,
    dragState.originY + deltaY / scale
  )
  supportX.value = position.x
  supportY.value = position.y
}

const finishDrag = (event) => {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  const moved = movedDuringDrag
  event.currentTarget.releasePointerCapture?.(event.pointerId)
  dragState = null
  movedDuringDrag = false
  dragging.value = false
  if (moved) {
    userPositioned.value = true
    saveLauncherPosition()
  } else {
    openPanel()
  }
}

const onPointerUp = (event) => finishDrag(event)
const onPointerCancel = (event) => finishDrag(event)

const parseBlocks = (value) => {
  const text = String(value || '')
    .replace(/\r/g, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .trim()
  if (!text) return [{type: 'paragraph', text: ''}]

  const blocks = []
  let paragraph = []
  let ordered = []
  let unordered = []

  const flush = () => {
    if (paragraph.length) {
      blocks.push({type: 'paragraph', text: paragraph.join(' ')})
      paragraph = []
    }
    if (ordered.length) {
      blocks.push({type: 'ordered', items: ordered})
      ordered = []
    }
    if (unordered.length) {
      blocks.push({type: 'unordered', items: unordered})
      unordered = []
    }
  }

  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim()
    if (!line) {
      flush()
      continue
    }
    const orderedMatch = line.match(/^\d+[.、]\s*(.+)$/)
    const unorderedMatch = line.match(/^[-*•]\s*(.+)$/)
    if (orderedMatch) {
      if (paragraph.length || unordered.length) flush()
      ordered.push(orderedMatch[1].trim())
    } else if (unorderedMatch) {
      if (paragraph.length || ordered.length) flush()
      unordered.push(unorderedMatch[1].trim())
    } else {
      if (ordered.length || unordered.length) flush()
      paragraph.push(line)
    }
  }
  flush()
  return blocks.length ? blocks : [{type: 'paragraph', text}]
}

const renderedMessages = computed(() => messages.value.map(message => ({
  ...message,
  blocks: parseBlocks(message.text)
})))

const scrollToBottom = async () => {
  await nextTick()
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
}

const submit = async (preset) => {
  const query = String(preset || input.value || '').trim()
  if (!query || loading.value) return
  input.value = ''
  messages.value.push({role: 'user', text: query})
  if (isHumanRequest(query)) {
    messages.value.push({
      role: 'assistant',
      text: '可以，我帮你转人工。请补充问题，也可以上传报错截图或视频，提交后会生成人工工单。',
      action: 'handoff'
    })
    handoffForm.value.issue = query
    await scrollToBottom()
    return
  }
  await scrollToBottom()
  loading.value = true
  try {
    const res = await askSupport(query)
    if (res.code !== 200) throw new Error(res.msg || '客服暂时不可用')
    messages.value.push({
      role: 'assistant',
      text: res.data?.answer || '项目文档中没有找到明确答案。',
      sources: res.data?.sources || [],
      provider: res.data?.provider || ''
    })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      text: e?.response?.data?.msg || e?.message || '客服暂时不可用，请稍后再试。'
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const selectHandoffFiles = (event) => {
  const selected = Array.from(event.target.files || [])
  event.target.value = ''
  const valid = []
  for (const file of selected) {
    const isImage = file.type.startsWith('image/')
    const isVideo = file.type.startsWith('video/')
    if (!isImage && !isVideo) {
      ElMessage.warning(`${file.name} 不是支持的图片或视频`)
      continue
    }
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过 50MB`)
      continue
    }
    valid.push(file)
  }
  handoffFiles.value = [...handoffFiles.value, ...valid].slice(0, 5)
}

const openHandoff = () => {
  if (!handoffForm.value.issue.trim()) {
    const latestUser = [...messages.value].reverse().find(message => message.role === 'user')
    handoffForm.value.issue = latestUser?.text || ''
  }
  handoffOpen.value = true
}

const uploadHandoffFiles = async () => {
  const attachments = []
  for (let i = 0; i < handoffFiles.value.length; i++) {
    const file = handoffFiles.value[i]
    handoffStatus.value = `上传附件 ${i + 1}/${handoffFiles.value.length}…`
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadSupportAttachment(formData)
    if (res.code !== 200) throw new Error(res.msg || '附件上传失败')
    attachments.push({
      url: res.data.url,
      thumbUrl: res.data.thumbUrl || '',
      fileType: res.data.type === 'video' ? 'video' : 'image',
      originalName: file.name,
      duration: res.data.duration || ''
    })
  }
  return attachments
}

const submitHandoff = async () => {
  const issue = handoffForm.value.issue.trim()
  if (!issue) {
    ElMessage.warning('请填写需要人工处理的问题')
    return
  }
  handoffSubmitting.value = true
  handoffStatus.value = '提交中…'
  try {
    const attachments = await uploadHandoffFiles()
    handoffStatus.value = '创建工单…'
    const res = await createSupportHandoff(issue, handoffForm.value.contact.trim(), attachments)
    if (res.code !== 200) throw new Error(res.msg || '工单提交失败')
    const created = {
      ...res.data,
      issue,
      messages: [{
        senderType: 'USER',
        content: issue,
        createTime: res.data.createdAt
      }],
      attachments
    }
    const stored = loadStoredTickets()
    saveStoredTickets([created, ...stored.filter(item => item.ticketNo !== created.ticketNo)])
    messages.value.push({
      role: 'assistant',
      text: `人工工单已提交，编号：${created.ticketNo}。后台回复后，可以点右上角“工单”查看。`
    })
    handoffOpen.value = false
    handoffFiles.value = []
    handoffForm.value = {issue: '', contact: ''}
    await scrollToBottom()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '人工服务提交失败')
  } finally {
    handoffSubmitting.value = false
  }
}

const refreshTicket = async (ticket) => {
  const res = await getSupportTicket(ticket.ticketNo, ticket.accessToken)
  if (res.code !== 200) throw new Error(res.msg || '工单刷新失败')
  return {...ticket, ...res.data}
}

const openTickets = async () => {
  ticketsOpen.value = true
  selectedTicket.value = null
  ticketLoading.value = true
  const stored = loadStoredTickets()
  tickets.value = stored
  try {
    const refreshed = await Promise.all(stored.map(ticket =>
      refreshTicket(ticket).catch(() => ticket)
    ))
    tickets.value = refreshed
    saveStoredTickets(refreshed)
  } finally {
    ticketLoading.value = false
  }
}

const refreshSelectedTicket = async () => {
  if (!selectedTicket.value) return
  try {
    selectedTicket.value = await refreshTicket(selectedTicket.value)
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '工单刷新失败')
  }
}

const closeTickets = () => {
  ticketsOpen.value = false
  selectedTicket.value = null
}

const copySource = async (source) => {
  const text = `${source.filePath} / ${source.section}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('文档路径已复制')
  } catch {
    ElMessage.info(text)
  }
}

let layoutObserver = null
let layoutFrame = 0

const handleLayoutChange = () => {
  cancelAnimationFrame(layoutFrame)
  layoutFrame = requestAnimationFrame(syncLauncherPosition)
}

onMounted(() => {
  requestAnimationFrame(() => {
    if (!loadStoredLauncherPosition()) {
      setDefaultLauncherPosition()
    }
    if (open.value) positionPanel()
  })
  if (window.ResizeObserver && rootRef.value) {
    layoutObserver = new ResizeObserver(handleLayoutChange)
    layoutObserver.observe(rootRef.value)
  }
  window.addEventListener('resize', handleLayoutChange)
  window.visualViewport?.addEventListener('resize', handleLayoutChange)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(layoutFrame)
  layoutObserver?.disconnect()
  window.removeEventListener('resize', handleLayoutChange)
  window.visualViewport?.removeEventListener('resize', handleLayoutChange)
})
</script>

<style scoped>
.support-root {
  position: fixed;
  inset: 0;
  z-index: 1200;
  pointer-events: none;
}
.support-launcher {
  position: absolute;
  display: inline-flex;
  align-items: center;
  gap: 0;
  min-height: 54px;
  padding: 7px;
  border: 1px solid rgba(35, 105, 92, .16);
  border-radius: 18px;
  background: #fff;
  color: #183b34;
  box-shadow: 0 12px 30px rgba(32, 102, 90, .22);
  cursor: pointer;
  pointer-events: auto;
  touch-action: none;
  user-select: none;
  transition: gap .18s ease, padding .18s ease, box-shadow .18s ease, transform .18s ease;
}
.support-launcher:hover,
.support-launcher:focus-visible,
.support-launcher:active {
  gap: 9px;
  padding-right: 15px;
}
.support-launcher.dragging {
  box-shadow: 0 18px 38px rgba(32, 102, 90, .3);
  transform: scale(1.03);
}
.support-launcher .launcher-avatar {
  position: relative;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 14px;
  background: #e5f5f1;
  color: #216f62;
  font-size: 12px;
  font-weight: 800;
}
.support-launcher .launcher-avatar i {
  position: absolute;
  right: -1px;
  bottom: -1px;
  width: 10px;
  height: 10px;
  border: 2px solid #fff;
  border-radius: 50%;
  background: #49b876;
}
.support-launcher .launcher-copy {
  display: block;
  max-width: 0;
  overflow: hidden;
  opacity: 0;
  transform: translateX(-5px);
  text-align: left;
  white-space: nowrap;
  transition: max-width .18s ease, opacity .14s ease, transform .18s ease;
}
.support-launcher:hover .launcher-copy,
.support-launcher:focus-visible .launcher-copy,
.support-launcher:active .launcher-copy {
  max-width: 95px;
  opacity: 1;
  transform: translateX(0);
}
.support-launcher .launcher-copy b {
  display: block;
  color: #183b34;
  font-size: 13px;
  line-height: 1.1;
}
.support-launcher .launcher-copy small {
  display: block;
  margin-top: 3px;
  color: #71857f;
  font-size: 9px;
  font-weight: 600;
}
.support-panel {
  position: absolute;
  width: min(370px, calc(100vw - 24px));
  height: min(560px, calc(100dvh - 150px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(35, 105, 92, .16);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 22px 60px rgba(28, 72, 65, .22);
  pointer-events: auto;
}
.support-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 16px 13px;
  color: #fff;
  background: linear-gradient(135deg, #1f7568, #3b9886);
}
.support-head b {
  display: block;
  font-size: 16px;
}
.support-head span {
  display: block;
  margin-top: 3px;
  color: rgba(255,255,255,.75);
  font-size: 11px;
}
.support-head-actions {
  display: flex;
  align-items: center;
  gap: 7px;
}
.support-head button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  background: rgba(255,255,255,.14);
  color: #fff;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}
.support-head .support-ticket-entry {
  width: auto;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 30px;
}
.support-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  background: #f5faf8;
}
.support-message {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}
.support-message.user {
  flex-direction: row-reverse;
}
.message-avatar {
  width: 26px;
  height: 26px;
  flex: 0 0 26px;
  display: grid;
  place-items: center;
  border-radius: 9px;
  background: #dff3ed;
  color: #247568;
  font-size: 11px;
  font-weight: 800;
}
.support-message.user .message-avatar {
  background: #e9eef4;
  color: #526273;
}
.message-body {
  max-width: calc(100% - 34px);
}
.message-bubble {
  max-width: 100%;
  padding: 10px 12px;
  border-radius: 14px 14px 14px 4px;
  background: #fff;
  color: #29463f;
  font-size: 13px;
  line-height: 1.65;
  box-shadow: 0 4px 14px rgba(30, 80, 70, .06);
}
.message-bubble p {
  margin: 0 0 7px;
}
.message-bubble p:last-child,
.message-bubble ol:last-child,
.message-bubble ul:last-child {
  margin-bottom: 0;
}
.message-bubble ol,
.message-bubble ul {
  margin: 4px 0 9px;
  padding-left: 18px;
}
.message-bubble li {
  margin: 4px 0;
  padding-left: 2px;
}
.message-bubble ol li::marker {
  color: #2d8174;
  font-weight: 700;
}
.message-bubble ul li::marker {
  color: #57a392;
}
.support-message.user .message-bubble {
  border-radius: 14px 14px 4px 14px;
  background: #dff3ed;
  color: #185b50;
}
.support-sources {
  display: grid;
  gap: 5px;
  width: 92%;
  margin-top: 7px;
}
.support-sources span {
  color: #819791;
  font-size: 10px;
}
.support-sources button {
  overflow: hidden;
  padding: 7px 9px;
  border: 1px solid #d9ebe6;
  border-radius: 9px;
  background: #fff;
  color: #55746d;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  cursor: pointer;
}
.message-action {
  display: block;
  margin-top: 8px;
  padding: 7px 11px;
  border: 1px solid #b9ded4;
  border-radius: 10px;
  background: #f0faf7;
  color: #247568;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.support-prompts {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  padding: 8px 12px 0;
  background: #fff;
  scrollbar-width: none;
}
.support-prompts button {
  flex: 0 0 auto;
  border: 1px solid #d9ebe6;
  border-radius: 999px;
  background: #f7fcfb;
  color: #53766e;
  padding: 6px 9px;
  font-size: 11px;
  cursor: pointer;
}
.support-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 66px;
  gap: 8px;
  padding: 10px 12px 12px;
  background: #fff;
}
.support-composer textarea {
  min-height: 48px;
  max-height: 90px;
  resize: none;
  border: 1px solid #d8e9e5;
  border-radius: 12px;
  padding: 9px 10px;
  color: #24463f;
  font: inherit;
  font-size: 13px;
  outline: none;
}
.support-composer textarea:focus {
  border-color: #69b4a4;
}
.support-composer button {
  border: 0;
  border-radius: 12px;
  background: #26796c;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
.support-composer button:disabled {
  opacity: .45;
  cursor: default;
}
.support-sheet {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  z-index: 5;
  max-height: calc(100% - 68px);
  overflow-y: auto;
  padding: 14px;
  border: 1px solid #d9ebe6;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 -10px 36px rgba(28, 72, 65, .18);
}
.support-panel {
  position: relative;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #1e5f55;
}
.sheet-head b {
  font-size: 15px;
}
.sheet-head button {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: #f0f7f5;
  color: #426e65;
  font-size: 19px;
  cursor: pointer;
}
.support-sheet > p {
  margin: 7px 0 10px;
  color: #70857f;
  font-size: 12px;
  line-height: 1.55;
}
.support-sheet textarea,
.support-sheet input[type="text"],
.support-sheet > input {
  box-sizing: border-box;
  width: 100%;
  border: 1px solid #d8e9e5;
  border-radius: 11px;
  padding: 9px 10px;
  color: #24463f;
  font: inherit;
  font-size: 12.5px;
  outline: none;
}
.support-sheet textarea {
  resize: vertical;
  min-height: 72px;
}
.support-sheet > input {
  margin-top: 8px;
}
.support-sheet textarea:focus,
.support-sheet > input:focus {
  border-color: #69b4a4;
}
.support-upload-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 9px;
  color: #819791;
  font-size: 11px;
}
.support-upload-btn {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #cfe5df;
  border-radius: 9px;
  background: #f7fcfb;
  color: #34796d;
  font-size: 12px;
  cursor: pointer;
}
.support-upload-btn input {
  display: none;
}
.support-file-list {
  display: grid;
  gap: 5px;
  margin-top: 8px;
}
.support-file-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 8px;
  background: #f4f9f7;
  color: #526c65;
  font-size: 11px;
}
.support-file-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.support-file-list button {
  border: 0;
  background: transparent;
  color: #c65353;
  font-size: 11px;
  cursor: pointer;
}
.sheet-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
.sheet-actions button {
  min-width: 78px;
  height: 34px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.sheet-actions .ghost {
  border: 1px solid #d9e8e4;
  background: #fff;
  color: #5b7770;
}
.sheet-actions .primary {
  border: 0;
  background: #26796c;
  color: #fff;
}
.sheet-actions .primary:disabled {
  opacity: .55;
  cursor: default;
}
.ticket-sheet {
  max-height: calc(100% - 64px);
}
.ticket-empty {
  padding: 26px 0;
  color: #8b9d98;
  text-align: center;
  font-size: 12px;
}
.ticket-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  box-sizing: border-box;
  width: 100%;
  margin-top: 8px;
  padding: 10px;
  border: 1px solid #e2efeb;
  border-radius: 11px;
  background: #fbfffe;
  text-align: left;
  cursor: pointer;
}
.ticket-row > span {
  min-width: 0;
}
.ticket-row b,
.ticket-row small {
  display: block;
}
.ticket-row b {
  color: #245f55;
  font-size: 12px;
}
.ticket-row small {
  overflow: hidden;
  margin-top: 3px;
  color: #83938f;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}
.ticket-row em,
.ticket-meta span {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 999px;
  background: #edf4f2;
  color: #54766f;
  font-size: 10px;
  font-style: normal;
}
.ticket-row em.REPLIED,
.ticket-meta span.REPLIED { background: #e6f6ec; color: #2f8a52; }
.ticket-row em.PROCESSING,
.ticket-meta span.PROCESSING { background: #fff4dc; color: #ad7112; }
.ticket-row em.CLOSED,
.ticket-meta span.CLOSED { background: #f0f1f3; color: #7b828d; }
.ticket-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
  color: #265f56;
  font-size: 12px;
}
.ticket-thread {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow-y: auto;
  padding-right: 2px;
}
.ticket-message {
  padding: 8px 10px;
  border-radius: 10px;
  background: #f2f8f6;
  color: #33564f;
}
.ticket-message.ADMIN {
  background: #e8f5ef;
}
.ticket-message small {
  color: #81938e;
  font-size: 10px;
}
.ticket-message p {
  margin: 4px 0 0;
  white-space: pre-wrap;
  font-size: 12px;
  line-height: 1.55;
}
.ticket-attachments {
  display: grid;
  gap: 5px;
  margin-top: 10px;
}
.ticket-attachments > span {
  color: #81938e;
  font-size: 10px;
}
.ticket-attachments a {
  overflow: hidden;
  color: #24796d;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}
.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  min-width: 50px;
}
.loading-bubble i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #75a99e;
  animation: support-dot 1s infinite alternate;
}
.loading-bubble i:nth-child(2) { animation-delay: .2s; }
.loading-bubble i:nth-child(3) { animation-delay: .4s; }
.support-pop-enter-active,
.support-pop-leave-active { transition: opacity .18s ease, transform .18s ease; }
.support-pop-enter-from,
.support-pop-leave-to { opacity: 0; transform: translateY(8px) scale(.98); }
@keyframes support-dot {
  from { opacity: .3; transform: translateY(0); }
  to { opacity: 1; transform: translateY(-2px); }
}
@media (max-width: 620px) {
  .support-panel { height: min(520px, calc(100dvh - 142px)); }
}
</style>
