import {computed, onBeforeUnmount, ref} from 'vue'
import {getCollectFolders, getCollectListByFolder, getMyCollects, getMyDrafts, getMyInspires} from '@/api/inspire.js'
import {thumbOf} from '@/utils/media.js'

export function usePersonalContent({router, userInfo}) {
  const publishedList = ref([])

  // ===== 我的发布：虚拟列表 =====
  // 500 条灵感如果全部渲染成 DOM，滚动到底部时浏览器要维护 5000+ 节点；
  // 这里只渲染可视区 + 上下各 4 条的缓冲，滚动时复用同一批 DOM。
  const PUB_ITEM_H = 86         // 卡片高 76 + 间距 10（与预览一致）
  const PUB_OVERSCAN = 4
  // 列表可视高度用固定常量，和 .virt-scroll 的 CSS 高度保持一致。
  // 之前是运行时读 clientHeight，首次挂载时样式还没生效、量到 0，
  // 结果只渲染 4 条（ceil(0/86)+4）——现在彻底不依赖测量。
  const PUB_VIEW_H = 340
  const pubScrollTop = ref(0)
  let pubScrollFrame = 0

  const pubVisible = computed(() => {
    const total = publishedList.value.length
    if (!total) return []
    const start = Math.max(0, Math.floor(pubScrollTop.value / PUB_ITEM_H) - PUB_OVERSCAN)
    const end = Math.min(total, Math.ceil((pubScrollTop.value + PUB_VIEW_H) / PUB_ITEM_H) + PUB_OVERSCAN)
    const rows = []
    for (let i = start; i < end; i++) rows.push({ index: i, data: publishedList.value[i] })
    return rows
  })

  const onPubScroll = (e) => {
    // 无限滚动：离底部还有 240px 就补下一页
    const el = e.target
    if (pubScrollFrame) return
    pubScrollFrame = requestAnimationFrame(() => {
      pubScrollFrame = 0
      const quantized = Math.floor(el.scrollTop / PUB_ITEM_H) * PUB_ITEM_H
      if (quantized !== pubScrollTop.value) pubScrollTop.value = quantized
    })
    if (!pubLoading.value && pubHasMore.value &&
        el.scrollTop + el.clientHeight >= el.scrollHeight - 240) {
      loadPublished(false)
    }
  }

  const setPubContainer = (el) => {
    if (!el) return
    // 可视高度用常量 PUB_VIEW_H，这里只需要把滚动位置同步过来
    pubScrollTop.value = Math.floor(el.scrollTop / PUB_ITEM_H) * PUB_ITEM_H
  }
  const draftList = ref([])
  const collectList = ref([])
  const loading = ref(false)
  const pageError = ref('')
  const publishedError = ref('')
  const draftError = ref('')
  const foldersError = ref('')
  const folderCollectsError = ref('')
  const activeTab = ref('published')

  // 分页状态
  const pageSize = ref(5)
  const pubPage = ref(1); const pubTotal = ref(0)
  const draftPage = ref(1); const draftTotal = ref(0)
  const collPage = ref(1); const collTotal = ref(0)

  // 收藏夹（对应 collect_folder 表）：先展示文件夹，点进去才看该夹下的灵感
  const folders = ref([])
  const folderCounts = ref({})
  const activeFolder = ref(null)
  const folderCollects = ref([])
  const folderLoading = ref(false)
  const folderPage = ref(1)
  const folderTotal = ref(0)
  const folderHasMore = ref(false)
  const folderLoadingMore = ref(false)

  const personalEmptyText = computed(() => {
    if (activeTab.value === 'published') return '还没有发布过灵感'
    if (activeTab.value === 'drafts') return '还没有草稿'
    return activeFolder.value ? '该收藏夹还没有灵感' : '还没有收藏夹'
  })
  const personalState = computed(() => {
    if (loading.value) return 'loading'
    if (pageError.value) return 'error'
    if (activeTab.value === 'published') {
      if (publishedError.value && !publishedList.value.length) return 'error'
      return publishedList.value.length ? 'ready' : 'empty'
    }
    if (activeTab.value === 'drafts') {
      if (draftError.value && !draftList.value.length) return 'error'
      return draftList.value.length ? 'ready' : 'empty'
    }
    if (activeFolder.value) {
      if (folderLoading.value) return 'loading'
      if (folderCollectsError.value && !folderCollects.value.length) return 'error'
      return folderCollects.value.length ? 'ready' : 'empty'
    }
    if (foldersError.value && !folders.value.length) return 'error'
    // 收藏夹为空时也要保留「管理收藏夹」入口，不能落进 empty 插槽把入口隐藏。
    return 'ready'
  })
  const reloadActiveTab = async () => {
    pageError.value = ''
    if (activeTab.value === 'published') {
      publishedError.value = ''
      await loadPublished(true)
    } else if (activeTab.value === 'drafts') {
      draftError.value = ''
      await loadDrafts(1)
    } else if (activeFolder.value) {
      folderCollectsError.value = ''
      await openFolder(activeFolder.value)
    } else {
      foldersError.value = ''
      await loadFolders()
    }
  }

  // Banner 头像：优先 emoji，否则取昵称首字
  const avatarText = computed(() => {
    const a = userInfo.value.avatar
    if (a) return a
    const n = userInfo.value.nickname
    return n ? n[0] : '👤'
  })

  // 分页（仅「我的发布」「我的草稿」使用）
  const pagerTotal = computed(() => activeTab.value === 'drafts' ? draftTotal.value : pubTotal.value)
  const pagerPage = computed(() => activeTab.value === 'drafts' ? draftPage.value : pubPage.value)
  const totalPages = computed(() => Math.max(1, Math.ceil(pagerTotal.value / pageSize.value)))

  const goPage = (p) => {
    if (p < 1 || p > totalPages.value) return
    if (activeTab.value === 'drafts') loadDrafts(p)
    else loadPublished(p)
  }

  const goDetail = (id) => {
    if (id !== null && id !== undefined && String(id).trim()) {
      router.push({ name: 'InspireDetail', params: { id: String(id) } })
    }
  }

  // 标签补一个 emoji，和创建页的分类图标保持一致
  const TAG_ICON = {
    '家居':'🏠','美食':'🍜','旅行':'🏕','摄影':'📷','穿搭':'👗',
    '运动':'🏃','文案':'✍️','电影':'🎬','生活':'🌿','手作':'🧶','其他':'✨'
  }
  const tagText = (t) => t ? ((TAG_ICON[t] ? TAG_ICON[t] + ' ' : '') + t) : ''

  // 缩略图：有图用真图，没有就用渐变占位（与设计稿一致）
  const GRADS = [
    'linear-gradient(135deg,#dbeafe,#ede9fe)',
    'linear-gradient(135deg,#fef3c7,#fde68a)',
    'linear-gradient(135deg,#dcfce7,#bbf7d0)',
    'linear-gradient(135deg,#cffafe,#a5f3fc)',
    'linear-gradient(135deg,#fee2e2,#fecaca)',
    'linear-gradient(135deg,#e0e7ff,#c7d2fe)'
  ]
  const firstImage = (item) => {
    if (Array.isArray(item?.images) && item.images.length) return item.images[0]
    if (typeof item?.images === 'string' && item.images.trim()) {
      try {
        const arr = JSON.parse(item.images)
        if (Array.isArray(arr) && arr.length) return arr[0]
      } catch (e) { /* 非 JSON 就当作单张地址 */ }
      return item.images
    }
    return item?.img || ''
  }
  const thumbUrl = (item) => {
    const url = firstImage(item)
    return url ? thumbOf(url, 200) : ''
  }
  const thumbFallbackStyle = (idx) => {
    return { background: GRADS[idx % GRADS.length] }
  }
  const hideBrokenImage = (event) => {
    event.currentTarget.style.display = 'none'
  }

  /**
   * 我的发布：改成「分批追加 + 无限滚动」，配合上面的虚拟列表。
   * reset=true 时回到第一页（切换 tab / 刷新），否则追加下一页。
   */
  const PUB_PAGE_SIZE = 20
  const pubLoadedPage = ref(0)
  const pubHasMore = ref(true)
  const pubLoading = ref(false)
  const pubCursor = ref('')

  const loadPublished = async (reset = false) => {
    if (pubLoading.value) return
    if (!reset && !pubHasMore.value) return
    pubLoading.value = true
    publishedError.value = ''
    const nextPage = reset ? 1 : pubLoadedPage.value + 1
    try {
      const json = await getMyInspires(nextPage, PUB_PAGE_SIZE, reset ? '' : pubCursor.value)
      if (json.code !== 200) throw new Error(json.msg || 'load published failed')
      const rows = json.data?.records || []
      publishedList.value = reset ? rows : [...publishedList.value, ...rows]
      if (reset || Number(json.data?.total || 0) > 0) pubTotal.value = json.data?.total || 0
      pubLoadedPage.value = nextPage
      pubCursor.value = json.data?.nextCursor || ''
      pubHasMore.value = json.data?.hasMore !== undefined
        ? Boolean(json.data.hasMore)
        : rows.length >= PUB_PAGE_SIZE
    } catch (e) {
      console.error(e)
      publishedError.value = e?.message || 'load published failed'
    } finally { pubLoading.value = false }
  }

  const loadDrafts = async (page) => {
    if (page !== undefined) draftPage.value = page
    draftError.value = ''
    try {
      const json = await getMyDrafts(draftPage.value, pageSize.value)
      if (json.code !== 200) throw new Error(json.msg || 'load drafts failed')
      draftList.value = json.data?.records || []
      draftTotal.value = json.data?.total || 0
    } catch (e) {
      console.error(e)
      draftError.value = e?.message || 'load drafts failed'
    }
  }

  const loadCollects = async (page) => {
    if (page !== undefined) collPage.value = page
    try {
      const json = await getMyCollects(collPage.value, pageSize.value)
      if (json.code !== 200) throw new Error(json.msg || 'load collects failed')
      collectList.value = json.data?.records || []
      collTotal.value = json.data?.total || 0
    } catch (e) { console.error(e) }
  }

  // 拉取收藏夹列表，并并行统计每个夹里的灵感条数
  const loadFolders = async () => {
    foldersError.value = ''
    try {
      const res = await getCollectFolders()
      folders.value = res.data || []
      folderCounts.value = Object.fromEntries(
        folders.value.map(f => [f.id, Number(f.count || 0)])
      )
    } catch (e) {
      folders.value = []
      folderCounts.value = {}
      foldersError.value = e?.message || 'load folders failed'
    }
  }

  const FOLDER_PAGE_SIZE = 20

  // 进入某个收藏夹：接口真正分页，首屏只拿 20 条
  const openFolder = async (f, append = false) => {
    if (!append) {
      activeFolder.value = f
      folderCollects.value = []
      folderPage.value = 1
      folderTotal.value = 0
      folderHasMore.value = false
      folderLoading.value = true
    } else {
      folderLoadingMore.value = true
    }
    folderCollectsError.value = ''
    try {
      const res = await getCollectListByFolder(f.id, folderPage.value, FOLDER_PAGE_SIZE)
      const rows = res.data?.records || []
      folderCollects.value = append ? [...folderCollects.value, ...rows] : rows
      folderTotal.value = Number(res.data?.total || 0)
      folderHasMore.value = folderCollects.value.length < folderTotal.value
      if (folderHasMore.value) folderPage.value += 1
    } catch (e) {
      if (!append) folderCollects.value = []
      folderCollectsError.value = e?.message || 'load folder collects failed'
    } finally {
      folderLoading.value = false
      folderLoadingMore.value = false
    }
  }

  const loadMoreFolder = () => {
    if (activeFolder.value && folderHasMore.value && !folderLoadingMore.value) {
      openFolder(activeFolder.value, true)
    }
  }

  onBeforeUnmount(() => {
    if (pubScrollFrame) cancelAnimationFrame(pubScrollFrame)
  })

  return {
    publishedList,
    PUB_ITEM_H,
    pubVisible,
    onPubScroll,
    setPubContainer,
    draftList,
    collectList,
    loading,
    pageError,
    publishedError,
    draftError,
    foldersError,
    folderCollectsError,
    activeTab,
    pageSize,
    pubPage,
    pubTotal,
    draftPage,
    draftTotal,
    collPage,
    collTotal,
    folders,
    folderCounts,
    activeFolder,
    folderCollects,
    folderLoading,
    folderPage,
    folderTotal,
    folderHasMore,
    folderLoadingMore,
    personalEmptyText,
    personalState,
    reloadActiveTab,
    avatarText,
    pagerTotal,
    pagerPage,
    totalPages,
    goPage,
    goDetail,
    tagText,
    thumbUrl,
    thumbFallbackStyle,
    hideBrokenImage,
    PUB_PAGE_SIZE,
    pubLoadedPage,
    pubHasMore,
    pubLoading,
    pubCursor,
    loadPublished,
    loadDrafts,
    loadCollects,
    loadFolders,
    openFolder,
    loadMoreFolder
  }
}
