/**
 * 文件：frontend/src/pages/create/composables/useCreateImageSuggestions.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {computed, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {suggestImages as suggestImagesApi, uploadFromUrl} from '@/api/inspire.js'

export function useCreateImageSuggestions({form, coverImage}) {
  const imageSuggestOpen = ref(false)
  const imageSuggestions = ref([])
  const selectedSuggests = ref([])          // 已勾选的配图（可跨「换一批」累积）
  const imageKeywords = ref('')
  const addingSuggest = ref(false)

  // 点图切换勾选状态（支持多选）
  const toggleSuggestPick = (url) => {
    const i = selectedSuggests.value.indexOf(url)
    if (i >= 0) selectedSuggests.value.splice(i, 1)
    else selectedSuggests.value.push(url)
  }

  // 展开/收起内联 AI 配图面板；首次展开时拉取推荐
  const toggleSuggest = async () => {
    imageSuggestOpen.value = !imageSuggestOpen.value
    if (imageSuggestOpen.value && imageSuggestions.value.length === 0) await suggestImages()
  }

  const suggestLoading = ref(false)
  const suggestError = ref('')
  const suggestPage = ref(1)                 // 当前批次页码，「换一批」递增
  const suggestState = computed(() => {
    if (suggestLoading.value && !imageSuggestions.value.length) return 'loading'
    if (suggestError.value && !imageSuggestions.value.length) return 'error'
    return imageSuggestions.value.length ? 'ready' : 'empty'
  })

  // 取一批推荐图（走统一请求封装：自动带上 baseURL 与 Token）
  const fetchSuggest = async (keyword, page) => {
    const res = await suggestImagesApi(keyword, page)
    return (res && res.code === 200 && Array.isArray(res.data)) ? res.data : []
  }

  // next=true 表示「换一批」：往后翻一页，返回不同批次
  const suggestImages = async ({ next = false } = {}) => {
    const plain = (form.value.content || '').replace(/<[^>]*>/g, ' ').trim()
    const keyword = form.value.title || plain.slice(0, 50) || 'inspiration'
    if (keyword !== imageKeywords.value) {
      // 关键词变了：从头开始
      imageKeywords.value = keyword
      suggestPage.value = 1
    } else if (next) {
      suggestPage.value += 1
    }
    suggestLoading.value = true
    suggestError.value = ''
    try {
      let list = await fetchSuggest(keyword, suggestPage.value)
      // 翻到末页后回到第一页，保证「换一批」永远有内容
      if (!list.length && suggestPage.value > 1) {
        suggestPage.value = 1
        list = await fetchSuggest(keyword, 1)
      }
      imageSuggestions.value = list
      if (!list.length) ElMessage.warning('没有找到合适的配图，换个标题或关键词再试试')
    } catch (e) {
      console.error('suggestImages failed:', e)
      imageSuggestions.value = []
      suggestError.value = e?.message || 'suggest images failed'
      ElMessage.error('获取配图失败')
    } finally {
      suggestLoading.value = false
    }
  }



  // 把勾选的配图批量转存到自己的存储并加入图片列表（面板保持展开，方便继续换一批再挑）
  const useSuggestedImages = async () => {
    if (!selectedSuggests.value.length) return
    const urls = [...selectedSuggests.value]
    addingSuggest.value = true
    try {
      const results = await Promise.all(urls.map(u => uploadFromUrl(u).catch(() => null)))
      let ok = 0
      results.forEach(data => {
        if (data && data.code === 200 && data.data?.url) {
          form.value.images.push(data.data.url)
          if (!coverImage.value) coverImage.value = data.data.url
          ok++
        }
      })
      if (ok) {
        ElMessage.success(`已添加 ${ok} 张配图`)
        selectedSuggests.value = []
      } else {
        ElMessage.error('配图添加失败，请重试')
      }
    } finally {
      addingSuggest.value = false
    }
  }

  return {
    imageSuggestOpen,
    imageSuggestions,
    selectedSuggests,
    imageKeywords,
    addingSuggest,
    toggleSuggestPick,
    toggleSuggest,
    suggestLoading,
    suggestError,
    suggestPage,
    suggestState,
    suggestImages,
    useSuggestedImages
  }
}
