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
      <AppState
        :state="searchState"
        :rows="4"
        empty-icon="🔍"
        empty-text="换个关键词试试"
        error-text="搜索失败，请稍后重试"
        @retry="doSearch"
      >
        <p class="result-tip">共找到 {{ searchResult.length }} 条匹配灵感</p>
        <div id="inspire-card-list" class="result-list">
          <InspireCard v-for="item in searchResult" :key="item.id" :item="item" @click-card="goDetail" @collect="handleCollect" />
        </div>
      </AppState>
    </div>
    <div v-else class="hot-section">
      <p class="sub-title">🔥 热门灵感</p>
      <AppState
        :state="hotState"
        :rows="3"
        empty-icon="🔥"
        empty-text="暂时没有热门灵感"
        error-text="热门灵感加载失败"
        @retry="loadHot"
      >
        <div class="hot-card-list">
          <AppCard
            class="hot-card"
            v-for="item in hotList"
            :key="item.id"
            padding="16px"
            clickable
            @click="goDetail(item.id)"
          >
            <div class="card-img">
              <img
                loading="lazy"
                decoding="async"
                :src="thumbOf(item.img) || 'https://picsum.photos/id/102/300/160'"
                :srcset="srcsetOf(item.img)"
                sizes="(max-width: 640px) 45vw, 300px"
              />
            </div>
            <div class="card-content">
              <div class="card-top"><h3 class="card-title">{{ item.title }}</h3><AppTag tone="warning">{{ item.heat }} 热度</AppTag></div>
              <p class="card-desc">{{ item.content || item.title }}</p>
            </div>
          </AppCard>
        </div>
      </AppState>
    </div>
  </div>

  <!-- 收藏文件夹选择器 -->
  <el-dialog v-model="folderDialogVisible" title="选择收藏夹" width="320px">
    <div style="display:flex;flex-wrap:wrap;gap:10px;margin-bottom:16px;">
      <AppCard v-for="f in collectFolders" :key="f.id" class="folder-option"
           :selected="selectedFolder === f.id"
           padding="12px"
           clickable
           style="flex:1;min-width:100px;text-align:center;"
           @click="selectedFolder = f.id">
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
import { ref, computed, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/uiFeedback.js'
import { searchInspires, getInspireList, getCollectFolders, createCollectFolder, collectToFolder } from '@/api/inspire.js'
import { thumbOf, srcsetOf } from '@/utils/media.js'
const router = useRouter()
const goDetail = (id) => {
  if (id !== null && id !== undefined && String(id).trim()) {
    router.push({ name: 'InspireDetail', params: { id: String(id) } })
  }
}
const goCreate = () => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); router.push('/login'); return }
  router.push('/create')
}
const goPersonal = () => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录账号'); router.push('/login'); return }
  router.push('/personal')
}
const keyword = ref(''); const searched = ref(false); const searchResult = ref([])
const hotList = ref([]); const loadingHot = ref(false); const hotError = ref('')
const searchLoading = ref(false); const searchError = ref('')
const hasMore = ref(true)
const searchAfter = ref('')
const folderDialogVisible = ref(false)
const collectFolders = ref([])
const selectedFolder = ref(null)
const newFolderName = ref('')
const pendingCollectId = ref(null)
const searchState = computed(() => {
  if (searchLoading.value && !searchResult.value.length) return 'loading'
  if (searchError.value && !searchResult.value.length) return 'error'
  return searchResult.value.length ? 'ready' : 'empty'
})
const hotState = computed(() => {
  if (loadingHot.value) return 'loading'
  if (hotError.value) return 'error'
  return hotList.value.length ? 'ready' : 'empty'
})

const doSearch = async (loadMore) => {
  if (!keyword.value.trim()) return ElMessage.warning('请输入搜索关键词')
  searched.value = true
  if (!loadMore) { searchResult.value = []; searchAfter.value = '' }
  searchLoading.value = true
  searchError.value = ''
  try {
    const params = { keyword: keyword.value, size: 20 }
    if (searchAfter.value) params.searchAfter = searchAfter.value
    const res = await searchInspires(params)
    if (res.data && res.data.length > 0) {
      searchResult.value.push(...res.data)
      const last = res.data[res.data.length - 1]
      searchAfter.value = (last.heat || 0) + '_' + (last.id || '')
      hasMore.value = res.data.length >= 20
    } else {
      hasMore.value = false
    }
  } catch (e) {
    console.error(e)
    searchError.value = e?.message || 'search failed'
  } finally {
    searchLoading.value = false
  }
}
const handleCollect = async (id) => {
  if (!sessionStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); return }
  try {
    pendingCollectId.value = id
    const res = await getCollectFolders()
    collectFolders.value = res.data || []
    selectedFolder.value = collectFolders.value[0]?.id ?? null
    folderDialogVisible.value = true
  } catch (e) {
    ElMessage.error('收藏夹加载失败')
  }
}
const createAndUseCollectFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) return ElMessage.warning('请输入收藏夹名称')
  try {
    const res = await createCollectFolder(name, '📁')
    const created = res.data || res
    await loadCollectFolders()
    selectedFolder.value = created?.id ?? collectFolders.value.at(-1)?.id ?? null
    newFolderName.value = ''
  } catch (e) {
    ElMessage.error('创建收藏夹失败')
  }
}
const loadCollectFolders = async () => {
  const res = await getCollectFolders()
  collectFolders.value = res.data || []
}
const confirmCollectToFolder = async () => {
  if (!pendingCollectId.value || !selectedFolder.value) return ElMessage.warning('请选择收藏夹')
  try {
    await collectToFolder(pendingCollectId.value, selectedFolder.value)
    ElMessage.success('收藏成功')
    folderDialogVisible.value = false
    pendingCollectId.value = null
  } catch (e) {
    ElMessage.error('收藏失败')
  }
}
const loadHot = async () => {
  loadingHot.value = true
  hotError.value = ''
  try { const res = await getInspireList({ sort: 'heat', page: 1, size: 10 }); hotList.value = res.data || [] }
  catch (e) { hotError.value = e?.message || 'hot failed' }
  finally { loadingHot.value = false }
}
onMounted(loadHot)
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
.hot-card { display:flex; gap:16px; margin-bottom:12px; border-radius:16px; }
.card-img { width:130px; height:86px; flex-shrink:0; border-radius:14px; overflow:hidden; background:#f5f5f5; display:flex; align-items:center; justify-content:center; }
.card-img img { max-width:100%; max-height:100%; object-fit:contain; }
.card-content { flex:1; }
.card-top { display:flex; justify-content:space-between; margin-bottom:6px; }
.card-title { font-size:16px; font-weight:500; margin:0; }
.card-desc { font-size:13px; color:#6e6e73; line-height:1.5; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; margin:0; }
.result-block { background:#fff; border-radius:20px; padding:20px 16px; }
.result-tip { font-size:15px; color:#409eff; margin:0 0 18px; }
.result-list { display:flex; flex-direction:column; gap:16px; }
.empty-sub { text-align:center; color:#999; font-size:14px; padding:20px 0; }

.folder-option { cursor:pointer; }
</style>
