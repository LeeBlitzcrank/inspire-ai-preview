<template>
  <div class="detail-page">
    <header class="topbar">
      <button class="round-button" aria-label="返回" @click="goBack">‹</button>
      <span class="topbar-title">灵感手账</span>
      <button class="round-button" aria-label="分享" @click="handleShare">···</button>
    </header>

    <section class="gallery">
      <img class="gallery-main" :src="mainImage" alt="灵感配图" @error="mainImageError = true">
      <div v-if="imageList.length > 1" class="thumbs">
        <button
          v-for="(image, index) in imageList"
          :key="image + index"
          class="thumb-button"
          :class="{ active: activeImageIndex === index }"
          type="button"
          @click="selectImage(index)"
        >
          <img class="thumb-item" :src="image" :alt="`图片 ${index + 1}`">
        </button>
      </div>
      <div class="image-count">{{ activeImageIndex + 1 }}/{{ imageList.length }} · 点击缩略图切换</div>
    </section>

    <article class="article">
      <div class="kicker">{{ tagList[0] || '灵感手账' }}</div>
      <h1>{{ detail.title || '未命名灵感' }}</h1>
      <div class="lead" v-html="descHtml"></div>

      <div v-if="tagList.length" class="tags">
        <span v-for="tag in tagList" :key="tag" class="tag">#{{ tag }}</span>
      </div>

      <div class="author-card">
        <div class="author-avatar">
          <img
            v-if="isImageAvatar(detail.avatar) && !authorAvatarError"
            :src="detail.avatar"
            alt="头像"
            @error="authorAvatarError = true"
          >
          <span v-else>{{ avatarText(detail.avatar, detail.nickname || detail.username) }}</span>
        </div>
        <div class="author-meta">
          <b>{{ detail.nickname || detail.username || '灵感创作者' }}</b>
          <small>{{ publishText }}</small>
        </div>
        <button v-if="isOwnInspire" class="follow-button" type="button" @click="handleEdit">编辑</button>
        <button v-else-if="isLogin" class="follow-button" type="button" @click="handleToggleFollow">
          {{ isFollowing ? '已关注' : '关注' }}
        </button>
        <button v-else class="follow-button" type="button" @click="requireLogin">关注</button>
      </div>

      <button v-if="!isLogin" class="comment-gate" type="button" @click="requireLogin">
        <span class="comment-gate-title">登录后显示评论</span>
        <span class="comment-gate-desc">登录即可查看全部评论、回复与点赞</span>
        <span class="comment-gate-btn">去登录</span>
      </button>

      <div v-if="isLogin" class="comments-title">
        共 <span>{{ commentTotal }}</span> 条评论 ·
        已显示 <span>{{ loadedCommentCount }}</span>/<span>{{ commentTotal }}</span>
      </div>

      <div v-if="isLogin" class="comment-list">
        <div v-for="commentItem in displayComments" :key="commentItem.id" class="comment-root">
          <div class="comment-head">
            <span class="comment-avatar">
              <img
                v-if="isImageAvatar(commentItem.avatar) && !commentItem._avatarErr"
                :src="commentItem.avatar"
                alt="头像"
                @error="commentItem._avatarErr = true"
              >
              <span v-else>{{ avatarText(commentItem.avatar, commentItem.nickname || commentItem.username) }}</span>
            </span>
            <b>{{ commentItem.nickname || commentItem.username || '宁静小猫' }}</b>
            <span>{{ commentItem.createTime ? formatRelativeTime(commentItem.createTime) : '' }}</span>
          </div>
          <p class="comment-text">{{ commentItem.content }}</p>
          <div class="comment-tools">
            <span class="comment-like" :class="{ liked: commentItem.liked }" @click="likeComment(commentItem)">
              ♡ {{ commentItem.likeCount ?? 0 }}
            </span>
            <span @click="replyTo(commentItem)">回复</span>
          </div>

          <div class="replies" :class="{ empty: !commentItem.replies?.length }">
            <div v-for="reply in visibleReplies(commentItem)" :key="reply.id" class="reply">
              <div class="comment-head">
                <span class="comment-avatar">
                  <img
                    v-if="isImageAvatar(reply.avatar) && !reply._avatarErr"
                    :src="reply.avatar"
                    alt="头像"
                    @error="reply._avatarErr = true"
                  >
                  <span v-else>{{ avatarText(reply.avatar, reply.nickname || reply.username) }}</span>
                </span>
                <b>{{ reply.nickname || reply.username || '宁静小猫' }}</b>
                <span>{{ reply.createTime ? formatRelativeTime(reply.createTime) : '' }}</span>
              </div>
              <p class="comment-text">
                <span v-if="reply.replyUsername" class="reply-to">@{{ reply.replyUsername }}</span>
                {{ reply.content }}
              </p>
              <div class="comment-tools">
                <span class="comment-like" :class="{ liked: reply.liked }" @click="likeReply(reply)">
                  ♡ {{ reply.likeCount ?? 0 }}
                </span>
                <span @click="replyTo(reply, { userId: reply.userId, username: reply.nickname || reply.username })">回复</span>
              </div>
            </div>

            <div v-if="commentItem.replies?.length > 1" class="reply-actions">
              <span v-if="!commentItem._repliesExpanded" class="reply-toggle" @click="expandReplies(commentItem)">
                查看全部 {{ commentItem.replies.length }} 条回复
              </span>
              <template v-else>
                <span
                  v-if="commentItem._visibleReplyCount < commentItem.replies.length"
                  class="reply-more"
                  @click="loadMoreReplies(commentItem)"
                >
                  继续显示回复 {{ commentItem._visibleReplyCount }}/{{ commentItem.replies.length }}
                </span>
                <span v-else class="reply-collapse" @click="collapseReplies(commentItem)">收起回复</span>
                <span v-if="commentItem._visibleReplyCount < commentItem.replies.length" class="reply-collapse" @click="collapseReplies(commentItem)">收起</span>
              </template>
            </div>

            <form
              v-if="replyTarget && String(replyTarget.id) === String(commentItem.id)"
              class="reply-box"
              @submit.prevent="submitReply(commentItem)"
            >
              <input
                v-model="replyText"
                :placeholder="replyToUser ? `回复 @${replyToUser.username}` : '回复...'"
                maxlength="200"
              >
              <button type="submit" :disabled="!replyText.trim() || submittingComment">发送</button>
              <button type="button" class="reply-cancel" @click="cancelReply">取消</button>
            </form>
          </div>
        </div>
      </div>

      <div v-if="isLogin" class="comment-load-status">
        <span v-if="commentLoading">正在加载评论...</span>
        <span v-else-if="commentHasMore" @click="loadMoreComments">
          下滑加载 20 条评论 · 当前显示 {{ loadedCommentCount }}/{{ commentTotal }}
        </span>
        <span v-else>已显示全部 {{ commentTotal }} 条评论</span>
      </div>
    </article>

    <div class="bottom-action-bar">
      <form class="quick-comment" @submit.prevent="submitComment">
        <input
          v-model="quickCommentText"
          :readonly="!isLogin"
          placeholder="说点什么..."
          maxlength="200"
          enterkeyhint="send"
          aria-label="快速评论"
          @focus="handleQuickCommentFocus"
        >
      </form>
      <div class="action-group">
        <button class="action-item" :class="{ active: liked }" type="button" @click="handleLike">
          <span class="action-symbol">{{ liked ? '♥' : '♡' }}</span>
          <span>{{ detail.likeCount ?? 0 }}</span>
        </button>
        <button class="action-item" :class="{ active: collected }" type="button" @click="toggleCollect">
          <span class="action-symbol">{{ collected ? '★' : '☆' }}</span>
          <span>{{ detail.collectCount ?? 0 }}</span>
        </button>
      </div>
    </div>

    <div v-if="showSharePanel" class="overlay" @click.self="showSharePanel = false">
      <div class="share-panel">
        <div class="share-header">分享灵感</div>
        <div class="share-count">已分享 {{ detail.shareCount ?? 0 }} 次</div>
        <div class="share-divider"></div>
        <button class="share-option" type="button" @click="nativeShare">
          <span class="share-icon">↗</span>
          <span>分享到其他应用</span>
        </button>
        <button class="share-option" type="button" @click="copyShareLink">
          <span class="share-icon">⌁</span>
          <span>复制链接</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  collectInspire,
  createComment,
  followUser,
  getComments,
  getFollowing,
  getInspireDetail,
  getPublicUserInfo,
  likeInspire,
  shareInspire,
  uncollectInspire,
  unfollowUser,
  unlikeInspire
} from '@/api/inspire.js'
import { sanitizeHtml } from '@/utils/sanitizeHtml.js'

const route = useRoute()
const router = useRouter()
const FALLBACK_IMAGE = 'https://picsum.photos/id/102/900/650'
const DEFAULT_DESC = '把此刻的灵感慢慢写下来，给图片、文字和评论区留出舒服的呼吸感。'

const detail = ref({})
const liked = ref(false)
const collected = ref(false)
const isFollowing = ref(false)
const showSharePanel = ref(false)
const activeImageIndex = ref(0)
const mainImageError = ref(false)
const authorAvatarError = ref(false)

const comments = ref([])
const allCommentRecords = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentLoading = ref(false)
const quickCommentText = ref('')
const replyText = ref('')
const replyTarget = ref(null)
const replyToUser = ref(null)
const submittingComment = ref(false)
let scrollTicking = false

const isLogin = computed(() => localStorage.getItem('isLogin'))
const currentUserId = computed(() => localStorage.getItem('userId'))

const isOwnInspire = computed(() => {
  if (!detail.value.userId || !isLogin.value) return false
  let jwtUserId = ''
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      jwtUserId = String(payload.sub)
    }
  } catch (e) {}
  return String(detail.value.userId) === (jwtUserId || String(currentUserId.value))
})

const normalizeImages = (value) => {
  let list = []
  if (Array.isArray(value)) {
    list = value
  } else if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      list = Array.isArray(parsed) ? parsed : [value]
    } catch (e) {
      list = value.split(/[,，\n]/)
    }
  }

  const normalized = list
    .map(item => typeof item === 'string' ? item : (item?.url || item?.src))
    .filter(Boolean)

  if (detail.value.img && !normalized.includes(detail.value.img)) normalized.push(detail.value.img)
  return normalized.length ? normalized : [FALLBACK_IMAGE]
}

const imageList = computed(() => normalizeImages(detail.value.images))
const mainImage = computed(() => mainImageError.value ? FALLBACK_IMAGE : imageList.value[activeImageIndex.value])
const descHtml = computed(() => sanitizeHtml(detail.value.content || DEFAULT_DESC))

const tagList = computed(() => {
  if (!detail.value.tag) return []
  return detail.value.tag.split(/[,，、\s]+/).filter(Boolean)
})

const publishText = computed(() => {
  const parts = []
  if (detail.value.createTime) parts.push(formatRelativeTime(detail.value.createTime))
  if (detail.value.publishCity) parts.push(detail.value.publishCity)
  return parts.join(' · ') || '刚刚发布'
})

const displayComments = computed(() => comments.value)
const loadedCommentCount = computed(() => allCommentRecords.value.length)
const commentHasMore = computed(() => !commentLoading.value && allCommentRecords.value.length < commentTotal.value)

const isImageAvatar = (avatar) => typeof avatar === 'string'
  && (avatar.startsWith('http') || avatar.startsWith('/') || avatar.startsWith('data:'))

const firstGrapheme = (value) => {
  const chars = Array.from(String(value || '').trim())
  return chars.length ? chars[0] : ''
}

const avatarText = (avatar, name) => {
  if (avatar && !isImageAvatar(avatar)) return firstGrapheme(avatar) || firstGrapheme(name) || '灵'
  return firstGrapheme(name) || '灵'
}

const formatRelativeTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000)
  if (diffMin < 5) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const month = date.getMonth() + 1
  const day = date.getDate()
  if (date.getFullYear() === new Date().getFullYear()) return `${month}月${day}日`
  return `${date.getFullYear()}年${month}月${day}日`
}

/**
 * 游客触发需要登录的操作时统一处理：直接跳登录页，
 * 并把当前灵感写入 redirectPath，登录成功后自动回到这里。
 */
const requireLogin = () => {
  localStorage.setItem('redirectPath', detail.value.id ? `/detail/${detail.value.id}` : '/')
  router.push('/login')
}

const selectImage = (index) => {
  activeImageIndex.value = index
  mainImageError.value = false
}

watch(imageList, () => {
  activeImageIndex.value = 0
  mainImageError.value = false
})

const sortCommentRecords = (records) => records.slice().sort((a, b) => {
  const left = new Date(a.createTime || 0).getTime()
  const right = new Date(b.createTime || 0).getTime()
  return right - left
})

const regroupComments = () => {
  const previousState = new Map()
  comments.value.forEach(item => {
    previousState.set(String(item.id), {
      expanded: item._repliesExpanded,
      visibleCount: item._visibleReplyCount
    })
  })

  const records = sortCommentRecords(allCommentRecords.value)
  const recordMap = new Map(records.map(item => [String(item.id), item]))
  const rootIds = new Set(
    records
      .filter(item => !item.parentId || String(item.parentId) === '0')
      .map(item => String(item.id))
  )

  const resolveRootId = (item) => {
    let parentId = String(item.parentId || '0')
    let guard = 0
    while (parentId !== '0' && recordMap.has(parentId) && guard < 12) {
      const parent = recordMap.get(parentId)
      if (!parent.parentId || String(parent.parentId) === '0') return parentId
      parentId = String(parent.parentId)
      guard += 1
    }
    return parentId
  }

  const roots = []
  const replyMap = new Map()
  records.forEach(item => {
    if (!item.parentId || String(item.parentId) === '0') {
      roots.push(item)
      return
    }
    const rootId = resolveRootId(item)
    if (!rootIds.has(rootId)) return
    if (!replyMap.has(rootId)) replyMap.set(rootId, [])
    replyMap.get(rootId).push(item)
  })

  roots.forEach(root => {
    const replies = sortCommentRecords(replyMap.get(String(root.id)) || [])
    const previous = previousState.get(String(root.id))
    root.replies = replies
    root._repliesExpanded = previous?.expanded || false
    root._visibleReplyCount = previous?.visibleCount || Math.min(1, replies.length)
    replies.forEach(reply => { reply._avatarErr = false })
  })

  comments.value = roots
}

const loadComments = async (reset = false) => {
  if (!detail.value.id || commentLoading.value) return
  if (!isLogin.value) {
    comments.value = []
    allCommentRecords.value = []
    commentTotal.value = 0
    return
  }
  commentLoading.value = true
  const targetPage = reset ? 1 : commentPage.value
  try {
    const res = await getComments(detail.value.id, { page: targetPage, size: 20 })
    if (res.code !== 200) return
    const data = res.data || {}
    const records = Array.isArray(data) ? data : (data.records || [])
    const source = reset ? records : [...allCommentRecords.value, ...records]
    const merged = new Map(source.map(item => [String(item.id), item]))
    allCommentRecords.value = sortCommentRecords([...merged.values()])
    commentTotal.value = Number(data.total ?? allCommentRecords.value.length)
    commentPage.value = targetPage + 1
    regroupComments()
  } catch (e) {
    ElMessage.error('评论加载失败')
  } finally {
    commentLoading.value = false
  }
}

const loadData = async (id) => {
  authorAvatarError.value = false
  try {
    const res = await getInspireDetail(id)
    if (res.code === 200) {
      detail.value = res.data || {}
      liked.value = !!detail.value.liked
      collected.value = !!detail.value.collected
    }
  } catch (e) {}

  if (isLogin.value && detail.value.userId) {
    try {
      const userRes = await getPublicUserInfo(detail.value.userId)
      if (userRes.code === 200 && userRes.data) {
        detail.value.avatar = userRes.data.avatar
        authorAvatarError.value = false
      }
    } catch (e) {}
  }

  await loadComments(true)

  if (isLogin.value && detail.value.userId) {
    try {
      const res = await getFollowing()
      if (res.code === 200 && res.data) {
        isFollowing.value = res.data.some(item => String(item.id) === String(detail.value.userId))
      }
    } catch (e) {}
  }
}

watch(() => route.params.id, id => { if (id) loadData(id) }, { immediate: true })

const visibleReplies = (commentItem) => {
  if (!commentItem.replies?.length) return []
  const count = commentItem._repliesExpanded
    ? Math.min(commentItem._visibleReplyCount || 3, commentItem.replies.length)
    : Math.min(1, commentItem.replies.length)
  return commentItem.replies.slice(0, count)
}

const expandReplies = (commentItem) => {
  commentItem._repliesExpanded = true
  commentItem._visibleReplyCount = Math.min(3, commentItem.replies.length)
}

const loadMoreReplies = (commentItem) => {
  commentItem._visibleReplyCount = Math.min(
    (commentItem._visibleReplyCount || 3) + 3,
    commentItem.replies.length
  )
}

const collapseReplies = (commentItem) => {
  commentItem._repliesExpanded = false
  commentItem._visibleReplyCount = 1
}

const replyTo = (commentItem, userInfo = null) => {
  if (!isLogin.value) { requireLogin(); return }
  let root = commentItem
  if (userInfo) {
    root = comments.value.find(item => item.replies?.some(reply => String(reply.id) === String(commentItem.id))) || commentItem
  }
  replyTarget.value = root
  replyToUser.value = userInfo
  replyText.value = ''
  nextTick(() => document.querySelector('.reply-box input')?.focus())
}

const cancelReply = () => {
  replyTarget.value = null
  replyToUser.value = null
  replyText.value = ''
}

const submitComment = async () => {
  if (!isLogin.value) { requireLogin(); return }
  if (!quickCommentText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    const res = await createComment(detail.value.id, { content: quickCommentText.value.trim() })
    if (res.code === 200) {
      quickCommentText.value = ''
      ElMessage.success('评论成功')
      await loadComments(true)
      await nextTick()
      document.querySelector('.comments-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else {
      ElMessage.error(res.msg || '评论失败')
    }
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value = false
  }
}

const submitReply = async (root) => {
  if (!replyText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    const res = await createComment(detail.value.id, {
      content: replyText.value.trim(),
      parentId: root.id,
      replyUserId: replyToUser.value?.userId || root.userId,
      replyUsername: replyToUser.value?.username || root.nickname || root.username
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      cancelReply()
      await loadComments(true)
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    submittingComment.value = false
  }
}

const handleQuickCommentFocus = () => {
  if (!isLogin.value) requireLogin()
}

const loadMoreComments = () => {
  if (commentHasMore.value) loadComments(false)
}

const handleScroll = () => {
  if (scrollTicking) return
  scrollTicking = true
  requestAnimationFrame(() => {
    const nearBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 260
    if (nearBottom) loadMoreComments()
    scrollTicking = false
  })
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onBeforeUnmount(() => window.removeEventListener('scroll', handleScroll))

const likeComment = (commentItem) => {
  commentItem.liked = !commentItem.liked
  commentItem.likeCount = Math.max(0, (commentItem.likeCount ?? 0) + (commentItem.liked ? 1 : -1))
}

const likeReply = (reply) => {
  reply.liked = !reply.liked
  reply.likeCount = Math.max(0, (reply.likeCount ?? 0) + (reply.liked ? 1 : -1))
}

const handleLike = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (liked.value) {
      const res = await unlikeInspire(detail.value.id)
      if (res.code === 200) {
        liked.value = false
        detail.value.likeCount = Math.max(0, (detail.value.likeCount ?? 0) - 1)
      }
    } else {
      const res = await likeInspire(detail.value.id)
      if (res.code === 200) {
        liked.value = true
        detail.value.likeCount = (detail.value.likeCount ?? 0) + 1
      }
    }
  } catch (e) {}
}

const toggleCollect = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (collected.value) {
      const res = await uncollectInspire(detail.value.id)
      if (res.code === 200) {
        collected.value = false
        detail.value.collectCount = Math.max(0, (detail.value.collectCount ?? 0) - 1)
      }
    } else {
      const res = await collectInspire(detail.value.id)
      if (res.code === 200) {
        collected.value = true
        detail.value.collectCount = (detail.value.collectCount ?? 0) + 1
      }
    }
  } catch (e) {}
}

const handleShare = async () => {
  try {
    await shareInspire(detail.value.id)
    detail.value.shareCount = (detail.value.shareCount ?? 0) + 1
  } catch (e) {}
  showSharePanel.value = true
}

const copyShareLink = () => {
  const url = `${window.location.origin}/#/detail/${detail.value.id}`
  navigator.clipboard.writeText(url).then(() => ElMessage.success('链接已复制'))
  showSharePanel.value = false
}

const nativeShare = () => {
  if (navigator.share) {
    navigator.share({ title: detail.value.title, url: window.location.href }).catch(() => {})
  }
  showSharePanel.value = false
}

const handleEdit = () => router.push({ name: 'Edit', params: { id: detail.value.id } })
const goBack = () => router.back()

const handleToggleFollow = async () => {
  if (!isLogin.value) { requireLogin(); return }
  if (isOwnInspire.value) { ElMessage.warning('不能关注自己'); return }
  try {
    if (isFollowing.value) {
      const res = await unfollowUser(detail.value.userId)
      if (res.code === 200) {
        isFollowing.value = false
        ElMessage.success('已取消关注')
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } else {
      const res = await followUser(detail.value.userId)
      if (res.code === 200) {
        isFollowing.value = true
        ElMessage.success('关注成功')
      } else {
        ElMessage.error(res.msg || '关注失败')
      }
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '操作失败')
  }
}
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: #e8e0d4;
}

.detail-page {
  width: 100%;
  max-width: 860px;
  min-height: 100vh;
  margin: 0 auto;
  padding-bottom: 96px;
  color: #674026;
  background:
    radial-gradient(circle at 12% 9%, rgba(255, 255, 255, .72) 0 2px, transparent 3px),
    #fff2dc;
  box-shadow: 0 0 44px rgba(91, 63, 38, .16);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", Arial, sans-serif;
}

* { box-sizing: border-box; }

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 18px;
  background: rgba(255, 242, 220, .94);
  border-bottom: 1px dashed rgba(217, 144, 93, .48);
  backdrop-filter: blur(12px);
}

.round-button {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  color: #fff7ed;
  background: #e67833;
  font-size: 21px;
  line-height: 1;
  cursor: pointer;
}

.topbar-title { font-size: 16px; font-weight: 700; }

.gallery { padding: 16px 20px 0; }

.gallery-main {
  width: 100%;
  height: 390px;
  display: block;
  object-fit: cover;
  border-radius: 19px;
  box-shadow: 5px 6px 0 rgba(230, 120, 51, .16);
}

.thumbs {
  display: flex;
  gap: 9px;
  margin-top: 10px;
  padding-bottom: 3px;
  overflow-x: auto;
  scrollbar-width: none;
}

.thumbs::-webkit-scrollbar { display: none; }

.thumb-button {
  width: 55px;
  height: 55px;
  flex: 0 0 auto;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 13px;
  background: transparent;
  cursor: pointer;
}

.thumb-button.active { border-color: #e67833; }

.thumb-item {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  border-radius: 10px;
}

.image-count { margin-top: 8px; color: #a86631; font-size: 12px; }

.article { padding: 24px 42px 0; }

.kicker {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 13px;
  border-radius: 999px;
  color: #92501f;
  background: #ffd8ad;
  font-size: 12px;
  font-weight: 700;
  transform: rotate(-1.5deg);
}

h1 {
  margin: 13px 0 15px;
  font-family: Georgia, "Songti SC", "SimSun", serif;
  font-size: 31px;
  line-height: 1.35;
}

.lead {
  color: #624734;
  font-family: Georgia, "Songti SC", "SimSun", serif;
  font-size: 15px;
  line-height: 1.95;
  word-break: break-word;
}

.lead :deep(p) { margin: 0 0 12px; }

.lead :deep(h1),
.lead :deep(h2),
.lead :deep(h3) {
  margin: 18px 0 9px;
  color: #674026;
  line-height: 1.45;
}

.lead :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 14px;
  border-left: 3px solid #e67833;
  border-radius: 0 12px 12px 0;
  background: rgba(255, 216, 173, .48);
}

.lead :deep(img) { max-width: 100%; border-radius: 14px; }
.lead :deep(a) { color: #c56025; }

.tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 18px; }

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 4px 12px;
  border-radius: 999px;
  color: #92501f;
  background: #ffe6c7;
  font-size: 12.5px;
  transform: rotate(-.8deg);
}

.author-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding: 15px;
  border: 2px dashed rgba(103, 64, 38, .62);
  border-radius: 18px;
  background: rgba(255, 249, 240, .78);
  box-shadow: 4px 5px 0 rgba(230, 120, 51, .13);
}

.author-avatar {
  width: 56px;
  height: 56px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  color: #4f8a48;
  background: #e6f2e4;
  font-size: 19px;
  font-weight: 700;
}

.author-avatar img,
.comment-avatar img { width: 100%; height: 100%; display: block; object-fit: cover; }

.author-meta { min-width: 0; }
.author-meta b { display: block; margin-bottom: 3px; font-size: 14.5px; }
.author-meta small { color: #9a7653; font-size: 12px; }

.follow-button {
  margin-left: auto;
  padding: 8px 16px;
  border: 0;
  border-radius: 999px;
  color: #fff7ed;
  background: #e67833;
  font-size: 12.5px;
  cursor: pointer;
}

.comments-title { margin-top: 28px; font-size: 15px; font-weight: 700; }

/* 游客状态：评论区占位块，点击跳登录 */
.comment-gate {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-top: 28px;
  padding: 26px 18px;
  border: 2px dashed rgba(79, 138, 72, .5);
  border-radius: 18px;
  background: #e6f2e4;
  color: #3f6b3a;
  font-family: inherit;
  text-align: center;
  cursor: pointer;
}
.comment-gate:active { background: #dcecd9; }
.comment-gate-title { font-size: 15px; font-weight: 700; }
.comment-gate-desc { font-size: 12.5px; color: #6f8f6a; }
.comment-gate-btn {
  margin-top: 6px;
  padding: 7px 20px;
  border-radius: 999px;
  background: #4f8a48;
  color: #f3faf2;
  font-size: 13px;
  font-weight: 700;
}

.comment-list { margin-top: 5px; }

.comment-root {
  margin-top: 15px;
  padding: 15px 13px 6px;
  border-top: 1px solid rgba(230, 120, 51, .24);
  border-radius: 17px;
  background: rgba(255, 249, 240, .58);
  text-align: left;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  margin-bottom: 8px;
  text-align: left;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin-left: 0;
  border-radius: 50%;
  color: #4f8a48;
  background: #e6f2e4;
  font-size: 14px;
  font-weight: 700;
}

.comment-head b { font-size: 13.5px; text-align: left; }

.comment-head > span:last-child {
  margin-left: auto;
  color: #9a7653;
  font-size: 11.5px;
  opacity: .88;
}

.comment-text {
  margin: 0;
  color: #624734;
  font-size: 13.8px;
  line-height: 1.72;
  word-break: break-word;
}

.comment-tools {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  color: #9a7653;
  font-size: 12px;
}

.comment-tools span { cursor: pointer; }
.comment-like.liked { color: #e65f20; font-weight: 700; }

.replies {
  margin: 11px 0 0 22px;
  padding: 10px 12px;
  border-left: 3px solid #e67833;
  border-radius: 15px;
  background: rgba(255, 232, 203, .45);
}

.replies.empty {
  margin: 0;
  padding: 0;
  border: 0;
  background: none;
}

.reply { padding: 10px 0; border-top: 1px dotted rgba(103, 64, 38, .46); }
.reply:first-child { padding-top: 0; border-top: 0; }
.reply-to { color: #c56025; font-weight: 700; }

.reply-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 9px;
  font-size: 12px;
}

.reply-toggle,
.reply-more { color: #b75b22; font-weight: 700; cursor: pointer; }
.reply-collapse { color: #9a7653; cursor: pointer; }

.reply-box { display: flex; gap: 7px; margin-top: 11px; }

.reply-box input {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 12px;
  border: 1.5px solid #e67833;
  border-radius: 11px;
  outline: none;
  color: #674026;
  background: rgba(255, 255, 255, .48);
  font-family: inherit;
  font-size: 13px;
}

.reply-box button {
  flex: 0 0 auto;
  padding: 0 13px;
  border: 0;
  border-radius: 11px;
  color: #fff7ed;
  background: #e67833;
  font-size: 12.5px;
  cursor: pointer;
}

.reply-box button:disabled { opacity: .4; cursor: default; }

.reply-box .reply-cancel {
  color: #9a7653;
  background: transparent;
  border: 1px dashed rgba(103, 64, 38, .5);
}

.comment-load-status {
  margin: 18px 0 8px;
  color: #a86631;
  text-align: center;
  font-size: 12.5px;
  font-weight: 600;
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 40;
  width: min(100%, 860px);
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  transform: translateX(-50%);
  border-top: 2px dashed #d9905d;
  background: rgba(255, 239, 218, .97);
  backdrop-filter: blur(12px);
}

.quick-comment {
  flex: 1;
  min-width: 0;
  height: 44px;
  overflow: hidden;
  border: 2px solid #e67833;
  border-radius: 999px;
  background: #fff9f0;
}

.quick-comment input {
  width: 100%;
  height: 100%;
  padding: 0 16px;
  border: 0;
  outline: none;
  color: #674026;
  background: transparent;
  font-family: inherit;
  font-size: 13.5px;
}

.quick-comment input::placeholder { color: #b78a63; }
.action-group { display: flex; align-items: center; gap: 12px; }

.action-item {
  min-width: 42px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 0;
  border: 0;
  color: #795238;
  background: transparent;
  font-size: 11px;
  cursor: pointer;
}

.action-item.active { color: #e65f20; }
.action-symbol { font-size: 22px; line-height: 1; }

.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(51, 34, 22, .46);
}

.share-panel {
  width: 100%;
  max-width: 620px;
  padding: 22px;
  border: 2px solid #e67833;
  border-radius: 22px 22px 0 0;
  background: #fff8ec;
}

.share-header { text-align: center; font-size: 18px; font-weight: 700; }
.share-count { margin-top: 5px; color: #9a7653; text-align: center; font-size: 13px; }
.share-divider { height: 1px; margin: 16px 0; border-top: 1px dashed #d9905d; }

.share-option {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px;
  border: 0;
  border-radius: 13px;
  color: #674026;
  background: transparent;
  font-size: 15px;
  text-align: left;
  cursor: pointer;
}

.share-option:active { background: #ffe6c7; }

.share-icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff7ed;
  background: #e67833;
  font-size: 18px;
}

@media (max-width: 640px) {
  .detail-page { max-width: none; box-shadow: none; }
  .topbar { height: 58px; padding: 0 14px; }
  .gallery { padding: 12px 14px 0; }
  .gallery-main { height: 250px; border-radius: 16px; }
  .article { padding: 22px 14px 0; }
  h1 { font-size: 26px; }
  .lead { font-size: 14.5px; }
  .replies { margin-left: 10px; padding: 9px 10px; }
  .bottom-action-bar { height: 70px; gap: 8px; padding: 9px 12px; }
  .quick-comment { height: 42px; }
  .action-group { gap: 4px; }
  .action-item { min-width: 36px; }
}
</style>
