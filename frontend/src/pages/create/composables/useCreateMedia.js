import {computed, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {compressVideo, trimVideo, uploadFile} from '@/api/inspire.js'

export function useCreateMedia({form}) {
  // 文件上传（支持一次选多张，逐张并发上传）
  const fileInput = ref(null)
  const MAX_IMAGES = 9
  const uploadingCount = ref(0)
  const imageProgress = ref({})            // 本地预览地址 -> 该张图片的上传百分比
  const uploading = computed(() => uploadingCount.value > 0)
  const uploadPercent = computed(() => {
    const list = Object.values(imageProgress.value)
    if (!list.length) return 0
    return Math.round(list.reduce((a, b) => a + b, 0) / list.length)
  })
  const triggerUpload = () => { fileInput.value?.click() }

  // ===== 图片滤镜（纯前端 Canvas 烘焙，不需要后端） =====
  const IMAGE_FILTERS = [
    { key: 'none', label: '原图', css: 'none' },
    { key: 'fresh', label: '清新', css: 'saturate(1.18) brightness(1.06) contrast(0.98)' },
    { key: 'warm', label: '暖阳', css: 'sepia(0.28) saturate(1.22) brightness(1.05)' },
    { key: 'film', label: '胶片', css: 'contrast(1.18) saturate(0.82) sepia(0.18)' },
    { key: 'cool', label: '冷调', css: 'hue-rotate(-14deg) saturate(1.08) brightness(1.02)' },
    { key: 'mono', label: '黑白', css: 'grayscale(1) contrast(1.06)' }
  ]
  const selectedImage = ref('')
  const filtering = ref(false)
  const coverImage = ref('')
  const gridPreviewOpen = ref(false)
  const dragImageIndex = ref(-1)
  const gridImages = computed(() => {
    if (!form.value.images.length) return []
    const cover = coverImage.value && form.value.images.includes(coverImage.value)
      ? coverImage.value
      : form.value.images[0]
    return [cover, ...form.value.images.filter(img => img !== cover)].slice(0, 9)
  })
  const setCover = (url) => {
    coverImage.value = url
    ElMessage.success('已设为封面')
  }
  const dropImage = (targetIndex) => {
    const from = dragImageIndex.value
    dragImageIndex.value = -1
    if (from < 0 || from === targetIndex) return
    const list = [...form.value.images]
    const [moved] = list.splice(from, 1)
    list.splice(targetIndex, 0, moved)
    form.value.images = list
  }
  // 记录「当前图 -> 原图」，保证每次滤镜都是从原图重新烘焙，不会叠加
  const imageOrigin = ref({})
  const originOf = (url) => imageOrigin.value[url] || url
  /** 滤镜预览与烘焙都以原图为准 */
  const filterPreviewSrc = computed(() => originOf(selectedImage.value))

  /** 把 CSS 滤镜烘焙进图片，返回处理后的 Blob */
  const bakeFilter = (src, css) => new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = img.naturalWidth || img.width
        canvas.height = img.naturalHeight || img.height
        const ctx = canvas.getContext('2d')
        // Canvas filter：Chrome 52+ / Firefox 49+ / Safari 17+
        if ('filter' in ctx) ctx.filter = css
        ctx.drawImage(img, 0, 0)
        canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('导出失败')), 'image/jpeg', 0.92)
      } catch (e) {
        reject(e)
      }
    }
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = src
  })

  const applyFilter = async (preset) => {
    const url = selectedImage.value
    if (!url || filtering.value) return
    if (/\.(mp4|webm|mov|m4v)(\?.*)?$/i.test(String(url))) return ElMessage.warning('视频不支持滤镜')
    const origin = originOf(url)

    // 选「原图」= 还原成最初上传的那张
    if (preset.key === 'none') {
      if (origin !== url) {
        replaceMediaUrl(url, origin)
        imageOrigin.value[origin] = origin
        delete imageOrigin.value[url]
        selectedImage.value = origin
        ElMessage.success('已还原为原图')
      }
      return
    }

    filtering.value = true
    try {
      // 始终基于原图烘焙，避免「黑白 + 暖阳」这种叠加
      const blob = await bakeFilter(origin, preset.css)
      const fd = new FormData()
      fd.append('file', blob, 'filtered.jpg')
      const res = await uploadFile(fd)
      if (res.code === 200 && res.data?.url) {
        imageOrigin.value[res.data.url] = origin
        delete imageOrigin.value[url]
        replaceMediaUrl(url, res.data.url)
        selectedImage.value = res.data.url
        ElMessage.success('已应用「' + preset.label + '」')
      } else {
        ElMessage.error(res.msg || '滤镜应用失败')
      }
    } catch (e) {
      ElMessage.error('滤镜应用失败：' + (e.message || '未知错误'))
    } finally {
      filtering.value = false
    }
  }

  // ===== 视频 =====
  const VIDEO_MAX_SIZE = 50 * 1024 * 1024
  const videoMeta = ref({})                 // 服务端 url -> { name, sizeText, duration, processing }
  const videoUploadTasks = ref([])
  const keepOriginalOnly = ref(false)       // 只保留最终版：处理后删掉原片
  const isVideoUrl = (u) => /\.(mp4|webm|mov|m4v)(\?.*)?$/i.test(String(u || ''))
  const videoItems = computed(() => form.value.images
    .filter(isVideoUrl)
    .map(url => ({ url, ...(videoMeta.value[url] || {}) })))

  const replaceMediaUrl = (oldUrl, newUrl) => {
    const idx = form.value.images.indexOf(oldUrl)
    if (idx >= 0) form.value.images.splice(idx, 1, newUrl)
  }

  // 视频不做前端压缩，原样上传，压缩/裁剪交给服务端 ffmpeg
  const uploadVideoOne = async (raw) => {
    if (raw.size > VIDEO_MAX_SIZE) { ElMessage.warning('视频不能超过 50MB'); return }
    const localUrl = URL.createObjectURL(raw)
    form.value.images.push(localUrl)
    const task = {
      id: localUrl,
      raw,
      localUrl,
      serverUrl: '',
      name: raw.name || '视频',
      sizeText: (raw.size / 1024 / 1024).toFixed(1) + 'MB',
      progress: 0,
      status: 'uploading',
      error: '',
      objectUrlRevoked: false
    }
    videoUploadTasks.value.push(task)
    await performVideoUpload(task)
  }

  const performVideoUpload = async (task) => {
    if (!task?.raw) return
    task.status = 'uploading'
    task.error = ''
    task.progress = Math.max(0, Number(task.progress || 0))
    imageProgress.value[task.localUrl] = task.progress
    uploadingCount.value++
    try {
      const fd = new FormData()
      fd.append('file', task.raw, task.raw.name)
      const res = await uploadFile(fd, (evt) => {
        const total = evt.total || evt.loaded || 1
        const percent = Math.min(100, Math.round(evt.loaded * 100 / total))
        task.progress = percent
        if (percent >= 100) task.status = 'processing'
        imageProgress.value[task.localUrl] = percent
      })
      const idx = form.value.images.indexOf(task.localUrl)
      if (res.code === 200 && res.data?.url) {
        if (idx >= 0) form.value.images.splice(idx, 1, res.data.url)
        if (!coverImage.value) coverImage.value = res.data.url
        videoMeta.value[res.data.url] = {
          name: res.data.name || task.name,
          sizeText: task.sizeText,
          duration: Number(res.data.duration || 0),
          processing: ''
        }
        task.serverUrl = res.data.url
        task.status = 'done'
        task.progress = 100
        delete imageProgress.value[task.localUrl]
        if (!task.objectUrlRevoked) {
          URL.revokeObjectURL(task.localUrl)
          task.objectUrlRevoked = true
        }
      } else {
        throw new Error(res.msg || '视频上传失败')
      }
    } catch (e) {
      task.status = 'failed'
      task.error = e?.response?.data?.msg || e?.message || '网络中断，请重试'
      ElMessage.error(task.error)
    } finally {
      uploadingCount.value = Math.max(0, uploadingCount.value - 1)
    }
  }

  const retryVideoUpload = (task) => {
    task.progress = 0
    task.status = 'uploading'
    imageProgress.value[task.localUrl] = 0
    return performVideoUpload(task)
  }

  const applyProcessed = (oldUrl, data) => {
    replaceMediaUrl(oldUrl, data.url)
    delete videoMeta.value[oldUrl]
    videoMeta.value[data.url] = {
      name: data.name,
      sizeText: data.sizeText || '',
      duration: Number(data.duration || 0),
      processing: ''
    }
  }

  const doCompress = async (v) => {
    if (videoMeta.value[v.url]) videoMeta.value[v.url].processing = 'compress'
    else videoMeta.value[v.url] = { processing: 'compress' }
    try {
      const res = await compressVideo(v.url, 28, !keepOriginalOnly.value)
      if (res.code === 200 && res.data?.url) {
        applyProcessed(v.url, res.data)
        ElMessage.success('压缩完成，当前大小 ' + (res.data.sizeText || ''))
      } else {
        ElMessage.error(res.msg || '压缩失败')
        if (videoMeta.value[v.url]) videoMeta.value[v.url].processing = ''
      }
    } catch (e) {
      ElMessage.error('压缩失败，请确认服务端已安装 ffmpeg')
      if (videoMeta.value[v.url]) videoMeta.value[v.url].processing = ''
    }
  }

  const trimDialogVisible = ref(false)
  const trimTarget = ref(null)
  const trimStart = ref('0')
  const trimDuration = ref('')
  const trimming = ref(false)

  const openTrim = (v) => {
    trimTarget.value = v
    trimStart.value = '0'
    trimDuration.value = v.duration ? String(v.duration) : ''
    trimDialogVisible.value = true
  }

  const doTrim = async () => {
    const target = trimTarget.value
    if (!target) return
    if (!trimDuration.value || Number(trimDuration.value) <= 0) {
      ElMessage.warning('请填写需要保留的时长（秒）')
      return
    }
    trimming.value = true
    try {
      const res = await trimVideo(target.url, Number(trimStart.value) || 0, Number(trimDuration.value), !keepOriginalOnly.value)
      if (res.code === 200 && res.data?.url) {
        applyProcessed(target.url, res.data)
        trimDialogVisible.value = false
        ElMessage.success('裁剪完成')
      } else {
        ElMessage.error(res.msg || '裁剪失败')
      }
    } catch (e) {
      ElMessage.error('裁剪失败，请确认服务端已安装 ffmpeg')
    } finally {
      trimming.value = false
    }
  }

  // C. 前端压缩：Canvas 缩到最大边 1920，输出 jpeg（质量 0.85），GIF 不处理
  const compressImage = (file) => new Promise((resolve) => {
    if (!file.type.startsWith('image/') || file.type === 'image/gif') return resolve(file)
    const img = new Image()
    const objUrl = URL.createObjectURL(file)
    img.onload = () => {
      const maxSide = 1920
      let w = img.width, h = img.height
      if (Math.max(w, h) > maxSide) {
        const ratio = maxSide / Math.max(w, h)
        w = Math.round(w * ratio); h = Math.round(h * ratio)
      }
      const canvas = document.createElement('canvas')
      canvas.width = w; canvas.height = h
      canvas.getContext('2d').drawImage(img, 0, 0, w, h)
      URL.revokeObjectURL(objUrl)
      canvas.toBlob(blob => resolve(blob || file), 'image/jpeg', 0.85)
    }
    img.onerror = () => { URL.revokeObjectURL(objUrl); resolve(file) }
    img.src = objUrl
  })

  // 单张上传：立即本地预览 → 压缩 → 带进度上传 → 用服务端地址替换预览
  const uploadOne = async (raw) => {
    const localUrl = URL.createObjectURL(raw)
    form.value.images.push(localUrl)
    imageProgress.value[localUrl] = 0
    uploadingCount.value++

    try {
      const blob = await compressImage(raw)
      const fd = new FormData()
      fd.append('file', blob, raw.name.replace(/\.[^.]+$/, '') + '.jpg')

      const res = await uploadFile(fd, (evt) => {
        const total = evt.total || evt.loaded || 1
        imageProgress.value[localUrl] = Math.min(99, Math.round(evt.loaded * 100 / total))
      })
      const idx = form.value.images.indexOf(localUrl)
      if (res.code === 200 && res.data?.url) {
        if (idx >= 0) form.value.images.splice(idx, 1, res.data.url)
        if (!coverImage.value) coverImage.value = res.data.url
        // 记录原图，滤镜始终从这张开始烘焙
        imageOrigin.value[res.data.url] = res.data.url
      } else {
        if (idx >= 0) form.value.images.splice(idx, 1)
        ElMessage.error(res.msg || '上传失败')
      }
    } catch (err) {
      const idx = form.value.images.indexOf(localUrl)
      if (idx >= 0) form.value.images.splice(idx, 1)
      ElMessage.error(err?.response?.data?.msg || err?.message || '上传失败')
    } finally {
      URL.revokeObjectURL(localUrl)
      delete imageProgress.value[localUrl]
      uploadingCount.value = Math.max(0, uploadingCount.value - 1)
    }
  }

  // 批量：一次可选中多张，超出上限的部分自动忽略
  const handleFile = async (e) => {
    const files = Array.from(e.target.files || [])
    e.target.value = ''
    if (!files.length) return

    const images = files.filter(f => f.type.startsWith('image/'))
    const videos = files.filter(f => f.type.startsWith('video/'))
    if (!images.length && !videos.length) return ElMessage.warning('请选择图片或视频文件')

    const room = MAX_IMAGES - form.value.images.length
    if (room <= 0) return ElMessage.warning(`最多上传 ${MAX_IMAGES} 个文件`)
    const picked = [...images, ...videos]
    if (picked.length > room) ElMessage.warning(`最多 ${MAX_IMAGES} 个，已忽略多余的 ${picked.length - room} 个`)

    await Promise.all(picked.slice(0, room)
      .map(f => f.type.startsWith('video/') ? uploadVideoOne(f) : uploadOne(f)))
  }

  const removeImage = (idx) => {
    const url = form.value.images[idx]
    const task = videoUploadTasks.value.find(item => item.localUrl === url || item.serverUrl === url)
    if (task) {
      if (!task.objectUrlRevoked) {
        URL.revokeObjectURL(task.localUrl)
        task.objectUrlRevoked = true
      }
      delete imageProgress.value[task.localUrl]
      videoUploadTasks.value = videoUploadTasks.value.filter(item => item !== task)
    }
    form.value.images = form.value.images.filter((_, index) => index !== idx)
    if (selectedImage.value === url) selectedImage.value = form.value.images[0] || ''
    if (coverImage.value === url || !form.value.images.includes(coverImage.value)) {
      coverImage.value = form.value.images[0] || ''
    }
  }

  return {
    fileInput,
    MAX_IMAGES,
    uploadingCount,
    imageProgress,
    uploading,
    uploadPercent,
    triggerUpload,
    IMAGE_FILTERS,
    selectedImage,
    filtering,
    coverImage,
    gridPreviewOpen,
    dragImageIndex,
    gridImages,
    setCover,
    dropImage,
    imageOrigin,
    originOf,
    filterPreviewSrc,
    applyFilter,
    VIDEO_MAX_SIZE,
    videoMeta,
    videoUploadTasks,
    keepOriginalOnly,
    isVideoUrl,
    videoItems,
    replaceMediaUrl,
    uploadVideoOne,
    performVideoUpload,
    retryVideoUpload,
    applyProcessed,
    doCompress,
    trimDialogVisible,
    trimTarget,
    trimStart,
    trimDuration,
    trimming,
    openTrim,
    doTrim,
    compressImage,
    uploadOne,
    handleFile,
    removeImage
  }
}
