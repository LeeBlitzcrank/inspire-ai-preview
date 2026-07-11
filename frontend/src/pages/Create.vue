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
        <div class="ai-row">
          <el-input v-model="aiKeyword" placeholder="输入关键词，AI探索灵感" size="large" @keyup.enter="handleExplore">
            <template #append><el-button type="warning" :loading="exploring" @click="handleExplore">✨ 探索</el-button>
        </template>
          </el-input>
        </div>

        <!-- 面包屑 -->
        <div v-if="pathLabels.length > 0" class="breadcrumb">
          <span class="crumb-item" @click="resetExplore">{{ aiKeyword }}</span>
          <span v-for="(label, idx) in pathLabels" :key="idx" class="crumb-item">
            <span class="crumb-sep">›</span>
            <span @click="goToLevel(idx)">{{ label }}</span>
          </span>
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
            <div class="upload-placeholder"><div class="upload-icon">+</div><div class="upload-text">添加图片</div></div>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
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
const triggerUpload = () => { fileInput.value?.click() }
const handleFile = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  const fd = new FormData(); fd.append('file', file)
  try {
    const res = await uploadFile(fd)
    if (res.code === 200 && res.data?.url) form.value.images.push(res.data.url)
    else ElMessage.error('上传失败')
  } catch (e) { ElMessage.error('上传失败') }
  e.target.value = ''
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
    imageKeywords.value = keyword
    const res = await fetch('/api/inspire/public/suggest-images', {
      method: 'POST',
      headers: {'Content-Type':'application/json', 'Authorization': 'Bearer ' + localStorage.getItem('token')},
      body: JSON.stringify({ keyword })
    })
    const d = await res.json()
    imageSuggestions.value = d.data || []
    if (imageSuggestions.value.length > 0) {
      selectedSuggest.value = imageSuggestions.value[0]
      imageSuggestDialog.value = true
    }
  } catch (e) { ElMessage.error('获取配图失败') }
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
</style>
