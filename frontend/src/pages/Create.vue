<template>
  <div class="create-page">
    <div class="top-nav">
      <div v-if="editId" class="left-logo" @click="goBack">←</div>
      <div v-else class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/personal')">👤</div>
      </div>
    </div>
    <div class="input-box">
      <h2 class="page-title">{{ editId ? '编辑灵感' : '录入新灵感' }}</h2>
      <p class="page-desc">{{ editId ? '修改内容后保存' : '填写创意内容，保存到灵感库' }}</p>

      <!-- AI 探索区 -->
      <div v-if="!editId" class="ai-section">
        <!-- 薄荷绿书法词云面板（合并原「输入框 + 面包屑」） -->
        <div class="cloud-scene">
          <div class="cloud-hint">
            <span class="dot"></span>
            <template v-if="pathLabels.length">
              <span class="crumb" @click="resetExplore">{{ aiKeyword }}</span>
              <span v-for="(label, idx) in pathLabels" :key="idx" class="crumb">
                <span class="sep">›</span><span class="crumb-txt" @click="goToLevel(idx)">{{ label }}</span>
              </span>
            </template>
            <template v-else>点选一个方向，继续深入</template>
          </div>

          <div v-for="(lane, li) in cloudLanes" :key="li" class="lane" :style="{ top: laneTop(li) }">
            <div class="track" :style="{ animationDuration: laneDur(li) }">
              <template v-for="round in 2" :key="round">
                <span v-for="(w, wi) in lane" :key="round + '-' + wi"
                      class="word" :class="[w.size, { picked: pickedWordId === w.id, deco: w.deco }]"
                      :style="{ '--rot': w.rot + 'deg' }"
                      @click="pickCloudWord(w)">{{ w.label }}</span>
              </template>
            </div>
          </div>

          <div class="cloud-bar">
            <input v-model="aiKeyword" placeholder="输入关键词，重新探索…" @keyup.enter="handleExplore" />
            <button :disabled="exploring" @click="handleExplore">✨ 探索</button>
          </div>
        </div>

        <!-- 摘要 -->
        <div v-if="summary" class="summary">{{ summary }}</div>

        <!-- 选项卡片 -->
        <div v-if="options.length > 0" class="option-grid">
          <div v-for="opt in options" :key="opt.id" class="option-card" @click="selectOption(opt)">
            {{ opt.label }}
          </div>
          <div class="option-card option-shuffle" @click="reshuffle">
            🔄 换一批
          </div>
        </div>

        <!-- 内容到达 -->
        <div v-if="leafContent" class="leaf-notice">
          ✅ 灵感已生成，在下方编辑后发布
        </div>
      </div>

      <div class="row"><label>灵感标题</label><el-input v-model="form.title" placeholder="输入简短标题"></el-input></div>
      <div class="row">
        <label>所属分类</label>
        <el-select v-model="form.tag" placeholder="选择分类" style="width:100%">
          <el-option v-for="t in tags" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
          <!-- Markdown 工具栏 -->
    <div style="display:flex;gap:4px;margin-bottom:6px;flex-wrap:wrap;align-items:center;">
      <button type="button" class="md-btn" @click="insertMd('**','**')" title="加粗"><b>B</b></button>
      <button type="button" class="md-btn" @click="insertMd('*','*')" title="斜体"><i>I</i></button>
      <button type="button" class="md-btn" @click="insertMd('# ','')" title="标题1">H1</button>
      <button type="button" class="md-btn" @click="insertMd('## ','')" title="标题2">H2</button>
      <button type="button" class="md-btn" @click="insertMd('- ','')" title="列表">•</button>
      <button type="button" class="md-btn" @click="insertMd('[','](url)')" title="链接">🔗</button>
      <button type="button" class="md-btn" @click="insertMd('```\n','\n```')" title="代码块">&lt;/&gt;</button>
      <span style="flex:1;"></span>
      <button type="button" class="md-btn" @click="suggestImages" style="color:#6366f1;" title="AI 配图">🤖 AI 配图</button>
    </div>
<div class="row"><label>灵感详情</label><el-input v-model="form.content" type="textarea" :rows="5" placeholder="详细描述你的创意"></el-input></div>
      <div class="row voice-row">
        <label>语音输入</label>
        <div class="voice-bar">
          <el-button :type="voiceActive ? 'danger' : 'default'" circle @click="toggleVoice">{{ voiceActive ? '🔴' : '🎤' }}</el-button>
          <span class="voice-hint">{{ voiceActive ? '正在聆听...点击停止' : '点击开始语音输入' }}</span>
          <span v-if="voiceError" class="voice-error">{{ voiceError }}</span>
        </div>
      </div>
      <div class="row"><label>图片（可多张）</label>
        <div class="image-grid">
          <div v-for="(img, idx) in form.images" :key="idx" class="upload-box" style="width:100%;height:100%;">
            <div class="preview-wrap"><img loading="lazy" :src="img" class="preview-img" /><span class="preview-del" @click.stop="removeImage(idx)">✕</span></div>
          </div>
          <div class="upload-box add-box" @click="triggerUpload">
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleFile" />
            <div v-if="!uploading" class="upload-placeholder"><div class="upload-icon">+</div><div class="upload-text">添加图片</div></div>
            <div v-else class="upload-placeholder">
              <el-progress type="circle" :percentage="uploadPercent" :width="52" />
              <div class="upload-text">上传中 {{ uploadPercent }}%</div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="editId" class="btn-row">
        <el-button class="publish-btn" type="primary" :loading="loading" @click="submit(1)">📤 发布</el-button>
        <el-button class="draft-btn" :loading="loading" @click="submit(0)">💾 保存到草稿</el-button>
      </div>
      <el-button v-else class="submit-btn" type="primary" :loading="loading" @click="submit(1)">保存灵感</el-button>
    </div>

  <!-- AI 配图建议 -->
  <el-dialog v-model="imageSuggestDialog" title="AI 配图建议" width="380px">
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
      <div v-for="(url, i) in imageSuggestions" :key="i" class="suggest-img"
           :class="{selected: selectedSuggest === url}"
           @click="selectedSuggest = url"
           style="border:2px solid transparent;border-radius:8px;overflow:hidden;cursor:pointer;position:relative;">
        <img :src="url" style="width:100%;height:120px;object-fit:cover;display:block;" />
        <div v-if="selectedSuggest === url" style="position:absolute;top:6px;right:6px;width:22px;height:22px;border-radius:50%;background:#409eff;color:#fff;display:flex;align-items:center;justify-content:center;font-size:14px;font-weight:bold;box-shadow:0 2px 6px rgba(0,0,0,0.2);z-index:2;">✓</div>      </div>
    </div>
    <div style="margin-top:12px;text-align:right;">
      <el-button @click="imageSuggestDialog = false">取消</el-button>
      <el-button type="primary" @click="useSuggestedImage">使用此图</el-button>
    </div>
  </el-dialog>



  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createInspire, updateInspire, getInspireDetail, exploreInspiration, uploadFile, uploadFromUrl, getUserInfo } from '@/api/inspire.js'
const router = useRouter()
const route = useRoute()
const editId = computed(() => route.params.id)

const tags = ['美食','运动','电影','穿搭','文案','旅游','摄影','其他']
const loading = ref(false)
const form = ref({ title: '', tag: '', content: '', images: [], publishCity: '' })

// AI 探索状态
const aiKeyword = ref('')
const exploring = ref(false)
const options = ref([])
const summary = ref('')
const path = ref([])
const pathLabels = ref([])
const leafContent = ref(null)

// —— AI 探索 · 词云（把当前层的选项渲染成流动的书法词） ——
const pickedWordId = ref(null)
const CLOUD_SIZES = ['big', 'mid', 'small', 'mid']
const DEFAULT_CLOUD_WORDS = ['灵感', '生活', '旅行', '美食', '摄影', '家居', '手作', '穿搭']

const cloudLanes = computed(() => {
  const src = options.value.length
    ? options.value.map((o, i) => ({
        id: o.id, label: o.label,
        size: CLOUD_SIZES[i % CLOUD_SIZES.length],
        rot: ((i * 37) % 13) - 6
      }))
    : DEFAULT_CLOUD_WORDS.map((label, i) => ({
        id: 'd' + i, label, deco: true,
        size: CLOUD_SIZES[i % CLOUD_SIZES.length],
        rot: ((i * 37) % 13) - 6
      }))
  const laneCount = src.length <= 4 ? 1 : (src.length <= 8 ? 2 : 3)
  const lanes = Array.from({ length: laneCount }, () => [])
  src.forEach((w, i) => lanes[i % laneCount].push(w))
  return lanes
})

const laneTop = (i) => {
  if (cloudLanes.value.length === 1) return '42%'
  return ['24%', '52%', '78%'][i] || '52%'
}
const laneDur = (i) => ['30s', '22s', '26s'][i] || '26s'

// 点词 = 选择该方向，继续深入
const pickCloudWord = (w) => {
  if (w.deco) return
  pickedWordId.value = w.id
  const opt = options.value.find(o => String(o.id) === String(w.id))
  if (opt) selectOption(opt)
}

// 每次刷新出一批新选项时，清掉上一次的选中态
watch(options, () => { pickedWordId.value = null })

const handleExplore = async () => {
  if (!aiKeyword.value.trim()) return ElMessage.warning('请输入关键词')
  exploring.value = true; options.value = []; summary.value = ''; leafContent.value = null
  path.value = []; pathLabels.value = []
  try {
    const res = await exploreInspiration({ keyword: aiKeyword.value, path: '' })
    if (res.code === 200) {
      options.value = res.data?.options || []
      summary.value = res.data?.summary || ''
      if (res.data?.content) applyContent(res.data.content)
    }
  } catch (e) { ElMessage.warning('探索失败请重试') }
  finally { exploring.value = false }
}

const selectOption = async (opt) => {
  path.value.push(opt.id)
  pathLabels.value.push(opt.label)
  exploring.value = true; options.value = []; summary.value = ''
  try {
    const res = await exploreInspiration({ keyword: aiKeyword.value, path: path.value.join(',') })
    if (res.code === 200) {
      options.value = res.data?.options || []
      summary.value = res.data?.summary || ''
      if (res.data?.content) applyContent(res.data.content)
    }
  } catch (e) { console.error(e) }
  finally { exploring.value = false }
}

const applyContent = (c) => {
  leafContent.value = c
  options.value = []
  form.value.title = c.title || form.value.title
  form.value.tag = c.tag || form.value.tag
  form.value.content = (form.value.content ? form.value.content + '\n' : '') + (c.text || '')
}

const reshuffle = async () => {
  exploring.value = true
  try {
    const p = path.value.join(',')
    const res = await exploreInspiration({ keyword: aiKeyword.value, path: p, refresh: true })
    if (res.code === 200) { options.value = res.data?.options || []; summary.value = res.data?.summary || '' }
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

// 语音识别
const voiceActive = ref(false)
const voiceError = ref('')
let recognition = null

const initRecognition = () => {
  const SR = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SR) { voiceError.value = '当前浏览器不支持语音识别（推荐使用 Chrome/Safari）'; return null }
  const r = new SR()
  r.lang = 'zh-CN'
  r.interimResults = true
  r.continuous = true
  r.maxAlternatives = 1
  r.onresult = (e) => {
    let transcript = ''
    for (let i = e.resultIndex; i < e.results.length; i++) {
      transcript += e.results[i][0].transcript
    }
    form.value.content += transcript
  }
  r.onerror = (e) => {
    voiceError.value = '语音识别错误: ' + e.error
    voiceActive.value = false
  }
  r.onend = () => {
    if (voiceActive.value) {
      try { r.start() } catch(e) {}
    }
  }
  return r
}

const toggleVoice = () => {
  voiceError.value = ''
  if (voiceActive.value) {
    if (recognition) { recognition.stop(); recognition = null }
    voiceActive.value = false
    return
  }
  const r = initRecognition()
  if (!r) return
  recognition = r
  try {
    r.start()
    voiceActive.value = true
  } catch (e) {
    voiceError.value = '启动失败，请检查麦克风权限'
  }
}

onUnmounted(() => {
  if (recognition) { recognition.abort(); recognition = null }
})

// 文件上传
const fileInput = ref(null)
const uploading = ref(false)
const uploadPercent = ref(0)
const triggerUpload = () => { fileInput.value?.click() }

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

const handleFile = async (e) => {
  const raw = e.target.files[0]
  if (!raw) return
  e.target.value = ''
  if (!raw.type.startsWith('image/')) return ElMessage.warning('请选择图片文件')

  // A. 立即本地预览（不等上传完成）
  const localUrl = URL.createObjectURL(raw)
  form.value.images.push(localUrl)

  try {
    uploading.value = true
    uploadPercent.value = 0
    // C. 上传前压缩
    const blob = await compressImage(raw)
    const fd = new FormData()
    fd.append('file', blob, raw.name.replace(/\.[^.]+$/, '') + '.jpg')

    // D. 带进度的异步上传
    const res = await uploadFile(fd, (evt) => {
      const total = evt.total || evt.loaded || 1
      uploadPercent.value = Math.min(99, Math.round(evt.loaded * 100 / total))
    })
    const idx = form.value.images.indexOf(localUrl)
    if (res.code === 200 && res.data?.url) {
      if (idx >= 0) form.value.images.splice(idx, 1, res.data.url)   // 用服务端地址替换本地预览
      uploadPercent.value = 100
    } else {
      if (idx >= 0) form.value.images.splice(idx, 1)
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (err) {
    const idx = form.value.images.indexOf(localUrl)
    if (idx >= 0) form.value.images.splice(idx, 1)
    ElMessage.error('上传失败')
  } finally {
    URL.revokeObjectURL(localUrl)
    uploading.value = false
    uploadPercent.value = 0
  }
}

const removeImage = (idx) => { form.value.images.splice(idx, 1) }
// 编辑模式：预填表单
onMounted(async () => {
  if (route.params.id) {
    document.title = '编辑灵感'
    try {
      const res = await getInspireDetail(route.params.id)
      if (res.data) {
        form.value.title = res.data.title || ''
        form.value.tag = res.data.tag || ''
        form.value.content = res.data.content || ''
        form.value.publishCity = res.data.publishCity || ''
        if (res.data.images && res.data.images.length > 0) {
          form.value.images = res.data.images
        } else if (res.data.img) {
          form.value.images = [res.data.img]
        }
      }
    } catch (e) { console.error(e) }
  } else {
    // 新创建时从用户信息自动填充发布城市
    try {
      const userRes = await getUserInfo()
      if (userRes.code === 200 && userRes.data?.city) {
        form.value.publishCity = userRes.data.city
      }
    } catch (e) {}
  }
})

const goBack = () => { router.back() }
const submit = async (status) => {
  if (uploading.value) return ElMessage.warning('图片上传中，请稍候再提交')
  // 剔除未完成上传的本地预览（blob:）地址
  const pending = form.value.images.filter(i => typeof i === 'string' && i.startsWith('blob:'))
  if (pending.length > 0) return ElMessage.warning('有图片尚未上传完成，请稍候')
  if (!form.value.title) return ElMessage.warning('请填写标题')
  if (!form.value.tag) return ElMessage.warning('请选择分类')
  if (!form.value.content) return ElMessage.warning('请填写灵感详情')
  loading.value = true
  try {
    let payload = { ...form.value, status: status !== undefined ? status : 1 }; payload.images = JSON.stringify(payload.images)
    let res
    if (editId.value) {
      res = await updateInspire(editId.value, payload)
    } else {
      res = await createInspire(payload)
    }
    ElMessage.success(res.msg || (editId.value ? '修改成功' : '发布成功'))
    router.push('/')
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const imageSuggestDialog = ref(false)
const imageSuggestions = ref([])
const selectedSuggest = ref('')
const imageKeywords = ref('')

const insertMd = (before, after) => {
  const ta = document.querySelector('textarea')
  if (!ta) return
  const start = ta.selectionStart, end = ta.selectionEnd
  const text = form.value.content
  form.value.content = text.slice(0, start) + before + text.slice(start, end) + after + text.slice(end)
  ta.focus()
  ta.selectionStart = ta.selectionEnd = start + before.length + (end - start)
}

const suggestImages = async () => {
  const keyword = form.value.title || form.value.content?.slice(0, 50) || 'inspiration'
  try {
    console.log('✅ 函数开始执行 keyword=', keyword)
    imageKeywords.value = keyword
    const baseUrl = import.meta.env.VITE_API_BASE
    console.log('✅ VITE_API_BASE =', baseUrl)
    const url = baseUrl + '/api/inspire/public/suggest-images'
    console.log('✅ 最终请求url=', url)

    const res = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type':'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('token')
      },
      body: JSON.stringify({ keyword })
    })
    console.log('✅ fetch完成，http状态码：', res.status)
    if (!res.ok) {
      throw new Error(`HTTP error! status: ${res.status}`)
    }
    const d = await res.json()
    console.log('✅ 后端返回数据 d=', d)
    imageSuggestions.value = d.data || []
    if (imageSuggestions.value.length > 0) {
      selectedSuggest.value = imageSuggestions.value[0]
      imageSuggestDialog.value = true
    }
  } catch (e) {
    console.error('❌ suggestImages异常：', e)
    ElMessage.error('获取配图失败')
  }
}



const useSuggestedImage = async () => {
  if (!selectedSuggest.value) return
  try {
    const data = await uploadFromUrl(selectedSuggest.value)
    if (data.code === 200 && data.data?.url) {
      form.value.images.push(data.data.url)
      imageSuggestDialog.value = false
      ElMessage.success("配图已添加")
    } else {
      ElMessage.error(data.msg || "上传失败")
    }
  } catch (e) { ElMessage.error("配图失败: " + (e.message || "网络错误")) }
}
</script>
<style scoped>
.create-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.input-box { background:#fff; border-radius:20px; padding:30px 24px; margin-top:20px; }
.page-title { font-size:24px; font-weight:500; text-align:center; margin:0 0 6px; color:#1d1d1f; }
.page-desc { text-align:center; font-size:14px; color:#86868b; margin-bottom:28px; }
.ai-section { margin-bottom:24px; background:#fef9ef; border-radius:16px; padding:16px; }
.breadcrumb { font-size:13px; margin-bottom:12px; color:#909399; }
.crumb-item { cursor:pointer; }
.crumb-item:hover { color:#409eff; }
.crumb-sep { margin:0 6px; }
.summary { font-size:14px; color:#606266; margin-bottom:14px; line-height:1.5; }
.option-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:10px; }
.option-card { padding:14px; background:#fff; border:1px solid #eee; border-radius:12px; text-align:center; cursor:pointer; font-size:14px; color:#333; transition:0.2s; }
.option-card:hover { border-color:#409eff; color:#409eff; }
.option-shuffle { color:#e6a23c; border-color:#faecd8; }
.option-shuffle:hover { border-color:#e6a23c; color:#e6a23c; background:#fdf6ec; }
.leaf-notice { padding:10px; background:#f0f9eb; border-radius:10px; font-size:13px; color:#67c23a; text-align:center; }
.voice-row { margin-bottom:18px; }
.voice-bar { display:flex; align-items:center; gap:12px; background:#f8f9fc; border-radius:12px; padding:10px 16px; }
.voice-hint { font-size:13px; color:#909399; }
.voice-error { font-size:12px; color:#f56c6c; margin-left:8px; }
.row { margin-bottom:18px; }
.row label { display:block; font-size:14px; color:#1d1d1f; margin-bottom:8px; }
.submit-btn { width:100%; height:46px; border-radius:14px; font-size:16px; background:#409eff; border:none; margin-top:10px; }
.upload-box { border:2px dashed #dcdfe6; border-radius:12px; padding:12px; text-align:center; cursor:pointer; transition:0.25s; }
.image-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(100px, 1fr)); gap:8px; }
.add-box { display:flex; align-items:center; justify-content:center; aspect-ratio:1; }
.upload-box:hover { border-color:#409eff; }
.upload-placeholder { padding:10px 0; }
.upload-icon { font-size:32px; color:#c0c4cc; }
.upload-text { font-size:13px; color:#909399; margin-top:6px; }
.preview-wrap { position:relative; display:inline-block; }
.preview-img { width:100%; aspect-ratio:1; object-fit:cover; border-radius:8px; }
.preview-del { position:absolute; top:-8px; right:-8px; width:20px; height:20px; border-radius:50%; background:#f56c6c; color:#fff; display:flex; align-items:center; justify-content:center; font-size:12px; cursor:pointer; }
.btn-row { display:flex; gap:10px; margin-top:16px; }
.publish-btn { flex:1; }
.draft-btn { flex:1; background:#f5f5f7; color:#1d1d1f; border:none; }
.draft-btn:hover { background:#e8e8ed; }

.md-btn{padding:4px 10px;border:1px solid #e4e7ed;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;color:#333;}
.md-btn:hover{border-color:#409eff;color:#409eff;}
.suggest-img.selected{border-color:#409eff;}
.suggest-img:hover{border-color:#409eff44;}

/* ================= AI 探索 · 薄荷绿书法词云 ================= */
.cloud-scene{
  position:relative;width:100%;height:250px;border-radius:16px;overflow:hidden;
  background:url('/bg/explore-bg.svg') center / cover no-repeat, #a8dcd2;
  box-shadow:0 6px 20px rgba(60,120,110,.18);
  margin-bottom:14px;
}
.cloud-hint{
  position:absolute;z-index:4;left:14px;top:12px;font-size:12px;color:rgba(38,68,63,.72);
  display:flex;align-items:center;gap:6px;flex-wrap:wrap;
}
.cloud-hint .dot{width:6px;height:6px;border-radius:50%;background:#d98200;box-shadow:0 0 8px rgba(217,130,0,.9)}
.cloud-hint .crumb{color:#0f766e;cursor:pointer}
.cloud-hint .crumb .sep{color:rgba(38,68,63,.4);margin:0 4px}
.cloud-hint .crumb-txt:hover{text-decoration:underline}

/* 轨道：右 → 左无缝流动 */
.lane{position:absolute;left:0;width:100%;height:0}
.lane .track{
  position:absolute;top:0;left:0;display:flex;align-items:center;white-space:nowrap;
  will-change:transform;animation:cloudMarquee linear infinite;
}
@keyframes cloudMarquee{ from{transform:translateX(0)} to{transform:translateX(-50%)} }
.lane:hover .track{ animation-play-state:paused }   /* 悬停暂停，方便点选 */

.cloud-scene .word{
  position:static;flex:0 0 auto;margin-right:3rem;cursor:pointer;user-select:none;
  font-family:"Ma Shan Zheng","STKaiti","KaiTi","Songti SC",serif;font-weight:400;color:#2f4f4a;
  line-height:1;white-space:nowrap;rotate:var(--rot,0deg);
  animation:cloudTwinkle 5s ease-in-out infinite;
  transition:color .22s, text-shadow .22s, scale .22s, opacity .22s;
}
.cloud-scene .word.deco{cursor:default}
.cloud-scene .word.small{font-size:15px;--tmin:.42;--tmax:.72}
.cloud-scene .word.mid{font-size:21px;--tmin:.6;--tmax:.9}
.cloud-scene .word.big{font-size:32px;--tmin:.78;--tmax:1}
@keyframes cloudTwinkle{0%,100%{opacity:var(--tmin,.5)}50%{opacity:var(--tmax,.95)}}
.cloud-scene .word:hover{scale:1.08}
/* 选中的词：琥珀色 + 暖光脉冲（覆盖闪烁动画，避免被压暗） */
.cloud-scene .word.picked{
  color:#d98200;
  animation:cloudPickGlow 1.8s ease-in-out infinite;
}
@keyframes cloudPickGlow{
  0%,100%{text-shadow:0 0 6px rgba(217,130,0,.35),0 0 16px rgba(217,130,0,.18)}
  50%{text-shadow:0 0 12px rgba(217,130,0,.72),0 0 28px rgba(217,130,0,.42)}
}

.cloud-bar{position:absolute;z-index:5;left:12px;right:12px;bottom:12px;display:flex;gap:8px}
.cloud-bar input{
  flex:1;height:40px;border-radius:11px;border:1px solid rgba(47,79,74,.18);
  background:rgba(255,255,255,.82);color:#2f4f4a;padding:0 14px;font-size:14px;outline:none;
}
.cloud-bar input::placeholder{color:rgba(47,79,74,.45)}
.cloud-bar button{
  height:40px;padding:0 16px;border:none;border-radius:11px;font-size:14px;font-weight:700;
  color:#5a3d00;background:linear-gradient(135deg,#ffd76a,#f0b429);cursor:pointer;white-space:nowrap;
  box-shadow:0 3px 12px rgba(240,180,41,.35);
}
.cloud-bar button:disabled{opacity:.7;cursor:not-allowed}
</style>
