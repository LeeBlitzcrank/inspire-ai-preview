<!--
  文件：frontend/src/pages/Create.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="create-page">
    <div class="topbar">
      <span class="ico" @click="editId ? goBack() : $router.push('/')">{{ editId ? '←' : '🍎' }}</span>
      <span class="page-title">{{ editId ? '编辑灵感' : '录入新灵感' }}</span>
      <div class="topbar-right">
        <span v-if="!editId" class="ico draft-ico" title="本地草稿" @click="toggleDraftPanel">📝</span>
        <span class="ico" @click="$router.push('/personal')">👤</span>
      </div>
    </div>
    <div v-if="draftPanelOpen && !editId" class="local-draft-panel">
      <div class="local-draft-head">
        <b>本地草稿</b>
        <span>{{ localDrafts.length }} 份 · 自动保存</span>
        <button type="button" @click="newLocalDraft">新建</button>
      </div>
      <div v-if="localDrafts.length" class="local-draft-list">
        <div v-for="item in localDrafts" :key="item.id" class="local-draft-item"
             :class="{ active: item.id === localDraftId }" @click="openLocalDraft(item)">
          <b>{{ item.title || '未命名草稿' }}</b>
          <small>{{ formatDraftTime(item.updatedAt) }}</small>
          <button type="button" @click.stop="removeLocalDraft(item.id)">删除</button>
        </div>
      </div>
      <div v-else class="local-draft-empty">还没有本地草稿</div>
    </div>
    <div class="form-body">

      <div v-if="quoteSource" class="card quote-create-card">
        <img v-if="quoteSource.img" :src="thumbOf(quoteSource.img, 240)" alt="">
        <div class="quote-create-copy">
          <small>引用 {{ quoteSource.nickname || '灵感创作者' }}</small>
          <b>{{ quoteSource.title }}</b>
        </div>
        <button type="button" @click="quoteSource = null">移除</button>
      </div>

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
            <input :value="aiKeyword" placeholder="输入关键词，重新探索…" @input="setAiKeyword($event.target.value)" @keyup.enter="handleExplore" />
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

        <div v-if="historyList.length" class="history-panel">
          <button class="history-toggle" type="button" @click="toggleHistoryPanel">
            <span>最近探索 · {{ historyList.length }}</span>
            <span>{{ historyOpen ? '收起' : '展开' }}</span>
          </button>
          <div v-if="historyOpen" class="history-list">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
              :class="{ active: String(currentHistoryId) === String(item.id) }"
              @click="useHistory(item)"
            >
              <div class="history-main">
                <b>{{ item.keyword || '未命名探索' }}</b>
                <span>{{ item.createTime ? formatHistoryTime(item.createTime) : '' }}</span>
              </div>
              <small>{{ item.selectedTitle || '未使用' }}</small>
              <button type="button" @click.stop="removeHistory(item.id)">删除</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ============ 灵感标题 ============ -->
      <div class="card">
        <div class="title-head">
          <label class="label">灵感标题</label>
          <button type="button" class="ai-mini" :disabled="titleGenerating" @click="generateTitles">
            {{ titleGenerating ? '生成中…' : 'AI 生成标题' }}
          </button>
        </div>
        <input v-model="form.title" class="title-input" maxlength="16" placeholder="输入简短标题…" />
        <div class="title-count">{{ titleLength }}/16</div>
        <div v-if="titleSuggestions.length" class="title-suggestions">
          <button v-for="(item, idx) in titleSuggestions" :key="idx" type="button" @click="applyTitle(item)">
            {{ item }}
          </button>
        </div>
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
            <button type="button" class="tb-btn ai-rewrite-btn" title="改写选中的正文"
                    @mousedown.prevent="captureEditorSelection" @click="toggleRewritePanel">AI 改写</button>
          </div>
          <div v-if="rewriteOpen" class="rewrite-bar">
            <span>改写选中内容：</span>
            <button v-for="style in REWRITE_STYLES" :key="style" type="button"
                    :disabled="rewriting" @click="rewriteSelection(style)">
              {{ rewriting ? '处理中…' : style }}
            </button>
          </div>
          <div ref="descRef" class="editable" contenteditable="true"
               data-placeholder="详细描述你的创意；可用上方按钮加粗、设标题，或点左下角语音输入…"
               @input="onDescInput"
               @mouseup="captureEditorSelection"
               @keyup="captureEditorSelection"></div>
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
          <div class="img-head-actions">
            <button type="button" class="ai-img-btn" :disabled="!form.images.length" @click="setGridPreviewOpen(true)">九宫格预览</button>
            <button type="button" class="ai-img-btn" @click="toggleSuggest">🤖 AI 配图</button>
          </div>
        </div>
        <div class="image-grid">
          <div
            v-for="(img, idx) in form.images"
            :key="img + '#' + idx"
            class="thumb"
            :class="{ picked: selectedImage === img, cover: coverImage === img }"
            draggable="true"
            @click="selectMediaImage(img)"
            @dragstart="startImageDrag(idx)"
            @dragover.prevent
            @drop="dropImage(idx)"
          >
            <video v-if="isVideoUrl(img)" :src="img" muted playsinline preload="metadata"></video>
            <img v-else loading="lazy" decoding="async" :src="thumbOf(img, 400)" alt="" />
            <span v-if="isVideoUrl(img)" class="video-badge">▶</span>
            <span v-if="coverImage === img" class="cover-badge">封面</span>
            <div v-if="imageProgress[img] !== undefined" class="thumb-mask">{{ imageProgress[img] }}%</div>
            <button v-if="!isVideoUrl(img)" type="button" class="cover-btn" @click.stop="setCover(img)">设为封面</button>
            <button type="button" class="del" aria-label="删除图片"
                    @pointerdown.stop @click.stop.prevent="removeImage(idx)">×</button>
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

        <div v-if="gridPreviewOpen" class="overlay grid-preview-overlay" @click.self="setGridPreviewOpen(false)">
          <div class="grid-preview-panel">
            <div class="grid-preview-head">
              <div>
                <h3>九宫格预览</h3>
                <p>按当前顺序展示，封面图优先显示。</p>
              </div>
              <button type="button" @click="setGridPreviewOpen(false)">×</button>
            </div>
            <div class="grid-preview">
              <div v-for="idx in 9" :key="idx" class="grid-cell">
                <template v-if="gridImages[idx - 1]">
                  <video v-if="isVideoUrl(gridImages[idx - 1])" :src="gridImages[idx - 1]" muted playsinline></video>
                  <img v-else :src="thumbOf(gridImages[idx - 1], 400)" alt="">
                  <span v-if="coverImage === gridImages[idx - 1]" class="grid-cover">封面</span>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-if="videoUploadTasks.length" class="video-upload-list">
          <div v-for="task in videoUploadTasks" :key="task.id" class="video-upload-item">
            <div class="video-upload-head">
              <span class="video-upload-name">🎬 {{ task.name }}</span>
              <span class="video-upload-size">{{ task.sizeText }}</span>
              <span class="video-upload-state">
                {{ task.status === 'done' ? '上传完成'
                  : task.status === 'processing' ? '服务器处理中...'
                  : task.status === 'failed' ? '上传失败'
                  : `${task.progress}%` }}
              </span>
            </div>
            <div class="video-upload-bar">
              <i :style="{ width: task.progress + '%' }" :class="{ failed: task.status === 'failed' }"></i>
            </div>
            <div v-if="task.status === 'failed'" class="video-upload-error">
              <span>{{ task.error || '网络中断，请重试' }}</span>
              <button type="button" @click="retryVideoUpload(task)">重新上传</button>
            </div>
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
            <input :checked="keepOriginalOnly" type="checkbox" @change="setKeepOriginalOnly($event.target.checked)" />
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
    <el-dialog :model-value="trimDialogVisible" title="裁剪视频" width="320px" append-to-body
               @update:model-value="setTrimDialogVisible">
      <div class="trim-tip">
        {{ trimTarget?.name || '视频' }}<template v-if="trimTarget?.duration"> · 总时长 {{ trimTarget.duration }} 秒</template>
      </div>
      <el-input :model-value="trimStart" placeholder="开始时间（秒）" @update:model-value="setTrimStart" />
      <el-input :model-value="trimDuration" placeholder="保留时长（秒）" style="margin-top:10px" @update:model-value="setTrimDuration" />
      <template #footer>
        <el-button @click="setTrimDialogVisible(false)">取消</el-button>
        <el-button type="primary" :loading="trimming" @click="doTrim">确定裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getInspireDetail, getUserInfo} from '@/api/inspire.js'
import {useCreateMedia} from './create/composables/useCreateMedia.js'
import {useCreateAssistant} from './create/composables/useCreateAssistant.js'
import {useCreateImageSuggestions} from './create/composables/useCreateImageSuggestions.js'
import {useCreateVoiceInput} from './create/composables/useCreateVoiceInput.js'
import {useCreateDrafts} from './create/composables/useCreateDrafts.js'
import {useCreateEditor} from './create/composables/useCreateEditor.js'
import {useCreateSubmission} from './create/composables/useCreateSubmission.js'
import {useCreateCategories} from './create/composables/useCreateCategories.js'
import {thumbOf} from '@/utils/media.js'

const router = useRouter()
const route = useRoute()
const editId = computed(() => route.params.id)

// 分类由后台维护，改成读接口；接口异常时退回一份默认值
const {tags, loadTags} = useCreateCategories()

const loading = ref(false)
const form = ref({ title: '', tag: '', content: '', images: [], img: '', publishCity: '' })
const quoteSource = ref(null)
const {
  descRef,
  contentLen,
  setContent,
  onDescInput,
  execCmd,
  execBlock,
  clearFormat,
  getEditorPlainText,
  autoFormatContent
} = useCreateEditor(form)

const {
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
} = useCreateAssistant({form, descRef, setContent, getEditorPlainText, onDescInput})

const {voiceActive, voiceError, toggleVoice} = useCreateVoiceInput({form, descRef, setContent, onDescInput})

const {
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
} = useCreateMedia({form})

const {
  localDrafts,
  localDraftId,
  draftPanelOpen,
  draftHydrating,
  loadLocalDrafts,
  saveLocalDraftNow,
  scheduleLocalDraft,
  openLocalDraft,
  newLocalDraft,
  removeLocalDraft,
  formatDraftTime
} = useCreateDrafts({form, editId, coverImage, quoteSource, clampTitle, setContent})

const setAiKeyword = value => { aiKeyword.value = value }; const toggleDraftPanel = () => { draftPanelOpen.value = !draftPanelOpen.value }
const toggleHistoryPanel = () => { historyOpen.value = !historyOpen.value }; const toggleRewritePanel = () => { rewriteOpen.value = !rewriteOpen.value }
const setGridPreviewOpen = value => { gridPreviewOpen.value = value }; const selectMediaImage = url => { selectedImage.value = url }
const startImageDrag = index => { dragImageIndex.value = index }; const setKeepOriginalOnly = value => { keepOriginalOnly.value = value }
const setTrimDialogVisible = value => { trimDialogVisible.value = value }; const setTrimStart = value => { trimStart.value = value }; const setTrimDuration = value => { trimDuration.value = value }

watch([form, coverImage], scheduleLocalDraft, { deep: true })

// 编辑模式：预填表单
onMounted(async () => {
  loadLocalDrafts()
  loadCloudWords()
  loadTags()
  loadAiHistory()
  if (route.params.id) {
    document.title = '编辑灵感'
    try {
      const res = await getInspireDetail(route.params.id)
      if (res.data) {
        form.value.title = clampTitle(res.data.title || '')
        form.value.tag = res.data.tag || ''
        setContent(res.data.content || '')
        form.value.publishCity = res.data.publishCity || ''
        if (res.data.images && res.data.images.length > 0) {
          form.value.images = res.data.images
        } else if (res.data.img) {
          form.value.images = [res.data.img]
        }
        coverImage.value = res.data.img || form.value.images[0] || ''
      }
    } catch (e) { console.error(e) }
  } else {
    if (route.query.quoteId) {
      try {
        const quoteRes = await getInspireDetail(route.query.quoteId)
        if (quoteRes.code === 200 && quoteRes.data?.id) quoteSource.value = quoteRes.data
      } catch (e) {
        console.error('[quote source]', e)
      }
    }
    // 新创建时从用户信息自动填充发布城市
    try {
      const userRes = await getUserInfo()
      if (userRes.code === 200 && userRes.data?.city) {
        form.value.publishCity = userRes.data.city
      }
    } catch (e) {}
  }
  draftHydrating.value = false
})

const {goBack, submit} = useCreateSubmission({
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
})

const {
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
} = useCreateImageSuggestions({form, coverImage})
</script>
<style scoped src="./create/styles/create.css"></style>
