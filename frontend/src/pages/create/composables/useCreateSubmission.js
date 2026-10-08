/**
 * 文件：frontend/src/pages/create/composables/useCreateSubmission.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {ElMessage} from '@/utils/uiFeedback.js'
import {createInspire, updateInspire} from '@/api/inspire.js'
import {INPUT_LIMITS} from '@/utils/validation.js'

export function useCreateSubmission({
  router,
  form,
  editId,
  quoteSource,
  coverImage,
  uploading,
  titleLength,
  TITLE_MAX_LENGTH,
  contentLen,
  loading,
  localDraftId,
  removeLocalDraft
}) {
  const goBack = () => { router.back() }
  const submit = async (status) => {
    if (uploading.value) return ElMessage.warning('图片上传中，请稍候再提交')
    // 剔除未完成上传的本地预览（blob:）地址
    const pending = form.value.images.filter(i => typeof i === 'string' && i.startsWith('blob:'))
    if (pending.length > 0) return ElMessage.warning('有图片尚未上传完成，请稍候')
    if (!form.value.title) return ElMessage.warning('请填写标题')
    if (titleLength.value > TITLE_MAX_LENGTH) return ElMessage.warning(`标题不能超过 ${TITLE_MAX_LENGTH} 个字`)
    if (!form.value.tag) return ElMessage.warning('请选择分类')
    if (!contentLen.value) return ElMessage.warning('请填写灵感详情')
    if (contentLen.value > INPUT_LIMITS.contentMax) {
      return ElMessage.warning(`正文不能超过 ${INPUT_LIMITS.contentMax} 个字`)
    }
    loading.value = true
    try {
      let payload = { ...form.value, status: status !== undefined ? status : 1 }
      if (!editId.value && quoteSource.value?.id) payload.quoteInspireId = quoteSource.value.id
      payload.img = coverImage.value || payload.images[0] || ''
      payload.images = JSON.stringify(payload.images)
      let res
      if (editId.value) {
        res = await updateInspire(editId.value, payload)
      } else {
        res = await createInspire(payload)
      }
      ElMessage.success(res.msg || (editId.value ? '修改成功' : '发布成功'))
      if (localDraftId.value) removeLocalDraft(localDraftId.value)
      router.push('/')
    } catch (e) { console.error(e) } finally { loading.value = false }
  }

  return {goBack, submit}
}
