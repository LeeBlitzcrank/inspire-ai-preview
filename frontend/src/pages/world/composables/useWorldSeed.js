/**
 * 文件：frontend/src/pages/world/composables/useWorldSeed.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {computed, onBeforeUnmount, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '@/stores/auth'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
    clearWorldSeedCache,
    getWorldChapters,
    getWorldSeed,
    getWorldSeeds,
    getWorldTask,
    submitWorldChapter,
    submitWorldSeed
} from '@/api/world.js'

const choiceOptions = [
  {key: 'continue', label: '顺着当前线索继续'},
  {key: 'change', label: '改变一个关键条件'},
  {key: 'question', label: '追问结尾留下的问题'}
]

export function useWorldSeed() {
  const router = useRouter()
  const auth = useAuthStore()
  const view = ref('home')
  const seeds = ref([])
  const listLoading = ref(true)
  const listError = ref('')
  const selectedSeed = ref(null)
  const detailLoading = ref(false)
  const detailError = ref('')
  const activeLineId = ref(null)
  const activeBranchId = ref(null)
  const backgroundExpanded = ref(false)
  const selectedChoiceKey = ref('')
  const customChoice = ref('')
  const chapters = ref([])
  const expandedChapterIds = ref(new Set())
  const chapterCursor = ref(null)
  const chapterHasMore = ref(false)
  const chapterLoading = ref(false)
  const task = ref(null)
  const creating = ref(false)
  const generating = ref(false)
  const form = ref({sourceTitle: '', sourceAuthor: '', sourceText: '', guidance: ''})
  let pollVersion = 0

  const pageTitle = computed(() => ({
    home: '平行世界',
    detail: '世界线详情',
    create: '创建世界种子'
  })[view.value])

  const pageSubtitle = computed(() => ({
    home: '同一篇原文，多条世界线同时生长',
    detail: selectedSeed.value?.title || '阅读并决定下一章',
    create: '原文保持稳定，只改变一个条件'
  })[view.value])

  const listState = computed(() => {
    if (listLoading.value && !seeds.value.length) return 'loading'
    if (listError.value && !seeds.value.length) return 'error'
    return seeds.value.length ? 'ready' : 'empty'
  })

  const detailState = computed(() => {
    if (detailLoading.value && !selectedSeed.value) return 'loading'
    if (detailError.value && !selectedSeed.value) return 'error'
    return selectedSeed.value ? 'ready' : 'empty'
  })

  const activeLine = computed(() => {
    const lines = selectedSeed.value?.lines || []
    return lines.find(line => line.id === activeLineId.value) || lines[0] || null
  })

  const activeBranch = computed(() => {
    const branches = activeLine.value?.branches || []
    return branches.find(branch => branch.id === activeBranchId.value) || branches[0] || null
  })

  const voteTotal = computed(() =>
    Object.values(activeBranch.value?.voteCounts || {})
      .reduce((sum, count) => sum + Number(count || 0), 0)
  )

  const taskActive = computed(() =>
    task.value && ['PENDING', 'RUNNING'].includes(task.value.status)
  )

  const generateButtonText = computed(() => {
    if (taskActive.value) return `生成中 ${task.value.progress || 0}%`
    if (generating.value) return '正在提交生成任务…'
    if (!auth.isLogin) return '登录后生成下一章'
    return '提交选择并生成下一章'
  })

  const createButtonText = computed(() => {
    if (taskActive.value && view.value === 'create') return `生成中 ${task.value.progress || 0}%`
    if (creating.value) return '正在提交生成任务…'
    return '生成世界种子'
  })

  const paragraphs = (text) => String(text || '')
    .split(/\n+/)
    .map(item => item.trim())
    .filter(Boolean)

  const textLength = (text) => String(text || '').replace(/\s/g, '').length

  const votePercent = (choiceKey) => {
    if (!activeBranch.value || !voteTotal.value) return 0
    const count = Number(activeBranch.value.voteCounts?.[choiceKey] || 0)
    return Math.round(count * 100 / voteTotal.value)
  }

  const branchVoteTotal = (branch) =>
    Object.values(branch?.voteCounts || {})
      .reduce((sum, count) => sum + Number(count || 0), 0)

  const loadSeeds = async () => {
    listLoading.value = true
    listError.value = ''
    try {
      const res = await getWorldSeeds()
      if (res.code !== 200) throw new Error(res.msg || '加载失败')
      seeds.value = res.data || []
    } catch (error) {
      listError.value = error?.response?.data?.msg || error?.message || '加载失败'
    } finally {
      listLoading.value = false
    }
  }

  const loadDetail = async (seedId) => {
    if (!seedId) return
    detailLoading.value = true
    detailError.value = ''
    const previousLineId = activeLineId.value
    const previousBranchId = activeBranchId.value
    try {
      const res = await getWorldSeed(seedId)
      if (res.code !== 200) throw new Error(res.msg || '加载失败')
      selectedSeed.value = res.data
      const lines = res.data?.lines || []
      const line = lines.find(item => item.id === previousLineId) || lines[0] || null
      activeLineId.value = line?.id || null
      const branches = line?.branches || []
      const branch = branches.find(item => item.id === previousBranchId) || branches[0] || null
      activeBranchId.value = branch?.id || null
      backgroundExpanded.value = false
      await loadChapters(true)
    } catch (error) {
      detailError.value = error?.response?.data?.msg || error?.message || '加载失败'
    } finally {
      detailLoading.value = false
    }
  }

  const loadChapters = async (reset = false) => {
    const branch = activeBranch.value
    if (!branch || chapterLoading.value) return
    chapterLoading.value = true
    try {
      const res = await getWorldChapters(
        branch.id,
        reset ? null : chapterCursor.value,
        10
      )
      if (res.code !== 200) throw new Error(res.msg || '章节加载失败')
      const incoming = [...(res.data?.items || [])].reverse()
      if (reset) {
        chapters.value = incoming
        const latest = incoming[incoming.length - 1]
        expandedChapterIds.value = new Set(latest ? [latest.id] : [])
      } else {
        const existing = new Set(chapters.value.map(item => item.id))
        chapters.value = [...incoming.filter(item => !existing.has(item.id)), ...chapters.value]
      }
      chapterCursor.value = res.data?.nextBeforeChapterNo || null
      chapterHasMore.value = Boolean(res.data?.hasMore)
    } catch (error) {
      ElMessage.error(error?.response?.data?.msg || error?.message || '章节加载失败')
    } finally {
      chapterLoading.value = false
    }
  }

  const isChapterExpanded = (chapterId) => expandedChapterIds.value.has(chapterId)

  const toggleChapter = (chapterId) => {
    const next = new Set(expandedChapterIds.value)
    if (next.has(chapterId)) next.delete(chapterId)
    else next.add(chapterId)
    expandedChapterIds.value = next
  }

  const openDetail = async (seed) => {
    stopPolling()
    view.value = 'detail'
    selectedSeed.value = null
    activeLineId.value = null
    activeBranchId.value = null
    chapters.value = []
    await loadDetail(seed.id)
  }

  const selectLine = async (lineId) => {
    activeLineId.value = lineId
    activeBranchId.value = activeLine.value?.branches?.[0]?.id || null
    selectedChoiceKey.value = ''
    customChoice.value = ''
    await loadChapters(true)
  }

  const selectBranch = async (branchId) => {
    activeBranchId.value = branchId
    await loadChapters(true)
  }

  const selectChoice = (choice) => {
    selectedChoiceKey.value = choice.key
    customChoice.value = choice.label
  }

  const openCreate = () => {
    if (!auth.isLogin) {
      ElMessage.warning('请先登录后创建世界种子')
      router.push('/login')
      return
    }
    view.value = 'create'
  }

  const createSeed = async () => {
    if (!auth.isLogin) {
      router.push('/login')
      return
    }
    creating.value = true
    try {
      const submitted = await submitWorldSeed(form.value, createIdempotencyKey())
      requireSuccess(submitted)
      task.value = submitted.data
      const completed = await waitForTask(submitted.data.id)
      const seedId = completed.result?.seedId
      clearWorldSeedCache()
      await loadSeeds()
      form.value = {sourceTitle: '', sourceAuthor: '', sourceText: '', guidance: ''}
      const seed = seeds.value.find(item => String(item.id) === String(seedId))
      if (seed) await openDetail(seed)
      else await loadDetail(seedId)
    } catch (error) {
      ElMessage.error(error?.response?.data?.msg || error?.message || '生成失败')
    } finally {
      creating.value = false
      task.value = null
    }
  }

  const continueStory = async () => {
    if (!auth.isLogin) {
      ElMessage.warning('请先登录后参与推演')
      router.push('/login')
      return
    }
    const line = activeLine.value
    const branch = activeBranch.value
    if (!line || !branch) return
    const selected = choiceOptions.find(item => item.key === selectedChoiceKey.value)
    const choiceText = customChoice.value.trim() || selected?.label || '顺着当前线索继续'
    const choiceKey = selectedChoiceKey.value || 'continue'
    generating.value = true
    try {
      const submitted = await submitWorldChapter({
        lineId: line.id,
        branchId: branch.id,
        choiceKey,
        choiceText
      }, createIdempotencyKey())
      requireSuccess(submitted)
      task.value = submitted.data
      const completed = await waitForTask(submitted.data.id)
      activeBranchId.value = completed.result?.branchId || activeBranchId.value
      customChoice.value = ''
      selectedChoiceKey.value = ''
      clearWorldSeedCache()
      await loadDetail(selectedSeed.value.id)
      ElMessage.success('下一章已生成')
    } catch (error) {
      ElMessage.error(error?.response?.data?.msg || error?.message || '推演失败')
    } finally {
      generating.value = false
      task.value = null
    }
  }

  const waitForTask = async (taskId) => {
    const version = ++pollVersion
    while (version === pollVersion) {
      await delay(1200)
      const res = await getWorldTask(taskId)
      if (res.code !== 200 || !res.data) throw new Error(res.msg || '任务状态读取失败')
      task.value = res.data
      if (res.data.status === 'SUCCESS') return res.data
      if (res.data.status === 'FAILED') {
        throw new Error(res.data.errorMessage || '生成失败，请重试')
      }
    }
    throw new Error('任务已取消')
  }

  const stopPolling = () => {
    pollVersion += 1
    task.value = null
  }

  const goBack = () => {
    stopPolling()
    if (view.value === 'home') {
      router.back()
      return
    }
    view.value = 'home'
    chapters.value = []
  }

  const requireSuccess = (res) => {
    if (res.code !== 200) throw new Error(res.msg || '请求失败')
  }

  const createIdempotencyKey = () => {
    if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
    return `${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))

  onBeforeUnmount(stopPolling)

  return {
    view,
    seeds,
    listState,
    detailState,
    selectedSeed,
    activeLine,
    activeBranch,
    activeLineId,
    activeBranchId,
    backgroundExpanded,
    selectedChoiceKey,
    customChoice,
    chapters,
    chapterHasMore,
    chapterLoading,
    task,
    taskActive,
    creating,
    generating,
    form,
    choiceOptions,
    pageTitle,
    pageSubtitle,
    voteTotal,
    generateButtonText,
    createButtonText,
    paragraphs,
    textLength,
    votePercent,
    branchVoteTotal,
    loadSeeds,
    loadChapters,
    isChapterExpanded,
    toggleChapter,
    openDetail,
    selectLine,
    selectBranch,
    selectChoice,
    openCreate,
    createSeed,
    continueStory,
    goBack
  }
}
