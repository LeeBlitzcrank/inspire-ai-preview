<template>
  <div class="create-page">
    <div class="topbar">
      <span class="ico" @click="editId ? goBack() : $router.push('/')">{{ editId ? '←' : '🍎' }}</span>
      <span class="page-title">{{ editId ? '编辑灵感' : '录入新灵感' }}</span>
      <span class="ico" @click="$router.push('/personal')">👤</span>
    </div>
    <div class="form-body">

      <!-- AI 探索区 -->
      <div v-if="!editId" class="card ai-card">
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
              <template v-for="round in 4" :key="round">
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

        <!-- 摘要 / 探索中提示 -->
        <div v-if="exploring || summary" class="summary">
          <span v-if="exploring" class="spin"></span>{{ exploring ? '正在为你探索…' : summary }}
        </div>

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

        <!-- 一次生成的多组风格候选，点一下切换 -->
        <div v-if="contentVariants.length > 1" class="variant-bar">
          <span class="variant-label">换风格</span>
          <button
            v-for="(v, i) in contentVariants"
            :key="i"
            type="button"
            class="variant-chip"
            :class="{ active: activeVariant === i }"
            @click="applyVariant(i)"
          >
            {{ v.style || ('风格' + (i + 1)) }}
          </button>
        </div>
      </div>

      <!-- ============ 灵感标题 ============ -->
      <div class="card">
        <label class="label">灵感标题</label>
        <input v-model="form.title" class="title-input" placeholder="输入简短标题…" />
      </div>

      <!-- ============ 所属分类 ============ -->
      <div class="card">
        <label class="label">所属分类</label>
        <div class="chips">
          <span v-for="t in tags" :key="t" class="chip" :class="{ on: form.tag === t }" @click="form.tag = t">{{ t }}</span>
        </div>
      </div>

      <!-- ============ 灵感详情（富文本 + 语音） ============ -->
      <div class="card">
        <label class="label">灵感详情</label>
        <div class="editor">
          <div class="toolbar">
            <button type="button" class="tb-btn bold" title="加粗" @mousedown.prevent @click="execCmd('bold')">B</button>
            <button type="button" class="tb-btn italic" title="斜体" @mousedown.prevent @click="execCmd('italic')">I</button>
            <span class="tb-divider"></span>
            <button type="button" class="tb-btn h1" title="一级标题" @mousedown.prevent @click="execBlock('H1')">H1</button>
            <button type="button" class="tb-btn h2" title="二级标题" @mousedown.prevent @click="execBlock('H2')">H2</button>
            <button type="button" class="tb-btn" title="引用" @mousedown.prevent @click="execBlock('BLOCKQUOTE')">❝</button>
            <button type="button" class="tb-btn" title="列表" @mousedown.prevent @click="execCmd('insertUnorderedList')">☰</button>
            <button type="button" class="tb-btn" title="分割线" @mousedown.prevent @click="execCmd('insertHorizontalRule')">—</button>
            <button type="button" class="tb-btn" title="清除格式" @mousedown.prevent @click="clearFormat">⌫</button>
            <span class="tb-divider"></span>
            <button type="button" class="tb-btn auto" title="按段落、编号、清单自动排版" @mousedown.prevent @click="autoFormatContent">自动排版</button>
          </div>
          <div ref="descRef" class="editable" contenteditable="true"
               data-placeholder="详细描述你的创意；可用上方按钮加粗、设标题，或点左下角语音输入…"
               @input="onDescInput"></div>
          <div class="editor-foot">
            <button type="button" class="mic" :class="{ rec: voiceActive }" title="语音输入" @click="toggleVoice">
              <span class="ico">🎤</span>
              <span>{{ voiceActive ? '正在聆听…点击停止' : (voiceError || '点击说话') }}</span>
            </button>
            <span class="count">{{ contentLen }} 字</span>
          </div>
        </div>
      </div>

      <!-- ============ 图片（可多张）+ AI 配图 ============ -->
      <div class="card">
        <div class="img-head">
          <span class="label" style="margin:0">图片（可多张）</span>
          <button type="button" class="ai-img-btn" @click="toggleSuggest">🤖 AI 配图</button>
        </div>
        <div class="image-grid">
          <div
            v-for="(img, idx) in form.images"
            :key="img + '#' + idx"
            class="thumb"
            :class="{ picked: selectedImage === img }"
            @click="selectedImage = img"
          >
            <video v-if="isVideoUrl(img)" :src="img" muted playsinline preload="metadata"></video>
            <img v-else loading="lazy" decoding="async" :src="thumbOf(img, 400)" alt="" />
            <span v-if="isVideoUrl(img)" class="video-badge">▶</span>
            <div v-if="imageProgress[img] !== undefined" class="thumb-mask">{{ imageProgress[img] }}%</div>
            <span class="del" @click.stop="removeImage(idx)">✕</span>
          </div>
          <div class="thumb add" @click="triggerUpload">
            <input ref="fileInput" type="file" accept="image/*,video/*" multiple hidden @change="handleFile" />
            <template v-if="!uploading">
              <span class="plus">+</span><span>添加</span>
            </template>
            <template v-else>
              <el-progress type="circle" :percentage="uploadPercent" :width="44" />
              <span>上传中 {{ uploadingCount }} 张</span>
            </template>
          </div>
        </div>

        <!-- 视频处理：压缩 / 裁剪（服务端 ffmpeg） -->
        <!-- 图片滤镜：选中某张图后可一键套用预设滤镜 -->
        <div v-if="selectedImage && !isVideoUrl(selectedImage)" class="filter-bar">
          <span class="filter-label">滤镜</span>
          <button
            v-for="f in IMAGE_FILTERS"
            :key="f.key"
            type="button"
            class="filter-chip"
            :disabled="filtering"
            @click="applyFilter(f)"
          >
            <img :src="filterPreviewSrc" :style="{ filter: f.css }" alt="">
            <span>{{ f.label }}</span>
          </button>
          <span v-if="filtering" class="filter-tip">处理中…</span>
        </div>

        <div v-if="videoItems.length" class="video-tools">
          <label class="video-keep">
            <input v-model="keepOriginalOnly" type="checkbox" />
            <span>只保留最终版（压缩/裁剪完成后自动删除原片）</span>
          </label>
          <div v-for="v in videoItems" :key="v.url" class="video-tool-row">
            <span class="video-tool-name">🎬 {{ v.name || '视频' }}</span>
            <span class="video-tool-meta">{{ v.sizeText }}<template v-if="v.duration"> · {{ v.duration }}s</template></span>
            <button type="button" class="ghost" :disabled="!!v.processing" @click="doCompress(v)">
              {{ v.processing === 'compress' ? '压缩中…' : '压缩' }}
            </button>
            <button type="button" class="ghost" :disabled="!!v.processing" @click="openTrim(v)">裁剪</button>
          </div>
        </div>

        <!-- AI 配图推荐面板（内联，不再弹窗） -->
        <div v-if="imageSuggestOpen" class="suggest-panel">
          <div class="suggest-title">🤖 为你推荐（点击可多选，换一批可继续挑）</div>
          <AppState
            :state="suggestState"
            :rows="3"
            loading-variant="grid"
            empty-icon="🖼"
            empty-text="暂无推荐，换个关键词再试试"
            error-text="配图加载失败"
            @retry="suggestImages"
          >
          <div v-if="imageSuggestions.length" class="suggest-grid">
            <div v-for="(url, i) in imageSuggestions" :key="i" class="suggest-img"
                 :class="{ selected: selectedSuggests.includes(url) }" @click="toggleSuggestPick(url)">
              <img :src="thumbOf(url, 400)" alt="" loading="lazy" decoding="async" />
              <span class="check">✓</span>
            </div>
          </div>
          </AppState>
          <div class="suggest-actions">
            <button type="button" class="ghost" :disabled="suggestLoading || addingSuggest" @click="suggestImages({ next: true })">🔄 换一批</button>
            <button type="button" class="primary"
                    :disabled="!selectedSuggests.length || suggestLoading || addingSuggest"
                    @click="useSuggestedImages">
              {{ addingSuggest ? '添加中…' : (selectedSuggests.length ? `添加选中（${selectedSuggests.length}）` : '使用选中') }}
            </button>
          </div>
        </div>
      </div>

      <!-- ============ 底部按钮 ============ -->
      <div class="actions">
        <button type="button" class="btn draft" :disabled="loading" @click="submit(0)">保存草稿</button>
        <button type="button" class="btn publish" :disabled="loading" @click="submit(1)">发布</button>
      </div>
    </div>

    <!-- 视频裁剪：按秒指定开始时间与保留时长 -->
    <el-dialog v-model="trimDialogVisible" title="裁剪视频" width="320px">
      <div class="trim-tip">
        {{ trimTarget?.name || '视频' }}<template v-if="trimTarget?.duration"> · 总时长 {{ trimTarget.duration }} 秒</template>
      </div>
      <el-input v-model="trimStart" placeholder="开始时间（秒）" />
      <el-input v-model="trimDuration" placeholder="保留时长（秒）" style="margin-top:10px" />
      <template #footer>
        <el-button @click="trimDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="trimming" @click="doTrim">确定裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createInspire, updateInspire, getInspireDetail, exploreInspiration, uploadFile, uploadFromUrl, getUserInfo, suggestImages as suggestImagesApi, compressVideo, trimVideo, getWordCloud, getCategoryTree } from '@/api/inspire.js'
import { autoFormatHtml } from '@/utils/autoFormat.js'
import { thumbOf } from '@/utils/media.js'
const router = useRouter()
const route = useRoute()
const editId = computed(() => route.params.id)

// 分类由后台维护，改成读接口；接口异常时退回一份默认值
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
const loading = ref(false)
const form = ref({ title: '', tag: '', content: '', images: [], publishCity: '' })

// —— 富文本详情（contenteditable）：以 HTML 字符串存储，提交与回显均走 innerHTML ——
const descRef = ref(null)
const contentLen = computed(() => {
  const html = form.value.content || ''
  const text = html.replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ')
  return text.replace(/\s/g, '').length
})
const onDescInput = () => { if (descRef.value) form.value.content = descRef.value.innerHTML }
const setContent = (html) => {
  form.value.content = html || ''
  if (descRef.value) descRef.value.innerHTML = form.value.content
}
const execCmd = (cmd) => { descRef.value?.focus(); document.execCommand(cmd); onDescInput() }
const execBlock = (tag) => { descRef.value?.focus(); document.execCommand('formatBlock', false, tag); onDescInput() }
const clearFormat = () => {
  descRef.value?.focus()
  document.execCommand('removeFormat')
  document.execCommand('formatBlock', false, 'P')
  onDescInput()
}
// 读取编辑器里的纯文本：按块级元素补换行，并给列表/标题补回标记，
// 这样反复点「自动排版」也不会把已有的列表结构弄丢
const getEditorPlainText = () => {
  const el = descRef.value
  if (!el) return ''
  const BLOCK = new Set(['P', 'DIV', 'LI', 'H1', 'H2', 'H3', 'BLOCKQUOTE'])
  let out = ''
  const walk = (node) => {
    node.childNodes.forEach(child => {
      if (child.nodeType === 3) { out += child.textContent; return }
      if (child.nodeType !== 1) return
      const tag = child.tagName
      if (tag === 'BR') { out += '\n'; return }
      const isBlock = BLOCK.has(tag)
      if (isBlock && out && !out.endsWith('\n')) out += '\n'
      if (tag === 'LI') {
        // 有序列表补 "1. "，无序列表补 "- "，让下次排版仍能识别成列表
        const parent = child.parentElement
        if (parent && parent.tagName === 'OL') {
          out += (Array.prototype.indexOf.call(parent.children, child) + 1) + '. '
        } else {
          out += '- '
        }
      }
      if (tag === 'H1' || tag === 'H2' || tag === 'H3') out += '【'
      walk(child)
      if (tag === 'H1' || tag === 'H2' || tag === 'H3') out += '】'
      if (isBlock && !out.endsWith('\n')) out += '\n'
    })
  }
  walk(el)
  return out.replace(/\n{3,}/g, '\n\n').trim()
}

// 自动排版：按段落 / 编号 / 清单把纯文本整理成结构化富文本
const autoFormatContent = () => {
  const plain = getEditorPlainText() || (form.value.content || '').replace(/<[^>]*>/g, ' ')
  if (!plain.trim()) return ElMessage.warning('还没有内容可以排版')
  const before = form.value.content || ''
  const formatted = autoFormatHtml(plain)
  if (formatted === before) return ElMessage.info('这段内容已经是排版后的效果')
  setContent(formatted)
  ElMessage.success('已自动排版')
}

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
      options.value = res.data?.options || []
      summary.value = res.data?.summary || summary.value
      // 词云固定不变，保留点选高亮，指示当前正在探索哪个方向
      if (res.data?.content) applyContent(res.data.content)
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
  exploring.value = true
  // 保留旧词与高亮，等新数据回来再替换（避免闪烁，和预览一致）
  try {
    const res = await exploreInspiration({ keyword: aiKeyword.value, path: path.value.join(',') })
    if (res.code === 200) {
      options.value = res.data?.options || []
      summary.value = res.data?.summary || ''
      // 词云固定展示方向词：这里只刷新下方的选项，也不动词云上的点选高亮
      if (res.data?.content) applyContent(res.data.content)
    }
  } catch (e) { console.error(e) }
  finally { exploring.value = false }
}

const applyContent = (c) => {
  leafContent.value = c
  options.value = []
  // 一次生成的多组风格候选（后端 content.variants）
  contentVariants.value = Array.isArray(c.variants) ? c.variants.filter(v => v && v.text) : []
  activeVariant.value = 0
  form.value.title = c.title || form.value.title
  form.value.tag = c.tag || form.value.tag
  // 编辑器以 HTML 存储：AI 返回的纯文本先自动排版（段落/编号/清单）再追加
  const add = c.text ? autoFormatHtml(c.text) : ''
  setContent((form.value.content || '') + add)
}

/** 切换风格：直接替换标题与正文（避免和上一组内容叠加） */
const applyVariant = (index) => {
  const v = contentVariants.value[index]
  if (!v) return
  activeVariant.value = index
  if (v.title) form.value.title = v.title
  if (v.text) setContent(autoFormatHtml(v.text))
  ElMessage.success('已切换到「' + (v.style || ('风格' + (index + 1))) + '」')
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
    // 语音结果插入富文本编辑器光标处
    if (descRef.value) {
      descRef.value.focus()
      document.execCommand('insertText', false, transcript)
      onDescInput()
    }
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
  imageProgress.value[localUrl] = 0
  uploadingCount.value++
  try {
    const fd = new FormData()
    fd.append('file', raw, raw.name)
    const res = await uploadFile(fd, (evt) => {
      const total = evt.total || evt.loaded || 1
      imageProgress.value[localUrl] = Math.min(99, Math.round(evt.loaded * 100 / total))
    })
    const idx = form.value.images.indexOf(localUrl)
    if (res.code === 200 && res.data?.url) {
      if (idx >= 0) form.value.images.splice(idx, 1, res.data.url)
      videoMeta.value[res.data.url] = {
        name: res.data.name || raw.name,
        sizeText: (raw.size / 1024 / 1024).toFixed(1) + 'MB',
        duration: Number(res.data.duration || 0),
        processing: ''
      }
    } else {
      if (idx >= 0) form.value.images.splice(idx, 1)
      ElMessage.error(res.msg || '视频上传失败')
    }
  } catch (e) {
    const idx = form.value.images.indexOf(localUrl)
    if (idx >= 0) form.value.images.splice(idx, 1)
    ElMessage.error('视频上传失败')
  } finally {
    URL.revokeObjectURL(localUrl)
    delete imageProgress.value[localUrl]
    uploadingCount.value = Math.max(0, uploadingCount.value - 1)
  }
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
      // 记录原图，滤镜始终从这张开始烘焙
      imageOrigin.value[res.data.url] = res.data.url
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

const removeImage = (idx) => { form.value.images.splice(idx, 1) }

// 编辑模式：预填表单
onMounted(async () => {
  loadCloudWords()
  loadTags()
  if (route.params.id) {
    document.title = '编辑灵感'
    try {
      const res = await getInspireDetail(route.params.id)
      if (res.data) {
        form.value.title = res.data.title || ''
        form.value.tag = res.data.tag || ''
        setContent(res.data.content || '')
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
  if (!contentLen.value) return ElMessage.warning('请填写灵感详情')
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
</script>
<style scoped>
@font-face {
  font-family: "Ma Shan Zheng";
  src: url("/fonts/ma-shan-zheng.woff2") format("woff2");
  font-display: swap;
  font-style: normal;
  font-weight: 400;
}

.create-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }

/* ---------- 顶部栏 ---------- */
.topbar { display:flex; align-items:center; justify-content:space-between; padding:8px 4px 14px; }
.topbar .page-title { font-size:17px; font-weight:600; }
.topbar .ico { width:38px; height:38px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:17px; box-shadow:0 1px 6px rgba(0,0,0,.05); cursor:pointer; }

/* ---------- 通用卡片 ---------- */
.card { background:#fff; border-radius:16px; padding:16px; box-shadow:0 1px 3px rgba(0,0,0,.04); margin-bottom:14px; }
.label { font-size:13px; color:#6b7280; margin-bottom:8px; display:block; }
.ai-card { background:linear-gradient(180deg,#f5f9ff 0%,#fff 60%); border:1px solid #e8f1ff; }

/* ---------- AI 探索：摘要 / 选项 ---------- */
.summary { margin-top:10px; font-size:13px; color:#6b7280; line-height:1.7; background:#f8fafc; border-radius:10px; padding:10px 12px; }
.spin { display:inline-block; width:11px; height:11px; margin-right:8px; vertical-align:-1px; border-radius:50%; border:2px solid #cfe4ff; border-top-color:#409eff; animation:spin .7s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }
.option-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-top:12px; }
.option-card { border:1px solid #ebeef5; border-radius:12px; padding:14px 12px; font-size:14px; text-align:center; cursor:pointer; background:#fff; transition:.15s; }
.option-card:hover { border-color:#b3d8ff; background:#ecf5ff; color:#409eff; }
.option-shuffle { color:#6b7280; background:#fafbfc; }
.option-shuffle:hover { border-color:#b3d8ff; background:#ecf5ff; color:#409eff; }
.leaf-notice { margin-top:12px; font-size:13px; color:#67c23a; background:#f0f9eb; border-radius:10px; padding:10px 12px; text-align:center; }
.variant-bar {
  margin-top:10px; padding:8px 10px;
  display:flex; align-items:center; gap:8px; flex-wrap:wrap;
  background:#f4fbf6; border-radius:10px;
}
.variant-label { font-size:12.5px; color:#4f8a48; }
.variant-chip {
  padding:5px 14px; font-size:12.5px;
  border:1.5px solid #cfe6cf; border-radius:999px;
  background:#fff; color:#4f8a48; cursor:pointer;
}
.variant-chip:hover { border-color:#7ec07a; }
.variant-chip.active { background:#4f8a48; border-color:#4f8a48; color:#f3faf2; font-weight:600; }

/* ---------- 标题 / 分类 ---------- */
.title-input { width:100%; border:none; outline:none; font-size:19px; font-weight:600; color:#1d1d1f; background:transparent; padding:4px 0; }
.title-input::placeholder { color:#c0c4cc; font-weight:400; }
.chips { display:flex; flex-wrap:wrap; gap:8px; }
.chip { padding:6px 14px; border-radius:999px; font-size:13px; color:#6b7280; background:#f5f7fa; cursor:pointer; border:1px solid transparent; transition:.15s; }
.chip.on { color:#409eff; background:#ecf5ff; border-color:#b3d8ff; }

/* ---------- 富文本编辑器 ---------- */
.editor { border:1px solid #ebeef5; border-radius:14px; overflow:hidden; background:#fff; }
.toolbar { display:flex; align-items:center; gap:2px; padding:8px 10px; background:#fafbfc; border-bottom:1px solid #ebeef5; overflow-x:auto; }
.toolbar::-webkit-scrollbar { display:none; }
.tb-btn { flex:0 0 auto; min-width:34px; height:32px; padding:0 8px; border:none; background:transparent; border-radius:8px; cursor:pointer; font-size:14px; color:#4b5563; display:inline-flex; align-items:center; justify-content:center; transition:.15s; }
.tb-btn:hover { background:#eef1f5; }
.tb-btn.bold { font-weight:800; }
.tb-btn.italic { font-style:italic; font-family:Georgia,serif; }
.tb-btn.h1 { font-weight:800; font-size:15px; }
.tb-btn.h2 { font-weight:700; font-size:14px; }
.tb-btn.auto { padding:0 10px; font-size:12.5px; font-weight:600; color:#409eff; }
.tb-btn.auto:hover { background:#ecf5ff; }
.tb-divider { width:1px; height:18px; background:#ebeef5; margin:0 4px; flex:0 0 auto; }
.editable { min-height:150px; padding:14px 16px; outline:none; font-size:15.5px; line-height:1.75; color:#1d1d1f; }
.editable:empty:before { content:attr(data-placeholder); color:#c0c4cc; }
.editable h1 { font-size:22px; font-weight:700; margin:8px 0; }
.editable h2 { font-size:18px; font-weight:700; margin:8px 0; }
.editable blockquote { margin:8px 0; padding:6px 12px; border-left:3px solid #409eff; background:#f7fbff; color:#4b5563; border-radius:0 8px 8px 0; }
.editable ul { margin:8px 0; padding-left:22px; }
.editable hr { border:none; border-top:1px solid #ebeef5; margin:14px 0; }
.editor-foot { display:flex; align-items:center; justify-content:space-between; padding:8px 12px; border-top:1px solid #ebeef5; background:#fafbfc; }
.mic { display:inline-flex; align-items:center; gap:8px; border:none; cursor:pointer; background:transparent; padding:6px 10px; border-radius:999px; color:#6b7280; font-size:13px; font-family:inherit; transition:.15s; }
.mic:hover { background:#eef1f5; }
.mic .ico { width:30px; height:30px; border-radius:50%; background:#ecf5ff; color:#409eff; display:inline-flex; align-items:center; justify-content:center; font-size:15px; transition:.15s; }
.mic.rec { color:#f56c6c; }
.mic.rec .ico { background:#fff0f0; color:#f56c6c; animation:pulse 1.1s infinite; }
@keyframes pulse { 0%{box-shadow:0 0 0 0 rgba(245,108,108,.5)} 70%{box-shadow:0 0 0 12px rgba(245,108,108,0)} 100%{box-shadow:0 0 0 0 rgba(245,108,108,0)} }
.count { font-size:12px; color:#a8abb2; }

/* ---------- 图片 + AI 配图 ---------- */
.img-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.ai-img-btn { display:inline-flex; align-items:center; gap:5px; height:30px; padding:0 12px; border-radius:999px; border:1px solid #b3d8ff; background:#ecf5ff; color:#409eff; font-size:12.5px; font-weight:600; cursor:pointer; font-family:inherit; }
.image-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.thumb { position:relative; aspect-ratio:1; border-radius:12px; overflow:hidden; background:#f5f7fa; display:flex; align-items:center; justify-content:center; }
.thumb img { width:100%; height:100%; object-fit:cover; }
.thumb video { width:100%; height:100%; object-fit:cover; background:#000; }
.video-badge {
  position:absolute; left:6px; bottom:6px;
  width:22px; height:22px; border-radius:50%;
  display:flex; align-items:center; justify-content:center;
  background:rgba(0,0,0,.55); color:#fff; font-size:11px;
}
.video-tools { margin-top:12px; display:flex; flex-direction:column; gap:8px; }
.thumb.picked { outline:2px solid #409eff; outline-offset:2px; }
.filter-bar {
  margin-top:12px; padding:10px 12px;
  display:flex; align-items:center; gap:8px; flex-wrap:wrap;
  border-radius:12px; background:#f4f7fd;
}
.filter-label { font-size:12.5px; color:#6b7280; }
.filter-chip {
  display:flex; flex-direction:column; align-items:center; gap:4px;
  padding:6px 8px; border:2px solid transparent; border-radius:10px;
  background:#fff; cursor:pointer; font-size:11px; color:#6b7280;
}
.filter-chip:hover:not(:disabled) { border-color:#c6dcff; }
.filter-chip:disabled { opacity:.55; cursor:not-allowed; }
.filter-chip img { width:42px; height:42px; object-fit:cover; border-radius:6px; display:block; }
.filter-tip { font-size:12px; color:#409eff; }
.video-keep {
  display:flex; align-items:center; gap:8px;
  font-size:12.5px; color:#6b7280; cursor:pointer;
}
.video-keep input { width:14px; height:14px; accent-color:#409eff; }
.video-tool-row {
  display:flex; align-items:center; gap:10px;
  padding:10px 12px; border-radius:12px;
  background:#f4f7fd; font-size:13px; color:#374151;
}
.video-tool-name { flex:1; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.video-tool-meta { color:#8a94a6; font-size:12px; }
.video-tool-row .ghost { padding:5px 12px; font-size:12px; }
.trim-tip { margin-bottom:10px; font-size:13px; color:#6b7280; }
.thumb .del { position:absolute; top:6px; right:6px; width:20px; height:20px; border-radius:50%; background:rgba(0,0,0,.55); color:#fff; font-size:12px; display:flex; align-items:center; justify-content:center; cursor:pointer; }
.thumb.add { border:1.5px dashed #d3dce6; color:#a8abb2; flex-direction:column; gap:4px; cursor:pointer; font-size:12px; }
.thumb.add .plus { font-size:24px; line-height:1; }
.thumb.add :deep(.el-progress__text) { font-size:11px !important; }
.thumb-mask { position:absolute; inset:0; background:rgba(0,0,0,.35); color:#fff; font-size:12px; font-weight:600; display:flex; align-items:center; justify-content:center; }

.suggest-panel { margin-top:14px; border-top:1px dashed #ebeef5; padding-top:12px; }
.suggest-title { font-size:13px; color:#6b7280; margin-bottom:10px; }
.suggest-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.suggest-img { position:relative; aspect-ratio:1; border-radius:12px; overflow:hidden; cursor:pointer; border:2px solid transparent; background:#f5f7fa; }
.suggest-img img { width:100%; height:100%; object-fit:cover; display:block; }
.suggest-img.selected { border-color:#409eff; }
.suggest-img .check { position:absolute; top:6px; right:6px; width:22px; height:22px; border-radius:50%; background:#409eff; color:#fff; display:none; align-items:center; justify-content:center; font-size:13px; font-weight:700; }
.suggest-img.selected .check { display:flex; }
.suggest-empty { font-size:13px; color:#a8abb2; padding:8px 0; }
.suggest-actions { display:flex; gap:10px; margin-top:12px; }
.suggest-actions .ghost { flex:1; height:40px; border-radius:10px; border:1px solid #ebeef5; background:#fff; color:#6b7280; font-size:14px; cursor:pointer; font-family:inherit; }
.suggest-actions .primary { flex:1.6; height:40px; border-radius:10px; border:none; background:#409eff; color:#fff; font-size:14px; font-weight:600; cursor:pointer; font-family:inherit; }
.suggest-actions button:disabled { opacity:.55; cursor:not-allowed; }

/* ---------- 底部按钮 ---------- */
.actions { display:flex; gap:12px; margin-top:18px; }
.actions .btn { flex:1; height:46px; border:none; border-radius:12px; font-size:15px; font-weight:600; cursor:pointer; font-family:inherit; }
.actions .btn.publish { background:#409eff; color:#fff; box-shadow:0 4px 12px rgba(64,158,255,.28); }
.actions .btn.draft { background:#f5f7fa; color:#6b7280; }
.actions .btn:disabled { opacity:.6; cursor:not-allowed; }

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
.cloud-scene .word.small{font-size:15px;--tmin:.42;--tmax:.72}
.cloud-scene .word.mid{font-size:21px;--tmin:.6;--tmax:.9}
.cloud-scene .word.big{font-size:32px;--tmin:.78;--tmax:1}
.cloud-scene .word.main{font-size:40px;--tmin:.82;--tmax:1}
@keyframes cloudTwinkle{0%,100%{opacity:var(--tmin,.5)}50%{opacity:var(--tmax,.95)}}
.cloud-scene .word:hover{scale:1.08}
/* 选中的词：琥珀色 + 暖光脉冲（覆盖闪烁动画，避免被压暗） */
.cloud-scene .word.picked{
  color:#d98200;
  animation:cloudPop .34s ease-out, cloudPickGlow 1.8s ease-in-out infinite;
}
@keyframes cloudPop{0%{scale:1}40%{scale:1.22}100%{scale:1}}
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
