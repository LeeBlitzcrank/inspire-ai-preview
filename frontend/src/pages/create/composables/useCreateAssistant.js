import {computed, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
    aiRewrite,
    aiTitles,
    deleteAiHistory,
    exploreInspiration,
    getAiHistory,
    getWordCloud,
    markAiHistorySelected,
    saveAiHistory
} from '@/api/inspire.js'
import {autoFormatHtml} from '@/utils/autoFormat.js'

export function useCreateAssistant({form, descRef, setContent, getEditorPlainText, onDescInput}) {
  // AI 探索状态
  const aiKeyword = ref('')
  const exploring = ref(false)
  const options = ref([])
  const summary = ref('')
  const path = ref([])
  const pathLabels = ref([])
  const leafContent = ref(null)
  const contentVariants = ref([])
  const activeVariant = ref(0)
  const historyList = ref([])
  const historyOpen = ref(false)
  const currentHistoryId = ref('')
  const TITLE_MAX_LENGTH = 16
  const titleLength = computed(() => Array.from(form.value.title || '').length)
  const titleSuggestions = ref([])
  const titleGenerating = ref(false)
  const rewriteOpen = ref(false)
  const rewriting = ref(false)
  const savedEditorRange = ref(null)
  const REWRITE_STYLES = ['简洁', '温柔', '文艺', '干货', '活泼']

  const applyTitle = (title) => {
    form.value.title = clampTitle(title)
    titleSuggestions.value = []
  }

  const generateTitles = async () => {
    const content = getEditorPlainText()
    if (!content.trim()) return ElMessage.warning('请先写一些正文内容')
    titleGenerating.value = true
    try {
      const res = await aiTitles({ content })
      titleSuggestions.value = (res.data?.titles || []).map(clampTitle).filter(Boolean)
      if (!titleSuggestions.value.length) ElMessage.warning('没有生成可用标题')
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || 'AI 标题生成失败')
    } finally {
      titleGenerating.value = false
    }
  }

  const captureEditorSelection = () => {
    const selection = window.getSelection()
    if (!selection || !selection.rangeCount || !descRef.value) return
    const range = selection.getRangeAt(0)
    if (descRef.value.contains(range.commonAncestorContainer)) {
      savedEditorRange.value = range.cloneRange()
    }
  }

  const rewriteSelection = async (style) => {
    const range = savedEditorRange.value
    const text = range?.toString().trim()
    if (!text) return ElMessage.warning('请先选中要改写的正文')
    rewriting.value = true
    try {
      const res = await aiRewrite({ text, style })
      const nextText = res.data?.text
      if (!nextText) throw new Error('AI 未返回内容')
      range.deleteContents()
      range.insertNode(document.createTextNode(nextText))
      range.collapse(false)
      const selection = window.getSelection()
      selection.removeAllRanges()
      selection.addRange(range)
      onDescInput()
      rewriteOpen.value = false
      ElMessage.success(`已改写为「${style}」风格`)
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || 'AI 改写失败')
    } finally {
      rewriting.value = false
    }
  }

  // —— AI 探索 · 词云（把当前层的选项渲染成流动的书法词） ——
  const pickedWordId = ref(null)
  const CLOUD_SIZES = ['main', 'mid', 'small', 'mid']
  // 词云固定展示的一级方向词：点词只更新下方选项，词云本身永不变化
  // 词云是「推荐词/探索方向」，不是分类；这里是接口不可用时的兜底词
  const DEFAULT_CLOUD_WORDS = ['小户型收纳', '一人食', '秋日露营', '通勤穿搭', '手机摄影', '周末短途', '手冲咖啡', '情绪管理']
  // 后台可配置：优先读取 sys_word_cloud，接口不可用时退回默认词
  const cloudWords = ref(DEFAULT_CLOUD_WORDS.map(w => ({ word: w, weight: 0 })))

  const loadCloudWords = async () => {
    try {
      const res = await getWordCloud()
      const words = (res.data || [])
        .map(w => ({ word: w.word, weight: Number(w.weight || 0) }))
        .filter(w => w.word)
      if (words.length) cloudWords.value = words
    } catch (e) {
      console.error('[word-cloud]', e)
    }
  }

  const cloudLanes = computed(() => {
    const src = cloudWords.value.map((item, i) => ({
      id: 'd' + i,
      label: item.word,
      deco: true,
      size: item.weight >= 5 ? 'main'
        : item.weight >= 3 ? 'mid'
        : item.weight > 0 ? 'small'
        : CLOUD_SIZES[i % CLOUD_SIZES.length],
      rot: ((i * 37) % 13) - 6
    }))
    const laneCount = src.length <= 4 ? 1 : (src.length <= 8 ? 2 : 3)
    const lanes = Array.from({ length: laneCount }, () => [])
    src.forEach((w, i) => lanes[i % laneCount].push(w))
    return lanes
  })

  const laneTop = (i) => {
    if (cloudLanes.value.length === 1) return '44%'
    return ['20%', '48%', '76%'][i] || '48%'
  }
  const laneDur = (i) => ['30s', '22s', '26s'][i] || '26s'

  // 点词 = 以该方向为关键词探索；结果只更新下方选项与摘要，词云保持不动
  const pickCloudWord = (w) => {
    pickedWordId.value = w.id
    // 立即给出反馈（与预览一致）：高亮 + 提示文案
    summary.value = '已选择方向「' + w.label + '」，正在为你探索…'
    exploreByWord(w.label)
  }

  // 用某个词作为关键词发起探索（点默认词时的入口）
  const exploreByWord = async (keyword) => {
    if (!keyword) return
    aiKeyword.value = keyword
    path.value = []; pathLabels.value = []
    leafContent.value = null
    exploring.value = true
    try {
      const res = await exploreInspiration({ keyword, path: '' })
      if (res.code === 200) {
        applyExploreData(res.data, keyword, '')
      }
    } catch (e) {
      console.error(e)
      ElMessage.warning('探索失败，请重试')
    } finally {
      exploring.value = false
    }
  }

  const handleExplore = async () => {
    if (!aiKeyword.value.trim()) return ElMessage.warning('请输入关键词')
    exploring.value = true; options.value = []; summary.value = ''; leafContent.value = null
    pickedWordId.value = null
    path.value = []; pathLabels.value = []
    try {
      const res = await exploreInspiration({ keyword: aiKeyword.value, path: '' })
      if (res.code === 200) {
        applyExploreData(res.data, aiKeyword.value, '')
      }
    } catch (e) { ElMessage.warning('探索失败请重试') }
    finally { exploring.value = false }
  }

  const selectOption = async (opt) => {
    path.value.push(opt.id)
    pathLabels.value.push(opt.label)
    exploring.value = true
    // 保留旧词与高亮，等新数据回来再替换（避免闪烁，和预览一致）
    try {
      const res = await exploreInspiration({ keyword: aiKeyword.value, path: path.value.join(',') })
      if (res.code === 200) {
        applyExploreData(res.data, aiKeyword.value, path.value.join(','))
      }
    } catch (e) { console.error(e) }
    finally { exploring.value = false }
  }

  const clampTitle = (value) => Array.from(String(value || '')).slice(0, TITLE_MAX_LENGTH).join('')

  const applyContent = (c) => {
    leafContent.value = c
    options.value = []
    // 一次生成的多组风格候选（后端 content.variants）
    contentVariants.value = Array.isArray(c.variants) ? c.variants.filter(v => v && v.text) : []
    activeVariant.value = 0
    form.value.title = clampTitle(c.title || form.value.title)
    form.value.tag = c.tag || form.value.tag
    // 编辑器以 HTML 存储：AI 返回的纯文本先自动排版（段落/编号/清单）再追加
    const add = c.text ? autoFormatHtml(c.text) : ''
    setContent((form.value.content || '') + add)
  }

  const loadAiHistory = async () => {
    if (!sessionStorage.getItem('isLogin')) return
    try {
      const res = await getAiHistory(20)
      historyList.value = res.data || []
    } catch (e) {
      console.error('[ai-history]', e)
    }
  }

  const recordAiHistory = async (data, keyword, pathString) => {
    if (!sessionStorage.getItem('isLogin') || !data?.content) return
    try {
      const res = await saveAiHistory({
        keyword,
        path: pathString || '',
        cacheKey: data.cacheKey || '',
        result: data
      })
      currentHistoryId.value = res.data?.id || ''
      historyList.value = [res.data, ...historyList.value.filter(item => item.id !== res.data?.id)].slice(0, 20)
    } catch (e) {
      console.error('[ai-history-save]', e)
    }
  }

  const applyExploreData = (data, keyword, pathString) => {
    options.value = data?.options || []
    summary.value = data?.summary || ''
    if (data?.content) {
      applyContent(data.content)
      recordAiHistory(data, keyword, pathString)
    } else {
      currentHistoryId.value = ''
    }
  }

  /** 切换风格：直接替换标题与正文（避免和上一组内容叠加） */
  const applyVariant = (index) => {
    const v = contentVariants.value[index]
    if (!v) return
    activeVariant.value = index
    if (v.title) form.value.title = clampTitle(v.title)
    if (v.text) setContent(autoFormatHtml(v.text))
    if (currentHistoryId.value) {
      markAiHistorySelected(currentHistoryId.value, {
        selectedIndex: index,
        selectedTitle: v.title || form.value.title || ''
      }).catch(() => {})
    }
    ElMessage.success('已切换到「' + (v.style || ('风格' + (index + 1))) + '」')
  }

  const useHistory = (item) => {
    const result = item?.result || {}
    const content = result.content
    if (!content) return
    aiKeyword.value = item.keyword || ''
    path.value = item.path ? String(item.path).split(',').filter(Boolean) : []
    pathLabels.value = []
    options.value = []
    summary.value = result.summary || ''
    leafContent.value = content
    contentVariants.value = Array.isArray(content.variants) ? content.variants.filter(v => v?.text) : []
    activeVariant.value = Math.max(0, Number(item.selectedIndex ?? 0))
    form.value.title = content.title || form.value.title
    form.value.tag = content.tag || form.value.tag
    if (content.text) setContent(autoFormatHtml(content.text))
    currentHistoryId.value = item.id
    ElMessage.success('已载入探索记录')
  }

  const removeHistory = async (id) => {
    try {
      await deleteAiHistory(id)
      historyList.value = historyList.value.filter(item => String(item.id) !== String(id))
      if (String(currentHistoryId.value) === String(id)) currentHistoryId.value = ''
    } catch (e) {
      ElMessage.error('删除探索记录失败')
    }
  }

  const formatHistoryTime = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  const reshuffle = async () => {
    exploring.value = true
    try {
      const p = path.value.join(',')
      const res = await exploreInspiration({ keyword: aiKeyword.value, path: p, refresh: true })
      if (res.code === 200) {
        options.value = res.data?.options || []
        summary.value = res.data?.summary || ''
      }
    } catch (e) { console.error(e) }
    finally { exploring.value = false }
  }

  const goToLevel = (idx) => {
    path.value = path.value.slice(0, idx + 1)
    pathLabels.value = pathLabels.value.slice(0, idx + 1)
    leafContent.value = null
    // 重载该层
    const last = path.value.join(',')
    exploring.value = true; options.value = []; summary.value = ''
    exploreInspiration({ keyword: aiKeyword.value, path: last || '' }).then(res => {
      if (res.code === 200) { options.value = res.data?.options || []; summary.value = res.data?.summary || '' }
    }).finally(() => exploring.value = false)
  }

  const resetExplore = () => {
    path.value = []; pathLabels.value = []; options.value = []; summary.value = ''; leafContent.value = null
    handleExplore()
  }

  return {
    aiKeyword,
    exploring,
    options,
    summary,
    path,
    pathLabels,
    leafContent,
    contentVariants,
    activeVariant,
    historyList,
    historyOpen,
    currentHistoryId,
    TITLE_MAX_LENGTH,
    titleLength,
    titleSuggestions,
    titleGenerating,
    rewriteOpen,
    rewriting,
    savedEditorRange,
    REWRITE_STYLES,
    applyTitle,
    generateTitles,
    captureEditorSelection,
    rewriteSelection,
    pickedWordId,
    DEFAULT_CLOUD_WORDS,
    cloudWords,
    loadCloudWords,
    cloudLanes,
    laneTop,
    laneDur,
    pickCloudWord,
    exploreByWord,
    handleExplore,
    selectOption,
    clampTitle,
    applyContent,
    loadAiHistory,
    useHistory,
    removeHistory,
    formatHistoryTime,
    reshuffle,
    goToLevel,
    resetExplore,
    applyVariant
  }
}
