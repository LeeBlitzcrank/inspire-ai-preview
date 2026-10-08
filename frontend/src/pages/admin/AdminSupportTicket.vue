<!--
  文件：frontend/src/pages/admin/AdminSupportTicket.vue
  所属模块：后台管理前端页面
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="ticket-page">
    <div class="head">
      <div>
        <h3>人工客服工单</h3>
        <p>处理用户在 AI 客服中提交的转人工请求、报错截图和视频。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div class="filters">
      <el-select v-model="status" clearable placeholder="全部状态" style="width:150px" @change="load">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已回复" value="REPLIED" />
        <el-option label="已关闭" value="CLOSED" />
      </el-select>
      <span>共 {{ total }} 条</span>
    </div>

    <el-table v-loading="loading" :data="list" style="width:100%">
      <el-table-column prop="ticketNo" label="工单号" min-width="190" />
      <el-table-column prop="issue" label="问题" min-width="260" show-overflow-tooltip />
      <el-table-column prop="contactValue" label="联系方式" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span class="status" :class="row.status">{{ statusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="lastReply" label="最近回复" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="工单处理" width="720px" append-to-body>
      <div v-if="detail.ticket" class="detail">
        <div class="detail-head">
          <div>
            <b>{{ detail.ticket.ticketNo }}</b>
            <span class="status" :class="detail.ticket.status">{{ statusText(detail.ticket.status) }}</span>
          </div>
          <span>{{ detail.ticket.createTime }}</span>
        </div>
        <dl>
          <dt>问题</dt>
          <dd>{{ detail.ticket.issue }}</dd>
          <dt>联系方式</dt>
          <dd>{{ detail.ticket.contactValue || '未填写' }}</dd>
        </dl>

        <div v-if="detail.attachments?.length" class="attachments">
          <b>报错附件</b>
          <div class="attachment-grid">
            <a
              v-for="file in detail.attachments"
              :key="file.id"
              :href="file.fileUrl"
              target="_blank"
              rel="noreferrer"
            >
              <img v-if="file.fileType === 'image'" :src="file.thumbUrl || file.fileUrl" alt="" />
              <video v-else :src="file.fileUrl" :poster="file.thumbUrl || undefined" controls preload="metadata"></video>
              <span>{{ file.originalName || '附件' }}</span>
            </a>
          </div>
        </div>

        <div class="thread">
          <div
            v-for="message in detail.messages || []"
            :key="message.id"
            class="message"
            :class="message.senderType"
          >
            <small>{{ message.senderType === 'ADMIN' ? '人工客服' : '用户' }} · {{ message.createTime }}</small>
            <p>{{ message.content }}</p>
          </div>
        </div>

        <el-input
          v-model="reply"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="输入给用户的回复"
          :disabled="detail.ticket.status === 'CLOSED'"
        />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">取消</el-button>
        <el-button
          :disabled="detail.ticket?.status === 'CLOSED'"
          :loading="acting"
          @click="claim"
        >认领</el-button>
        <el-button
          type="danger"
          :disabled="detail.ticket?.status === 'CLOSED'"
          :loading="acting"
          @click="closeTicket"
        >关闭</el-button>
        <el-button
          type="primary"
          :disabled="detail.ticket?.status === 'CLOSED'"
          :loading="acting"
          @click="replyTicket"
        >回复用户</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
  adminSupportTicketClaim,
  adminSupportTicketClose,
  adminSupportTicketDetail,
  adminSupportTicketList,
  adminSupportTicketReply
} from '@/api/support.js'

const loading = ref(false)
const acting = ref(false)
const status = ref('')
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref({ticket: null, messages: [], attachments: []})
const reply = ref('')

const statusText = (value) => ({
  PENDING: '待处理',
  PROCESSING: '处理中',
  REPLIED: '已回复',
  CLOSED: '已关闭'
}[value] || value)

const load = async () => {
  loading.value = true
  try {
    const res = await adminSupportTicketList({status: status.value || undefined, page: 1, size: 100})
    if (res.code !== 200) throw new Error(res.msg || '加载失败')
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '工单加载失败')
  } finally {
    loading.value = false
  }
}

const loadDetail = async (id) => {
  const res = await adminSupportTicketDetail(id)
  if (res.code !== 200) throw new Error(res.msg || '详情加载失败')
  detail.value = res.data || {ticket: null, messages: [], attachments: []}
}

const openDetail = async (row) => {
  detailVisible.value = true
  reply.value = ''
  try {
    await loadDetail(row.id)
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '详情加载失败')
  }
}

const runAction = async (action, successMessage) => {
  const id = detail.value.ticket?.id
  if (!id) return
  acting.value = true
  try {
    const res = await action(id)
    if (res.code !== 200) throw new Error(res.msg || '操作失败')
    ElMessage.success(successMessage)
    await loadDetail(id)
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

const claim = () => runAction(adminSupportTicketClaim, '已认领')
const closeTicket = () => runAction(adminSupportTicketClose, '工单已关闭')

const replyTicket = async () => {
  const content = reply.value.trim()
  if (!content) return ElMessage.warning('请填写回复内容')
  const id = detail.value.ticket?.id
  if (!id) return
  acting.value = true
  try {
    const res = await adminSupportTicketReply(id, content)
    if (res.code !== 200) throw new Error(res.msg || '回复失败')
    ElMessage.success('回复成功')
    reply.value = ''
    await loadDetail(id)
    await load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '回复失败')
  } finally {
    acting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ticket-page { padding: 4px 2px; }
.head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:18px; }
.head h3 { margin:0; }
.head p { margin:6px 0 0; color:#8a94a6; font-size:12.5px; }
.filters { display:flex; align-items:center; gap:12px; margin-bottom:14px; color:#8a94a6; font-size:12px; }
.status { display:inline-block; padding:3px 8px; border-radius:999px; background:#eef3f2; color:#5e716d; font-size:11px; }
.status.PROCESSING { background:#fff4dc; color:#ad7112; }
.status.REPLIED { background:#e6f6ec; color:#2f8a52; }
.status.CLOSED { background:#f0f1f3; color:#7b828d; }
.detail-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; color:#87918f; font-size:12px; }
.detail-head > div { display:flex; align-items:center; gap:10px; }
.detail-head b { color:#1f4f48; font-size:15px; }
dl { display:grid; grid-template-columns:78px minmax(0,1fr); gap:9px 12px; margin:0 0 16px; font-size:13px; }
dt { color:#8a94a6; }
dd { margin:0; color:#263b37; white-space:pre-wrap; }
.attachments { margin-bottom:15px; }
.attachments > b { display:block; margin-bottom:8px; font-size:13px; }
.attachment-grid { display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:10px; }
.attachment-grid a { min-width:0; color:#27786d; text-decoration:none; font-size:11px; }
.attachment-grid img,
.attachment-grid video { display:block; width:100%; height:110px; object-fit:cover; border-radius:10px; background:#eef3f2; }
.attachment-grid span { display:block; overflow:hidden; margin-top:4px; text-overflow:ellipsis; white-space:nowrap; }
.thread { display:grid; gap:9px; max-height:280px; overflow-y:auto; margin-bottom:14px; padding:10px; border-radius:12px; background:#f7faf9; }
.message { padding:9px 10px; border-radius:10px; background:#fff; }
.message.ADMIN { background:#e8f5ef; }
.message small { color:#84918e; font-size:10px; }
.message p { margin:4px 0 0; color:#2d4842; white-space:pre-wrap; font-size:12.5px; line-height:1.6; }
</style>
