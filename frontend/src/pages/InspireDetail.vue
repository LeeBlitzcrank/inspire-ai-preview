<template>
  <div class="detail-page">
    <header class="topbar">
      <button class="round-button" aria-label="返回" @click="goBack">‹</button>
      <span class="topbar-title">灵感手账</span>
      <div class="topbar-actions">
        <button
          v-if="isOwnInspire"
          class="top-edit-button"
          type="button"
          @click="handleEdit"
        >
          编辑
        </button>
        <button class="round-button" aria-label="分享" @click="handleShare">···</button>
      </div>
    </header>

    <AppState
      :state="detailState"
      :rows="5"
      loading-variant="text"
      error-text="灵感加载失败，请稍后重试"
      @retry="reloadDetail"
    >
      <section class="gallery">
        <video
          v-if="isVideo(mainImage)"
          class="gallery-main video-main"
          :src="mainImage"
          controls
          playsinline
          preload="metadata"
        ></video>
        <img v-else class="gallery-main" :src="mainImage" alt="灵感配图" @error="mainImageError = true">
        <div v-if="imageList.length > 1" class="thumbs">
          <button
            v-for="(image, index) in imageList"
            :key="image + index"
            class="thumb-button"
            :class="{ active: activeImageIndex === index }"
            type="button"
            @click="selectImage(index)"
          >
            <video v-if="isVideo(image)" class="thumb-item" :src="image" muted playsinline preload="metadata"></video>
            <img v-else class="thumb-item" :src="image" :alt="`图片 ${index + 1}`">
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

      <button
        v-if="detail.quoteInspireId"
        class="quote-entry"
        type="button"
        @click="router.push(`/detail/${detail.quoteInspireId}`)"
      >
        <img v-if="detail.quoteImg" :src="thumbOf(detail.quoteImg, 240)" alt="">
        <span class="quote-copy">
          <small>引用自 {{ detail.quoteNickname || '灵感创作者' }}</small>
          <b>{{ detail.quoteTitle || '查看原灵感' }}</b>
        </span>
        <span class="quote-arrow">›</span>
      </button>

      <button v-if="detail.seriesId" class="series-entry" type="button" @click="router.push(`/series/${detail.seriesId}`)">
        <span class="series-entry-label">系列 · {{ detail.seriesName }}</span>
        <b>{{ detail.seriesOrder }}/{{ detail.seriesTotal }}</b>
        <span>查看系列 ›</span>
      </button>

      <div class="author-card">
        <div class="author-avatar">
          <img
            v-if="isImageAvatar(detail.avatar) && !authorAvatarError"
            :src="detail.avatar"
            alt="头像"
            @error="authorAvatarError = true"
          >
          <span v-else>{{ avatarText(detail.avatar, detail.nickname) }}</span>
        </div>
        <div class="author-meta">
          <b>{{ detail.nickname || '灵感创作者' }}</b>
          <small>{{ publishText }}</small>
        </div>
        <button v-if="!isOwnInspire && isLogin" class="follow-button" type="button" @click="handleToggleFollow">
          {{ isFollowing ? '已关注' : '关注' }}
        </button>
        <button v-else-if="!isOwnInspire" class="follow-button" type="button" @click="requireLogin">关注</button>
      </div>

      <div v-if="detail.seriesId && (detail.prevSeriesId || detail.nextSeriesId)" class="series-nav">
        <button v-if="detail.prevSeriesId" type="button" @click="router.push(`/detail/${detail.prevSeriesId}`)">
          <small>上一篇</small><b>{{ detail.prevSeriesTitle }}</b>
        </button>
        <button v-if="detail.nextSeriesId" type="button" @click="router.push(`/detail/${detail.nextSeriesId}`)">
          <small>下一篇</small><b>{{ detail.nextSeriesTitle }}</b>
        </button>
      </div>

      <button v-if="!isLogin" class="comment-gate" type="button" @click="requireLogin">
        <span class="comment-gate-title">登录后显示评论</span>
        <span class="comment-gate-desc">登录即可查看全部评论、回复与点赞</span>
        <span class="comment-gate-btn">去登录</span>
      </button>

      <div v-if="isLogin" class="comments-toolbar">
        <div class="comments-title">
          共 <span>{{ commentTotal }}</span> 条评论 ·
          已显示 <span>{{ loadedCommentCount }}</span>/<span>{{ commentTotal }}</span>
        </div>
        <button
          class="comment-sort-toggle"
          type="button"
          :disabled="commentLoading"
          @click="toggleCommentSort"
        >
          {{ commentSort === 'hot' ? '按热度' : '按时间' }}
        </button>
      </div>

      <AppState
        v-if="isLogin"
        :state="commentState"
        :rows="4"
        loading-variant="text"
        empty-icon="💬"
        empty-text="还没有评论"
        error-text="评论加载失败"
        @retry="loadComments(true)"
      >
      <div class="comment-list">
        <div
          v-for="commentItem in displayComments"
          :key="commentItem.id"
          class="comment-root"
          :data-comment-id="commentItem.id"
        >
          <div class="comment-head">
            <span class="comment-avatar">
              <img
                v-if="isImageAvatar(commentItem.avatar) && !commentItem._avatarErr"
                :src="commentItem.avatar"
                alt="头像"
                @error="commentItem._avatarErr = true"
              >
              <span v-else>{{ avatarText(commentItem.avatar, commentItem.nickname) }}</span>
            </span>
            <b>{{ commentItem.nickname || '灵感用户' }}</b>
            <span>{{ commentItem.createTime ? formatCommentTime(commentItem.createTime) : '' }}</span>
          </div>
          <p class="comment-text">{{ commentItem.content }}</p>
          <div class="comment-tools">
            <span class="comment-like" :class="{ liked: commentItem.liked }" @click="toggleCommentLike(commentItem)">
              ♡ {{ commentItem.likeCount ?? 0 }}
            </span>
            <span @click="replyTo(commentItem)">回复</span>
          </div>

          <div class="replies" :class="{ empty: !commentItem.replies?.length }">
            <div
              v-for="reply in visibleReplies(commentItem)"
              :key="reply.id"
              class="reply"
              :data-comment-id="reply.id"
            >
              <div class="comment-head">
                <span class="comment-avatar">
                  <img
                    v-if="isImageAvatar(reply.avatar) && !reply._avatarErr"
                    :src="reply.avatar"
                    alt="头像"
                    @error="reply._avatarErr = true"
                  >
                  <span v-else>{{ avatarText(reply.avatar, reply.nickname) }}</span>
                </span>
                <b>{{ reply.nickname || '灵感用户' }}</b>
                <span>{{ reply.createTime ? formatCommentTime(reply.createTime) : '' }}</span>
              </div>
              <p class="comment-text">
                <span v-if="reply.replyUsername" class="reply-to">@{{ reply.replyUsername }}</span>
                {{ reply.content }}
              </p>
              <div class="comment-tools">
                <span class="comment-like" :class="{ liked: reply.liked }" @click="toggleCommentLike(reply)">
                  ♡ {{ reply.likeCount ?? 0 }}
                </span>
                <span @click="replyTo(reply, { userId: reply.userId, nickname: reply.nickname || '灵感用户' })">回复</span>
              </div>
            </div>

            <div v-if="commentItem.replyCount" class="reply-actions">
              <button
                v-if="!commentItem._repliesExpanded"
                class="reply-toggle"
                type="button"
                @click="expandReplies(commentItem)"
              >
                展开 {{ commentItem.replyCount }} 条回复
              </button>
              <template v-else>
                <button
                  v-if="commentItem._visibleReplyCount < commentItem.replyCount"
                  class="reply-more"
                  type="button"
                  :disabled="commentItem._repliesLoading"
                  @click="loadMoreReplies(commentItem)"
                >
                  {{ commentItem._repliesLoading
                    ? '加载中...'
                    : `继续显示 ${commentItem._visibleReplyCount}/${commentItem.replyCount}` }}
                </button>
                <button class="reply-collapse" type="button" @click="collapseReplies(commentItem)">
                  收起回复
                </button>
              </template>
            </div>

            <form
              v-if="replyTarget && String(replyTarget.id) === String(commentItem.id)"
              class="reply-box"
              @submit.prevent="submitReply(commentItem)"
            >
              <div v-if="mentionOpen && mentionTarget === 'reply'" class="mention-panel reply-mention-panel">
                <div v-if="mentionLoading" class="mention-empty">加载中…</div>
                <div v-else-if="!mentionCandidates.length" class="mention-empty">没有匹配的关注用户</div>
                <button
                  v-for="user in mentionCandidates"
                  :key="user.id"
                  type="button"
                  class="mention-item"
                  @mousedown.prevent="selectMention(user)"
                >
                  <span>{{ avatarText(user.avatar, user.nickname) }}</span>
                  <b>{{ user.nickname || '灵感用户' }}</b>
                  <small>已关注</small>
                </button>
              </div>
              <input
                v-model="replyText"
                :placeholder="replyToUser ? `回复 @${replyToUser.nickname}` : '回复...'"
                maxlength="200"
                @input="onMentionInput($event, 'reply')"
                @keydown.esc="closeMention"
              >
              <button type="submit" :disabled="!replyText.trim() || submittingComment">发送</button>
              <button type="button" class="reply-cancel" @click="cancelReply">取消</button>
            </form>
          </div>
        </div>
      </div>
      </AppState>

      <div v-if="isLogin && commentState === 'ready'" class="comment-load-status">
        <span v-if="commentLoading">正在加载评论...</span>
        <button
          v-else-if="commentHasMore"
          class="comment-load-btn"
          type="button"
          @click="loadMoreComments"
        >
          点击加载下一批 · 当前显示 {{ loadedCommentCount }}/{{ commentTotal }}
        </button>
        <span v-else>已显示全部 {{ commentTotal }} 条评论</span>
      </div>
      </article>

      <div class="bottom-action-bar">
        <div v-if="mentionOpen && mentionTarget === 'quick'" class="mention-panel quick-mention-panel">
          <div v-if="mentionLoading" class="mention-empty">加载中…</div>
          <div v-else-if="!mentionCandidates.length" class="mention-empty">没有匹配的关注用户</div>
          <button
            v-for="user in mentionCandidates"
            :key="user.id"
            type="button"
            class="mention-item"
            @mousedown.prevent="selectMention(user)"
          >
            <span>{{ avatarText(user.avatar, user.nickname) }}</span>
            <b>{{ user.nickname || '灵感用户' }}</b>
            <small>已关注</small>
          </button>
        </div>
        <form class="quick-comment" @submit.prevent="submitComment">
          <input
            v-model="quickCommentText"
            :readonly="!isLogin"
            placeholder="说点什么，回车发送"
            maxlength="200"
            enterkeyhint="send"
            aria-label="快速评论"
            @focus="handleQuickCommentFocus"
            @input="onMentionInput($event, 'quick')"
            @keydown.esc="closeMention"
          >
        </form>
        <div class="mini-action-group">
          <button class="mini-action" type="button" title="引用再创作" @click="quoteCreate">
            <span>❝</span><small>引用</small>
          </button>
          <button class="mini-action" :class="{ active: liked }" type="button" title="点赞" @click="handleLike">
            <span>{{ liked ? '♥' : '♡' }}</span><small>{{ detail.likeCount ?? 0 }}</small>
          </button>
          <button class="mini-action" :class="{ active: collected }" type="button" title="收藏" @click="toggleCollect">
            <span>{{ collected ? '★' : '☆' }}</span><small>{{ detail.collectCount ?? 0 }}</small>
          </button>
        </div>
      </div>
    </AppState>

    <div v-if="showSharePanel" class="overlay" @click.self="showSharePanel = false">
      <div class="share-panel">
        <div class="share-header">分享灵感</div>
        <div class="share-count">已分享 {{ detail.shareCount ?? 0 }} 次</div>
        <div class="share-divider"></div>
        <button class="share-option" type="button" @click="copyShareLink">
          <span class="share-icon">⌁</span>
          <span>复制链接</span>
        </button>
        <button class="share-option" type="button" :disabled="posterBuilding" @click="openPosterTemplatePicker">
          <span class="share-icon">🖼</span>
          <span>生成分享海报</span>
        </button>
      </div>
    </div>

    <div
      v-if="posterTemplatePickerVisible"
      class="overlay poster-template-overlay"
      @click.self="posterTemplatePickerVisible = false"
    >
      <div class="poster-template-panel">
        <div class="poster-template-head">
          <div>
            <h3>选择海报样式</h3>
            <p>选择后生成带二维码的分享海报。</p>
          </div>
          <button type="button" aria-label="关闭" @click="posterTemplatePickerVisible = false">×</button>
        </div>
        <div class="poster-template-grid">
          <button
            v-for="template in posterTemplates"
            :key="template.id"
            class="poster-template-card"
            :class="{ selected: selectedPosterTemplate === template.id }"
            type="button"
            @click="selectedPosterTemplate = template.id"
          >
            <span class="poster-template-thumb" :class="`thumb-${template.id}`">
              <span class="thumb-line wide"></span>
              <span class="thumb-image"></span>
              <span class="thumb-line"></span>
              <span class="thumb-line short"></span>
              <span class="thumb-qr"></span>
            </span>
            <b>{{ template.name }}</b>
            <small>{{ template.description }}</small>
          </button>
        </div>
        <div class="poster-template-actions">
          <button class="ghost" type="button" :disabled="posterBuilding" @click="posterTemplatePickerVisible = false">取消</button>
          <button
            class="primary"
            type="button"
            :disabled="posterBuilding"
            @click="makePoster(selectedPosterTemplate)"
          >
            {{ posterBuilding ? '正在生成...' : '生成海报预览' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="posterVisible" class="overlay poster-overlay" @click.self="posterVisible = false">
      <div class="poster-panel">
        <img v-if="posterUrl" class="poster-img" :src="posterUrl" alt="分享海报">
        <div class="poster-actions">
          <a
            v-if="posterUrl"
            class="poster-btn primary"
            :href="posterUrl"
            :download="`inspire-${detail.id || 'poster'}.png`"
          >保存图片</a>
          <button class="poster-btn" type="button" @click="reopenPosterTemplatePicker">重新选择</button>
          <button class="poster-btn" type="button" @click="posterVisible = false">关闭</button>
        </div>
      </div>
    </div>
    <CollectFolderDialog
      v-model="collectDialogVisible"
      :target-id="detail.id"
      @collected="handleCollected"
    />
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {useAuthStore} from '@/stores/auth'
// 注意：qrcode 体积不小，改成命中「生成海报」时再动态加载，避免进详情页就打包进去
import {
  createComment,
  followUser,
  getCommentReplies,
  getComments,
  getFollowing,
  getInspireDetail,
  likeComment as likeCommentApi,
  likeInspire,
  shareInspire,
  uncollectInspire,
  unfollowUser,
  unlikeComment as unlikeCommentApi,
  unlikeInspire
} from '@/api/inspire.js'
import {sanitizeHtml} from '@/utils/sanitizeHtml.js'
import {thumbOf} from '@/utils/media.js'
import {getAccessToken} from '@/utils/tokenStorage.js'

// 海报封面代理：站外图片经本站转发，返回的响应带 ACAO，canvas 不会被跨域污染
const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api' : '/api'
const posterCoverProxy = (url) => `${API_BASE}/file/poster-cover?url=${encodeURIComponent(url)}`

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const FALLBACK_IMAGE = 'https://picsum.photos/id/102/900/650'
const DEFAULT_DESC = '把此刻的灵感慢慢写下来，给图片、文字和评论区留出舒服的呼吸感。'

const detail = ref({})
const detailLoading = ref(false)
const detailError = ref('')
const liked = ref(false)
const collected = ref(false)
const isFollowing = ref(false)
const showSharePanel = ref(false)
const collectDialogVisible = ref(false)
const posterVisible = ref(false)
const posterUrl = ref('')
const posterBuilding = ref(false)
const posterTemplatePickerVisible = ref(false)
const selectedPosterTemplate = ref('paper')
const posterTemplates = [
  { id: 'paper', name: '文艺纸感', description: '纸张底色 · 衬线标题' },
  { id: 'minimal', name: '极简留白', description: '白底 · 大留白' },
  { id: 'magazine', name: '杂志封面', description: '大图 · 视觉冲击' }
]
const activeImageIndex = ref(0)
const mainImageError = ref(false)
const authorAvatarError = ref(false)

const comments = ref([])
const allCommentRecords = ref([])
const commentPage = ref(1)
const commentTotal = ref(0)
const commentSort = ref('hot')
const commentLoading = ref(false)
const commentError = ref('')
const quickCommentText = ref('')
const replyText = ref('')
const replyTarget = ref(null)
const replyToUser = ref(null)
const submittingComment = ref(false)
const mentionOpen = ref(false)
const mentionLoading = ref(false)
const mentionLoaded = ref(false)
const mentionTarget = ref('')
const mentionStart = ref(0)
const mentionEnd = ref(0)
const mentionQuery = ref('')
const mentionUsers = ref([])
let detailRequestSeq = 0
let commentRequestSeq = 0
let loadingDetailId = ''

const isLogin = computed(() => Boolean(
  auth.isLogin
  || auth.userId
  || getAccessToken()
  || sessionStorage.getItem('isLogin') === '1'
))
const currentUserId = computed(() => (
  auth.userId || sessionStorage.getItem('userId') || ''
))
const detailState = computed(() => {
  if (detailLoading.value && !detail.value.id) return 'loading'
  if (detailError.value && !detail.value.id) return 'error'
  return 'ready'
})

const isOwnInspire = computed(() => {
  if (!detail.value.userId || !isLogin.value) return false
  let jwtUserId = ''
  const tokens = [getAccessToken(), sessionStorage.getItem('token')]
  for (const token of tokens) {
    if (!token) continue
    try {
      const payload = JSON.parse(atob(String(token).split('.')[1]))
      if (payload?.sub) {
        jwtUserId = String(payload.sub)
        break
      }
    } catch (e) {
      // 继续尝试下一个 token 来源
    }
  }
  const ownUserId = jwtUserId || String(currentUserId.value || '')
  return Boolean(ownUserId) && String(detail.value.userId) === ownUserId
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
const commentState = computed(() => {
  if (commentLoading.value && !comments.value.length) return 'loading'
  if (commentError.value && !comments.value.length) return 'error'
  return comments.value.length ? 'ready' : 'empty'
})
const loadedCommentCount = computed(() => comments.value.length)
const commentHasMore = computed(() => !commentLoading.value && comments.value.length < commentTotal.value)
const mentionCandidates = computed(() => {
  const keyword = mentionQuery.value.trim().toLowerCase()
  if (!keyword) return mentionUsers.value.slice(0, 10)
  return mentionUsers.value.filter(user => {
    const nickname = String(user.nickname || '').toLowerCase()
    return nickname.includes(keyword)
  }).slice(0, 10)
})

const isImageAvatar = (avatar) => typeof avatar === 'string'
  && (avatar.startsWith('http') || avatar.startsWith('/') || avatar.startsWith('data:'))

/** 判断媒体地址是否为视频，详情页据此渲染 <video> 播放器 */
const isVideo = (u) => /\.(mp4|webm|mov|m4v)(\?.*)?$/i.test(String(u || ''))

const quoteCreate = () => {
  if (!isLogin.value) return requireLogin()
  router.push({ path: '/create', query: { quoteId: String(detail.value.id) } })
}

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

const ensureMentionUsers = async () => {
  if (mentionLoaded.value || mentionLoading.value) return
  mentionLoading.value = true
  try {
    const res = await getFollowing()
    mentionUsers.value = res.data || []
    mentionLoaded.value = true
  } catch (e) {
    mentionUsers.value = []
  } finally {
    mentionLoading.value = false
  }
}

const closeMention = () => {
  mentionOpen.value = false
  mentionTarget.value = ''
  mentionQuery.value = ''
}

const onMentionInput = (event, target) => {
  const value = target === 'reply' ? replyText.value : quickCommentText.value
  const cursor = event.target.selectionStart ?? value.length
  const before = value.slice(0, cursor)
  const match = before.match(/@([^@\s]*)$/)
  if (!match) {
    closeMention()
    return
  }
  mentionOpen.value = true
  mentionTarget.value = target
  mentionStart.value = cursor - match[0].length
  mentionEnd.value = cursor
  mentionQuery.value = match[1]
  ensureMentionUsers()
}

const selectMention = (user) => {
  const target = mentionTarget.value
  const value = target === 'reply' ? replyText.value : quickCommentText.value
  const token = `@${user.nickname || '灵感用户'} `
  const next = value.slice(0, mentionStart.value) + token + value.slice(mentionEnd.value)
  if (target === 'reply') replyText.value = next
  else quickCommentText.value = next
  closeMention()
  nextTick(() => {
    const selector = target === 'reply' ? '.reply-box input' : '.quick-comment input'
    document.querySelector(selector)?.focus()
  })
}

const formatCommentTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const diffMin = Math.floor((now.getTime() - date.getTime()) / 60000)
  if (diffMin >= 0 && diffMin < 5) return '刚刚'
  if (diffMin >= 0 && diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour >= 0 && diffHour < 24 && date.toDateString() === now.toDateString()) {
    return `${diffHour}小时前`
  }
  const month = date.getMonth() + 1
  const day = date.getDate()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (date.getFullYear() === now.getFullYear()) return `${month}月${day}日 ${time}`
  return `${date.getFullYear()}年${month}月${day}日 ${time}`
}

/**
 * 游客触发需要登录的操作时统一处理：直接跳登录页，
 * 并把当前灵感写入 redirectPath，登录成功后自动回到这里。
 */
const requireLogin = () => {
  sessionStorage.setItem('redirectPath', detail.value.id ? `/detail/${detail.value.id}` : '/')
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
  if (commentSort.value === 'hot') {
    const likeDiff = Number(b.likeCount || 0) - Number(a.likeCount || 0)
    if (likeDiff !== 0) return likeDiff
  }
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
    root._visibleReplyCount = previous?.visibleCount || 0
    replies.forEach(reply => { reply._avatarErr = false })
  })

  comments.value = roots
}

const loadComments = async (reset = false, inspireIdOverride = null) => {
  const inspireId = String(inspireIdOverride || detail.value.id || '')
  if (!inspireId || commentLoading.value) return
  if (!isLogin.value) {
    comments.value = []
    allCommentRecords.value = []
    commentTotal.value = 0
    return
  }
  const requestSeq = ++commentRequestSeq
  commentLoading.value = true
  commentError.value = ''
  const targetPage = reset ? 1 : commentPage.value
  try {
    const res = await getComments(inspireId, {
      page: targetPage,
      size: 20,
      sort: commentSort.value
    })
    if (requestSeq !== commentRequestSeq
        || (!inspireIdOverride && String(detail.value.id) !== inspireId)) return
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
    if (requestSeq !== commentRequestSeq
        || (!inspireIdOverride && String(detail.value.id) !== inspireId)) return
    commentError.value = e?.message || 'load comments failed'
    ElMessage.error('评论加载失败')
  } finally {
    if (requestSeq === commentRequestSeq) {
      commentLoading.value = false
    }
  }
}

const resetDetailState = () => {
  detail.value = {}
  liked.value = false
  collected.value = false
  isFollowing.value = false
  activeImageIndex.value = 0
  mainImageError.value = false
  authorAvatarError.value = false
  comments.value = []
  allCommentRecords.value = []
  commentPage.value = 1
  commentTotal.value = 0
  commentSort.value = 'hot'
  commentLoading.value = false
  commentError.value = ''
}

const loadData = async (rawId, force = false) => {
  const id = String(rawId || '').trim()
  if (!id) {
    detailError.value = '灵感地址无效'
    return
  }
  if (!force && detailLoading.value && loadingDetailId === id) {
    return
  }
  loadingDetailId = id

  const requestSeq = ++detailRequestSeq
  commentRequestSeq += 1
  resetDetailState()
  detailLoading.value = true
  detailError.value = ''

  // 评论不依赖详情响应体，和详情请求并行，避免“详情返回后再等一轮评论”。
  if (isLogin.value) loadComments(true, id)

  try {
    const res = await getInspireDetail(id)
    if (requestSeq !== detailRequestSeq) return
    if (res?.code !== 200 || !res.data?.id) {
      throw new Error(res?.msg || '灵感不存在或已被删除')
    }
    detail.value = res.data
    liked.value = !!detail.value.liked
    collected.value = !!detail.value.collected
    isFollowing.value = !!detail.value.following
    detailLoading.value = false
  } catch (e) {
    if (requestSeq !== detailRequestSeq) return
    detailError.value = e?.response?.data?.msg || e?.message || '灵感加载失败'
    detailLoading.value = false
    return
  } finally {
    if (requestSeq === detailRequestSeq) {
      loadingDetailId = ''
    }
  }

  if (requestSeq !== detailRequestSeq) return
  authorAvatarError.value = false
}

const reloadDetail = () => {
  if (route.params.id) loadData(route.params.id, true)
}

watch(() => route.params.id, id => { if (id) loadData(id) }, { immediate: true })

const visibleReplies = (commentItem) => {
  if (!commentItem.replies?.length) return []
  if (!commentItem._repliesExpanded) return []
  const count = Math.min(commentItem._visibleReplyCount || 3, commentItem.replies.length)
  return commentItem.replies.slice(0, count)
}

const expandReplies = (commentItem) => {
  commentItem._repliesExpanded = true
  commentItem._visibleReplyCount = Math.min(3, commentItem.replies.length)
  if ((commentItem.replies?.length || 0) < Number(commentItem.replyCount || 0)) {
    loadMoreReplies(commentItem)
  }
}

const loadMoreReplies = (commentItem) => {
  if (commentItem._repliesLoading) return Promise.resolve()
  commentItem._repliesLoading = true
  const nextPage = commentItem._replyPage || 1
  return getCommentReplies(detail.value.id, commentItem.id, {
    page: nextPage,
    size: 20,
    sort: commentSort.value
  }).then(res => {
    if (res.code !== 200) throw new Error(res.msg || '回复加载失败')
    const rows = res.data?.records || []
    const merged = new Map(allCommentRecords.value.map(item => [String(item.id), item]))
    rows.forEach(item => merged.set(String(item.id), item))
    allCommentRecords.value = sortCommentRecords([...merged.values()])
    const previousPage = commentItem._replyPage || 1
    regroupComments()
    const root = comments.value.find(item => String(item.id) === String(commentItem.id))
    if (root) {
      root._repliesExpanded = true
      root._visibleReplyCount = root.replies?.length || 0
      root._replyPage = previousPage + 1
      root._repliesLoading = false
      root.replyCount = Number(res.data?.total || root.replyCount || root.replies?.length || 0)
    }
  }).catch(() => {
    ElMessage.error('回复加载失败')
    commentItem._repliesLoading = false
  })
}

const collapseReplies = (commentItem) => {
  commentItem._repliesExpanded = false
  commentItem._visibleReplyCount = 0
}

const replyTo = (commentItem, userInfo = null) => {
  closeMention()
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
  closeMention()
  replyTarget.value = null
  replyToUser.value = null
  replyText.value = ''
}

/**
 * 评论列表按热度分页时，新评论/新回复的点赞数最低，可能不在第一页。
 * 服务端创建成功后直接合并返回的记录，保证用户马上能看到自己的内容。
 */
const mergeCreatedComment = (created) => {
  if (!created?.id) return null
  const existed = allCommentRecords.value.some(item => String(item.id) === String(created.id))
  const isRootComment = !created.parentId || String(created.parentId) === '0'
  const record = { ...created, _avatarErr: false }
  const next = new Map(allCommentRecords.value.map(item => [String(item.id), item]))
  next.set(String(record.id), record)
  allCommentRecords.value = sortCommentRecords([...next.values()])
  if (!existed && isRootComment) commentTotal.value += 1
  regroupComments()

  const parentId = String(record.parentId || '0')
  const root = parentId === '0'
    ? comments.value.find(item => String(item.id) === String(record.id))
    : comments.value.find(item =>
        String(item.id) === parentId
        || item.replies?.some(reply => String(reply.id) === String(record.id))
      )
  if (root && parentId !== '0') {
    if (!existed) root.replyCount = Number(root.replyCount || root.replies?.length || 0) + 1
    root._repliesExpanded = true
    root._visibleReplyCount = root.replies?.length || 0
    root._replyPage = Math.max(1, Math.ceil(root.replies.length / 20))
  }
  return { record, root }
}

const focusCreatedComment = async (created, root) => {
  await nextTick()
  const targetId = String(created?.id || root?.id || '')
  const target = Array.from(document.querySelectorAll('[data-comment-id]'))
    .find(el => el.dataset.commentId === targetId)
  target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const submitComment = async () => {
  if (!isLogin.value) { requireLogin(); return }
  if (!quickCommentText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    closeMention()
    const res = await createComment(detail.value.id, { content: quickCommentText.value.trim() })
    if (res.code === 200) {
      quickCommentText.value = ''
      ElMessage.success('评论成功')
      const merged = mergeCreatedComment(res.data)
      if (merged?.record) {
        await focusCreatedComment(merged.record, merged.root)
      } else {
        await loadComments(true)
        await nextTick()
        document.querySelector('.comments-title')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    } else {
      ElMessage.error(res.msg || '评论失败')
    }
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value = false
  }
}

const submitReply = async (rootItem) => {
  if (!replyText.value.trim() || submittingComment.value) return
  submittingComment.value = true
  try {
    closeMention()
    const res = await createComment(detail.value.id, {
      content: replyText.value.trim(),
      parentId: rootItem.id,
      replyUserId: replyToUser.value?.userId || rootItem.userId,
      replyUsername: replyToUser.value?.nickname || rootItem.nickname || '灵感用户'
    })
    if (res.code === 200) {
      ElMessage.success('回复成功')
      cancelReply()
      const created = res.data
      await loadComments(true)
      let root = comments.value.find(item => String(item.id) === String(rootItem.id))
      if (!root) {
        root = comments.value.find(item =>
          item.replies?.some(reply => String(reply.id) === String(created?.id))
        )
      }
      if (root) {
        root._repliesExpanded = true
        root._visibleReplyCount = Math.min(root.replies?.length || 0, Number(root.replyCount || 0))
        if (created?.id && !root.replies?.some(reply => String(reply.id) === String(created.id))) {
          await loadMoreReplies(root)
          root = comments.value.find(item => String(item.id) === String(root.id)) || root
        }
      }
      await focusCreatedComment(created, root)
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

const changeCommentSort = (sort) => {
  if (commentSort.value === sort || commentLoading.value) return
  commentSort.value = sort
  commentPage.value = 1
  comments.value = []
  allCommentRecords.value = []
  loadComments(true)
}

const toggleCommentSort = () => {
  changeCommentSort(commentSort.value === 'hot' ? 'time' : 'hot')
}

onBeforeUnmount(() => {
  detailRequestSeq += 1
  commentRequestSeq += 1
})

const toggleCommentLike = async (commentItem) => {
  if (!isLogin.value) {
    requireLogin()
    return
  }
  const wasLiked = Boolean(commentItem.liked)
  const oldCount = Number(commentItem.likeCount || 0)
  commentItem.liked = !wasLiked
  commentItem.likeCount = Math.max(0, oldCount + (wasLiked ? -1 : 1))
  try {
    const res = wasLiked
      ? await unlikeCommentApi(detail.value.id, commentItem.id)
      : await likeCommentApi(detail.value.id, commentItem.id)
    if (res.code !== 200) throw new Error(res.msg || 'comment like failed')
  } catch (e) {
    commentItem.liked = wasLiked
    commentItem.likeCount = oldCount
    ElMessage.error('评论点赞失败')
  }
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
      collectDialogVisible.value = true
    }
  } catch (e) {}
}

const handleCollected = () => {
  if (!collected.value) {
    collected.value = true
    detail.value.collectCount = (detail.value.collectCount ?? 0) + 1
  }
}

const handleShare = async () => {
  // 分享计数接口需要登录，游客只打开分享面板，不要因为 401 被弹回登录页
  if (isLogin.value) {
    try {
      await shareInspire(detail.value.id)
      detail.value.shareCount = (detail.value.shareCount ?? 0) + 1
    } catch (e) {}
  }
  showSharePanel.value = true
}

const copyShareLink = () => {
  const url = `${window.location.origin}/#/detail/${detail.value.id}`
  navigator.clipboard.writeText(url).then(() => ElMessage.success('链接已复制'))
  showSharePanel.value = false
}

const openPosterTemplatePicker = () => {
  showSharePanel.value = false
  posterVisible.value = false
  posterTemplatePickerVisible.value = true
}

const reopenPosterTemplatePicker = () => {
  posterVisible.value = false
  posterTemplatePickerVisible.value = true
}

/* ==================== 分享海报（纯前端 Canvas 生成） ==================== */

const POSTER_W = 1080
const POSTER_H = 1440

/** 以 cover 方式把图片裁切铺满目标区域 */
const drawImageCover = (ctx, img, x, y, w, h) => {
  const scale = Math.max(w / img.width, h / img.height)
  const sw = w / scale
  const sh = h / scale
  const sx = (img.width - sw) / 2
  const sy = (img.height - sh) / 2
  ctx.drawImage(img, sx, sy, sw, sh, x, y, w, h)
}

const roundRectPath = (ctx, x, y, w, h, r) => {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

/** 按最大宽度折行，超出 maxLines 时补省略号 */
const wrapText = (ctx, text, maxWidth, maxLines) => {
  const chars = Array.from(String(text || '').replace(/\s+/g, ' ').trim())
  const lines = []
  let current = ''
  for (const ch of chars) {
    const test = current + ch
    if (ctx.measureText(test).width > maxWidth && current) {
      lines.push(current)
      current = ch
      if (lines.length === maxLines) break
    } else {
      current = test
    }
  }
  if (lines.length < maxLines && current) lines.push(current)
  if (lines.length === maxLines) {
    let last = lines[maxLines - 1]
    const rest = chars.slice(lines.join('').length)
    if (rest.length > 0) {
      while (ctx.measureText(last + '…').width > maxWidth && last.length > 1) {
        last = last.slice(0, -1)
      }
      lines[maxLines - 1] = last + '…'
    }
  }
  return lines
}

const loadImage = (src) => new Promise((resolve) => {
  if (!src) { resolve(null); return }
  const img = new Image()
  // 只有跨域图片才需要 crossOrigin。同源图片（例如本站 /uploads/xxx.jpg）设了反而会
  // 触发一次 CORS 校验，服务端没回 ACAO 时图片会直接加载失败，海报里就只剩渐变底色。
  try {
    const abs = new URL(src, window.location.href)
    if (abs.origin !== window.location.origin) img.crossOrigin = 'anonymous'
  } catch (e) {
    img.crossOrigin = 'anonymous'
  }
  img.onload = () => resolve(img)
  img.onerror = () => resolve(null)
  img.src = src
})

/**
 * 海报封面加载：优先直连；如果图源不支持跨域（canvas 会被污染，导出会失败），
 * 就走后端 /file/upload-from-url 把图抓成自己站内的副本再画，保证海报一定能导出。
 */
const loadPosterCover = async (src) => {
  if (!src) return null
  try {
    const abs = new URL(src, window.location.href)
    // 站外图源统一走本站代理接口，CORS 头由我们自己控制，
    // 不再依赖 picsum / 各家 CDN 在不同网络下是否返回 ACAO（游客也可用）
    if (abs.origin !== window.location.origin) {
      const proxied = await loadImage(posterCoverProxy(src))
      if (proxied) return proxied
    }
  } catch (e) {
    console.warn('[poster] 封面代理失败，回退直连', e)
  }
  return await loadImage(src)
}

const makeModernPoster = async (template) => {
  if (posterBuilding.value) return
  posterBuilding.value = true
  try {
    const canvas = document.createElement('canvas')
    canvas.width = POSTER_W
    canvas.height = POSTER_H
    const ctx = canvas.getContext('2d')
    const coverSrc = imageList.value.find(u => !isVideo(u)) || ''
    const cover = await loadPosterCover(coverSrc)
    const title = detail.value.title || '未命名灵感'
    const plain = String(detail.value.content || DEFAULT_DESC).replace(/\s+/g, ' ')
    const authorName = detail.value.nickname || '灵感创作者'
    const tag = tagList.value[0] || '灵感'
    const shareUrl = `${window.location.origin}/#/detail/${detail.value.id}`
    const { default: QRCode } = await import('qrcode')
    const qrDataUrl = await QRCode.toDataURL(shareUrl, {
      width: 320,
      margin: 0,
      color: { dark: '#111715', light: '#ffffff' }
    })
    const qrImg = await loadImage(qrDataUrl)

    const drawQr = (x, y, size) => {
      if (!qrImg) return
      ctx.fillStyle = '#ffffff'
      roundRectPath(ctx, x - 12, y - 12, size + 24, size + 24, 14)
      ctx.fill()
      ctx.drawImage(qrImg, x, y, size, size)
    }

    if (template === 'minimal') {
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, POSTER_W, POSTER_H)
      ctx.fillStyle = '#a0a6a2'
      ctx.font = '500 24px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText('LINGGANGAN · INSPIRE DAILY', 80, 100)

      ctx.save()
      roundRectPath(ctx, 80, 160, 920, 650, 8)
      ctx.clip()
      if (cover) drawImageCover(ctx, cover, 80, 160, 920, 650)
      else { ctx.fillStyle = '#e7ecea'; ctx.fillRect(80, 160, 920, 650) }
      ctx.restore()

      ctx.fillStyle = '#202522'
      ctx.font = '500 58px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      let cursorY = 905
      wrapText(ctx, title, 920, 2).forEach((line, i) => ctx.fillText(line, 80, cursorY + i * 72))
      cursorY += 150

      ctx.fillStyle = '#8d9490'
      ctx.font = '400 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText(`${tag} · ${authorName} · ${publishText.value}`, 80, cursorY)
      cursorY += 58
      ctx.fillStyle = '#555e5a'
      ctx.font = '400 29px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      wrapText(ctx, plain, 920, 2).forEach((line, i) => ctx.fillText(line, 80, cursorY + i * 46))

      ctx.strokeStyle = '#e8ecea'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(80, 1270)
      ctx.lineTo(1000, 1270)
      ctx.stroke()
      ctx.fillStyle = '#9aa19d'
      ctx.font = '400 23px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText('扫码查看完整灵感', 80, 1340)
      drawQr(875, 1220, 150)
    } else {
      if (cover) drawImageCover(ctx, cover, 0, 0, POSTER_W, POSTER_H)
      else { ctx.fillStyle = '#27322f'; ctx.fillRect(0, 0, POSTER_W, POSTER_H) }
      const shade = ctx.createLinearGradient(0, POSTER_H * .35, 0, POSTER_H)
      shade.addColorStop(0, 'rgba(4,8,7,0)')
      shade.addColorStop(1, 'rgba(4,8,7,.88)')
      ctx.fillStyle = shade
      ctx.fillRect(0, 0, POSTER_W, POSTER_H)

      ctx.fillStyle = '#ffffff'
      ctx.font = '700 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText('INSPIRE DAILY', 70, 95)
      ctx.textAlign = 'right'
      ctx.fillText('ISSUE 09', POSTER_W - 70, 95)
      ctx.textAlign = 'left'

      ctx.fillStyle = '#f3c56b'
      ctx.font = '800 22px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      const tagW = ctx.measureText(tag.toUpperCase()).width + 40
      ctx.fillRect(70, 840, Math.max(tagW, 150), 46)
      ctx.fillStyle = '#261d0d'
      ctx.fillText(tag.toUpperCase(), 88, 872)

      ctx.fillStyle = '#ffffff'
      ctx.font = '800 60px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      let cursorY = 960
      wrapText(ctx, title, 880, 2).forEach((line, i) => ctx.fillText(line, 70, cursorY + i * 76))
      cursorY += 165
      ctx.fillStyle = 'rgba(255,255,255,.82)'
      ctx.font = '400 29px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      wrapText(ctx, plain, 880, 2).forEach((line, i) => ctx.fillText(line, 70, cursorY + i * 46))

      ctx.strokeStyle = 'rgba(255,255,255,.32)'
      ctx.beginPath()
      ctx.moveTo(70, 1265)
      ctx.lineTo(1010, 1265)
      ctx.stroke()
      ctx.fillStyle = 'rgba(255,255,255,.88)'
      ctx.font = '500 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText(authorName, 70, 1340)
      drawQr(875, 1215, 150)
    }

    posterUrl.value = canvas.toDataURL('image/png')
    posterVisible.value = true
    posterTemplatePickerVisible.value = false
    showSharePanel.value = false
  } catch (e) {
    console.error('[poster]', e)
    ElMessage.error('海报生成失败，请稍后再试')
  } finally {
    posterBuilding.value = false
  }
}

const makePoster = async (template = 'paper') => {
  if (template !== 'paper') {
    await makeModernPoster(template)
    return
  }
  if (posterBuilding.value) return
  posterBuilding.value = true
  try {
    const canvas = document.createElement('canvas')
    canvas.width = POSTER_W
    canvas.height = POSTER_H
    const ctx = canvas.getContext('2d')

    // 纸张底色 + 细边框
    const bg = ctx.createLinearGradient(0, 0, POSTER_W, POSTER_H)
    bg.addColorStop(0, '#fdfaf5')
    bg.addColorStop(1, '#eef5ea')
    ctx.fillStyle = bg
    ctx.fillRect(0, 0, POSTER_W, POSTER_H)
    ctx.strokeStyle = 'rgba(79,138,72,.35)'
    ctx.lineWidth = 3
    roundRectPath(ctx, 32, 32, POSTER_W - 64, POSTER_H - 64, 36)
    ctx.stroke()

    const pad = 84
    const contentW = POSTER_W - pad * 2

    // 顶部标识
    ctx.fillStyle = '#4f8a48'
    ctx.font = '600 30px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    ctx.fillText('灵感手账 · INSPIRE DAILY', pad, 140)

    // 主图
    const imgY = 180
    const imgH = 560
    // 海报封面优先取图片，避免视频首帧取不到
    const coverSrc = imageList.value.find(u => !isVideo(u)) || ''
    const cover = await loadPosterCover(coverSrc)
    ctx.save()
    roundRectPath(ctx, pad, imgY, contentW, imgH, 32)
    ctx.clip()
    if (cover) {
      drawImageCover(ctx, cover, pad, imgY, contentW, imgH)
    } else {
      const g = ctx.createLinearGradient(pad, imgY, pad + contentW, imgY + imgH)
      g.addColorStop(0, '#cfe4c8')
      g.addColorStop(1, '#f3ddc4')
      ctx.fillStyle = g
      ctx.fillRect(pad, imgY, contentW, imgH)
    }
    ctx.restore()

    // 标签
    let cursorY = imgY + imgH + 76
    const tag = tagList.value[0]
    if (tag) {
      ctx.font = '600 26px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      const tagW = ctx.measureText('#' + tag).width + 44
      ctx.fillStyle = '#e6f2e4'
      roundRectPath(ctx, pad, cursorY - 34, tagW, 52, 26)
      ctx.fill()
      ctx.fillStyle = '#4f8a48'
      ctx.fillText('#' + tag, pad + 22, cursorY + 2)
    }

    // 标题
    cursorY += 96
    ctx.fillStyle = '#3f362c'
    ctx.font = '700 54px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    const titleLines = wrapText(ctx, detail.value.title || '未命名灵感', contentW, 2)
    titleLines.forEach((line, i) => ctx.fillText(line, pad, cursorY + i * 72))
    cursorY += titleLines.length * 72 + 26

    // 正文摘要
    ctx.fillStyle = '#6b5c4c'
    ctx.font = '400 30px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    const plain = String(detail.value.content || DEFAULT_DESC).replace(/\s+/g, ' ')
    const descLines = wrapText(ctx, plain, contentW, 2)
    descLines.forEach((line, i) => ctx.fillText(line, pad, cursorY + i * 46))

    // 底部：作者 + 二维码
    const footY = POSTER_H - 250
    ctx.strokeStyle = 'rgba(79,138,72,.28)'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(pad, footY - 50)
    ctx.lineTo(POSTER_W - pad, footY - 50)
    ctx.stroke()

    const authorName = detail.value.nickname || '灵感创作者'
    ctx.fillStyle = '#3f362c'
    ctx.font = '600 34px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    ctx.fillText(authorName, pad, footY + 20)
    ctx.fillStyle = '#93826f'
    ctx.font = '400 26px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    ctx.fillText(publishText.value, pad, footY + 68)

    // 二维码
    const shareUrl = `${window.location.origin}/#/detail/${detail.value.id}`
    const { default: QRCode } = await import('qrcode')
    const qrDataUrl = await QRCode.toDataURL(shareUrl, {
      width: 300,
      margin: 0,
      color: { dark: '#3f362c', light: '#ffffff' }
    })
    const qrImg = await loadImage(qrDataUrl)
    const qrSize = 180
    const qrX = POSTER_W - pad - qrSize
    const qrY = footY - 30
    if (qrImg) {
      ctx.fillStyle = '#ffffff'
      roundRectPath(ctx, qrX - 14, qrY - 14, qrSize + 28, qrSize + 28, 18)
      ctx.fill()
      ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize)
    }
    ctx.fillStyle = '#93826f'
    ctx.font = '400 24px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('扫码查看灵感', qrX + qrSize / 2, qrY + qrSize + 46)
    ctx.textAlign = 'left'

    try {
      posterUrl.value = canvas.toDataURL('image/png')
    } catch (err) {
      // 画布被跨域图片污染时无法导出，给出明确提示而不是静默失败
      console.error('[poster] 导出失败', err)
      ElMessage.error('这张封面图不支持导出，已跳过图片，请重试')
      return
    }
    posterVisible.value = true
    posterTemplatePickerVisible.value = false
    showSharePanel.value = false
  } catch (e) {
    ElMessage.error('海报生成失败，请稍后再试')
    console.error('[poster]', e)
  } finally {
    posterBuilding.value = false
  }
}

const handleEdit = () => router.push({ name: 'Edit', params: { id: detail.value.id } })
const goBack = () => {
  const historyBack = window.history.state?.back
  const safeInternalHistory = typeof historyBack === 'string'
    && (historyBack.startsWith('/') || historyBack.startsWith('#'))

  let sameOriginReferrer = false
  try {
    sameOriginReferrer = Boolean(document.referrer)
      && new URL(document.referrer).origin === window.location.origin
  } catch (e) {
    sameOriginReferrer = false
  }

  if (safeInternalHistory || sameOriginReferrer) {
    router.back()
    return
  }
  router.replace('/')
}

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
  border: 1px solid rgba(168, 105, 58, .42);
  border-radius: 50%;
  color: #8b5732;
  background: rgba(255, 249, 240, .82);
  font-size: 21px;
  line-height: 1;
  cursor: pointer;
  transition: background .2s ease, border-color .2s ease;
}

.round-button:hover {
  border-color: rgba(168, 105, 58, .72);
  background: #fffaf3;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-edit-button {
  height: 38px;
  padding: 0 13px;
  border: 1px solid rgba(168, 105, 58, .42);
  border-radius: 999px;
  color: #8b5732;
  background: rgba(255, 249, 240, .82);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background .2s ease, border-color .2s ease;
}

.top-edit-button:hover {
  border-color: rgba(168, 105, 58, .72);
  background: #fffaf3;
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

/* 视频不裁切，保留完整画面 */
.gallery-main.video-main {
  object-fit: contain;
  background: #000;
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
.quote-entry {
  width:100%; margin-top:15px; padding:10px 12px; display:flex; align-items:center; gap:10px;
  border:1px solid #e6ddd4; border-radius:14px; background:#fffaf5; color:#674026;
  text-align:left; font:inherit; cursor:pointer;
}
.quote-entry img { width:48px; height:48px; flex:0 0 auto; border-radius:10px; object-fit:cover; }
.quote-copy { flex:1; min-width:0; }
.quote-copy small { display:block; margin-bottom:4px; color:#a17b5c; font-size:10.5px; }
.quote-copy b { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12.5px; }
.quote-arrow { color:#b19478; font-size:20px; }
.series-entry {
  width:100%; margin-top:16px; padding:12px 14px; display:flex; align-items:center; gap:10px;
  border:1px solid #cfe6e1; border-radius:14px; background:#f2faf8; color:#0f766e;
  text-align:left; font:inherit; cursor:pointer;
}
.series-entry-label { flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:13px; font-weight:700; }
.series-entry b { font-size:12px; }
.series-entry > span:last-child { color:#6d8e88; font-size:11px; }
.series-nav { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:18px; }
.series-nav button { min-width:0; padding:12px; border:1px solid #dcebe8; border-radius:13px; background:#f8fcfb; text-align:left; cursor:pointer; }
.series-nav small { display:block; color:#8a9b98; font-size:10px; margin-bottom:4px; }
.series-nav b { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#315f5a; font-size:12px; }

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

.comments-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 28px;
}

.comments-title { margin: 0; font-size: 15px; font-weight: 700; }

.comment-sort-toggle {
  height: auto;
  padding: 0;
  border: 0;
  background: transparent;
  color: #82908b;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.4;
  cursor: pointer;
}

.comment-sort-toggle:disabled { opacity: .55; cursor: default; }

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
  /* 长列表优化：评论是变高元素，用 content-visibility 让浏览器跳过屏外条目的
     渲染与布局（等价于浏览器内置的懒渲染），200+ 条评论滚动也不会卡。
     contain-intrinsic-size 提供占位高度，避免滚动条跳动。 */
  content-visibility: auto;
  contain-intrinsic-size: auto 130px;
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
.reply-to {
  display: inline;
  padding: 0;
  border-radius: 0;
  color: #4f7d73;
  background: transparent;
  font-weight: 600;
}

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

.reply-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
}

.reply-box { position: relative; display: flex; gap: 7px; margin-top: 11px; }

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
.comment-load-btn {
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid #e8bd94;
  border-radius: 999px;
  background: #fff8f0;
  color: #a85b25;
  font: inherit;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}
.comment-load-btn:active { transform: scale(.985); }

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 40;
  width: min(100%, 860px);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px 11px;
  transform: translateX(-50%);
  border-top: 1px dashed #d9905d;
  background: rgba(255, 239, 218, .97);
  backdrop-filter: blur(12px);
}

.quick-comment {
  flex: 1;
  min-width: 0;
  height: 42px;
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
.mini-action-group { display: flex; align-items: center; gap: 6px; flex: 0 0 auto; }
.mini-action {
  width: 42px;
  height: 42px;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
  border: 1px solid rgba(214, 159, 112, .5);
  border-radius: 12px;
  background: rgba(255, 255, 255, .78);
  color: #795238;
  font: inherit;
  cursor: pointer;
}
.mini-action span { font-size: 17px; line-height: 1; }
.mini-action small { margin-top: 2px; font-size: 9px; line-height: 1; }
.mini-action.active { border-color:#e67833; background:#fff4e7; color:#c85b1d; }
.mention-panel {
  position: absolute;
  z-index: 80;
  left: 14px;
  right: 14px;
  bottom: calc(100% + 8px);
  max-height: 236px;
  overflow: auto;
  padding: 7px;
  border: 1px solid #dcebe8;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 16px 38px rgba(34, 70, 62, .18);
}
.reply-mention-panel {
  left: 0;
  right: 0;
  bottom: calc(100% + 7px);
}
.mention-empty { padding: 22px 12px; text-align: center; color: #93a5a1; font-size: 12px; }
.mention-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #294a45;
  text-align: left;
  cursor: pointer;
}
.mention-item:hover,
.mention-item:focus-visible { outline: 0; background: #f2faf8; }
.mention-item > span {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #e8f6f2;
  color: #0f766e;
  font-size: 13px;
}
.mention-item b { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12.5px; }
.mention-item small { color: #9aaba7; font-size: 10.5px; }

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

.poster-template-overlay {
  z-index: 120;
}

.poster-template-panel {
  width: 100%;
  max-width: 620px;
  padding: 20px;
  border: 2px solid #e67833;
  border-radius: 22px 22px 0 0;
  background: #fff8ec;
}

.poster-template-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.poster-template-head h3 {
  margin: 0 0 4px;
  color: #674026;
  font-size: 18px;
}

.poster-template-head p {
  margin: 0;
  color: #9a7653;
  font-size: 12px;
}

.poster-template-head button {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 50%;
  background: #f3e5d5;
  color: #8b5732;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.poster-template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 9px;
  margin-top: 16px;
}

.poster-template-card {
  padding: 7px;
  border: 1px solid #ead9c5;
  border-radius: 14px;
  background: #fff;
  color: #674026;
  text-align: left;
  cursor: pointer;
}

.poster-template-card.selected {
  border-color: #e67833;
  box-shadow: 0 0 0 2px rgba(230, 120, 51, .18);
}

.poster-template-card b {
  display: block;
  margin: 7px 1px 2px;
  font-size: 12.5px;
}

.poster-template-card small {
  display: block;
  color: #a1846d;
  font-size: 9.5px;
  line-height: 1.35;
}

.poster-template-thumb {
  position: relative;
  display: block;
  aspect-ratio: 3/4;
  overflow: hidden;
  border-radius: 9px;
  background: #f4ead9;
}

.poster-template-thumb .thumb-line,
.poster-template-thumb .thumb-image,
.poster-template-thumb .thumb-qr {
  position: absolute;
  display: block;
}

.poster-template-thumb .thumb-line {
  left: 9px;
  bottom: 42px;
  width: 45%;
  height: 4px;
  border-radius: 4px;
  background: #8b5732;
}

.poster-template-thumb .thumb-line.wide {
  top: 8px;
  bottom: auto;
  width: 34%;
  height: 3px;
  opacity: .55;
}

.poster-template-thumb .thumb-line.short {
  bottom: 30px;
  width: 62%;
  opacity: .45;
}

.poster-template-thumb .thumb-image {
  top: 26px;
  left: 9px;
  right: 9px;
  height: 48%;
  border-radius: 6px;
  background:
    linear-gradient(145deg, rgba(109, 151, 126, .88), rgba(102, 86, 61, .7)),
    #8cae98;
}

.poster-template-thumb .thumb-qr {
  right: 9px;
  bottom: 9px;
  width: 28px;
  height: 28px;
  border: 3px solid #fff;
  background:
    repeating-linear-gradient(90deg, #202522 0 3px, #fff 3px 6px),
    repeating-linear-gradient(0deg, #202522 0 3px, #fff 3px 6px);
  background-blend-mode: multiply;
}

.thumb-minimal {
  background: #fff;
}

.thumb-minimal .thumb-line {
  background: #606864;
}

.thumb-minimal .thumb-image {
  background: linear-gradient(145deg, #d6d9d7, #8e9893);
}

.thumb-magazine {
  background: #111;
}

.thumb-magazine .thumb-line {
  background: #fff;
}

.thumb-magazine .thumb-image {
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  border-radius: 0;
  background:
    linear-gradient(180deg, rgba(0,0,0,.05), rgba(0,0,0,.8)),
    linear-gradient(145deg, #576a63, #151a18);
}

.thumb-magazine .thumb-line.wide {
  top: 9px;
}

.thumb-magazine .thumb-line {
  bottom: 43px;
}

.thumb-magazine .thumb-line.short {
  bottom: 31px;
}

.poster-template-actions {
  display: flex;
  gap: 9px;
  margin-top: 16px;
}

.poster-template-actions button {
  flex: 1;
  height: 42px;
  border: 0;
  border-radius: 13px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.poster-template-actions .ghost {
  background: #eee3d5;
  color: #765d49;
}

.poster-template-actions .primary {
  background: #e67833;
  color: #fff7ed;
}

.poster-template-actions button:disabled {
  opacity: .55;
  cursor: default;
}

/* 分享海报预览 */
.poster-overlay { align-items: flex-start; }
.poster-panel {
  width: min(92vw, 420px);
  max-height: 88vh;
  margin: auto;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 249, 240, .96);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.poster-img {
  width: 100%;
  max-height: 68vh;
  object-fit: contain;
  border-radius: 14px;
  background: #fdfaf5;
}
.poster-actions { display: flex; gap: 8px; justify-content: center; }
.poster-btn {
  flex: 1;
  padding: 10px 6px;
  border: 0;
  border-radius: 999px;
  background: #f0e6d8;
  color: #6b5745;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 700;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
}
.poster-btn.primary { background: #4f8a48; color: #f3faf2; }

@media (max-width: 640px) {
  .detail-page { max-width: none; box-shadow: none; }
  .topbar { height: 58px; padding: 0 14px; }
  .gallery { padding: 12px 14px 0; }
  .gallery-main { height: 250px; border-radius: 16px; }
  .article { padding: 22px 14px 0; }
  h1 { font-size: 26px; }
  .lead { font-size: 14.5px; }
  .replies { margin-left: 10px; padding: 9px 10px; }
  .bottom-action-bar { gap: 6px; padding: 8px 9px 10px; }
  .quick-comment { height: 42px; }
  .mini-action-group { gap: 5px; }
  .mini-action { width: 39px; height: 42px; }
}
</style>
