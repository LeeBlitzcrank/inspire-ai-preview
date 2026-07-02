<template>
  <div id="search-root" class="search-page">
    <div id="search-top-nav" class="top-nav">
      <div id="search-nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="search-nav-icon-group" class="right-icons">
        <div id="search-icon-create" class="icon-item" @click="goCreate">✨</div>
        <div id="search-icon-search" class="icon-item active" @click="$router.push('/search')">🔍</div>
        <div id="search-icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>
    <div id="search-page-head" class="page-head">
      <h2>灵感检索</h2>
      <p>搜索你感兴趣的灵感内容</p>
    </div>
    <div id="search-input-wrap" class="search-input-wrap">
      <el-input v-model="keyword" placeholder="输入关键词查找灵感" clearable size="large" @keyup.enter="doSearch">
        <template #append><el-button type="primary" size="large" @click="doSearch">搜索</el-button></template>
      </el-input>
    </div>
    <div id="search-result-block" class="result-block" v-if="searched">
      <p class="result-tip" v-if="searchResult.length > 0">共找到 {{ searchResult.length }} 条匹配灵感</p>
      <p class="result-tip" v-else>🔍 换个关键词试试</p>
      <div id="inspire-card-list" class="result-list" v-if="searchResult.length > 0">
        <InspireCard v-for="item in searchResult" :key="item.id" :item="item" @click-card="goDetail" @collect="handleCollect" />
      </div>
    </div>
    <div v-else class="hot-section">
      <p class="sub-title">🔥 热门灵感</p>
      <div v-if="loadingHot" class="hot-card-list"><div v-for="n in 3" :key="n" class="hot-card"><div class="card-img skeleton-img"></div><div class="card-content"><div class="s-line s-w-60"></div><div class="s-line s-w-90"></div></div></div></div>
      <div v-else class="hot-card-list">
        <div class="hot-card" v-for="item in hotList" :key="item.id" @click="goDetail(item.id)">
          <div class="card-img"><img :src="item.img || 'https://picsum.photos/id/102/300/160'" /></div>
          <div class="card-content">
            <div class="card-top"><h3 class="card-title">{{ item.title }}</h3><span class="heat">{{ item.heat }} 热度</span></div>
            <p class="card-desc">{{ item.content || item.title }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { searchInspires, getInspireList, collectInspire } from '@/api/inspire'
const router = useRouter()
const goDetail = (id) => { router.push({ name: 'InspireDetail', params: { id } }) }
const goCreate = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); router.push('/login'); return }
  router.push('/create')
}
const goPersonal = () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/personal')
}
const keyword = ref(''); const searched = ref(false); const searchResult = ref([])
const hotList = ref([]); const loadingHot = ref(false)

const doSearch = async () => {
  if (!keyword.value.trim()) return ElMessage.warning('请输入搜索关键词')
  searched.value = true; searchResult.value = []
  try {
    const res = await searchInspires({ keyword: keyword.value, page: 1, size: 20 })
    searchResult.value = res.data || []
  } catch (e) {}
}
const handleCollect = async (id) => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); return }
  try { const res = await collectInspire(id)
    if (res.code === 200) ElMessage.success('收藏成功')
    else ElMessage.warning(res.msg || '操作失败')
  } catch (e) { ElMessage.error('网络异常请重试') }
}
onMounted(async () => {
  loadingHot.value = true
  try { const res = await getInspireList({ sort: 'heat', page: 1, size: 10 }); hotList.value = res.data || [] }
  catch (e) {} finally { loadingHot.value = false }
})
</script>
<style scoped>
.search-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 28px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.icon-item.active { background:#409eff; color:#fff }
.page-head { text-align:center; margin-bottom:28px; }
.page-head h2 { font-size:26px; font-weight:600; color:#1d1d1f; margin:0 0 8px; }
.page-head p { font-size:14px; color:#86868b; }
.search-input-wrap { margin-bottom:32px; }
.hot-section { margin-top:20px; }
.sub-title { font-size:18px; font-weight:600; color:#1d1d1f; margin-bottom:16px; }
.hot-card { display:flex; gap:16px; padding:16px; background:#fff; border-radius:16px; margin-bottom:12px; cursor:pointer; border:1px solid #f0f3f9; }
.card-img { width:130px; height:86px; flex-shrink:0; border-radius:14px; overflow:hidden; background:#f5f5f5; display:flex; align-items:center; justify-content:center; }
.card-img img { max-width:100%; max-height:100%; object-fit:contain; }
.card-content { flex:1; }
.card-top { display:flex; justify-content:space-between; margin-bottom:6px; }
.card-title { font-size:16px; font-weight:500; margin:0; }
.heat { font-size:13px; color:#ff7d00; }
.card-desc { font-size:13px; color:#6e6e73; line-height:1.5; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; margin:0; }
.result-block { background:#fff; border-radius:20px; padding:20px 16px; }
.result-tip { font-size:15px; color:#409eff; margin:0 0 18px; }
.result-list { display:flex; flex-direction:column; gap:16px; }
.skeleton-img { width:130px; height:86px; border-radius:14px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; }
.s-line { height:14px; border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; margin-bottom:10px; }
.s-w-60 { width:60%; } .s-w-90 { width:90%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }
.empty-sub { text-align:center; color:#999; font-size:14px; padding:20px 0; }
</style>
