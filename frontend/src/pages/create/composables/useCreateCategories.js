/**
 * 文件：frontend/src/pages/create/composables/useCreateCategories.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
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
