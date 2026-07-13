<template>
  <div class="detail-page">
    <!-- 顶部用户栏 -->
    <div class="top-user-bar">
      <div class="close-icon" @click="goBack">×</div>
      <div class="user-info-wrap">
        <img v-if="detail.avatar && (detail.avatar.startsWith('http') || detail.avatar.startsWith('/'))" class="avatar" :src="detail.avatar" alt="头像">
        <span v-else-if="detail.avatar && detail.avatar.length <= 2" class="avatar-emoji">{{ detail.avatar }}</span>
        <span v-else class="avatar-emoji">{{ (detail.nickname || detail.username || '?')[0] }}</span>
        <div class="username">{{ detail.nickname || detail.username || '宁静小猫' }}</div>
      </div>
      <button v-if="isOwnInspire" class="follow-btn" @click="handleEdit">编辑</button>
      <button v-else-if="isLogin && !isOwnInspire" class="follow-btn" @click="handleToggleFollow">{{ isFollowing ? '已关注' : '关注' }}</button>
      <button v-else-if="!isLogin" class="follow-btn" @click="requireLogin">关注</button>
    </div>

    <!-- 大图 -->
    <img class="note-img" :src="mainImage" alt="灵感配图">

    <!-- 标题+正文+话题 -->
    <div class="text-wrap">
      <h2 class="note-title">{{ detail.title || '古道尘烟：李白诗中的苍茫旅迹' }}</h2>
      <div class="note-desc">{{ detail.content || '李白笔下的古道，常是"古道连绵走西京，紫阙落日浮云生"的壮阔，或是"长歌吟松风，曲尽星河稀"的孤寂。可结合其《蜀道难》的险峻与《丁都护歌》的苍凉，描绘一条连接历史与侠客梦的沙石路径。' }}</div>
      <span class="tag-item" v-for="t in tagList" :key="t">#{{ t }}</span>
    </div>

    <!-- 猜你搜索 -->
    <div class="search-tip">
      <span>Q</span> 猜你想搜 {{ detail.title || '灵感创作素材' }}
    </div>

    <!-- 发布信息 -->
    <div class="publish-row">
      <span>{{ publishText }}</span>
      <div class="more-operate">⋯</div>
    </div>

    <!-- 评论总数 -->
    <div class="comment-count-title">共 {{ comments.length }} 条评论</div>

    <!-- 评论列表 -->
    <div class="comment-list">
      <div class="comment-main" v-for="c in displayComments" :key="c.id">
        <div class="comment-header">
          <img v-if="c.avatar && (c.avatar.startsWith('http') || c.avatar.startsWith('/')) && !c._avatarErr" class="comment-avatar" :src="c.avatar" @error="c._avatarErr = true" alt="头像">
          <span v-else-if="c.avatar && c.avatar.length <= 2" class="avatar-emoji" style="font-size:36px">{{ c.avatar }}</span>
          <span v-else class="avatar-emoji" style="font-size:36px">{{ (c.nickname || c.username || '?')[0] }}</span>
          <span class="comment-name">{{ c.nickname || c.username || '宁静小猫' }}</span>
          <div class="comment-more">⋯</div>
        </div>
        <div class="comment-text">{{ c.content }}</div>
        <div class="comment-bottom-info">
          <span>{{ c.createTime ? formatRelativeTime(c.createTime) : '' }}</span>
          <div class="comment-sub-btn" :class="{ 'like-red': c.liked }" @click="likeComment(c)">
            <svg class="small-svg" viewBox="0 0 24 24" :fill="c.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
            {{ c.likeCount ?? 0 }}
          </div>
          <div class="comment-sub-btn" @click="replyTo(c)">回复</div>
        </div>

        <!-- 回复输入框 -->
        <div v-if="replyTarget && replyTarget.id === c.id" class="reply-input-wrap">
          <div class="reply-input-header">回复 @{{ c.nickname || c.username }}</div>
          <div class="reply-input-row">
            <input v-model="commentText" class="reply-input" placeholder="输入回复..." @keyup.enter="submitReply(c)" />
            <button class="reply-send-btn" :disabled="!commentText.trim() || submittingComment" @click="submitReply(c)">发送</button>
            <button class="reply-cancel-btn" @click="cancelReply">取消</button>
          </div>
        </div>

        <div v-if="c.replies && c.replies.length" class="reply-wrap">
          <div class="reply-item" v-for="r in c.replies" :key="r.id">
            <div class="comment-header">
              <img v-if="r.avatar && (r.avatar.startsWith('http') || r.avatar.startsWith('/')) && !r._avatarErr" class="comment-avatar" :src="r.avatar" @error="r._avatarErr = true" alt="头像">
              <span v-else-if="r.avatar && r.avatar.length <= 2" class="avatar-emoji" style="font-size:36px">{{ r.avatar }}</span>
              <span v-else class="avatar-emoji" style="font-size:36px">{{ (r.nickname || r.username || '?')[0] }}</span>
              <span class="comment-name">{{ r.nickname || r.username || '宁静小猫' }}</span>
              <span class="reply-target">回复 @{{ r.replyUsername || r.replyTo || '用户' }}</span>
              <div class="comment-more">⋯</div>
            </div>
            <div class="comment-text">{{ r.content }}</div>
            <div class="reply-bottom">
              <span>{{ r.createTime ? formatRelativeTime(r.createTime) : '' }}</span>
              <div class="reply-like-btn" :class="{ 'like-red': r.liked }" @click="likeReply(r)">
                <svg class="small-svg" viewBox="0 0 24 24" :fill="r.liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                {{ r.likeCount ?? 0 }}
              </div>
              <span class="reply-sub-btn" @click="replyTo(r, {userId: r.userId, username: r.nickname || r.username})">回复</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 评论输入弹层 -->
    <div v-if="showCommentInput" class="comment-overlay" @click.self="closeCommentInput">
      <div class="comment-input-panel">
        <div class="comment-input-header">发表评论</div>
        <textarea v-model="commentText" class="comment-textarea" rows="3" placeholder="说点什么..." maxlength="500"></textarea>
        <div class="comment-input-footer">
          <span class="char-count">{{ commentText.length }}/500</span>
          <button class="comment-submit-btn" :disabled="!commentText.trim() || submittingComment" @click="submitComment">发表</button>
        </div>
      </div>
    </div>

    <!-- 底部固定操作栏 -->
    <div class="bottom-action-bar">
      <div class="comment-input-short" @click="openCommentInput">说点什么...</div>
      <div class="action-icon-group">
        <div class="action-item" :class="{ 'like-active': liked }" @click="handleLike">
          <svg viewBox="0 0 24 24" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
          <span>{{ detail.likeCount ?? 0 }}</span>
        </div>
        <div class="action-item" :class="{ 'like-active': collected }" @click="toggleCollect">
          <svg viewBox="0 0 24 24" :fill="collected ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2"><path d="M17 3H7c-1.1 0-2 .9-2 2v16l7-3 7 3V5c0-1.1-.9-2-2-1z"/></svg>
          <span>{{ detail.collectCount ?? 0 }}</span>
        </div>
        <div class="action-item">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.9-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8A8.5 8.5 0 0 1 11.5 3a8.38 8.38 0 0 1 3.8.9 8.5 8.5 0 0 1 4.7 7.6z"/></svg>
          <span>{{ comments.length }}</span>
        </div>
        <div class="action-item" @click="handleShare">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
          <span>{{ detail.shareCount ?? 0 }}</span>
        </div>
      </div>
    </div>

    <!-- 分享弹窗 -->
    <div v-if="showSharePanel" class="overlay" @click.self="showSharePanel = false">
      <div class="share-panel">
        <div class="share-header">分享灵感</div>
        <div class="share-count">已分享 {{ detail.shareCount ?? 0 }} 次</div>
        <div class="share-divider"></div>
        <div class="share-option" @click="nativeShare"><span class="share-icon">📱</span><span class="share-label">分享到其他应用</span></div>
        <div class="share-option" @click="copyShareLink"><span class="share-icon">🔗</span><span class="share-label">复制链接</span></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInspireDetail, getPublicUserInfo, shareInspire, collectInspire, uncollectInspire, likeInspire, unlikeInspire, getComments, createComment, followUser, unfollowUser, getFollowing } from '@/api/inspire.js'

const requireLogin = () => {
  ElMessageBox.alert('请先登录后进行操作', '提示', {
    confirmButtonText: '去登录',
    type: 'warning',
    closeOnClickModal: false
  }).then(() => {
    localStorage.setItem('redirectPath', window.location.pathname)
    router.push('/login')
  }).catch(() => {})
}

const route = useRoute(); const router = useRouter()
const isLogin = computed(() => localStorage.getItem('isLogin'))
const currentUserId = computed(() => localStorage.getItem('userId'))
const isOwnInspire = computed(() => {
  if (!detail.value.userId || !isLogin.value) {
    console.log('[DEBUG] isOwnInspire: false — detail.userId=', detail.value.userId, 'isLogin=', isLogin.value)
    return false
  }
  // 从 JWT 中解析精确的 userId（避免 localStorage 精度丢失）
  let jwtUserId = ''
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      jwtUserId = String(payload.sub)
    }
  } catch (e) {}
  console.log('[DEBUG] isOwnInspire compare: detail.userId=', detail.value.userId, 'localStorage=', currentUserId.value, 'jwt=', jwtUserId)
  const result = String(detail.value.userId) === (jwtUserId || String(currentUserId.value))
  return result
})
const detail = ref({})
const liked = ref(false)
const collected = ref(false)
const isFollowing = ref(false)
const showSharePanel = ref(false)
const comments = ref([])
const showCommentInput = ref(false)
const commentText = ref('')
const replyTarget = ref(null)
const replyToUser = ref(null)
const submittingComment = ref(false)

const mainImage = computed(() => {
  const imgs = detail.value.images
  if (imgs && imgs.length > 0) return imgs[0]
  if (detail.value.img) return detail.value.img
  return 'https://picsum.photos/id/102/400/300'
})

const tagList = computed(() => {
  if (detail.value.tag) return detail.value.tag.split(/[,，、\s]+/).filter(Boolean)
  return ['古风灵感', '山水创作']
})

const publishText = computed(() => {
  let s = ''
  if (detail.value.createTime) s += formatRelativeTime(detail.value.createTime)
  if (detail.value.publishCity) s += ' 📍' + detail.value.publishCity
  if (!s) s = '未知'
  return s
})

const displayComments = computed(() => {
  if (comments.value.length > 0) return comments.value
  return []
})

async function loadData(id) {
  try {
    const res = await getInspireDetail(id)
    if (res.code === 200) {
      detail.value = res.data || {}
      liked.value = !!detail.value.liked
      collected.value = !!detail.value.collected
    }
  } catch (e) { /* ignore */ }
  try {
    const userRes = await getPublicUserInfo(detail.value.userId)
    if (userRes.code === 200 && userRes.data) detail.value.avatar = userRes.data.avatar
  } catch (e) { /* ignore */ }

  try {
    const res = await getComments(id)
    if (res.code === 200) {
      let flat = res.data?.records || res.data || []
      if (Array.isArray(flat)) {
        const top = []; const map = {}
        for (const c of flat) {
          if (c.parentId && c.parentId !== '0' && c.parentId !== 0) {
            if (!map[c.parentId]) map[c.parentId] = []
            map[c.parentId].push(c)
          } else {
            top.push(c)
          }
        }
        for (const c of top) c.replies = map[c.id] || []
        comments.value = top
      } else {
        comments.value = Array.isArray(flat) ? flat : []
      }
    }
  } catch (e) { /* ignore */ }
  // 加载完成后检查关注状态
  if (isLogin.value && detail.value.userId) {
    try {
      const r = await getFollowing()
      if (r.code === 200 && r.data) {
        isFollowing.value = r.data.some(f => String(f.id) === String(detail.value.userId))
      }
    } catch (e) {}
  }
}

watch(() => route.params.id, (id) => { if (id) loadData(id) }, { immediate: true })

const formatDate = (d) => {
  if (!d) return ''
  const s = d.substring(0, 10)
  const p = s.split('-')
  return p[1] + '-' + p[2]
}

const goBack = () => { router.back() }

const formatRelativeTime = (d) => {
  if (!d) return ''
  const now = new Date()
  const date = new Date(d)
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 5) return '刚刚'
  if (diffMin < 60) return diffMin + '分钟前'
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return diffHour + '小时前'
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  if (year === now.getFullYear()) return month + '月' + day + '日'
  return year + '年' + month + '月' + day + '日'
}

const openCommentInput = () => {
  if (!isLogin.value) { requireLogin(); return }
  replyTarget.value = null
  commentText.value = ''
  showCommentInput.value = true
}

const closeCommentInput = () => {
  showCommentInput.value = false
  commentText.value = ''
}

const replyTo = (c, replyToUserInfo) => {
  if (!isLogin.value) { requireLogin(); return }
  showCommentInput.value = false
  // 如果是回复二级评论，找到所属的顶层评论作为 parent
  let target = c
  if (replyToUserInfo) {
    for (const top of comments.value) {
      if (top.replies && top.replies.some(r => String(r.id) === String(c.id))) {
        target = top
        break
      }
    }
  }
  replyTarget.value = target
  replyToUser.value = replyToUserInfo || null
  commentText.value = ''
  setTimeout(() => {
    const el = document.querySelector('.reply-input-wrap input')
    if (el) el.focus()
  }, 100)
}

const cancelReply = () => {
  replyTarget.value = null
  replyToUser.value = null
  commentText.value = ''
}

const submitComment = async () => {
  if (!commentText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    const res = await createComment(detail.value.id, { content: commentText.value.trim() })
    if (res.code === 200) {
      ElMessage.success('评论成功')
      closeCommentInput()
      loadData(route.params.id)
    } else {
      ElMessage.error(res.msg || '评论失败')
    }
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value = false
  }
}

const submitReply = async (parent) => {
  if (!commentText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    const res = await createComment(detail.value.id, {
      content: commentText.value.trim(),
      parentId: parent.id,
      replyUserId: replyToUser.value ? replyToUser.value.userId : parent.userId,
      replyUsername: replyToUser.value ? replyToUser.value.username : (parent.nickname || parent.username)
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      cancelReply()
      loadData(route.params.id)
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    submittingComment.value = false
  }
}

const likeComment = (c) => { c.liked = !c.liked; c.likeCount = (c.likeCount ?? 0) + (c.liked ? 1 : -1) }
const likeReply = (r) => { r.liked = !r.liked; r.likeCount = (r.likeCount ?? 0) + (r.liked ? 1 : -1) }

const handleLike = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (liked.value) {
      const res = await unlikeInspire(detail.value.id)
      if (res.code === 200) { liked.value = false; detail.value.likeCount-- }
    } else {
      const res = await likeInspire(detail.value.id)
      if (res.code === 200) { liked.value = true; detail.value.likeCount++ }
    }
  } catch (e) { /* ignore */ }
}

const toggleCollect = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (collected.value) {
      const res = await uncollectInspire(detail.value.id)
      if (res.code === 200) { collected.value = false; detail.value.collectCount-- }
    } else {
      const res = await collectInspire(detail.value.id)
      if (res.code === 200) { collected.value = true; detail.value.collectCount++ }
    }
  } catch (e) { /* ignore */ }
}

const handleShare = async () => {
  try { await shareInspire(detail.value.id); detail.value.shareCount = (detail.value.shareCount ?? 0) + 1 } catch (e) {}
  showSharePanel.value = true
}

const copyShareLink = () => {
  const url = window.location.origin + '/detail/' + detail.value.id
  navigator.clipboard.writeText(url).then(() => ElMessage.success('链接已复制'))
  showSharePanel.value = false
}

const nativeShare = () => {
  if (navigator.share) navigator.share({ title: detail.value.title, url: window.location.href }).catch(() => {})
  showSharePanel.value = false
}

const handleEdit = () => { router.push({ name: 'Edit', params: { id: detail.value.id } }) }

const handleToggleFollow = async () => {
  if (!isLogin.value) { requireLogin(); return }
  if (isOwnInspire.value) { ElMessage.warning('不能关注自己'); return }
  try {
    if (isFollowing.value) {
      const res = await unfollowUser(detail.value.userId)
      if (res.code === 200) { isFollowing.value = false; ElMessage.success('已取消关注') }
      else { ElMessage.error(res.msg || '操作失败') }
    } else {
      const res = await followUser(detail.value.userId)
      if (res.code === 200) { isFollowing.value = true; ElMessage.success('关注成功') }
      else { ElMessage.error(res.msg || '关注失败') }
    }
  } catch (e) { const msg = e?.response?.data?.msg || e?.message || '操作失败'; ElMessage.error(msg) }
}

// 关注状态已迁移到 loadData 中检查
</script>

<style scoped>
.avatar-emoji { display:inline-flex; align-items:center; justify-content:center; width:40px; height:40px; border-radius:50%; background:#f4f4f4; }
* { margin: 0; padding: 0; box-sizing: border-box; font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", system-ui, sans-serif; }
body { background: #ffffff; color: #222; padding-bottom: 70px; }
svg { width: 22px; height: 22px; fill: currentColor; }
.small-svg { width: 14px; height: 14px; }

.top-user-bar { display: flex; align-items: center; justify-content: space-between; height: 64px; padding: 0 14px; border-bottom: 1px solid #f4f4f4; }
.close-icon { font-size: 22px; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 50%; cursor: pointer; }
.close-icon:active { background: #f4f4f4; }
.user-info-wrap { display: flex; align-items: center; gap: 10px; flex: 1; margin-left: 8px; }
.avatar { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.username { font-size: 16px; font-weight: 500; }
.follow-btn { background: #ff2442; color: #fff; border: none; padding: 7px 18px; border-radius: 20px; font-size: 14px; cursor: pointer; }

.note-img { width: 100%; max-height: 60vh; object-fit: cover; display: block; }

.text-wrap { padding: 16px 14px; }
.note-title { font-size: 20px; font-weight: 600; margin-bottom: 12px; line-height: 1.4; }
.note-desc { font-size: 15px; line-height: 1.75; color: #333; margin-bottom: 10px; }
.tag-item { color: #0066cc; font-size: 15px; cursor: pointer; margin-right: 6px; }

.search-tip { margin: 0 14px 16px; padding: 8px 12px; background: #f7f7f7; border-radius: 8px; display: flex; align-items: center; gap: 6px; font-size: 14px; color: #999; }
.search-tip span { color: #333; }

.publish-row { padding: 0 14px 16px; display: flex; justify-content: space-between; align-items: center; font-size: 13px; color: #999; border-bottom: 1px solid #eee; padding-bottom: 16px; }
.more-operate { font-size: 18px; cursor: pointer; }

.comment-count-title { padding: 16px 14px 10px; font-size: 16px; font-weight: 500; }
.comment-list { padding: 0 14px; padding-bottom: 100px; }
.comment-main { padding: 14px 0; border-bottom: 1px solid #f4f4f4; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-avatar { width: 36px; height: 36px; border-radius: 50%; }
.comment-name { font-size: 14px; font-weight: 500; color: #666; }
.comment-more { margin-left: auto; font-size: 16px; color: #999; }
.comment-text { font-size: 15px; line-height: 1.7; margin-bottom: 10px; }
.comment-bottom-info { display: flex; align-items: center; gap: 16px; font-size: 13px; color: #999; }
.comment-sub-btn { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.like-red { color: #ff2442; }

.reply-wrap { margin-top: 10px; padding-left: 44px; }
.reply-item { padding: 10px 0; border-top: 1px solid #f4f4f4; }
.reply-item:first-child { border-top: none; }
.reply-target { color: #0066cc; }
.reply-bottom { display: flex; align-items: center; gap: 14px; margin-top: 6px; font-size: 12px; color: #999; }
.reply-like-btn { display: flex; align-items: center; gap: 3px; cursor: pointer; }

.reply-input-wrap { margin: 10px 0 10px 44px; padding: 10px 12px; background: #f9f9f9; border-radius: 8px; }
.reply-input-header { font-size: 13px; color: #666; margin-bottom: 8px; }
.reply-input-row { display: flex; gap: 6px; flex-wrap: nowrap; align-items: center; }
.reply-input { flex: 1; min-width: 0; height: 36px; border: 1px solid #ddd; border-radius: 18px; padding: 0 12px; font-size: 14px; outline: none; }
.reply-input:focus { border-color: #ff2442; }
.reply-send-btn { flex-shrink: 0; height: 36px; padding: 0 14px; border: none; background: #ff2442; color: #fff; border-radius: 18px; font-size: 14px; cursor: pointer; white-space: nowrap; }
.reply-send-btn:disabled { opacity: 0.5; cursor: default; }
.reply-cancel-btn { flex-shrink: 0; height: 36px; padding: 0 10px; border: 1px solid #ddd; background: #fff; border-radius: 18px; font-size: 14px; color: #666; cursor: pointer; white-space: nowrap; }

.comment-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 200; display: flex; align-items: flex-end; justify-content: center; }
.comment-input-panel { background: #fff; width: 100%; max-width: 400px; border-radius: 16px 16px 0 0; padding: 20px; }
.comment-input-header { font-size: 18px; font-weight: 600; text-align: center; margin-bottom: 16px; }
.comment-textarea { width: 100%; border: 1px solid #ddd; border-radius: 10px; padding: 12px; font-size: 15px; resize: none; outline: none; line-height: 1.6; }
.comment-textarea:focus { border-color: #ff2442; }
.comment-input-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.char-count { font-size: 13px; color: #999; }
.comment-submit-btn { height: 38px; padding: 0 24px; border: none; background: #ff2442; color: #fff; border-radius: 20px; font-size: 15px; cursor: pointer; }
.comment-submit-btn:disabled { opacity: 0.5; cursor: default; }

.bottom-action-bar { position: fixed; bottom: 0; left: 0; width: 100%; height: 64px; border-top: 1px solid #eee; background: #fff; display: flex; align-items: center; padding: 0 14px; gap: 12px; z-index: 100; }
.comment-input-short { flex: 1; background: #f4f4f4; border-radius: 20px; height: 40px; display: flex; align-items: center; padding: 0 14px; font-size: 14px; color: #999; cursor: pointer; }
.action-icon-group { display: flex; align-items: center; gap: 24px; }
.action-item { display: flex; flex-direction: column; align-items: center; font-size: 12px; color: #666; gap: 3px; cursor: pointer; }
.action-item.like-active { color: #ff2442; }

.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); z-index: 200; display: flex; align-items: flex-end; justify-content: center; }
.share-panel { background: #fff; width: 100%; max-width: 400px; border-radius: 16px 16px 0 0; padding: 20px; }
.share-header { font-size: 18px; font-weight: 600; text-align: center; margin-bottom: 4px; }
.share-count { text-align: center; color: #999; font-size: 14px; margin-bottom: 12px; }
.share-divider { height: 1px; background: #eee; margin: 12px 0; }
.share-option { display: flex; align-items: center; gap: 12px; padding: 12px; cursor: pointer; border-radius: 8px; }
.share-option:active { background: #f4f4f4; }
.share-icon { font-size: 24px; }
.share-label { font-size: 16px; }
</style>
