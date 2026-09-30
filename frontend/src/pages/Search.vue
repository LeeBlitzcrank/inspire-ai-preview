<!--
  文件：frontend/src/pages/Search.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
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
        <button class="icon-button" type="button" aria-label="灵感问答" @click="$router.push('/rag')">🧠</button>
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
                :class="{ collected: isCollected(item.id) }"
                type="button"
                :aria-label="isCollected(item.id) ? '已收藏' : '收藏'"
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

  <CollectFolderDialog
    v-model="collectDialogVisible"
    :target-id="pendingCollectId"
    @collected="markCollected"
  />
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getCategoryTree, getInspireList, getWordCloud, searchInspires} from '@/api/inspire.js'
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

const collectDialogVisible = ref(false)
const pendingCollectId = ref(null)
const collectedIds = ref(new Set())

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
  pendingCollectId.value = id
  collectDialogVisible.value = true
}

const isCollected = (id) => collectedIds.value.has(String(id))

const markCollected = (id) => {
  collectedIds.value = new Set([...collectedIds.value, String(id)])
}

onMounted(() => {
  loadHot()
  loadSearchMeta()
})
</script>

<style scoped src="./search/styles/search.css"></style>
