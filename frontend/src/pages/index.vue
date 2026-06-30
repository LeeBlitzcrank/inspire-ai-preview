<template>
  <div id="index-root" class="index-page">
    <div id="index-top-nav" class="top-nav">
      <div id="nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="nav-icon-group" class="right-icons">
        <div id="icon-search" class="icon-item" @click="$router.push('/search')">🔍</div>
        <div id="icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>
    <p id="category-main-title" class="all-title">全部灵感分类</p>

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
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInspireList, collectInspire } from '@/api/inspire'
const router = useRouter()

const goPersonal = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/personal')
}
const goDetail = (id) => { router.push({ name: 'InspireDetail', params: { id } }) }

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
onMounted(() => window.addEventListener("scroll", onScroll))
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
</script>
<style scoped>
.index-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 0 24px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
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
</style>
