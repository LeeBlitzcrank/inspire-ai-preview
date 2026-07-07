<template>
  <div id="noti-root" class="noti-page">
    <div class="noti-top-nav">
      <div class="noti-back" @click="$router.back()">←</div>
      <div class="noti-title">消息通知</div>
      <div v-if="list.length > 0" class="noti-mark-all" @click="markAll">全部已读</div>
    </div>

    <div v-if="loading" class="noti-loading">
      <div v-for="n in 5" :key="n" class="s-line" style="margin-bottom:12px;height:14px"></div>
    </div>

    <div v-else-if="list.length === 0" class="noti-empty">
      <div class="noti-empty-icon">🔔</div>
      <div class="noti-empty-text">暂无消息通知</div>
    </div>

    <div v-else class="noti-list">
      <div v-for="item in list" :key="item.id" class="noti-item"
        :class="{ 'noti-unread': !item.isRead }"
        @click="handleClick(item)">
        <div class="noti-dot" v-if="!item.isRead"></div>
        <div class="noti-icon-wrap">
          <span class="noti-icon">{{ typeIcon(item.type) }}</span>
        </div>
        <div class="noti-body">
          <div class="noti-text">
            <strong>{{ item.actorName }}</strong>
            <span>{{ item.content }}</span>
          </div>
          <div v-if="item.targetTitle" class="noti-target">《{{ item.targetTitle }}》</div>
          <div class="noti-time">{{ formatTime(item.createTime) }}</div>
        </div>
      </div>
    </div>

    <div v-if="list.length > 0 && hasMore" class="noti-load-more" @click="loadMore">加载更多</div>
    <div v-if="!hasMore && list.length > 0" class="noti-no-more">-- 没有更多了 --</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markNotificationRead, markAllRead } from '@/api/inspire.js'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const hasMore = ref(true)

const loadMore = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await getNotifications({ page: page.value, size: 20 })
    if (res.data && res.data.length > 0) {
      list.value.push(...res.data)
      if (res.data.length < 20) hasMore.value = false
      page.value++
    } else {
      hasMore.value = false
    }
  } catch (e) { hasMore.value = false }
  finally { loading.value = false }
}

const typeIcon = (type) => {
  const map = { like: '⭐', collect: '🔖', comment: '💬', reply: '↩️', follow: '👤' }
  return map[type] || '📌'
}

const formatTime = (t) => {
  if (!t) return ''
  // 处理数组格式 [年,月,日,时,分,秒]
  var d
  if (Array.isArray(t)) {
    d = new Date(t[0], t[1] - 1, t[2], t[3] || 0, t[4] || 0, t[5] || 0)
  } else {
    d = new Date(t)
  }
  const now = new Date()
  const diff = now - d
  if (diff < 180000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const pad = (n) => String(n).padStart(2, '0')
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

const handleClick = async (item) => {
  // 标记已读
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = 1
  }
  // 跳转到灵感详情
  if (item.targetId && item.targetId !== 'null' && item.targetId !== '0') {
    router.push({ name: 'InspireDetail', params: { id: item.targetId } })
  }
}

const markAll = async () => {
  try {
    await markAllRead()
    list.value.forEach(item => item.isRead = 1)
  } catch (e) {}
}

onMounted(() => loadMore())
</script>

<style scoped>
.noti-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.noti-top-nav { display:flex; align-items:center; justify-content:space-between; padding:8px 16px 24px; }
.noti-back { font-size:22px; cursor:pointer; width:36px; height:36px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.noti-title { font-size:18px; font-weight:600; color:#1d1d1f; }
.noti-mark-all { font-size:13px; color:#409eff; cursor:pointer; padding:4px 8px; }
.noti-loading { padding:20px; }
.s-line { height:14px; border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }
.noti-empty { text-align:center; padding:80px 0; }
.noti-empty-icon { font-size:48px; margin-bottom:12px; }
.noti-empty-text { font-size:14px; color:#909399; }
.noti-list { display:flex; flex-direction:column; gap:1px; background:#f0f3f9; border-radius:16px; overflow:hidden; }
.noti-item { display:flex; align-items:flex-start; gap:10px; padding:14px 16px; background:#fff; cursor:pointer; transition:0.2s; position:relative; }
.noti-item:hover { background:#f5f8ff; }
.noti-unread { background:#f0f6ff; }
.noti-dot { position:absolute; left:6px; top:18px; width:6px; height:6px; border-radius:50%; background:#409eff; }
.noti-icon-wrap { width:36px; height:36px; border-radius:50%; background:#f4f7fd; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.noti-icon { font-size:16px; }
.noti-body { flex:1; min-width:0; }
.noti-text { font-size:14px; color:#1d1d1f; line-height:1.5; }
.noti-text strong { font-weight:600; margin-right:4px; }
.noti-target { font-size:13px; color:#409eff; margin-top:4px; }
.noti-time { font-size:12px; color:#c0c4cc; margin-top:4px; }
.noti-load-more { text-align:center; padding:16px; color:#409eff; font-size:14px; cursor:pointer; }
.noti-no-more { text-align:center; padding:16px; color:#c0c4cc; font-size:13px; }
</style>
