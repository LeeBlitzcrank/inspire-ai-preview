import {ref} from 'vue'
import {getCategoryTree} from '@/api/inspire.js'

export function useCreateCategories() {
  const DEFAULT_TAGS = ['美食','运动','电影','穿搭','文案','旅游','摄影','其他']
  const tags = ref([...DEFAULT_TAGS])

  const loadTags = async () => {
    try {
      const res = await getCategoryTree()
      const names = (res.data || []).map(c => c.name).filter(Boolean)
      if (names.length) {
        tags.value = names.includes('其他') ? names : [...names, '其他']
      }
    } catch (e) {
      console.error('[tags]', e)
    }
  }

  return {tags, loadTags}
}
