<template>
  <div id="detail-root" class="detail-page">
    <div id="detail-top-nav" class="top-nav">
      <div id="detail-back-btn" class="left-logo" @click="goBack">←</div>
      <div id="detail-title">灵感详情</div>
      <div class="placeholder"></div>
    </div>
    <div v-if="detail.id" id="detail-main-card" class="main-card">
      <div class="img-box"><img :src="detail.img || 'https://picsum.photos/id/102/300/160'" @error="imgFallback" /></div>
      <h2 class="title">{{ detail.title }}</h2>
      <p class="desc">{{ detail.content || detail.title }}</p>
      <div class="stat-row">
        <span>分类: {{ detail.tag }}</span>
        <span>浏览 {{ detail.viewCount }}</span>
        <span>热度 {{ detail.heat }}</span>
        <span>{{ detail.publishCity }}</span>
      </div>
      <div class="action-row">
        <el-button :type="liked ? 'primary' : ''" @click="handleLike">{{ liked ? '取消点赞' : '点赞' }} {{ detail.likeCount || '' }}</el-button>
        <el-button :type="collected ? 'primary' : ''" @click="handleCollect">{{ collected ? '取消收藏' : '收藏' }} {{ detail.collectCount || '' }}</el-button>
        <el-button @click="handleShare">分享</el-button>
      </div>
    </div>
    <div v-else-if="loadErr" class="loading-tip">{{ loadErr }}</div>
  <div v-else class="main-card">
    <div class="img-box skeleton-img"></div>
    <div class="s-line s-w-60" style="height:24px;margin-bottom:16px"></div>
    <div class="s-line s-w-90" style="margin-bottom:8px"></div>
    <div class="s-line s-w-80" style="margin-bottom:8px"></div>
    <div class="s-line s-w-70" style="margin-bottom:24px"></div>
    <div class="s-line s-w-40"></div>
  </div>
  </div>

  <!-- 评论区 -->
  <div v-if="detail.id" class="comment-section">
    <div class="comment-title">评论 ({{ totalComments }})</div>

    <!-- 评论输入 -->
    <div v-if="isLogin" class="comment-input-wrap">
      <div v-if="replyTo" class="reply-indicator">
        回复 @{{ replyTo.username }}
        <el-button text size="small" style="color:#999" @click="cancelReply">取消</el-button>
      </div>
      <el-input v-model="commentText" type="textarea" :rows="2" :placeholder="replyTo ? '回复 @' + replyTo.username + '...' : '说点什么...'" maxlength="500" show-word-limit></el-input>
      <div class="comment-input-actions">
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmitComment">{{ replyTo ? '回复' : '发表评论' }}</el-button>
      </div>
    </div>
    <div v-else class="comment-login-hint">
      <el-link type="primary" @click="$router.push('/login')">登录后发表评论</el-link>
    </div>

    <!-- 评论列表 -->
    <div v-if="commentsLoading" class="comment-loading">
      <div v-for="n in 3" :key="n" class="s-line s-w-80" style="margin-bottom:12px;height:12px"></div>
    </div>
    <div v-else-if="comments.length === 0" class="comment-empty">暂无评论，来说点什么吧</div>
    <div v-else class="comment-list">
      <div v-for="c in comments" :key="c.id" class="comment-item" :class="{ 'is-reply': c.parentId && c.parentId !== 0 }">
        <div class="comment-header">
          <span class="comment-avatar">{{ c.avatar || (c.username ? c.username[0] : '\U0001f464') }}</span>
          <span class="comment-username">{{ c.username }}</span>
          <span v-if="c.replyUsername && c.parentId !== 0" class="comment-reply-target">回复 @{{ c.replyUsername }}</span>
          <span class="comment-time">{{ formatTime(c.createTime) }}</span>
          <span class="comment-reply-btn" @click="startReply(c)">回复</span>
          <span class="comment-del" @click="handleDeleteComment(c.id)">删除</span>
        </div>
        <div class="comment-body">{{ c.content }}</div>
        <!-- 内联回复输入 -->
        <div v-if="replyTo && replyTo.id === c.id" class="inline-reply">
          <el-input v-model="commentText" type="textarea" :rows="1" placeholder="回复 @{{ c.username }}..." maxlength="500"></el-input>
          <div class="inline-reply-actions">
            <el-button size="small" text @click="cancelReply">取消</el-button>
            <el-button size="small" type="primary" :loading="submitting" @click="handleSubmitComment">回复</el-button>
          </div>
        </div>
        <!-- 子回复 -->
        <div v-if="c.children && c.children.length > 0" class="reply-list">
          <div v-for="child in c.children" :key="child.id" class="reply-item">
            <div class="comment-header">
              <span class="comment-avatar">{{ child.avatar || (child.username ? child.username[0] : '\U0001f464') }}</span>
              <span class="comment-username">{{ child.username }}</span>
              <span v-if="child.replyUsername" class="comment-reply-target">回复 @{{ child.replyUsername }}</span>
              <span class="comment-time">{{ formatTime(child.createTime) }}</span>
              <span class="comment-reply-btn" @click="startReply(child)">回复</span>
              <span class="comment-del" @click="handleDeleteComment(child.id)">删除</span>
            </div>
            <div class="comment-body">{{ child.content }}</div>
            <div v-if="replyTo && replyTo.id === child.id" class="inline-reply">
              <el-input v-model="commentText" type="textarea" :rows="1" placeholder="回复 @{{ child.username }}..." maxlength="500"></el-input>
              <div class="inline-reply-actions">
                <el-button size="small" text @click="cancelReply">取消</el-button>
                <el-button size="small" type="primary" :loading="submitting" @click="handleSubmitComment">回复</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="hasMoreComments" class="comment-more-wrap">
        <el-button text type="primary" :loading="commentsLoading" @click="loadMoreComments">加载更多</el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInspireDetail, collectInspire, uncollectInspire, likeInspire, unlikeInspire, getComments, createComment, deleteComment } from '@/api/inspire'
const route = useRoute(); const router = useRouter()
const detail = ref({}); const liked = ref(false); const collected = ref(false); const loadErr = ref('')

const loadData = async (id) => {
  try {
    const res = await getInspireDetail(id)
    if (res.code === 200) {
      detail.value = res.data || {}
      liked.value = !!detail.value.liked
      collected.value = !!detail.value.collected
    } else {
      loadErr.value = res.msg || '灵感不存在'
    }
  } catch (e) { loadErr.value = '灵感不存在或已删除' }
}
const handleLike = async () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); window.location.href = '/login'; return }
  try {
    if (liked.value) {
      const res = await unlikeInspire(detail.value.id)
      if (res.code === 200) { liked.value = false; detail.value.likeCount-- }
    } else {
      const res = await likeInspire(detail.value.id)
      if (res.code === 200) { liked.value = true; detail.value.likeCount++ }
    }
  } catch (e) {}
}
const handleCollect = async () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); window.location.href = '/login'; return }
  try {
    if (collected.value) {
      const res = await uncollectInspire(detail.value.id)
      if (res.code === 200) { collected.value = false; detail.value.collectCount-- }
    } else {
      const res = await collectInspire(detail.value.id)
      if (res.code === 200) { collected.value = true; detail.value.collectCount++ }
    }
  } catch (e) {}
}
watch(() => route.params.id, (id) => { if (id) loadData(id) }, { immediate: true })

const handleShare = () => {
  const url = window.location.href
  const ta = document.createElement('textarea')
  ta.value = url; ta.style.position = 'fixed'; ta.style.opacity = '0'
  document.body.appendChild(ta); ta.select()
  try { document.execCommand('copy'); ElMessage.success('链接已复制') }
  catch (e) { ElMessage.warning('复制失败') }
  document.body.removeChild(ta)
}
const imgFallback = (e) => { e.target.src = 'https://picsum.photos/id/102/300/160' }

const goBack = () => {
  // Use router.back() to preserve navigation context
  router.back()
}

// 评论
const comments = ref([])
const totalComments = ref(0)
const commentText = ref('')
const replyTo = ref(null)
const submitting = ref(false)
const commentsLoading = ref(false)
const commentPage = ref(1)
const hasMoreComments = ref(false)
const isLogin = computed(() => !!localStorage.getItem('isLogin'))

const loadComments = async (reset = false) => {
  if (reset) { commentPage.value = 1; comments.value = [] }
  commentsLoading.value = true
  try {
    const res = await getComments(detail.value.id, { page: commentPage.value, size: 10 })
    if (res.code === 200 && res.data) {
      const data = res.data
      const allRecords = data.records || []
      const topLevel = allRecords.filter(c => !c.parentId || c.parentId === '0' || c.parentId === 0)
      const childMap = {}
      allRecords.forEach(c => {
        if (c.parentId && c.parentId !== '0' && c.parentId !== 0) {
          if (!childMap[c.parentId]) childMap[c.parentId] = []
          childMap[c.parentId].push(c)
        }
      })
      topLevel.forEach(c => { c.children = childMap[c.id] || [] })
      if (reset) { comments.value = topLevel }
      else { comments.value = [...comments.value, ...topLevel] }
      totalComments.value = data.total || 0
      hasMoreComments.value = commentPage.value * 10 < (data.total || 0)
    }
  } catch (e) {}
  finally { commentsLoading.value = false }
}

const handleSubmitComment = async () => {
  if (!commentText.value.trim()) return
  const username = localStorage.getItem('userAccount') || '用户'
  const avatar = localStorage.getItem('userAvatar') || ''
  submitting.value = true
  const data = { content: commentText.value, username, avatar }
  if (replyTo.value) {
    data.parentId = replyTo.value.parentId && String(replyTo.value.parentId) !== '0' ? String(replyTo.value.parentId) : String(replyTo.value.id)
    data.replyUserId = String(replyTo.value.userId || '')
    data.replyUsername = replyTo.value.username
  }
  try {
    const res = await createComment(detail.value.id, data)
    if (res.code === 200) {
      commentText.value = ''
      replyTo.value = null
      totalComments.value++
      loadComments(true)
    }
  } catch (e) {}
  finally { submitting.value = false }
}

const startReply = (c) => {
  replyTo.value = c
  commentText.value = ''
}

const cancelReply = () => {
  replyTo.value = null
  commentText.value = ''
}

const handleDeleteComment = async (commentId) => {
  try {
    const res = await deleteComment(detail.value.id, commentId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadComments(true) // 直接从数据库重新加载，避免本地操作不同步
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

const loadMoreComments = () => {
  commentPage.value++
  loadComments()
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

// 详情加载后自动加载评论
watch(() => detail.value.id, (id) => {
  if (id) loadComments(true)
})

</script>
<style scoped>
.detail-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; align-items:center; justify-content:space-between; padding:8px 16px 24px; }
.left-logo { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); cursor:pointer; font-size:20px; }
#detail-title { font-size:18px; font-weight:600; }
.placeholder { width:40px; }
.main-card { background:#fff; border-radius:20px; padding:20px; margin-bottom:30px; }
.img-box { width:100%; aspect-ratio:16/10; border-radius:16px; overflow:hidden; margin-bottom:16px; background:#f5f5f5; display:flex; align-items:center; justify-content:center; }
.img-box img { max-width:100%; max-height:100%; object-fit:contain; }
.title { font-size:20px; margin:0 0 12px; color:#1d1d1f; }
.desc { font-size:15px; color:#666; line-height:1.6; margin-bottom:16px; }
.stat-row { display:flex; gap:20px; font-size:14px; color:#888; margin-bottom:20px; flex-wrap:wrap; }
.action-row { display:flex; gap:16px; }
.loading-tip { text-align:center; padding:60px 0; color:#999; font-size:15px; }
.skeleton-img { width:100%; aspect-ratio:16/10; border-radius:16px; margin-bottom:16px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; }
.s-w-60 { width:60%; } .s-w-70 { width:70%; } .s-w-80 { width:80%; } .s-w-90 { width:90%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }

/* 评论区 */
.comment-section { background:#fff; border-radius:20px; padding:20px; margin-bottom:30px; }
.comment-title { font-size:16px; font-weight:500; color:#1d1d1f; margin-bottom:16px; }
.comment-input-wrap { margin-bottom:20px; }
.comment-input-actions { display:flex; justify-content:flex-end; margin-top:8px; }
.comment-login-hint { text-align:center; padding:16px; font-size:14px; color:#909399; }
.comment-loading { padding:16px; }
.comment-empty { text-align:center; color:#999; font-size:14px; padding:24px 0; }
.comment-list { display:flex; flex-direction:column; gap:14px; }
.comment-item { border-bottom:1px solid #f5f5f5; padding-bottom:12px; }
.comment-item.is-reply { margin-left:16px; border-left:2px solid #ebeef5; padding-left:12px; }
.comment-item:last-child { border-bottom:none; }
.comment-header { display:flex; align-items:center; gap:8px; margin-bottom:6px; flex-wrap:wrap; }
.comment-avatar { width:24px; height:24px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; display:inline-flex; align-items:center; justify-content:center; font-size:12px; flex-shrink:0; }
.comment-username { font-size:13px; font-weight:500; color:#333; }
.comment-reply-target { font-size:12px; color:#909399; }
.comment-reply-target::before { content:"@"; }
.comment-time { font-size:12px; color:#aaa; }
.comment-reply-btn { font-size:12px; color:#409eff; cursor:pointer; }
.comment-del { font-size:12px; color:#f56c6c; cursor:pointer; margin-left:auto; }
.comment-body { font-size:14px; color:#555; line-height:1.5; }
.comment-more-wrap { text-align:center; padding-top:8px; }
.reply-indicator { font-size:13px; color:#909399; padding:4px 0 8px; display:flex; align-items:center; gap:8px; }
.inline-reply { margin-top:8px; padding:8px; background:#f8f9fc; border-radius:8px; }
.inline-reply-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:4px; }
.reply-list { margin-top:10px; margin-left:12px; border-left:2px solid #ebeef5; padding-left:12px; }
.reply-item { padding:8px 0; border-bottom:1px solid #f5f5f5; }
.reply-item:last-child { border-bottom:none; }
</style>
