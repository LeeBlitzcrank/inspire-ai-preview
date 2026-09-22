<template>
  <div id="search-root" class="search-page scheme-b">
    <header class="b-head">
      <div class="b-head-row">
        <button class="icon-button" type="button" aria-label="返回" @click="goBack">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
        </button>
        <b>{{ searched ? '探索结果' : '灵感搜索' }}</b>
        <button class="icon-button user-button" type="button" aria-label="个人页" @click="goPersonal">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M20 21a8 8 0 0 0-16 0" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </button>
      </div>
      <div class="b-search">
        <div class="search-field">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
               stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.6-3.6" />
          </svg>
          <input
            v-model="keyword"
            data-search-input
            :placeholder="searched ? '继续搜索灵感' : '今天想找什么灵感？'"
            autocomplete="off"
            @keydown.enter.prevent="doSearch()"
          >
          <button
            v-if="keyword"
            class="clear-button"
            type="button"
            aria-label="清空搜索"
            @click="clearKeyword"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </button>
        </div>
        <button class="search-submit" type="button" :disabled="searchLoading" @click="doSearch()">
          {{ searchLoading && !searchResult.length ? '搜索中' : '搜索' }}
        </button>
      </div>
    </header>

    <template v-if="!searched">
      <section class="hero">
        <h2>从一个词开始，发现一点新灵感</h2>
        <p>点热门词直接搜索，或者浏览下面的图片灵感。</p>
        <div class="word-cloud">
          <button
            v-for="word in hotWords"
            :key="word"
            type="button"
            @click="searchWord(word)"
          >
            {{ word }}
          </button>
        </div>
      </section>

      <section class="discovery-section">
        <div class="section-title">
          <b>正在流行</b>
          <button
            class="view-toggle"
            type="button"
            :aria-pressed="singleColumn"
            @click="singleColumn = !singleColumn"
          >
            {{ singleColumn ? '双列浏览' : '单列浏览' }}
          </button>
        </div>
        <AppState
          :state="hotState"
          :rows="3"
          empty-icon="✨"
          empty-text="暂时没有热门灵感"
          error-text="热门灵感加载失败"
          @retry="loadHot"
        >
          <div class="mosaic" :class="{ single: singleColumn }">
            <div
              v-for="item in hotList"
              :key="item.id"
              class="mosaic-card"
              role="button"
              tabindex="0"
              @click="goDetail(item.id)"
              @keydown.enter.prevent="goDetail(item.id)"
            >
              <img
                v-if="item.img"
                loading="lazy"
                decoding="async"
                :src="thumbOf(item.img, 400)"
                :srcset="srcsetOf(item.img)"
                sizes="45vw"
                :alt="item.title"
              >
              <div v-else class="image-placeholder"></div>
              <div class="mosaic-overlay">
                <div>
                  <b>{{ item.title }}</b>
                  <span>{{ item.tag || '灵感' }} · {{ formatHeat(item.heat) }} 热度</span>
                </div>
              </div>
            </div>
          </div>
        </AppState>
      </section>
    </template>

    <template v-else>
      <section class="result-section">
        <div class="category-strip">
          <button
            v-for="category in categories"
            :key="category"
            type="button"
            class="chip"
            :class="{ on: activeCategory === category }"
            @click="selectCategory(category)"
          >
            {{ category }}
          </button>
        </div>
        <div class="result-title">
          <b>为你找到</b>
          <div class="result-title-actions">
            <span v-if="!searchLoading || searchResult.length">{{ searchResult.length }} 条灵感</span>
            <button
              class="view-toggle"
              type="button"
              :aria-pressed="singleColumn"
              @click="singleColumn = !singleColumn"
            >
              {{ singleColumn ? '双列浏览' : '单列浏览' }}
            </button>
          </div>
        </div>
        <AppState
          :state="searchState"
          :rows="4"
          empty-icon="🔍"
          empty-text="换个关键词试试"
          error-text="搜索失败，请稍后重试"
          @retry="doSearch()"
        >
          <div class="mosaic" :class="{ single: singleColumn }">
            <div
              class="mosaic-card create-card"
              role="button"
              tabindex="0"
              @click="goCreate"
              @keydown.enter.prevent="goCreate"
            >
              <img
                :src="CREATE_CARD_IMAGE"
                alt="创建灵感"
                loading="eager"
                decoding="async"
                fetchpriority="high"
              >
              <div class="mosaic-overlay create-overlay">
                <div>
                  <b>我有一个灵感 <span class="create-plus">+</span></b>
                  <span>记录下来，开始创作</span>
                </div>
              </div>
            </div>
            <div
              v-for="item in searchResult"
              :key="item.id"
              class="mosaic-card"
              role="button"
              tabindex="0"
              @click="goDetail(item.id)"
              @keydown.enter.prevent="goDetail(item.id)"
            >
              <img
                v-if="item.img"
                loading="lazy"
                decoding="async"
                :src="thumbOf(item.img, 400)"
                :srcset="srcsetOf(item.img)"
                sizes="45vw"
                :alt="item.title"
              >
              <div v-else class="image-placeholder"></div>
              <div class="mosaic-overlay">
                <div>
                  <b>{{ item.title }}</b>
                  <span>{{ item.tag || '灵感' }} · {{ formatHeat(item.heat) }} 热度</span>
                </div>
              </div>
              <button
                class="collect-button"
                type="button"
                aria-label="收藏"
                @click.stop="handleCollect(item.id)"
              >
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M6 3h12v18l-6-4-6 4Z" />
                </svg>
              </button>
            </div>
          </div>
          <div v-if="searchResult.length && searchLoading" class="result-loading">加载更多...</div>
          <button
            v-if="searchResult.length && hasMore && !searchLoading"
            class="load-more"
            type="button"
            @click="doSearch(true)"
          >
            加载更多
          </button>
          <div v-else-if="searchResult.length && !hasMore" class="result-end">已经到底啦</div>
        </AppState>
      </section>
    </template>
  </div>

  <el-dialog v-model="folderDialogVisible" title="选择收藏夹" width="320px">
    <div class="folder-options">
      <AppCard
        v-for="folder in collectFolders"
        :key="folder.id"
        class="folder-option"
        :selected="selectedFolder === folder.id"
        padding="12px"
        clickable
        @click="selectedFolder = folder.id"
      >
        <div class="folder-icon">{{ folder.icon || '📁' }}</div>
        <div class="folder-name">{{ folder.name }}</div>
      </AppCard>
    </div>
    <div class="folder-create">
      <el-input v-model="newFolderName" placeholder="新建文件夹" size="small" />
      <el-button size="small" @click="createAndUseCollectFolder">新建</el-button>
    </div>
    <div class="folder-actions">
      <el-button @click="folderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirmCollectToFolder">收藏到此</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
  collectToFolder,
  createCollectFolder,
  getCategoryTree,
  getCollectFolders,
  getInspireList,
  getWordCloud,
  searchInspires
} from '@/api/inspire.js'
import {srcsetOf, thumbOf} from '@/utils/media.js'

const router = useRouter()
const CREATE_CARD_IMAGE = 'https://images.unsplash.com/photo-1455390582262-044cdead277a?auto=format&fit=crop&w=1000&q=82'
const DEFAULT_HOT_WORDS = ['城市漫步', '夏日咖啡', '山野露营', '极简摄影', '旧物改造', '夜跑路线', '胶片感', '小户型']
const DEFAULT_CATEGORIES = ['全部', '美食', '旅行', '摄影', '家居', '穿搭', '手作', '运动']

const keyword = ref('')
const searched = ref(false)
const singleColumn = ref(false)
const searchResult = ref([])
const searchLoading = ref(false)
const searchError = ref('')
const searchAfter = ref('')
const hasMore = ref(true)

const hotWords = ref([...DEFAULT_HOT_WORDS])
const categories = ref([...DEFAULT_CATEGORIES])
const activeCategory = ref('全部')

const hotList = ref([])
const loadingHot = ref(false)
const hotError = ref('')

const folderDialogVisible = ref(false)
const collectFolders = ref([])
const selectedFolder = ref(null)
const newFolderName = ref('')
const pendingCollectId = ref(null)

const hotState = computed(() => {
  if (loadingHot.value && !hotList.value.length) return 'loading'
  if (hotError.value && !hotList.value.length) return 'error'
  return hotList.value.length ? 'ready' : 'empty'
})

const searchState = computed(() => {
  if (searchLoading.value && !searchResult.value.length) return 'loading'
  if (searchError.value && !searchResult.value.length) return 'error'
  return searchResult.value.length ? 'ready' : 'empty'
})

const goDetail = (id) => {
  if (id === null || id === undefined || !String(id).trim()) return
  router.push({ name: 'InspireDetail', params: { id: String(id) } })
}

const goPersonal = () => {
  if (!sessionStorage.getItem('isLogin')) {
    ElMessage.warning('请先登录账号')
    router.push('/login')
    return
  }
  router.push('/personal')
}

const goCreate = () => {
  if (!sessionStorage.getItem('isLogin')) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/create')
}

const goBack = () => {
  if (searched.value) {
    resetSearchView()
    return
  }
  if (window.history.length > 1) router.back()
  else router.push('/')
}

const resetSearchView = () => {
  searched.value = false
  keyword.value = ''
  searchResult.value = []
  searchError.value = ''
  searchAfter.value = ''
  hasMore.value = true
  activeCategory.value = '全部'
}

const clearKeyword = () => {
  keyword.value = ''
  if (searched.value) resetSearchView()
}

const formatHeat = (value) => {
  const count = Number(value || 0)
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  return String(count)
}

const loadHot = async () => {
  loadingHot.value = true
  hotError.value = ''
  try {
    const res = await getInspireList({ sort: 'heat', page: 1, size: 10 })
    hotList.value = res.data || []
  } catch (e) {
    hotError.value = e?.message || 'hot failed'
  } finally {
    loadingHot.value = false
  }
}

const loadSearchMeta = async () => {
  const [wordResult, categoryResult] = await Promise.allSettled([
    getWordCloud(),
    getCategoryTree()
  ])

  if (wordResult.status === 'fulfilled') {
    const words = (wordResult.value.data || [])
      .map(item => item.word)
      .filter(Boolean)
    if (words.length) hotWords.value = words
  }

  if (categoryResult.status === 'fulfilled') {
    const names = (categoryResult.value.data || [])
      .map(item => item.name)
      .filter(Boolean)
    if (names.length) categories.value = ['全部', ...names]
  }
}

const doSearch = async (loadMore = false) => {
  const text = keyword.value.trim()
  if (!text) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  searched.value = true
  searchError.value = ''
  if (!loadMore) {
    searchResult.value = []
    searchAfter.value = ''
    hasMore.value = true
  }
  searchLoading.value = true

  try {
    const params = { keyword: text, size: 20 }
    if (activeCategory.value !== '全部') params.tag = activeCategory.value
    if (loadMore && searchAfter.value) params.searchAfter = searchAfter.value

    const res = await searchInspires(params)
    const rows = res.data || []
    if (loadMore) searchResult.value.push(...rows)
    else searchResult.value = rows

    if (rows.length) {
      const last = rows[rows.length - 1]
      searchAfter.value = `${last.heat || 0}_${last.id || ''}`
      hasMore.value = rows.length >= 20
    } else {
      hasMore.value = false
    }
  } catch (e) {
    searchError.value = e?.message || 'search failed'
  } finally {
    searchLoading.value = false
  }
}

const searchWord = (word) => {
  keyword.value = word
  doSearch()
}

const selectCategory = (category) => {
  if (activeCategory.value === category) return
  activeCategory.value = category
  if (searched.value) doSearch()
}

const handleCollect = async (id) => {
  if (!sessionStorage.getItem('isLogin')) {
    ElMessage.warning('请先登录')
    return
  }
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

const loadCollectFolders = async () => {
  const res = await getCollectFolders()
  collectFolders.value = res.data || []
}

const createAndUseCollectFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name) {
    ElMessage.warning('请输入收藏夹名称')
    return
  }
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

const confirmCollectToFolder = async () => {
  if (!pendingCollectId.value || !selectedFolder.value) {
    ElMessage.warning('请选择收藏夹')
    return
  }
  try {
    await collectToFolder(pendingCollectId.value, selectedFolder.value)
    ElMessage.success('收藏成功')
    folderDialogVisible.value = false
    pendingCollectId.value = null
  } catch (e) {
    ElMessage.error('收藏失败')
  }
}

onMounted(() => {
  loadHot()
  loadSearchMeta()
})
</script>

<style scoped>
.search-page {
  --ink: #17201d;
  --muted: #84918c;
  --line: #eee4d8;
  --warm: #b86d28;
  min-height: 100%;
  padding: 0 0 34px !important;
  color: var(--ink);
  background: #fffaf3;
}

.search-page button,
.search-page input {
  font-family: inherit;
}

.search-page button {
  cursor: pointer;
}

.icon {
  width: 18px;
  height: 18px;
  display: block;
}

.icon-button {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #293630;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.icon-button:hover {
  background: rgba(23, 32, 29, 0.06);
}

.b-head {
  position: sticky;
  top: 0;
  z-index: 8;
  padding: 12px 14px 10px;
  background: rgba(255, 250, 243, 0.98);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.b-head-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.b-head-row b {
  font-size: 15px;
}

.user-button {
  margin-left: auto;
}

.b-search {
  display: flex;
  gap: 7px;
}

.search-field {
  min-width: 0;
  flex: 1;
  height: 42px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5dacd;
  border-radius: 13px;
  background: #fff;
  color: #87948f;
}

.search-field:focus-within {
  border-color: #d7a47f;
}

.search-field input {
  width: 100%;
  min-width: 0;
  height: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ink);
  font-size: 13px;
}

.search-field input::placeholder {
  color: #a7a099;
}

.clear-button {
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #f2ede7;
  color: #81776e;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.clear-button .icon {
  width: 13px;
  height: 13px;
}

.search-submit {
  height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 13px;
  background: var(--warm);
  color: #fff;
  font-size: 12.5px;
  font-weight: 850;
  flex: 0 0 auto;
}

.search-submit:disabled {
  opacity: 0.55;
  cursor: default;
}

.hero {
  margin: 14px;
  padding: 17px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff;
}

.hero h2 {
  margin: 0 0 7px;
  font-size: 21px;
  line-height: 1.25;
}

.hero p {
  margin: 0;
  color: #8b7d70;
  font-size: 12px;
  line-height: 1.65;
}

.word-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 7px;
  margin-top: 14px;
}

.word-cloud button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #9a6b42;
}

.word-cloud button:nth-child(3n) {
  font-size: 18px;
  font-weight: 850;
}

.word-cloud button:nth-child(3n + 1) {
  font-size: 13px;
}

.word-cloud button:nth-child(3n + 2) {
  font-size: 15px;
  font-weight: 750;
}

.discovery-section,
.result-section {
  padding: 0 14px;
}

.section-title,
.result-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 17px 0 9px;
}

.section-title b,
.result-title b {
  font-size: 13.5px;
}

.section-title span,
.result-title span {
  color: #9a8b7d;
  font-size: 11px;
}

.result-title-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-toggle {
  padding: 0;
  border: 0;
  background: transparent;
  color: #82908b;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.4;
}

.view-toggle:hover {
  color: #6f7d78;
}

.mosaic {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}

.mosaic.single {
  grid-template-columns: 1fr;
}

.mosaic-card {
  position: relative;
  height: 157px;
  overflow: hidden;
  border-radius: 15px;
  background: #eadfd3;
  cursor: pointer;
}

.mosaic-card:nth-child(3n) {
  height: 188px;
}

.mosaic-card.create-card {
  grid-column: 1 / -1;
  height: 190px;
}

.mosaic.single .mosaic-card,
.mosaic.single .mosaic-card:nth-child(3n) {
  height: 228px;
}

.mosaic.single .mosaic-card.create-card {
  height: 210px;
}

.mosaic-card:hover {
  box-shadow: 0 8px 22px rgba(85, 61, 39, 0.13);
}

.mosaic-card img,
.image-placeholder {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.image-placeholder {
  background: #eadfd3;
}

.mosaic-overlay {
  position: absolute;
  inset: 0;
  padding: 12px;
  display: flex;
  align-items: flex-end;
  color: #fff;
  background: linear-gradient(180deg, transparent 38%, rgba(20, 24, 22, 0.72));
}

.mosaic-overlay b {
  display: block;
  font-size: 13px;
  line-height: 1.35;
}

.mosaic-overlay span {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 10.5px;
}

.create-overlay {
  background: linear-gradient(90deg, rgba(163, 83, 24, 0.88), rgba(184, 109, 40, 0.52));
}

.create-overlay b {
  font-size: 21px;
}

.mosaic-overlay .create-plus {
  display: inline;
  margin: 0 0 0 3px;
  color: inherit;
  margin-left: 3px;
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  vertical-align: -1px;
}

.collect-button {
  position: absolute;
  top: 9px;
  right: 9px;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.45);
  border-radius: 50%;
  background: rgba(20, 28, 25, 0.34);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.collect-button .icon {
  width: 15px;
  height: 15px;
}

.category-strip {
  display: flex;
  gap: 7px;
  overflow-x: auto;
  padding: 12px 0 10px;
}

.chip {
  padding: 7px 10px;
  border: 1px solid #eee4d8;
  border-radius: 999px;
  background: #fff;
  color: #806e5e;
  font-size: 11.5px;
  white-space: nowrap;
}

.chip.on {
  border-color: #e4b88d;
  background: #fff0df;
  color: #a65b1a;
}

.load-more {
  width: 100%;
  height: 40px;
  margin-top: 10px;
  border: 1px solid #eee4d8;
  border-radius: 12px;
  background: #fff;
  color: #8b6b57;
  font-size: 12px;
  font-weight: 800;
}

.result-loading,
.result-end {
  padding: 16px 0 8px;
  text-align: center;
  color: #9a8b7d;
  font-size: 11.5px;
}

.folder-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.folder-option {
  flex: 1;
  min-width: 100px;
  text-align: center;
  cursor: pointer;
}

.folder-icon {
  font-size: 24px;
}

.folder-name {
  margin-top: 4px;
  color: #1d1d1f;
  font-size: 13px;
}

.folder-create {
  display: flex;
  gap: 8px;
}

.folder-create :deep(.el-input) {
  flex: 1;
}

.folder-actions {
  margin-top: 16px;
  text-align: right;
}

@media screen and (max-width: 620px) {
  .search-field input {
    font-size: 16px !important;
  }
}
</style>
