import {ElMessage} from '@/utils/uiFeedback.js'
import {createInspire, updateInspire} from '@/api/inspire.js'

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
