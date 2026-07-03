<template>
  <div id="index-root" class="index-page">
    <div id="index-top-nav" class="top-nav">
      <div id="nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="nav-icon-group" class="right-icons">
        <div id="icon-create" class="icon-item" @click="goCreate">✨</div>
        <div id="icon-search" class="icon-item" @click="$router.push('/search')">🔍</div>
        <div id="icon-message" class="icon-item" @click="$router.push('/messages')" style="position:relative;">💬</div>
        <div v-if="isLogin" id="icon-noti" class="icon-item" @click="goNotifications">🔔<span v-if="unreadCount > 0" class="noti-badge">{{ unreadCount > 99 ? "99+" : unreadCount }}</span></div>        <div id="icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>
    <!-- 选项卡 -->
    <div class="tab-bar">
     <span class="tab-item" :class="{active: activeTab==='recommend'}" @click="switchTab('recommend')">👇 推荐卡片</span>
     <span class="tab-item" :class="{active: activeTab==='category'}" @click="switchTab('category')">📋 灵感分类</span>
     <span class="tab-item" :class="{active: activeTab==='hot'}" @click="switchTab('hot')">🔥 热门排行</span>
     <span v-if="isLogin" class="tab-item" :class="{active: activeTab==='following'}" @click="switchTab('following')">👥 关注</span>
   </div>

    <div v-if="activeTab === 'category'">
    <!-- 一级分类 -->
    <transition name="slide-fade">
      <div id="category-grid-wrap" class="category-grid" v-if="!activeCategory">
        <div class="category-card" v-for="(item, idx) in categoryList" :key="item.id"
          @click="handleClickCategory(item)" :style="{'--idx': idx}">
          <div class="cate-icon">{{ item.icon }}</div>
          <div class="cate-name">{{ item.name }}</div>
          <div class="cate-num">{{ item.count }}条灵感</div>
   </div>
      </div>
    </transition>

    <!-- 二级标签 -->
    <transition name="slide-fade">
      <div id="sub-tag-wrap" v-if="activeCategory && !activeSubItem" class="sub-wrap">
        <div id="back-to-category" class="back-btn" @click="resetCategory">← 返回全部分类</div>
        <div id="sub-tag-list" class="sub-tag-list">
          <div class="sub-tag" v-for="item in currentSubList" :key="item.id" @click="selectSubItem(item)">{{ item.name }}</div>
        </div>
        <div id="sub-empty-tip" v-if="currentSubList.length === 0" class="empty-sub">暂无子分类</div>
      </div>
    </transition>

    <!-- 灵感列表 -->
    <transition name="slide-fade">
      <div id="detail-inspire-wrap" v-if="activeSubItem" class="detail-wrap">
        <div id="back-to-subtag" class="back-btn" @click="resetSubItem">← 返回{{ activeCategory }}</div>
        <div v-if="loading" class="list-wrap">
          <div v-for="n in 3" :key="n" class="inspire-card skeleton-card">
            <div class="s-line s-title"></div>
            <div class="s-line s-desc"></div>
            <div class="s-line s-footer"></div>
          </div>
        </div>
        <div id="inspire-card-list" class="list-wrap" v-else>
          <InspireCard v-for="item in inspireList" :key="item.id" :item="item"
            :style="{'--idx': item.id}" @collect="handleCollect" @click-card="goDetail" />
        </div>
        <div v-if="!loading && inspireList.length === 0" class="empty-sub">📭 还没有灵感，去其他分类看看吧</div>
    <div v-if="loading && inspireList.length > 0" class="empty-sub" style="color:#409eff">加载更多...</div>
    <div v-if="!hasMore && inspireList.length > 0" class="empty-sub" style="color:#ccc">-- 没有更多了 --</div>
      </div>
    </transition>
    </div>
    <!-- 热门 / 推荐 -->
  <div v-if="activeTab === 'hot'" class="feed-list">
    <InspireCard v-for="item in inspireList" :key="item.id" :item="item" @collect="handleCollect" />
    <div v-if="loading" class="empty-sub" style="color:#666;padding:30px 0">⏳ 加载中...</div>
    <div v-if="!loading && inspireList.length === 0" class="empty-sub" style="padding:40px 0">💭 暂无内容</div>
    <div v-if="!hasMore && inspireList.length > 0" class="empty-sub" style="color:#ccc">-- 没有更多了 --</div>
  </div>

  <!-- 关注：关注的人列表 / 灵感列表 -->
  <div v-if="activeTab === 'following'" class="feed-list">
    <div v-if="!selectedFollowee && !loading" class="following-list">
      <div v-for="u in followingList" :key="u.id" class="follow-user-card" @click="selectFollowee(u)">
        <span class="follow-user-avatar">{{ u.avatar || (u.nickname ? u.nickname[0] : u.username[0]) || '👤' }}</span>
        <div class="follow-user-info">
          <div class="follow-user-name">{{ u.nickname || u.username }}</div>
          <div class="follow-user-meta">@{{ u.username }}</div>
        </div>
        <span class="follow-user-arrow" @click.stop="handleMsgFromFollow(u)" style="color:#6366f1;font-size:12px;border:1px solid #6366f1;border-radius:8px;padding:3px 8px;margin-right:6px;cursor:pointer;">💬 私信</span>
        <span class="follow-user-arrow" style="font-size:18px;">›</span>
      </div>
      <div v-if="followingList.length === 0" class="empty-sub" style="padding:40px 0">💭 还没有关注人</div>
    </div>
    <div v-if="selectedFollowee">
      <div class="back-btn" @click="selectedFollowee = null; inspireList=[]; loadInspireList()">&larr; 返回关注列表</div>
      <InspireCard v-for="item in inspireList" :key="item.id" :item="item" @collect="handleCollect" />
      <div v-if="loading" class="empty-sub" style="color:#666;padding:30px 0">⏳ 加载中...</div>
      <div v-if="!loading && inspireList.length === 0" class="empty-sub" style="padding:40px 0">💭 暂无内容</div>
      <div v-if="!hasMore && inspireList.length > 0" class="empty-sub" style="color:#ccc">-- 没有更多了 --</div>
    </div>
  </div>
  <!-- 推荐卡片滑动 -->
  <div v-if="activeTab === 'recommend'" class="swipe-section" style="padding:10px 0 40px;">
    <div style="text-align:center;margin-bottom:28px;">
      <h1 style="font-size:28px;background:linear-gradient(90deg,#409eff,#a855f7);-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;margin-bottom:6px;">灵感推荐</h1>
      <p style="color:#999;font-size:13px;">拖动卡片左右滑动，左滑跳过，右滑收藏</p>
    </div>
    <div class="swipe-container" style="width:100%;max-width:360px;height:420px;margin:0 auto;position:relative;overflow:hidden;">
      <div v-if="currentCard" class="card" :class="currentCard.type"
           style="width:100%;height:100%;border-radius:20px;display:flex;align-items:center;justify-content:center;font-size:24px;font-weight:500;position:absolute;top:0;left:0;cursor:grab;touch-action:none;user-select:none;box-shadow:0 4px 20px rgba(0,0,0,0.08);z-index:1;"
           @mousedown="onSwipeStart" @touchstart.passive="onSwipeStart" @mousemove="onSwipeMove" @touchmove="onSwipeMove" @mouseup="onSwipeEnd" @touchend="onSwipeEnd">
        <div class="like-mark" :style="{position:'absolute',top:'40px',right:'80px',fontSize:'32px',fontWeight:'bold',opacity:likeOpacity,transform:'rotate(-20deg)',pointerEvents:'none',zIndex:2,color:'#52c41a'}">收藏</div>
        <div class="pass-mark" :style="{position:'absolute',top:'40px',left:'80px',fontSize:'32px',fontWeight:'bold',opacity:passOpacity,transform:'rotate(-20deg)',pointerEvents:'none',zIndex:2,color:'#ff4d4f'}">跳过</div>
        <span class="card-word" @click.stop="goToCardDetail" style="padding:20px;text-align:center;line-height:1.5;">{{ currentCard.word }}</span>
      </div>
    </div>
    <div style="display:flex;gap:30px;margin-top:30px;justify-content:center;">
      <button class="btn-left" style="width:56px;height:56px;border-radius:50%;border:none;font-size:22px;cursor:pointer;box-shadow:0 3px 12px rgba(0,0,0,0.1);background:#fff;color:#ff4d4f;display:flex;align-items:center;justify-content:center;" @click="swipeLeft">✕</button>
      <button class="btn-right" style="width:56px;height:56px;border-radius:50%;border:none;font-size:22px;cursor:pointer;box-shadow:0 3px 12px rgba(0,0,0,0.1);background:#fff;color:#52c41a;display:flex;align-items:center;justify-content:center;" @click="swipeRight">♥</button>
    </div>
  </div>
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
      <el-button size="small" @click="createAndUseCollectFolder">新建</el-button>
    </div>
    <div style="margin-top:16px;text-align:right;">
      <el-button @click="folderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmCollectToFolder">收藏到此</el-button>
    </div>
  </el-dialog>

</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInspireList, collectInspire, getRecommendList, getFollowingFeed, getFollowing, getUnreadCount, getCollectFolders, createCollectFolder, collectToFolder } from '@/api/inspire'
import { startConversation } from '@/api/message'
const router = useRouter()
const unreadCount = ref(0)
const isLogin = computed(() => !!localStorage.getItem('isLogin'))
	const activeTab = ref('recommend')

const switchTab = async (tab) => {
  activeTab.value = tab
  inspireList.value = []
  currentPage.value = 1
  hasMore.value = true
  selectedFollowee.value = null
  resetCategory()
  if (tab === 'following') {
    try {
      const res = await getFollowing()
      followingList.value = res.data || []
    } catch (e) { followingList.value = [] }
  }
  loadInspireList()
}
const selectedFollowee = ref(null)
const followingList = ref([])

const selectFollowee = (u) => {
  selectedFollowee.value = u.id
  inspireList.value = []
  hasMore.value = true
  currentPage.value = 1
  loadInspireList()
}

const goCreate = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); router.push('/login'); return }
  router.push('/create')
}
const goPersonal = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/personal')
}
const goNotifications = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/notifications')
}
const goDetail = (id) => { router.push({ name: 'InspireDetail', params: { id } }) }

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count || 0
  } catch (e) {}
}

const categoryList = ref([
  { id:1, name:'美食', icon:'🍜', count:268 }, { id:2, name:'运动', icon:'🏃', count:135 },
  { id:3, name:'电影', icon:'🎬', count:96 }, { id:4, name:'穿搭', icon:'👗', count:182 },
  { id:5, name:'文案', icon:'✍️', count:321 }
])
const subData = {
  美食: [{id:101,name:'鸡腿'},{id:102,name:'火锅'},{id:103,name:'烧烤'}],
  运动: [{id:201,name:'跑步'},{id:202,name:'健身'},{id:203,name:'篮球'}],
  电影: [{id:301,name:'悬疑片'},{id:302,name:'纪录片'},{id:303,name:'喜剧片'}],
  穿搭: [{id:401,name:'夏日穿搭'},{id:402,name:'通勤穿搭'}],
  文案: [{id:501,name:'短视频文案'},{id:502,name:'朋友圈文案'}]
}

const activeCategory = ref('')
const activeSubItem = ref('')
const currentSubList = ref([])
const inspireList = ref([])
const loading = ref(false)

const handleClickCategory = (item) => {
  activeCategory.value = item.name; activeSubItem.value = ''; inspireList.value = []
  currentSubList.value = subData[item.name] ?? []
}
const currentPage = ref(1)
const hasMore = ref(true)

const selectSubItem = async (item) => {
  activeSubItem.value = item.name; currentPage.value = 1; hasMore.value = true
  loading.value = true
  try {
    const res = await getInspireList({ tag: activeCategory.value, page: 1, size: 20 })
    inspireList.value = res.data || []
    if (res.data && res.data.length < 20) hasMore.value = false
  } catch (e) { inspireList.value = []
  } finally { loading.value = false }
}

const loadInspireList = async () => {
  if (loading.value) return
  loading.value = true
  try {
    if (activeTab.value === 'hot') {
      const res = await getInspireList({ sort: 'heat', page: currentPage.value, size: 10 })
      if (res.data && res.data.length > 0) inspireList.value.push(...res.data)
      if (!res.data || res.data.length < 10) hasMore.value = false
    } else if (activeTab.value === 'recommend') {
      const res = await getRecommendList({ page: currentPage.value, size: 10 })
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
  } catch (e) {}
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
  } catch (e) {}
  finally { loading.value = false }
}

const onScroll = () => {
  if (loading.value || !hasMore.value) return
  const nearBottom = window.innerHeight + window.scrollY >= document.body.offsetHeight - 300
  if (nearBottom) loadMore()
}
onMounted(() => {
  window.addEventListener("scroll", onScroll)
  if (isLogin.value) fetchUnreadCount()
  loadSwipeCards()
})
onBeforeUnmount(() => window.removeEventListener("scroll", onScroll))

const resetCategory = () => { activeCategory.value = ''; currentSubList.value = []; activeSubItem.value = ''; inspireList.value = [] }
const resetSubItem = () => { activeSubItem.value = ''; inspireList.value = [] }

const handleCollect = async (targetId) => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); return }
  try {
    const res = await collectInspire(targetId)
    if (res.code === 200) ElMessage.success('收藏成功')
    else ElMessage.warning(res.msg || '操作失败')
  } catch (e) { ElMessage.error('网络异常请重试') }
}
  const swipeCards = ref([])
  const currentIndex = ref(0)
  const currentCard = computed(() => swipeCards.value[currentIndex.value] || null)
  const likeOpacity = ref(0)
  const passOpacity = ref(0)
  const isDragging = ref(false)
  const startX = ref(0)
const collectFolders = ref([])
const selectedFolder = ref(null)
const folderDialogVisible = ref(false)
const newFolderName = ref('')
  const offsetX = ref(0)
  
  function typeFromTag(tag) {
    if (tag === '美食' || tag === '饮食') return 'food'
    if (tag === '运动' || tag === '健身') return 'sport'
    if (tag === '电影') return 'movie'
    if (tag === '穿搭') return 'wear'
    if (tag === '文案') return 'text'
    return 'food'
  }
  
  async function loadSwipeCards() {
    try {
      const res = await getRecommendList({ page: 1, size: 20 })
      if (res.data && res.data.length > 0) {
        swipeCards.value = res.data.map(function(i) {
          return { word: i.title, type: typeFromTag(i.tag), inspireId: i.id }
        })
        currentIndex.value = 0
      }
    } catch (e) {}
  }
  
  const onSwipeStart = (e) => {
    isDragging.value = true
    startX.value = e.clientX || (e.touches && e.touches[0].clientX)
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
        }).catch(function() {})
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
    offsetX.value = 0
  }
  const nextCard = () => {
    currentIndex.value++
    if (currentIndex.value >= swipeCards.value.length) currentIndex.value = 0
    const c = document.querySelector('.card')
    if (c) { c.style.transition = 'none'; c.style.transform = 'translateX(0) rotate(0)' }
    likeOpacity.value = 0; passOpacity.value = 0
  }
  const goToCardDetail = () => {
    const c = currentCard.value
    if (c && c.inspireId) {
      router.push({ name: 'InspireDetail', params: { id: c.inspireId } })
    }
  }
  const swipeLeft = () => {
    const card = document.querySelector('.card')
    if (!card) return
    card.style.transition = 'transform 0.3s ease'
    card.style.transform = 'translateX(-450px) rotate(-25deg)'
    setTimeout(nextCard, 300)
  }
  const swipeRight = async () => {
    if (currentCard.value && currentCard.value.inspireId) {
      try {
        await loadCollectFolders()
        if (collectFolders.value.length === 0) {
          try { await createCollectFolder('默认收藏'); await loadCollectFolders() } catch (e) {}
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
    const card = document.querySelector('.card')
    if (!card) return
    card.style.transition = 'transform 0.3s ease'
    card.style.transform = 'translateX(450px) rotate(25deg)'
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
  } catch (e) {}
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
.follow-user-card { display:flex; align-items:center; gap:12px; padding:14px 16px; background:#fff; border-radius:14px; cursor:pointer; transition:0.2s; border:1px solid #f0f3f9; }
.follow-user-card:hover { border-color:#409eff; box-shadow:0 2px 8px rgba(64,158,255,.08); }
.follow-user-avatar { width:40px; height:40px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; display:flex; align-items:center; justify-content:center; font-size:16px; flex-shrink:0; }
.follow-user-info { flex:1; min-width:0; }
.follow-user-name { font-size:15px; font-weight:500; color:#1d1d1f; }
.follow-user-meta { font-size:12px; color:#909399; margin-top:2px; }
.follow-user-arrow { font-size:20px; color:#c0c4cc; }
.all-title { font-size:20px; font-weight:600; color:#1d1d1f; margin:0 0 20px; }
.category-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:18px; }
.category-card { padding:32px 14px; background:#fff; border-radius:20px; text-align:center; cursor:pointer; transition:all 0.26s; animation:fadeUp 0.4s forwards; animation-delay:calc(var(--idx)*65ms); opacity:0; border:1px solid #f0f3f9; }
@keyframes fadeUp { from{opacity:0;transform:translateY(10px)} to{opacity:1;transform:translateY(0)} }
.category-card:active { transform:scale(0.96) }
.cate-icon { font-size:40px; margin-bottom:14px; }
.cate-name { font-size:17px; font-weight:500; color:#1d1d1f; margin-bottom:6px; }
.cate-num { font-size:13px; color:#86868b; }
.back-btn { font-size:15px; color:#409eff; cursor:pointer; margin:24px 0 20px; }
#sub-tag-list { display:grid; grid-template-columns:repeat(2,1fr); gap:18px; }
.sub-tag { padding:32px 14px; background:#fff; border-radius:20px; text-align:center; cursor:pointer; border:1px solid #f0f3f9; font-size:17px; font-weight:500; color:#1d1d1f; }
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

.folder-option.selected { border-color: #409eff; background: #f0f8ff; }
.folder-option:hover { border-color: #409eff44; }
</style>
