<!--
  文件：frontend/src/pages/RagAssistant.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="rag-page">
    <header class="rag-header">
      <button class="icon-btn" type="button" aria-label="返回" @click="$router.back()">
        <span>←</span>
      </button>
      <div class="head-copy">
        <b>灵感问答</b>
        <span>基于全站灵感、图片描述与标签回答</span>
      </div>
      <button class="icon-btn" type="button" aria-label="搜索" @click="$router.push('/search')">⌕</button>
    </header>

    <section class="ask-panel">
      <textarea
        v-model="query"
        maxlength="500"
        placeholder="例如：帮我找几张低饱和、有生活感的露营灵感"
        @keydown.meta.enter.prevent="submit"
        @keydown.ctrl.enter.prevent="submit"
      />
      <div v-if="imagePreview" class="image-preview">
        <img :src="imagePreview" alt="待检索图片">
        <button type="button" @click="clearImage">移除</button>
      </div>
      <div class="ask-actions">
        <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onImage">
        <button class="ghost" type="button" @click="fileInput?.click()">上传图片</button>
        <button class="primary" type="button" :disabled="loading || (!query.trim() && !imageBase64)" @click="submit">
          {{ loading ? '思考中…' : '开始问答' }}
        </button>
      </div>
    </section>

    <div class="prompt-row">
      <button v-for="item in prompts" :key="item" type="button" @click="usePrompt(item)">{{ item }}</button>
    </div>

    <AppState
      :state="state"
      :rows="4"
      empty-icon="🧠"
      empty-text="输入问题，或上传一张图片开始检索"
      error-text="问答失败，请稍后重试"
    >
      <section v-if="answer" class="answer-card">
        <div class="answer-title">回答</div>
        <div class="answer-text">{{ answer }}</div>
      </section>

      <section v-if="sources.length" class="sources">
        <div class="sources-head">
          <b>引用来源</b>
          <span>{{ sources.length }} 条 · {{ duration }}ms</span>
        </div>
        <article
          v-for="(item, idx) in sources"
          :key="item.id"
          class="source-card"
          @click="$router.push(`/detail/${item.id}`)"
        >
          <img v-if="item.image" :src="thumbOf(item.image, 200)" alt="" loading="lazy" decoding="async">
          <div v-else class="source-placeholder">{{ idx + 1 }}</div>
          <div class="source-copy">
            <div class="source-top">
              <b>{{ item.title || '无标题' }}</b>
              <span>#{{ idx + 1 }}</span>
            </div>
            <p>{{ item.excerpt }}</p>
            <small v-if="item.tag">{{ item.tag }} · 相关度 {{ item.score }}</small>
          </div>
        </article>
      </section>
    </AppState>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {askRag} from '@/api/rag.js'
import {thumbOf} from '@/utils/media.js'
import AppState from '@/components/base/AppState.vue'

const query = ref('')
const imageBase64 = ref('')
const imagePreview = ref('')
const fileInput = ref(null)
const loading = ref(false)
const error = ref('')
const answer = ref('')
const sources = ref([])
const duration = ref(0)

const prompts = [
  '适合秋天露营的灵感',
  '低饱和胶片感照片',
  '适合周末独处的活动',
  '有手作感的家居布置'
]

const state = computed(() => {
  if (loading.value) return 'loading'
  if (error.value) return 'error'
  if (answer.value || sources.value.length) return 'ready'
  return 'empty'
})

const usePrompt = (text) => {
  query.value = text
}

const clearImage = () => {
  imageBase64.value = ''
  imagePreview.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

const onImage = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    clearImage()
    return
  }
  const dataUrl = await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
  imagePreview.value = dataUrl
  imageBase64.value = dataUrl
}

const submit = async () => {
  if (loading.value || (!query.value.trim() && !imageBase64.value)) return
  loading.value = true
  error.value = ''
  answer.value = ''
  sources.value = []
  try {
    const res = await askRag({
      query: query.value.trim(),
      imageBase64: imageBase64.value || undefined,
      topK: 8
    })
    if (res.code !== 200) throw new Error(res.msg || '问答失败')
    answer.value = res.data?.answer || ''
    sources.value = res.data?.sources || []
    duration.value = res.data?.tookMs || 0
  } catch (e) {
    error.value = e?.response?.data?.msg || e?.message || '问答失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.rag-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; color:#23423d; }
.rag-header { display:flex; align-items:center; gap:12px; margin-bottom:14px; }
.icon-btn { width:38px; height:38px; border-radius:50%; border:1px solid #d9ebe7; background:#fff; color:#356b62; font-size:20px; cursor:pointer; }
.head-copy { min-width:0; }
.head-copy b { display:block; font-size:17px; color:#173f39; }
.head-copy span { display:block; margin-top:2px; color:#78918c; font-size:11.5px; }
.head-copy + .icon-btn { margin-left:auto; }
.ask-panel { padding:14px; border:1px solid #dcece8; border-radius:17px; background:#fff; box-shadow:0 8px 24px rgba(30,90,80,.06); }
.ask-panel textarea { width:100%; min-height:108px; resize:vertical; box-sizing:border-box; border:0; outline:0; background:transparent; color:#193a35; font:inherit; font-size:15px; line-height:1.65; }
.ask-panel textarea::placeholder { color:#9aada9; }
.image-preview { position:relative; width:88px; height:88px; margin-top:8px; }
.image-preview img { width:100%; height:100%; object-fit:cover; border-radius:12px; }
.image-preview button { position:absolute; right:-6px; top:-6px; border:0; border-radius:999px; background:#254b45; color:#fff; font-size:11px; padding:3px 7px; cursor:pointer; }
.ask-actions { display:flex; justify-content:flex-end; gap:8px; padding-top:10px; border-top:1px solid #edf5f3; }
.ask-actions button { border-radius:999px; padding:8px 14px; font:inherit; font-size:12.5px; cursor:pointer; }
.ghost { border:1px solid #d5e8e3; background:#fff; color:#4e7f76; }
.primary { border:1px solid #3e8175; background:#3e8175; color:#fff; }
.primary:disabled { opacity:.45; cursor:not-allowed; }
.prompt-row { display:flex; gap:7px; overflow-x:auto; padding:12px 0 4px; scrollbar-width:none; }
.prompt-row::-webkit-scrollbar { display:none; }
.prompt-row button { flex:0 0 auto; border:1px solid #dcece8; border-radius:999px; background:#fff; color:#557d75; padding:6px 10px; font:inherit; font-size:11.5px; cursor:pointer; }
.answer-card { margin-top:14px; padding:15px; border-radius:16px; background:#f1f8f6; border:1px solid #d9ece7; }
.answer-title { margin-bottom:8px; color:#367267; font-size:12px; font-weight:700; letter-spacing:.04em; }
.answer-text { white-space:pre-wrap; color:#244b44; font-size:14px; line-height:1.75; }
.sources { margin-top:16px; }
.sources-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:9px; }
.sources-head b { font-size:14px; }
.sources-head span { color:#8aa09c; font-size:11px; }
.source-card { display:flex; gap:11px; padding:11px; margin-bottom:9px; border:1px solid #e0eeeb; border-radius:15px; background:#fff; cursor:pointer; transition:.15s; }
.source-card:hover { transform:translateY(-1px); border-color:#b9d9d2; box-shadow:0 7px 18px rgba(30,90,80,.08); }
.source-card img, .source-placeholder { width:66px; height:66px; flex:0 0 auto; border-radius:11px; object-fit:cover; }
.source-placeholder { display:grid; place-items:center; background:#e8f4f1; color:#5f8a82; font-weight:700; }
.source-copy { min-width:0; flex:1; }
.source-top { display:flex; gap:8px; align-items:flex-start; }
.source-top b { flex:1; min-width:0; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; font-size:13.5px; line-height:1.45; color:#1e4540; }
.source-top span { color:#a1b4b0; font-size:11px; }
.source-copy p { margin:5px 0; color:#6f8782; font-size:11.5px; line-height:1.45; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
.source-copy small { color:#90a6a2; font-size:10.5px; }
</style>
