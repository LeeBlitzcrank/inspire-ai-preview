<!--
  文件：frontend/src/pages/InspireDetail.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
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

      <DetailComments
        :detail="detail"
        :is-login="isLogin"
        :liked="liked"
        :collected="collected"
        @require-login="requireLogin"
        @quote="quoteCreate"
        @like="handleLike"
        @collect="toggleCollect"
      />
      </article>

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
      @click.self="setPosterTemplatePickerVisible(false)"
    >
      <div class="poster-template-panel">
        <div class="poster-template-head">
          <div>
            <h3>选择海报样式</h3>
            <p>选择后生成带二维码的分享海报。</p>
          </div>
          <button type="button" aria-label="关闭" @click="setPosterTemplatePickerVisible(false)">×</button>
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
          <button class="ghost" type="button" :disabled="posterBuilding" @click="setPosterTemplatePickerVisible(false)">取消</button>
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

    <div v-if="posterVisible" class="overlay poster-overlay" @click.self="setPosterVisible(false)">
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
          <button class="poster-btn" type="button" @click="setPosterVisible(false)">关闭</button>
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
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {useAuthStore} from '@/stores/auth'
import {useSharePoster} from './detail/composables/useSharePoster.js'
// 注意：qrcode 体积不小，改成命中「生成海报」时再动态加载，避免进详情页就打包进去
import {
  followUser,
  getInspireDetail,
  likeInspire,
  shareInspire,
  uncollectInspire,
  unfollowUser,
  unlikeInspire
} from '@/api/inspire.js'
import {sanitizeHtml} from '@/utils/sanitizeHtml.js'
import DetailComments from './detail/components/DetailComments.vue'
import {formatRelativeTime} from '@/utils/time.js'
import {thumbOf} from '@/utils/media.js'
import {getAccessToken} from '@/utils/tokenStorage.js'

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
const activeImageIndex = ref(0)
const mainImageError = ref(false)
const authorAvatarError = ref(false)

let detailRequestSeq = 0
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

/** 判断媒体地址是否为视频，详情页据此渲染 <video> 播放器 */
const isVideo = (u) => /\.(mp4|webm|mov|m4v)(\?.*)?$/i.test(String(u || ''))

const {
  posterVisible,
  posterUrl,
  posterBuilding,
  posterTemplatePickerVisible,
  posterTemplates,
  selectedPosterTemplate,
  makePoster
} = useSharePoster({detail, imageList, isVideo, tagList, publishText, showSharePanel, DEFAULT_DESC})

const setPosterVisible = (value) => { posterVisible.value = value }
const setPosterTemplatePickerVisible = (value) => { posterTemplatePickerVisible.value = value }

const isImageAvatar = (avatar) => typeof avatar === 'string'
  && (avatar.startsWith('http') || avatar.startsWith('/') || avatar.startsWith('data:'))

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

const resetDetailState = () => {
  detail.value = {}
  liked.value = false
  collected.value = false
  isFollowing.value = false
  activeImageIndex.value = 0
  mainImageError.value = false
  authorAvatarError.value = false
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
  resetDetailState()
  detailLoading.value = true
  detailError.value = ''

  // 评论不依赖详情响应体，和详情请求并行，避免“详情返回后再等一轮评论”。

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

const handleLike = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (liked.value) {
      const res = await unlikeInspire(detail.value.id)
      if (res.code === 200) {
        liked.value = false
        detail.value.likeCount = Math.max(0, (detail.value.likeCount ?? 0) - 1)
        ElMessage.success('已取消点赞')
      } else {
        ElMessage.error(res.msg || '取消点赞失败')
      }
    } else {
      const res = await likeInspire(detail.value.id)
      if (res.code === 200) {
        liked.value = true
        detail.value.likeCount = (detail.value.likeCount ?? 0) + 1
        ElMessage.success('点赞成功')
      } else {
        ElMessage.error(res.msg || '点赞失败')
      }
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '点赞失败')
  }
}

const toggleCollect = async () => {
  if (!isLogin.value) { requireLogin(); return }
  try {
    if (collected.value) {
      const res = await uncollectInspire(detail.value.id)
      if (res.code === 200) {
        collected.value = false
        detail.value.collectCount = Math.max(0, (detail.value.collectCount ?? 0) - 1)
        ElMessage.success('已取消收藏')
      } else {
        ElMessage.error(res.msg || '取消收藏失败')
      }
    } else {
      collectDialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '取消收藏失败')
  }
}

const handleCollected = () => {
  if (!collected.value) {
    collected.value = true
    detail.value.collectCount = (detail.value.collectCount ?? 0) + 1
  }
  ElMessage.success('收藏成功')
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

<style scoped src="./detail/styles/inspire-detail.css"></style>
