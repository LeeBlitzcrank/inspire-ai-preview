<template>
  <div id="index-root" class="index-page">
    <div id="index-top-nav" class="top-nav">
      <div id="nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="nav-icon-group" class="right-icons">
        <div id="icon-create" class="icon-item" @click="goCreate">✨</div>
        <div id="icon-search" class="icon-item" @click="$router.push('/search')">🔍</div>
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
        <span class="follow-user-avatar">{{ u.avatar || (u.nickname ? u.nickname[0] : u.username[0]) || '👤' }}</span>
        <div class="follow-user-info">
          <div class="follow-user-name">{{ u.nickname || u.username }}</div>
          <div class="follow-user-meta">@{{ u.username }}</div>
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

  <!-- 收藏文件夹选择器 -->
  <el-dialog v-model="folderDialogVisible" title="选择收藏夹" width="320px">
    <div style="display:flex;flex-wrap:wrap;gap:10px;margin-bottom:16px;">
      <AppCard v-for="f in collectFolders" :key="f.id" class="folder-option"
           :selected="selectedFolder === f.id"
           padding="12px"
           clickable
           @click="selectedFolder = f.id"
           style="flex:1;min-width:100px;text-align:center;">
        <div style="font-size:24px;">{{ f.icon || '📁' }}</div>
        <div style="font-size:13px;margin-top:4px;color:#1d1d1f;">{{ f.name }}</div>
      </AppCard>
    </div>
    <div style="display:flex;gap:8px;">
      <el-input v-model="newFolderName" placeholder="新建文件夹" size="small" style="flex:1;" />
      <el-button size="small" @click="createAndUseCollectFolder">新建</el-button>
    </div>
    <div style="margin-top:16px;text-align:right;">
      <el-button @click="folderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmCollectToFolder">收藏到此</el-button>
    </div>
  </el-dialog>

</template>
<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/uiFeedback.js'
import { getInspireList, collectInspire, getRecommendList, getFollowingFeed, getFollowing, getUnreadCount, getCollectFolders, createCollectFolder, collectToFolder, getCategoryTree } from '@/api/inspire.js'
import { startConversation } from '@/api/message.js'
import { thumbOf } from '@/utils/media.js'
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
  try {
    const res = await collectInspire(targetId)
    if (res.code === 200) ElMessage.success('收藏成功')
    else ElMessage.warning(res.msg || '操作失败')
  } catch (e) { ElMessage.error('网络异常请重试') }
}
  const swipeCards = ref([])
  const currentIndex = ref(0)
const wasSwiped = ref(false)
  const currentCard = computed(() => swipeCards.value[currentIndex.value] || null)
  const likeOpacity = ref(0)
  const passOpacity = ref(0)
  const startX = ref(0)
const collectFolders = ref([])
const selectedFolder = ref(null)
const folderDialogVisible = ref(false)
const newFolderName = ref('')
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
      // 右滑 → 收藏
      if (currentCard.value && currentCard.value.inspireId) {
        collectInspire(currentCard.value.inspireId).then(function(r) {
          if (r.code === 200) ElMessage.success('收藏成功')
        }).catch(function(e) { console.error(e) })
      }
      card.style.transform = 'translateX(450px) rotate(25deg)'
      setTimeout(nextCard, 300)
    } else if (offsetX.value < -100) {
      card.style.transform = 'translateX(-450px) rotate(-25deg)'
      setTimeout(nextCard, 300)
    } else {
      card.style.transform = 'translateX(0) rotate(0)'
      likeOpacity.value = 0; passOpacity.value = 0
    }
    wasSwiped.value = Math.abs(offsetX.value) > 50
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
    if (currentCard.value && currentCard.value.inspireId) {
      try {
        await loadCollectFolders()
        if (collectFolders.value.length === 0) {
          try { await createCollectFolder('默认收藏'); await loadCollectFolders() } catch (e) { console.error(e) }
        }
        if (collectFolders.value.length > 0) {
          await collectToFolder(currentCard.value.inspireId, collectFolders.value[0].id)
          ElMessage.success('已收藏到 ' + collectFolders.value[0].name)
        } else {
          const res = await collectInspire(currentCard.value.inspireId)
          if (res && res.code === 200) ElMessage.success('收藏成功')
        }
      } catch (e) { ElMessage.error('收藏失败') }
    }
    // 右滑收藏后同样翻到下一张
    cardTransition.value = 'transform 0.3s ease'
    offsetX.value = 450
    setTimeout(nextCard, 300)
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
<style scoped>
.index-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 24px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.tab-bar { display:flex; gap:8px; margin-bottom:20px; background:#f4f7fd; border-radius:12px; padding:4px; }
.tab-item { flex:1; text-align:center; padding:8px 4px; font-size:14px; color:#666; cursor:pointer; border-radius:10px; transition:0.25s; }
.tab-item.active { background:#fff; color:#409eff; font-weight:500; box-shadow:0 1px 4px rgba(0,0,0,0.06); }
.feed-list { display:flex; flex-direction:column; gap:16px; }
.following-list { display:flex; flex-direction:column; gap:10px; }
.follow-user-card { display:flex; align-items:center; gap:12px; border-radius:14px; transition:0.2s; }
.follow-user-avatar { width:40px; height:40px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; display:flex; align-items:center; justify-content:center; font-size:16px; flex-shrink:0; }
.follow-user-info { flex:1; min-width:0; }
.follow-user-name { font-size:15px; font-weight:500; color:#1d1d1f; }
.follow-user-meta { font-size:12px; color:#909399; margin-top:2px; }
.follow-user-arrow { font-size:20px; color:#c0c4cc; }
.all-title { font-size:20px; font-weight:600; color:#1d1d1f; margin:0 0 20px; }
.category-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:18px; }
.category-card { border-radius:20px; text-align:center; transition:all 0.26s; animation:fadeUp 0.4s forwards; animation-delay:calc(var(--idx)*65ms); opacity:0; }
@keyframes fadeUp { from{opacity:0;transform:translateY(10px)} to{opacity:1;transform:translateY(0)} }
.category-card:active { transform:scale(0.96) }
.cate-icon { font-size:40px; margin-bottom:14px; }
.cate-name { font-size:17px; font-weight:500; color:#1d1d1f; margin-bottom:6px; }
.cate-num { font-size:13px; color:#86868b; }
.back-btn { font-size:15px; color:#409eff; cursor:pointer; margin:24px 0 20px; }
#sub-tag-list { display:grid; grid-template-columns:repeat(2,1fr); gap:18px; }
.sub-tag { border-radius:20px; text-align:center; font-size:17px; font-weight:500; color:#1d1d1f; }
.slide-fade-enter-from { opacity:0; transform:translateX(14px) }
.slide-fade-leave-to { opacity:0; transform:translateX(-14px) }
.slide-fade-enter-active,.slide-fade-leave-active { transition:all 0.28s }
.list-wrap { display:flex; flex-direction:column; gap:16px; }
.detail-wrap { margin-top:8px; }
.empty-sub { text-align:center; color:#999; font-size:14px; padding:20px 0; }
.skeleton-card { padding:20px; }
.s-line { height:14px; border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; margin-bottom:12px; }
.s-title { width:60%; height:18px; }
.s-desc { width:90%; }
.s-footer { width:40%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }

.noti-badge { position:absolute; top:-2px; right:-2px; min-width:16px; height:16px; border-radius:8px; background:#f56c6c; color:#fff; font-size:11px; line-height:16px; text-align:center; padding:0 4px; font-weight:600; pointer-events:none; }
#icon-noti { position:relative; }

/* Swipe card styles */
.food{background:#fff1e9;color:#e64340;border:1px solid #ffccc7;}
.sport{background:#f0fff4;color:#00b42a;border:1px solid #b7eb8f;}
.movie{background:#e8f4ff;color:#1677ff;border:1px solid #91caff;}
.wear{background:#fff0f6;color:#eb2f96;border:1px solid #ffadd2;}
.text{background:#f9f0ff;color:#722ed1;border:1px solid #d3adf7;}
.btn-left:hover{transform:scale(1.1);}
.btn-right:hover{transform:scale(1.1);}

.folder-option { cursor:pointer; }
/* 推荐卡片 - 完全对照 demo 样式 */
.swipe-container{overflow:hidden}
.card{
  width:100%; height:100%; border-radius:20px; position:absolute; top:0; left:0;
  touch-action:none; user-select:none; overflow:hidden;
  box-shadow:0 4px 20px rgba(0,0,0,0.12); z-index:1;
}
.card-bg{width:100%;height:100%;object-fit:cover;display:block}
.card-mask{
  position:absolute; left:0; bottom:0; width:100%; height:50%;
  background:linear-gradient(to top, rgba(0,0,0,0.75), transparent);
  padding:30px 20px; display:flex; flex-direction:column; justify-content:flex-end;
}
.word-text{font-size:28px;font-weight:500;color:#fff;margin-bottom:8px}
.tag-label{font-size:14px;padding:4px 12px;border-radius:99px;display:inline-block;width:fit-content;color:#fff}
.food{background:#ff6b35}
.sport{background:#28c76f}
.movie{background:#0097e6}
.wear{background:#eb4d9c}
.text{background:#8b5cf6}
.mark{
  position:absolute; top:40px; font-size:36px; font-weight:bold; opacity:0;
  transform:rotate(-20deg); z-index:2;
}
.like-mark{right:30px;color:#52c41a;border:3px solid #52c41a;padding:6px 12px;border-radius:8px}
.pass-mark{left:30px;color:#ff4d4f;border:3px solid #ff4d4f;padding:6px 12px;border-radius:8px}
.btns{margin-top:40px}
.btn{width:60px;height:60px;border-radius:50%;border:none;font-size:24px;cursor:pointer;box-shadow:0 3px 12px rgba(0,0,0,0.1);transition:all 0.2s}
.btn-left{background:#fff;color:#ff4d4f}
.btn-right{background:#fff;color:#52c41a}
.btn:hover{transform:scale(1.1)}

.card-bg {
  width:100%; height:100%; background-size:cover; background-position:center;
}
.card-mask {
  position:absolute; left:0; bottom:0; width:100%; height:50%;
  background:linear-gradient(to top, rgba(0,0,0,0.75), transparent);
}
.card-texts {
  position:absolute; left:0; bottom:0; width:100%; padding:30px 20px;
  display:flex; flex-direction:column; justify-content:flex-end;
}
.card-word {
  font-size:28px; font-weight:500; color:#fff; margin-bottom:4px; line-height:1.3;
}
.card-tag {
  display:inline-block; width:fit-content; padding:4px 14px; border-radius:99px;
  font-size:13px; color:#fff; margin-bottom:8px;
}
.tag-food { background:#ff6b35; }
.tag-sport { background:#28c76f; }
.tag-movie { background:#0097e6; }
.tag-wear { background:#eb4d9c; }
.tag-text { background:#8b5cf6; }
.swipe-mark {
  position:absolute; top:40px; font-size:32px; font-weight:bold; z-index:2;
  pointer-events:none; transform:rotate(-20deg);
}
.like-mark { right:30px; color:#52c41a; border:3px solid #52c41a; padding:6px 12px; border-radius:8px; }
.pass-mark { left:30px; color:#ff4d4f; border:3px solid #ff4d4f; padding:6px 12px; border-radius:8px; }
</style>
