<template>
  <div id="index-root" class="index-page">
    <div id="index-top-nav" class="top-nav">
      <div id="nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="nav-icon-group" class="right-icons">
        <div id="icon-create" class="icon-item" @click="goCreate">✨</div>
        <div id="icon-search" class="icon-item" @click="$router.push('/search')">🔍</div>
        <div id="icon-rag" class="icon-item" @click="$router.push('/rag')">🧠</div>
        <div v-if="isLogin" id="icon-noti" class="icon-item" @click="goNotifications">🔔<span v-if="unreadCount > 0" class="noti-badge">{{ unreadCount > 99 ? "99+" : unreadCount }}</span></div>
        <div id="icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>
    <!-- 选项卡 -->
    <div class="tab-bar">
     <span class="tab-item" :class="{active: activeTab==='recommend'}" @click="switchTab('recommend')">👇 推荐卡片</span>
     <span class="tab-item" :class="{active: activeTab==='category'}" @click="switchTab('category')">📋 灵感分类</span>
   </div>

    <div v-if="activeTab === 'category'">
    <!-- 一级分类 -->
    <transition name="slide-fade">
      <div id="category-grid-wrap" class="category-grid" v-if="!activeCategory">
        <AppCard class="category-card" v-for="(item, idx) in categoryList" :key="item.id"
          padding="32px 14px"
          clickable
          @click="handleClickCategory(item)" :style="{'--idx': idx}">
          <div class="cate-icon">{{ item.icon }}</div>
          <div class="cate-name">{{ item.name }}</div>
          <div class="cate-num">{{ item.count }}条灵感</div>
   </AppCard>
      </div>
    </transition>

    <!-- 二级标签 -->
    <transition name="slide-fade">
      <div id="sub-tag-wrap" v-if="activeCategory && !activeSubItem" class="sub-wrap">
        <div id="back-to-category" class="back-btn" @click="resetCategory">← 返回全部分类</div>
        <div id="sub-tag-list" class="sub-tag-list">
          <AppCard class="sub-tag" v-for="item in currentSubList" :key="item.id"
            padding="32px 14px" clickable @click="selectSubItem(item)">{{ item.name }}</AppCard>
        </div>
        <div id="sub-empty-tip" v-if="currentSubList.length === 0" class="empty-sub">暂无子分类</div>
      </div>
    </transition>

    <!-- 灵感列表 -->
    <transition name="slide-fade">
      <div id="detail-inspire-wrap" v-if="activeSubItem" class="detail-wrap">
        <div id="back-to-subtag" class="back-btn" @click="resetSubItem">← 返回{{ activeCategory }}</div>
        <AppState :state="listState" :rows="4" empty-icon="📭"
          empty-text="还没有灵感，去其他分类看看吧" error-text="灵感加载失败"
          @retry="loadInspireList">
        <div id="inspire-card-list" class="list-wrap">
          <InspireCard v-for="item in inspireList" :key="item.id" :item="item"
            :style="{'--idx': item.id}" @collect="handleCollect" @click-card="goDetail" />
        </div>
        <div v-if="loading && inspireList.length > 0" class="empty-sub" style="color:#409eff">加载更多...</div>
        <div v-if="!hasMore && inspireList.length > 0" class="empty-sub" style="color:#ccc">-- 没有更多了 --</div>
        </AppState>
      </div>
    </transition>
    </div>
  <!-- 关注：关注的人列表 / 灵感列表 -->
  <div v-if="activeTab === 'following'" class="feed-list">
    <div v-if="!selectedFollowee" class="following-list">
      <AppState :state="followingState" :rows="4" empty-icon="💭"
        empty-text="还没有关注人" error-text="关注列表加载失败"
        @retry="loadFollowing">
      <AppCard v-for="u in followingList" :key="u.id" class="follow-user-card"
        padding="14px 16px" clickable @click="selectFollowee(u)">
        <span class="follow-user-avatar">{{ u.avatar || (u.nickname ? u.nickname[0] : '👤') }}</span>
        <div class="follow-user-info">
          <div class="follow-user-name">{{ u.nickname || '灵感用户' }}</div>
        </div>
        <span class="follow-user-arrow" @click.stop="handleMsgFromFollow(u)" style="color:#6366f1;font-size:12px;border:1px solid #6366f1;border-radius:8px;padding:3px 8px;margin-right:6px;cursor:pointer;">💬 私信</span>
        <span class="follow-user-arrow" style="font-size:18px;">›</span>
      </AppCard>
      </AppState>
    </div>
    <div v-if="selectedFollowee">
      <div class="back-btn" @click="selectedFollowee = null; inspireList=[]; loadInspireList()">&larr; 返回关注列表</div>
      <AppState :state="listState" :rows="4" empty-icon="💭" empty-text="暂无内容"
        error-text="内容加载失败" @retry="loadInspireList">
        <InspireCard v-for="item in inspireList" :key="item.id" :item="item" @collect="handleCollect" />
        <div v-if="loading && inspireList.length > 0" class="empty-sub" style="color:#666;padding:30px 0">⏳ 加载中...</div>
        <div v-if="!hasMore && inspireList.length > 0" class="empty-sub" style="color:#ccc">-- 没有更多了 --</div>
      </AppState>
    </div>
  </div>
  <!-- 推荐卡片滑动（完全对照demo） -->
  <div v-if="activeTab === 'recommend'" class="swipe-section" style="padding:10px 0 40px;">
    <div style="text-align:center;margin-bottom:28px;">
      <h1 style="font-size:28px;background:linear-gradient(90deg,#409eff,#a855f7);-webkit-background-clip:text;color:transparent;margin-bottom:8px;">灵感推荐</h1>
      <p class="tip" style="color:#999;font-size:14px;">拖动卡片左右滑动，左滑跳过，右滑收藏</p>
    </div>
    <div class="swipe-container" style="width:100%;max-width:360px;height:460px;margin:0 auto;position:relative;">
      <div v-if="currentCard" :key="currentIndex" class="card" :class="currentCard.type" :style="cardStyle"
           @mousedown="onSwipeStart" @touchstart.passive="onSwipeStart"
           @mousemove="onSwipeMove" @touchmove="onSwipeMove"
           @mouseup="onSwipeEnd" @touchend="onSwipeEnd"
           @click="goDetailSwipe">
        <img v-if="currentCard && currentCard.img" :src="thumbOf(currentCard.img)" class="card-bg">
        <div v-else class="card-bg" style="background:linear-gradient(135deg,#667eea,#764ba2)"></div>
        <div class="card-mask">
          <span class="tag-label" :class="currentCard.type">{{ currentCard.tag }}</span>
          <div class="word-text">{{ currentCard.word }}</div>
        </div>
        <div class="mark like-mark" :style="{opacity:likeOpacity}">收藏</div>
        <div class="mark pass-mark" :style="{opacity:passOpacity}">跳过</div>
      </div>
    </div>
    <div class="btns" style="display:flex;gap:30px;margin-top:40px;justify-content:center;">
      <button class="btn btn-left" @click="swipeLeft">✕</button>
      <button class="btn btn-right" @click="swipeRight">♥</button>
    </div>
  </div>
  </div>

  <CollectFolderDialog
    v-model="folderDialogVisible"
    :target-id="pendingCollectId"
    @collected="handleCollected"
  />

</template>
<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useAuthStore} from '@/stores/auth'
import InspireCard from '@/components/InspireCard.vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
  getCategoryTree,
  getFollowing,
  getFollowingFeed,
  getInspireList,
  getRecommendList,
  getUnreadCount
} from '@/api/inspire.js'
import {startConversation} from '@/api/message.js'
import {thumbOf} from '@/utils/media.js'

const auth = useAuthStore()
const router = useRouter()
const unreadCount = ref(0)
const isLogin = computed(() => auth.isLogin)
	const activeTab = ref('recommend')

const switchTab = async (tab) => {
  if (tab === 'recommend') {
    stopAutoAdvance()
    await loadSwipeCards(true)
    scheduleAutoAdvance()
  } else {
    stopAutoAdvance()
  }
  activeTab.value = tab
  inspireList.value = []
  currentPage.value = 1
  hasMore.value = true
  selectedFollowee.value = null
  resetCategory()
  if (tab === 'following' && followingList.value.length === 0) {
    await loadFollowing()
  }
  loadInspireList()
}
const selectedFollowee = ref(null)
const followingList = ref([])
const followingLoading = ref(false)
const followingError = ref('')
const followingState = computed(() => {
  if (followingLoading.value && !followingList.value.length) return 'loading'
  if (followingError.value && !followingList.value.length) return 'error'
  return followingList.value.length ? 'ready' : 'empty'
})
const loadFollowing = async () => {
  followingLoading.value = true
  followingError.value = ''
  try {
    const res = await getFollowing()
    followingList.value = res.data || []
  } catch (e) {
    followingError.value = e?.message || 'load following failed'
  } finally {
    followingLoading.value = false
  }
}

const selectFollowee = (u) => {
  selectedFollowee.value = u.id
  inspireList.value = []
  hasMore.value = true
  currentPage.value = 1
  loadInspireList()
}

const goCreate = () => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); router.push('/login'); return }
  router.push('/create')
}
const goPersonal = () => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/personal')
}
const goNotifications = () => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/notifications')
}
const goDetail = (id) => {
  if (id !== null && id !== undefined && String(id).trim()) {
    router.push({ name: 'InspireDetail', params: { id: String(id) } })
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch (e) { console.error(e) }
}

// 分类改为后台可配置：从 /api/inspire/public/categories 读取两级分类
const categoryList = ref([])

const loadCategories = async () => {
  try {
    const res = await getCategoryTree()
    categoryList.value = res.data || []
  } catch (e) {
    console.error('[categories]', e)
  }
}

const activeCategory = ref('')
const activeSubItem = ref('')
const currentSubList = ref([])
const inspireList = ref([])
const loading = ref(false)
const listError = ref('')
const listState = computed(() => {
  if (loading.value && !inspireList.value.length) return 'loading'
  if (listError.value && !inspireList.value.length) return 'error'
  return inspireList.value.length ? 'ready' : 'empty'
})

const handleClickCategory = (item) => {
  activeCategory.value = item.name; activeSubItem.value = ''; inspireList.value = []
  currentSubList.value = item.children || []
}
const currentPage = ref(1)
const hasMore = ref(true)

const selectSubItem = async (item) => {
  activeSubItem.value = item.name; currentPage.value = 1; hasMore.value = true
  loading.value = true
  listError.value = ''
  try {
    const res = await getInspireList({ tag: activeCategory.value, page: 1, size: 20 })
    inspireList.value = res.data || []
    if (res.data && res.data.length < 20) hasMore.value = false
  } catch (e) { inspireList.value = []; listError.value = e?.message || 'load failed'
  } finally { loading.value = false }
}

const loadInspireList = async () => {
  if (loading.value) return
  loading.value = true
  listError.value = ''
  try {
    if (activeTab.value === 'recommend') {
      const res = await getRecommendList({ page: currentPage.value, size: 10 })
            console.log("[SWIPE] recommend API response:", JSON.stringify(res.data ? res.data.slice(0,2) : null))
      if (res.data && res.data.length > 0) inspireList.value.push(...res.data)
      if (!res.data || res.data.length < 10) hasMore.value = false
    } else if (activeTab.value === 'following') {
      const params = { page: currentPage.value, size: 10 }
      if (selectedFollowee.value) params.followeeId = selectedFollowee.value
      const res = await getFollowingFeed(params)
      if (res.data && res.data.length > 0) inspireList.value.push(...res.data)
      if (!res.data || res.data.length < 10) hasMore.value = false
    } else if (activeSubItem.value) {
      const res = await getInspireList({ tag: activeCategory.value, page: currentPage.value, size: 10 })
      if (res.data && res.data.length > 0) inspireList.value.push(...res.data)
      if (!res.data || res.data.length < 10) hasMore.value = false
    } else {
      hasMore.value = false
    }
  } catch (e) { console.error(e); listError.value = e?.message || 'load failed' }
  finally { loading.value = false }
}

const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  currentPage.value++
  loading.value = true
  try {
    const res = await getInspireList({ tag: activeCategory.value, page: currentPage.value, size: 10 })
    if (res.data && res.data.length > 0) inspireList.value.push(...res.data)
    if (!res.data || res.data.length < 10) hasMore.value = false
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const onScroll = () => {
  if (loading.value || !hasMore.value) return
  const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 300
  if (nearBottom) loadMore()
}
const onVisibilityChange = () => {
  if (document.hidden) stopAutoAdvance()
  else scheduleAutoAdvance()
}

onMounted(async () => {
  window.addEventListener("scroll", onScroll)
  document.addEventListener("visibilitychange", onVisibilityChange)
  // 首页并行预取，避免点进去才发现要等接口
  const preload = []
  if (isLogin.value) {
    preload.push(fetchUnreadCount())
  }
  await Promise.all([loadSwipeCards(true), loadCategories(), ...preload])
  scheduleAutoAdvance()
})
onBeforeUnmount(() => {
  window.removeEventListener("scroll", onScroll)
  document.removeEventListener("visibilitychange", onVisibilityChange)
  stopAutoAdvance()
})

const resetCategory = () => { activeCategory.value = ''; currentSubList.value = []; activeSubItem.value = ''; inspireList.value = [] }
const resetSubItem = () => { activeSubItem.value = ''; inspireList.value = [] }

const handleCollect = async (targetId) => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); return }
  advanceAfterCollect.value = false
  pendingCollectId.value = targetId
  folderDialogVisible.value = true
}
const handleCollected = () => {
  pendingCollectId.value = null
  if (advanceAfterCollect.value) {
    advanceAfterCollect.value = false
    cardTransition.value = 'transform 0.3s ease'
    offsetX.value = 450
    setTimeout(nextCard, 300)
  }
}
  const swipeCards = ref([])
  const currentIndex = ref(0)
const wasSwiped = ref(false)
  const currentCard = computed(() => swipeCards.value[currentIndex.value] || null)
  const likeOpacity = ref(0)
  const passOpacity = ref(0)
  const startX = ref(0)
const folderDialogVisible = ref(false)
const pendingCollectId = ref(null)
const advanceAfterCollect = ref(false)
  const offsetX = ref(0)
  const cardTransition = ref('')
  const isDragging = ref(false)
  
  const cardStyle = computed(() => ({
    transform: 'translateX(' + offsetX.value + 'px) rotate(' + (offsetX.value / 12) + 'deg)',
    transition: cardTransition.value
  }))
  const bgStyle = computed(() => {
    var c = currentCard.value
    if (c && c.img) return { backgroundImage: 'url(' + thumbOf(c.img) + ')' }
    return { background: 'linear-gradient(135deg,#667eea,#764ba2)' }
  })

  watch(swipeCards, (val) => {
    if (val && val.length > 0) console.log("[SWIPE] swipeCards[0]:", JSON.stringify(val[0]))
  }, { deep: true })
  
  
  function typeFromTag(tag) {
    if (tag === '美食' || tag === '饮食') return 'food'
    if (tag === '运动' || tag === '健身') return 'sport'
    if (tag === '电影') return 'movie'
    if (tag === '穿搭') return 'wear'
    if (tag === '文案') return 'text'
    return 'food'
  }
  
  /** 推荐卡片：每批 10 条，读完自动再取下一批（第 11 条会去查 11-20 条） */
  const RECOMMEND_BATCH = 10
  const recommendPage = ref(1)
  const recommendHasMore = ref(true)
  const recommendLoading = ref(false)

  const toSwipeCard = (i) => ({
    word: i.title,
    type: typeFromTag(i.tag),
    tag: i.tag,
    inspireId: i.id,
    img: i.img || (i.images && i.images.length > 0 ? i.images[0] : '') || ''
  })

  async function loadSwipeCards(reset = true) {
    if (recommendLoading.value) return
    if (!reset && !recommendHasMore.value) return
    recommendLoading.value = true
    const page = reset ? 1 : recommendPage.value
    try {
      const res = await getRecommendList({ page, size: RECOMMEND_BATCH })
      const list = (res.data || []).map(toSwipeCard)
      if (reset) {
        swipeCards.value = list
        currentIndex.value = 0
        recommendPage.value = 1
      } else {
        const seen = new Set(swipeCards.value.map(c => String(c.inspireId)))
        swipeCards.value.push(...list.filter(c => !seen.has(String(c.inspireId))))
      }
      recommendHasMore.value = list.length >= RECOMMEND_BATCH
      if (!reset) recommendPage.value = page + 1
    } catch (e) {
      console.error(e)
    } finally {
      recommendLoading.value = false
    }
  }

  /** 快读完时提前把下一批拿回来，避免翻卡时等待 */
  const prefetchCards = () => {
    if (!recommendHasMore.value || recommendLoading.value) return
    if (swipeCards.value.length - currentIndex.value <= 3) loadSwipeCards(false)
  }
  
  const onSwipeStart = (e) => {
    stopAutoAdvance()
    isDragging.value = true
    startX.value = e.clientX || (e.touches && e.touches[0].clientX)
    cardTransition.value = ''
  }
  const onSwipeMove = (e) => {
    if (!isDragging.value) return
    const cx = e.clientX || (e.touches && e.touches[0].clientX)
    if (!cx) return
    offsetX.value = cx - startX.value
    const card = document.querySelector('.card')
    if (card) card.style.transform = 'translateX(' + offsetX.value + 'px) rotate(' + (offsetX.value / 12) + 'deg)'
    likeOpacity.value = offsetX.value > 0 ? Math.min(offsetX.value / 120, 1) : 0
    passOpacity.value = offsetX.value < 0 ? Math.min(Math.abs(offsetX.value) / 120, 1) : 0
  }
  const onSwipeEnd = () => {
    if (!isDragging.value) return
    isDragging.value = false
    const card = document.querySelector('.card')
    if (!card) return
    card.style.transition = 'transform 0.3s ease'
    if (offsetX.value > 100) {
      // 右滑 → 选择收藏夹，确认后再进入下一张
      if (currentCard.value?.inspireId && sessionStorage.getItem('isLogin')) {
        advanceAfterCollect.value = true
        pendingCollectId.value = currentCard.value.inspireId
        folderDialogVisible.value = true
      } else if (!sessionStorage.getItem('isLogin')) {
        ElMessage.warning('请先登录')
      }
      card.style.transform = 'translateX(0) rotate(0)'
    } else if (offsetX.value < -100) {
      card.style.transform = 'translateX(-450px) rotate(-25deg)'
      setTimeout(nextCard, 300)
    } else {
      card.style.transform = 'translateX(0) rotate(0)'
      likeOpacity.value = 0; passOpacity.value = 0
    }
    wasSwiped.value = offsetX.value < -50
    offsetX.value = 0
    scheduleAutoAdvance()
  }
  const goDetailSwipe = () => {
  if (!wasSwiped.value && currentCard.value) {
    router.push({ name: 'InspireDetail', params: { id: String(currentCard.value.inspireId) } })
  }
  wasSwiped.value = false
}

/** 自动滑动：无操作 6 秒后自动左滑跳过下一张 */
const AUTO_SWIPE_DELAY = 6000
let autoSwipeTimer = null

const stopAutoAdvance = () => {
  if (autoSwipeTimer) { clearTimeout(autoSwipeTimer); autoSwipeTimer = null }
}

const scheduleAutoAdvance = () => {
  stopAutoAdvance()
  if (activeTab.value !== 'recommend' || !currentCard.value) return
  autoSwipeTimer = setTimeout(() => {
    if (activeTab.value === 'recommend' && currentCard.value) swipeLeft()
  }, AUTO_SWIPE_DELAY)
}

const nextCard = async () => {
    wasSwiped.value = false
    const nextIndex = currentIndex.value + 1
    // 已经翻到本批最后一张时，先补充下一批再继续
    if (nextIndex >= swipeCards.value.length && recommendHasMore.value) {
      await loadSwipeCards(false)
    }
    currentIndex.value = nextIndex < swipeCards.value.length ? nextIndex : 0
    // 关键修复：重置位移，否则新卡片会带着上一张的 translateX 直接飞在屏幕外
    offsetX.value = 0
    cardTransition.value = ''
    const c = document.querySelector('.card')
    if (c) { c.style.transition = 'none'; c.style.transform = 'translateX(0) rotate(0)' }
    likeOpacity.value = 0; passOpacity.value = 0
    prefetchCards()
    scheduleAutoAdvance()
  }
  const goToCardDetail = () => {
    const c = currentCard.value
    if (c && c.inspireId) {
      router.push({ name: 'InspireDetail', params: { id: String(c.inspireId) } })
    }
  }
  const swipeLeft = () => {
    stopAutoAdvance()
    cardTransition.value = 'transform 0.3s ease'
    offsetX.value = -450
    setTimeout(nextCard, 300)
  }
  const swipeRight = async () => {
    stopAutoAdvance()
    if (!currentCard.value?.inspireId) return
    if (!sessionStorage.getItem('isLogin')) {
      ElMessage.warning('请先登录')
      return
    }
    advanceAfterCollect.value = true
    pendingCollectId.value = currentCard.value.inspireId
    folderDialogVisible.value = true
  }

const handleMsgFromFollow = async (u) => {
  const otherId = u.userId || u.id
  if (!otherId) return
  try {
    const res = await startConversation(otherId)
    if (res.data && res.data.id) {
      router.push('/messages?convId=' + res.data.id + '&direct=1')
    }
  } catch (e) { console.error(e) }
}

</script>
<style scoped src="./home/styles/home.css"></style>
