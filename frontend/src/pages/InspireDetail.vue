<template>
  <div id="detail-root" class="detail-page">
    <div id="detail-top-nav" class="top-nav">
      <div id="detail-back-btn" class="left-logo" @click="goBack">←</div>
      <div id="detail-title">灵感详情</div>
      <div class="placeholder"></div>
    </div>
    <div v-if="detail.id" id="detail-main-card" class="main-card">
      <div v-if="!images.length" class="img-box"><img loading="lazy" :src="detail.img || 'https://picsum.photos/id/102/300/160'" @error="imgFallback" /></div>
      <div v-else class="carousel-wrap">
        <img loading="lazy" :src="images[curImage]" class="carousel-img" @error="imgFallback" />
        <div v-if="images.length > 1" class="carousel-nav">
          <span class="carousel-arrow left" @click="prevImage">‹</span>
          <div class="carousel-dots"><span v-for="(_,i) in images" :key="i" class="dot" :class="{active:i===curImage}" @click="curImage=i"></span></div>
          <span class="carousel-arrow right" @click="nextImage">›</span>
        </div>
      </div>
      <div class="meta-row">
        <span class="meta-tag">{{ detail.tag }}</span>
        <span class="meta-user">👤 {{ detail.nickname || detail.username || '用户' }}</span>
        <span v-if="isLogin && detail.userId && String(detail.userId) !== currentUserId" class="meta-follow" @click="handleToggleFollow">{{ isFollowing ? '✅ 已关注' : '➕ 关注' }}</span><span v-if="isLogin && detail.userId && String(detail.userId) !== currentUserId" class="meta-msg" @click="goChat(detail.userId)" style="font-size:12px;color:#6366f1;cursor:pointer;padding:2px 10px;border:1px solid #6366f1;border-radius:12px;margin-left:6px;">💬 私信</span>
      </div>
      <h1 class="title">{{ detail.title }}</h1>
      <div v-if="detail.content" class="desc-wrap md-content" v-html="renderedContent"></div>
      <p v-else class="desc-p">{{ detail.title }}</p>
      <div class="stat-row">
        <span>👁 浏览 {{ detail.viewCount || 0 }}</span>
        <span>⭐ 点赞 {{ detail.likeCount || 0 }}</span>
        <span>🔖 收藏 {{ detail.collectCount || 0 }}</span>
        <span>🔥 热度 {{ detail.heat || 0 }}</span>
      </div>
      <div class="action-row">
        <div class="action-btn" :class="{ active: liked }" @click="handleLike">⭐ {{ detail.likeCount || 0 }}</div>
        <div class="action-btn" :class="{ active: collected }" @click="toggleCollect">🔖 {{ detail.collectCount || 0 }}</div>
        <div v-if="isLogin && String(detail.userId) === currentUserId" class="action-btn" @click="goEdit">✏️ 编辑</div>
        <div class="action-btn" @click="handleShare">🔗 {{ detail.shareCount || 0 }}</div>
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

  <!-- 分享弹窗 -->
  <div v-if="showSharePanel" class="overlay" @click.self="showSharePanel = false">
    <div class="share-panel">
      <div class="share-header">分享灵感</div>
      <div class="share-count">已分享 {{ detail.shareCount || 0 }} 次</div>
      <div class="share-divider"></div>
      <div class="share-option" @click="nativeShare">
        <span class="share-icon">📱</span>
        <span class="share-label">分享到其他应用</span>
      </div>
      <div class="share-option" @click="copyShareLink">
        <span class="share-icon">🔗</span>
        <span class="share-label">复制链接</span>
      </div>
      <div class="share-cancel" @click="showSharePanel = false">取消</div>
    </div>
  </div>

  <!-- 版本预览弹窗 -->
  <div v-if="versionPreview" class="overlay" @click.self="versionPreview = null">
    <div class="version-preview">
      <div class="vp-header">
        <span class="vp-title">📜 v{{ versionPreview.versionNumber || '?' }} {{ versionPreview.title }}</span>
        <span class="vp-close" @click="versionPreview = null">✕</span>
      </div>
      <div class="vp-time">{{ formatTime(versionPreview.createTime) }}</div>
      <div class="vp-content">{{ versionPreview.content }}</div>
    </div>
  </div>

  <!-- 版本历史 -->
  <div v-if="versions.length > 0" class="version-section">
    <div class="version-title">📜 版本历史</div>
    <div class="version-list">
      <div v-for="(v, i) in versions" :key="v.id" class="version-item" @click="viewVersion(v.id)">
        <span class="ver-num">v{{ versions.length - i }}</span>
        <span class="ver-time">{{ formatTime(v.createTime) }}</span>
        <span class="ver-title">{{ v.title }}</span>
      </div>
    </div>
  </div>

  <!-- 评论区 -->
  <div v-if="detail.id" class="comment-section">
    <div class="comment-title">评论 ({{ totalComments }})</div>



    <!-- 评论列表 -->
    <div v-if="commentsLoading" class="comment-loading">
      <div v-for="n in 3" :key="n" class="s-line s-w-80" style="margin-bottom:12px;height:12px"></div>
    </div>
    <div v-else-if="comments.length === 0" class="comment-empty">暂无评论，来说点什么吧</div>
    <div v-else class="comment-list">
      <div v-for="c in comments" :key="c.id" class="comment-item" :class="{ 'is-reply': c.parentId && c.parentId !== 0 }" @click="handleCommentClick($event, c)">
        <div class="comment-header">
          <span class="comment-avatar">{{ c.avatar || (c.username ? c.username[0] : '\U0001f464') }}</span>
          <span class="comment-username">{{ c.nickname || c.username }}</span>
          <span v-if="c.replyUsername && c.parentId !== 0" class="comment-reply-target">回复 @{{ c.replyUsername }}</span>
          <span class="comment-time">{{ formatTime(c.createTime) }}</span>
          <span class="comment-reply-btn" data-reply="true" @click.stop="startReply(c)">回复</span>
          <span v-if="String(c.userId) === currentUserId" class="comment-del" @click="handleDeleteComment(c.id)">删除</span>
        </div>
        <div class="comment-body">{{ c.content }}</div>

        <!-- 子回复 -->
        <div v-if="c.children && c.children.length > 0" class="reply-list">
          <div v-for="child in c.children" :key="child.id" class="reply-item" @click="handleCommentClick($event, child)">
            <div class="comment-header">
              <span class="comment-avatar">{{ child.avatar || (child.username ? child.username[0] : '\U0001f464') }}</span>
              <span class="comment-username">{{ child.nickname || child.username }}</span>
              <span v-if="child.replyUsername" class="comment-reply-target">回复 @{{ child.replyUsername }}</span>
              <span class="comment-time">{{ formatTime(child.createTime) }}</span>
              <span class="comment-reply-btn" data-reply="true" @click.stop="startReply(child)">回复</span>
              <span v-if="String(child.userId) === currentUserId" class="comment-del" @click="handleDeleteComment(child.id)">删除</span>
            </div>
            <div class="comment-body">{{ child.content }}</div>

          </div>
        </div>
      </div>
      <div v-if="hasMoreComments" class="comment-more-wrap">
        <el-button text type="primary" :loading="commentsLoading" @click="loadMoreComments">加载更多</el-button>
     </div>
    </div>
     <!-- 全局回复栏 -->
    <div v-if="replyToId" class="global-reply">
      <div class="reply-indicator">回复 @{{ replyToName }} <el-button text size="small" style="color:#999" @click="cancelReply">取消</el-button></div>
      <el-input v-model="commentText" type="textarea" :rows="2" :placeholder="'回复 @' + replyToName + '...'" maxlength="500" show-word-limit></el-input>
      <div class="comment-input-actions">
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmitComment">回复</el-button>
      </div>
    </div>

    <!-- 评论输入 -->
    <div v-if="isLogin && !replyToId" class="comment-input-wrap">
      <el-input v-model="commentText" type="textarea" :rows="2" placeholder="说点什么..." maxlength="500" show-word-limit></el-input>
      <div class="comment-input-actions">
        <el-button type="primary" size="small" :loading="submitting" @click="handleSubmitComment">发表评论</el-button>
      </div>
    </div>
    <div v-else-if="!isLogin" class="comment-login-hint">
      <el-link type="primary" @click="$router.push('/login')">登录后发表评论</el-link>
    </div>

    <!-- 收藏文件夹选择器 -->
    <el-dialog v-model="folderDialogVisible" title="选择收藏夹" width="320px">
      <div style="display:flex;flex-wrap:wrap;gap:10px;margin-bottom:16px;">
        <div v-for="f in collectFolders" :key="f.id" class="folder-option"
             :class="{selected: selectedFolder === f.id}"
             @click="selectedFolder = f.id"
             style="flex:1;min-width:100px;padding:12px;border-radius:12px;border:2px solid #e4e7ed;text-align:center;cursor:pointer;">
          <div style="font-size:24px;">{{ f.icon || '📁' }}</div>
          <div style="font-size:13px;margin-top:4px;color:#1d1d1f;">{{ f.name }}</div>
        </div>
      </div>
      <div style="display:flex;gap:8px;">
        <el-input v-model="newFolderName" placeholder="新建文件夹" size="small" style="flex:1;" />
        <el-button size="small" @click="createAndUseFolder">新建</el-button>
      </div>
      <div style="margin-top:16px;text-align:right;">
        <el-button @click="folderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCollectToFolder">收藏到此</el-button>
      </div>
    </el-dialog>

  </div>
</template>
<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInspireDetail, shareInspire, collectInspire, uncollectInspire, likeInspire, unlikeInspire, getComments, createComment, deleteComment, followUser, unfollowUser, getFollowing, getInspireVersions, getInspireVersionDetail, getCollectFolders, createCollectFolder, collectToFolder } from '@/api/inspire.js'
import { startConversation } from '@/api/message.js'
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
  } catch (e) { console.error(e) }
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
  } catch (e) { console.error(e) }
}
watch(() => route.params.id, (id) => { if (id) loadData(id) }, { immediate: true })

const showSharePanel = ref(false)
const handleShare = async () => {
  showSharePanel.value = true
  // 记录分享行为
  if (detail.value.id) {
    try {
      await shareInspire(detail.value.id)
      detail.value.shareCount = (detail.value.shareCount || 0) + 1
    } catch (e) { console.error(e) }
  }
}
const copyShareLink = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = url; ta.style.position = 'fixed'; ta.style.opacity = '0'
    document.body.appendChild(ta); ta.select()
    try { document.execCommand('copy'); ElMessage.success('链接已复制') }
    catch (e) { ElMessage.warning('复制失败') }
    document.body.removeChild(ta)
  })
  showSharePanel.value = false
}
const nativeShare = () => {
  const url = window.location.href
  if (navigator.share) {
    navigator.share({ title: document.title, url: url }).catch(e => console.error(e))
  } else {
    copyShareLink()
  }
  showSharePanel.value = false
}
const images = computed(() => {
  if (detail.value.images && detail.value.images.length > 0) return detail.value.images
  if (detail.value.img) return [detail.value.img]
  return []
})
const curImage = ref(0)
const prevImage = () => { curImage.value = (curImage.value - 1 + images.value.length) % images.value.length }
const nextImage = () => { curImage.value = (curImage.value + 1) % images.value.length }
const goEdit = () => { router.push('/edit/' + detail.value.id) }
const imgFallback = (e) => { e.target.src = 'https://picsum.photos/id/102/300/160' }

const handleToggleFollow = async () => {
  if (!isLogin.value) return
  try {
    let res
    if (isFollowing.value) {
      res = await unfollowUser(detail.value.userId)
    } else {
      res = await followUser(detail.value.userId)
    }
    if (res.code === 200) isFollowing.value = !isFollowing.value
    else ElMessage.error(res.msg || '操作失败')
  } catch (e) { console.error(e) }
}

const goBack = () => {
  router.back()
}

// 评论
const comments = ref([])
const totalComments = ref(0)
const commentText = ref('')
const replyToId = ref('')
const replyToName = ref('')
const submitting = ref(false)
const commentsLoading = ref(false)
const commentPage = ref(1)
const hasMoreComments = ref(false)
const isLogin = computed(() => !!localStorage.getItem('isLogin'))
const currentUserId = computed(() => {
  const t = localStorage.getItem('token')
  if (!t) return ''
  try { return JSON.parse(atob(t.split('.')[1])).sub || '' }
  catch (e) { return '' }
})
const isFollowing = ref(false)
const _unused_paragraphs = computed(() => {
  const text = detail.value.content
  if (!text) return []
  return text.split('\n').filter(p => p.trim())
})

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
  } catch (e) { console.error(e) }
  finally { 
    commentsLoading.value = false 
    console.log('[COMMENTS] 加载完成, 数量:', comments.value.length)
  }
}

const handleSubmitComment = async () => {
  if (!commentText.value.trim()) return
  const username = localStorage.getItem('userAccount') || '用户'
  const avatar = localStorage.getItem('userAvatar') || ''
  submitting.value = true
  const data = { content: commentText.value, username, avatar }
  if (replyToId.value) {
    // Reply: find the target comment in the list to get its parentId
    const target = comments.value.find(c => String(c.id) === replyToId.value) 
                  || comments.value.flatMap(c => c.children || []).find(child => String(child.id) === replyToId.value)
    data.parentId = target && target.parentId && String(target.parentId) !== '0' ? String(target.parentId) : replyToId.value
    data.replyUserId = replyToId.value
    data.replyUsername = replyToName.value
  }
  try {
    const res = await createComment(detail.value.id, data)
    if (res.code === 200) {
      commentText.value = ''
      replyToId.value = ''
      replyToName.value = ''
      totalComments.value++
      loadComments(true)
    }
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const startReply = (c) => {
  console.log('[回复] 点击回复 c.id=', c.id, 'c.username=', c.username)
  replyToId.value = String(c.id)
  replyToName.value = (c.nickname || c.username) || ''
  commentText.value = ''
  console.log('[回复] 已设置 replyToId=', replyToId.value)
}

const cancelReply = () => {
  console.log('[回复] 取消回复')
  replyToId.value = ''
  replyToName.value = ''
  commentText.value = ''
}

const handleCommentClick = (e, item) => {
  if (e.target.dataset.reply === 'true' || e.target.closest('[data-reply="true"]')) {
    startReply(item)
  }
}

const checkFollowing = async () => {
  if (detail.value.userId && String(detail.value.userId) !== currentUserId.value) {
    try {
      const res = await getFollowing()
      isFollowing.value = res.data?.some(u => String(u.id) === String(detail.value.userId)) || false
    } catch(e) {}
  }
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
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + day
}

const loadVersions = async (id) => {
  try {
    const res = await getInspireVersions(id)
    versions.value = res.data || []
    console.log('[VERSIONS] 加载完成, 数量:', versions.value.length, id)
  } catch (e) {
    versions.value = []
    console.warn('[VERSIONS] 加载失败:', e)
  }
}
const versions = ref([])
const versionPreview = ref(null)
const viewVersion = async (versionId) => {
  console.log('[VERSION] 点击版本:', versionId)
  try {
    const res = await getInspireVersionDetail(detail.value.id, versionId)
    console.log('[VERSION] 详情数据:', res.data)
    versionPreview.value = res.data
  } catch (e) { console.warn('[VERSION] 失败:', e) }
}

// 详情加载后自动加载评论、关注状态、版本历史
watch(() => detail.value.id, (id) => {
  if (id) { loadComments(true); checkFollowing(); loadVersions(id) }
})

// 调试：回复状态
watch(replyToId, (v) => { console.log('[回复] watch replyToId:', v); })
watch(replyToName, (v) => { console.log('[回复] watch replyToName:', v); })


const folderDialogVisible = ref(false)
const collectFolders = ref([])
const selectedFolder = ref(null)
const newFolderName = ref('')
const loadCollectFolders = async () => {
  try { const res = await getCollectFolders(); collectFolders.value = res.data || [] }
  catch (e) { collectFolders.value = [] }
}
const createAndUseFolder = async () => {
  if (!newFolderName.value.trim()) return
  try { const res = await createCollectFolder(newFolderName.value.trim()); collectFolders.value.push(res.data); selectedFolder.value = res.data.id; newFolderName.value = ''; ElMessage.success('创建成功') }
  catch (e) { ElMessage.error('创建失败') }
}
const confirmCollectToFolder = async () => {
  try { await collectToFolder(detail.value.id, selectedFolder.value); ElMessage.success('已收藏到文件夹'); folderDialogVisible.value = false; detail.value.collected = true }
  catch (e) { ElMessage.error('收藏失败') }
}
// 覆盖收藏按钮行为
const toggleCollect = async () => {
  if (!isLogin.value) { ElMessage.warning('请先登录'); return }
  if (detail.value.collected) {
    try { await uncollectInspire(detail.value.id); detail.value.collected = false; ElMessage.success('已取消收藏') }
    catch (e) { ElMessage.error('操作失败') }
  } else {
    await loadCollectFolders()
    if (collectFolders.value.length === 0) {
      // 没有文件夹时自动创建默认文件夹
      try {
        await createCollectFolder('默认收藏')
        await loadCollectFolders()
      } catch (e) { console.error(e) }
    }
    if (collectFolders.value.length > 0) {
      selectedFolder.value = collectFolders.value[0].id
      folderDialogVisible.value = true
    } else {
      try { await collectInspire(detail.value.id); detail.value.collected = true; ElMessage.success('收藏成功') }
      catch (e) { ElMessage.error('收藏失败') }
    }
  }
}


const goChat = async (userId) => {
  try {
    const res = await startConversation(userId)
    if (res.data && res.data.id) {
      router.push('/messages?convId=' + res.data.id)
    }
  } catch (e) { console.error(e) }
}

</script>
<style scoped>
.detail-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 24px; }
.top-nav { display:flex; align-items:center; justify-content:space-between; padding:8px 16px 24px; }
.left-logo { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); cursor:pointer; font-size:20px; }
#detail-title { font-size:18px; font-weight:600; }
.placeholder { width:40px; }
.main-card { background:#fff; border-radius:20px; padding:20px; margin-bottom:0px; box-shadow:0 1px 8px rgba(0,0,0,0.04); }
.img-box { width:100%; border-radius:14px; overflow:hidden; margin-bottom:16px; background:#f5f5f5; display:flex; align-items:center; justify-content:center; }
.img-box img { max-width:100%; max-height:70vh; height:auto; display:block; border-radius:14px; }
.meta-row { display:flex; align-items:center; gap:10px; margin-bottom:10px; }
.meta-tag { font-size:12px; color:#fff; background:#409eff; border-radius:4px; padding:2px 8px; font-weight:400; }
.meta-user { font-size:13px; color:#909399; }
.meta-follow { font-size:12px; color:#409eff; cursor:pointer; margin-left:auto; padding:2px 10px; border:1px solid #409eff; border-radius:12px; transition:0.2s; }
.meta-follow:hover { background:#409eff; color:#fff; }
.title { font-size:22px; font-weight:600; margin:0 0 12px; color:#1d1d1f; line-height:1.4; }
.desc-wrap { padding:16px 0 12px; border-top:1px solid #f0f0f0; margin-top:4px; }
.desc-p { font-size:16px; color:#2c2c2e; line-height:1.75; margin:0 0 10px; text-indent:2em; letter-spacing:0.3px; }
.stat-row { display:flex; gap:24px; font-size:13px; color:#909399; margin-bottom:20px; flex-wrap:wrap; border-top:1px solid #f0f0f0; padding-top:16px; }
.carousel-wrap { position:relative; border-radius:14px; overflow:hidden; margin-bottom:16px; background:#f5f5f5; }
.carousel-img { width:100%; display:block; }
.carousel-nav { position:absolute; bottom:12px; left:0; right:0; display:flex; align-items:center; justify-content:center; gap:12px; }
.carousel-arrow { width:32px; height:32px; border-radius:50%; background:rgba(0,0,0,.35); color:#fff; display:flex; align-items:center; justify-content:center; font-size:22px; cursor:pointer; transition:0.2s; }
.carousel-arrow:hover { background:rgba(0,0,0,.55); }
.carousel-dots { display:flex; gap:6px; }
.dot { width:6px; height:6px; border-radius:50%; background:rgba(255,255,255,.5); cursor:pointer; transition:0.2s; }
.dot.active { width:18px; border-radius:3px; background:#fff; }
.action-row { display:flex; gap:12px; justify-content:space-around; background:#f8f9fc; border-radius:12px; padding:10px 8px; }
.action-btn { flex:1; text-align:center; font-size:14px; color:#666; cursor:pointer; padding:6px; border-radius:8px; transition:all 0.2s; user-select:none; }
.action-btn:hover { background:#fff; box-shadow:0 1px 4px rgba(0,0,0,0.06); }
.action-btn.active { color:#409eff; font-weight:500; }
.overlay { position:fixed; top:0; left:0; right:0; bottom:0; z-index:1000; background:rgba(0,0,0,.4); display:flex; align-items:flex-end; justify-content:center; }
.share-panel { width:94%; max-width:400px; background:#fff; border-radius:20px 20px 0 0; padding:24px 20px 32px; }
.share-header { font-size:17px; font-weight:600; color:#1d1d1f; text-align:center; margin-bottom:8px; }
.share-count { font-size:13px; color:#909399; text-align:center; margin-bottom:16px; }
.share-divider { height:1px; background:#f0f3f9; margin:0 0 16px; }
.share-option { display:flex; align-items:center; gap:12px; padding:14px 12px; border-radius:12px; cursor:pointer; transition:0.2s; }
.share-option:hover { background:#f5f8ff; }
.share-icon { font-size:22px; }
.share-label { font-size:15px; color:#1d1d1f; }
.share-cancel { margin-top:16px; text-align:center; padding:14px; border-radius:12px; background:#f5f5f7; font-size:15px; color:#1d1d1f; cursor:pointer; }
.loading-tip { text-align:center; padding:60px 0; color:#999; font-size:15px; }
.skeleton-img { width:100%; aspect-ratio:16/10; border-radius:16px; margin-bottom:16px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; }
.s-w-60 { width:60%; } .s-w-70 { width:70%; } .s-w-80 { width:80%; } .s-w-90 { width:90%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }

/* 评论区 */
.comment-section { background:#fff; border-radius:16px; padding:20px; margin-bottom:12px; box-shadow:0 1px 8px rgba(0,0,0,0.04); }
.comment-title { font-size:15px; font-weight:600; color:#1d1d1f; margin-bottom:16px; padding-bottom:12px; border-bottom:2px solid #409eff; display:inline-block; }
.comment-input-wrap { margin-bottom:20px; background:#f8f9fc; border-radius:12px; padding:12px; }
.comment-input-actions { display:flex; justify-content:flex-end; margin-top:8px; }
.comment-login-hint { text-align:center; padding:20px 16px; font-size:14px; color:#909399; background:#f8f9fc; border-radius:12px; }
.comment-loading { padding:16px; }
.comment-empty { text-align:center; color:#aaa; font-size:14px; padding:32px 0; }
.comment-list { display:flex; flex-direction:column; gap:0; }
.comment-item { padding:14px 0; border-bottom:1px solid #f0f0f0; transition:background 0.15s; }
.comment-item:hover { background:#fafbfc; margin:0 -12px; padding:14px 12px; border-radius:8px; }
.comment-item.is-reply { margin-left:16px; border-left:2px solid #e4e7ed; padding-left:14px; }
.comment-item.is-reply:hover { margin-left:4px; }
.comment-item:last-child { border-bottom:none; }
.comment-header { display:flex; align-items:center; gap:8px; margin-bottom:4px; flex-wrap:wrap; }
.comment-avatar { width:30px; height:30px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; display:inline-flex; align-items:center; justify-content:center; font-size:14px; flex-shrink:0; }
.comment-username { font-size:13px; font-weight:500; color:#1d1d1f; }
.comment-reply-target { font-size:12px; color:#909399; }
.comment-reply-target::before { content:"@"; }
.comment-time { font-size:12px; color:#b0b0b0; }
.comment-reply-btn { font-size:12px; color:#409eff; cursor:pointer; margin-left:4px; }
.comment-del { font-size:12px; color:#c0c4cc; cursor:pointer; margin-left:auto; transition:color 0.15s; }
.comment-del:hover { color:#f56c6c; }
.comment-body { font-size:14px; color:#333; line-height:1.6; margin-top:4px; }
.comment-more-wrap { text-align:center; padding:12px 0 0; }
.reply-indicator { font-size:13px; color:#909399; padding:0 0 8px; display:flex; align-items:center; gap:8px; }
.global-reply { margin-top:0; margin-bottom:16px; padding:16px; background:#f0f4ff; border-radius:12px; border:1px solid #d6e4ff; }
.inline-reply-actions { display:flex; justify-content:flex-end; gap:8px; margin-top:6px; }
.reply-list { margin-top:8px; margin-left:12px; border-left:2px solid #ebeef5; padding-left:12px; }
.reply-item { padding:10px 0; border-bottom:1px solid #f5f5f5; }
.reply-item:last-child { border-bottom:none; }
.version-preview { width:94%; max-width:500px; max-height:80vh; overflow-y:auto; background:#fff; border-radius:20px; padding:24px; margin:auto; align-self:center; position:relative; }
.vp-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px; }
.vp-title { font-size:17px; font-weight:600; color:#1d1d1f; line-height:1.4; }
.vp-close { font-size:18px; color:#909399; cursor:pointer; padding:4px; }
.overlay:has(.version-preview) { align-items:center; }
.vp-time { font-size:13px; color:#909399; margin-bottom:16px; }
.vp-content { font-size:15px; color:#1d1d1f; line-height:1.7; white-space:pre-wrap; }
.version-section { background:#fff; border-radius:16px; padding:20px; margin-bottom:12px; box-shadow:0 1px 8px rgba(0,0,0,0.04); }
.version-title { font-size:15px; font-weight:600; color:#1d1d1f; margin-bottom:12px; }
.version-list { display:flex; flex-direction:column; gap:8px; }
.version-item { display:flex; align-items:center; gap:10px; padding:10px 12px; border-radius:10px; background:#f8f9fc; cursor:pointer; transition:0.2s; font-size:13px; }
.version-item:hover { background:#ecf5ff; }
.ver-num { font-weight:600; color:#409eff; min-width:28px; }
.ver-time { color:#909399; min-width:80px; }
.ver-title { color:#1d1d1f; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }

.folder-option.selected { border-color: #409eff; background: #f0f8ff; }
.folder-option:hover { border-color: #409eff44; }

.md-content { line-height:1.8; font-size:15px; color:#333; }
.md-content .md-h1 { font-size:22px; margin:20px 0 10px; font-weight:600; }
.md-content .md-h2 { font-size:18px; margin:16px 0 8px; font-weight:600; }
.md-content .md-h3 { font-size:16px; margin:14px 0 6px; font-weight:600; }
.md-content .md-p { margin:10px 0; }
.md-content .md-code { background:#f4f7fd; border-radius:8px; padding:12px 16px; overflow-x:auto; font-size:13px; }
.md-content .md-inline { background:#f4f7fd; padding:2px 6px; border-radius:4px; font-size:13px; }
.md-content .md-ul { padding-left:20px; margin:8px 0; }
.md-content .md-ul li { margin:4px 0; }
.md-content strong { font-weight:600; }
.md-content em { font-style:italic; }
.md-content a { color:#409eff; text-decoration:underline; }
</style>
