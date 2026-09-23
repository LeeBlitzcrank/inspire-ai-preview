<template>
  <div class="sm-page">
    <header class="sm-top">
      <button type="button" aria-label="返回" class="round" @click="goBack">‹</button>
      <b>{{ editingId ? '编辑系列' : '我的系列' }}</b>
      <button v-if="!editingId" type="button" aria-label="新建系列" class="round add" @click="openCreate">＋</button>
      <span v-else></span>
    </header>

    <AppState
      v-if="!editingId"
      :state="listState"
      :rows="4"
      empty-icon="📚"
      empty-text="还没有系列"
      error-text="系列加载失败"
      @retry="loadSeries"
    >
      <div class="overview">
        <button class="create-card" type="button" @click="openCreate">
          <span class="create-icon">＋</span>
          <span>
            <b>新建系列</b>
          </span>
          <span class="chev">›</span>
        </button>

        <article
          v-for="item in seriesList"
          :key="item.id"
          class="series-card"
          role="button"
          tabindex="0"
          @click="openEditor(item.id)"
          @keydown.enter="openEditor(item.id)"
        >
          <span class="cover" :style="coverStyle(item.cover)"></span>
          <span class="copy">
            <b>{{ item.name }}</b>
            <small>{{ item.description || '暂无描述' }}</small>
            <em>共 {{ item.total }} 篇</em>
          </span>
          <span class="card-ops" @click.stop>
            <button type="button" @click="openEdit(item)">编辑</button>
            <button type="button" class="danger" @click="confirmDelete(item)">删除</button>
          </span>
        </article>
      </div>
    </AppState>

    <AppState
      v-else
      :state="detailState"
      :rows="5"
      empty-icon="📄"
      empty-text="还没有文章"
      error-text="系列加载失败"
      @retry="loadDetail"
    >
      <template v-if="currentSeries">
        <div class="editor-actions">
          <button type="button" class="back-link" @click="backToList">‹ 全部系列</button>
          <button type="button" class="add-article" @click="openCandidates">＋ 添加灵感</button>
        </div>

        <section class="editor-hero">
          <span class="hero-cover" :style="coverStyle(currentSeries.cover)"></span>
          <div class="hero-copy">
            <span>INSPIRE SERIES</span>
            <h1>{{ currentSeries.name }}</h1>
            <p>{{ currentSeries.description || '暂无描述' }}</p>
            <small>{{ currentSeries.total }} 篇 · 更新于 {{ formatTime(currentSeries.updateTime) }}</small>
          </div>
          <div class="hero-ops">
            <button type="button" @click="openEdit(currentSeries)">编辑资料</button>
            <button type="button" @click="viewPublic">查看前台</button>
          </div>
        </section>

        <div class="list-head">
          <b>文章顺序</b>
          <span>{{ currentSeries.total }} / 100</span>
        </div>

        <section class="article-list">
          <div v-if="!currentSeries.articles?.length" class="empty-articles">还没有文章</div>
          <div
            v-for="(item, index) in currentSeries.articles"
            :key="item.id"
            class="article-row"
          >
            <span class="order">{{ pad(index + 1) }}</span>
            <span class="article-thumb" :style="articleCoverStyle(item)"></span>
            <span class="article-copy">
              <b>{{ item.title || '无标题' }}</b>
              <small>{{ item.tag || '灵感' }} · {{ formatTime(item.createTime) }}</small>
            </span>
            <span class="move-ops">
              <button
                type="button"
                aria-label="上移"
                :disabled="index === 0 || reordering"
                @click="moveArticle(index, -1)"
              >↑</button>
              <button
                type="button"
                aria-label="下移"
                :disabled="index === currentSeries.articles.length - 1 || reordering"
                @click="moveArticle(index, 1)"
              >↓</button>
              <button type="button" class="remove" @click="confirmRemove(item)">移除</button>
            </span>
          </div>
        </section>

        <div v-if="currentSeries.articles?.length" class="order-foot">
          <button type="button" @click="confirmDelete(currentSeries)">删除整个系列</button>
        </div>
      </template>
    </AppState>

    <el-dialog
      v-model="metaDialogVisible"
      :title="metaForm.id ? '编辑系列' : '新建系列'"
      width="90%"
      append-to-body
    >
      <div class="meta-form">
        <label>系列名称</label>
        <el-input v-model="metaForm.name" maxlength="50" show-word-limit placeholder="给系列起个名字" />
        <label>系列描述</label>
        <el-input
          v-model="metaForm.description"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
          placeholder="简单说明这组内容"
        />
      </div>
      <template #footer>
        <el-button @click="metaDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMeta" @click="saveMeta">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="candidateDialogVisible" title="添加灵感" width="90%" append-to-body>
      <div class="candidate-search">
        <el-input
          v-model="candidateKeyword"
          clearable
          placeholder="搜索我的灵感"
          @keyup.enter="loadCandidates(true)"
          @clear="loadCandidates(true)"
        />
        <button type="button" @click="loadCandidates(true)">搜索</button>
      </div>
      <div class="candidate-body">
        <div v-if="candidateLoading && !candidates.length" class="candidate-tip">加载中…</div>
        <div v-else-if="candidateError && !candidates.length" class="candidate-tip error">
          {{ candidateError }}
          <button type="button" @click="loadCandidates(true)">重试</button>
        </div>
        <div v-else-if="!candidates.length" class="candidate-tip">没有可添加的灵感</div>
        <button
          v-for="item in candidates"
          :key="item.id"
          type="button"
          class="candidate-row"
          :disabled="addingId === String(item.id)"
          @click="handleAdd(item)"
        >
          <span class="article-thumb" :style="articleCoverStyle(item)"></span>
          <span class="article-copy">
            <b>{{ item.title || '无标题' }}</b>
            <small>{{ item.tag || '灵感' }} · {{ formatTime(item.createTime) }}</small>
          </span>
          <span class="candidate-add">{{ addingId === String(item.id) ? '…' : '＋' }}</span>
        </button>
        <button
          v-if="candidateHasMore"
          type="button"
          class="candidate-more"
          :disabled="candidateLoading"
          @click="loadCandidates(false)"
        >{{ candidateLoading ? '加载中…' : '加载更多' }}</button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  addSeriesArticle,
  createSeries,
  deleteSeries,
  getMySeries,
  getMySeriesDetail,
  getSeriesCandidates,
  removeSeriesArticle,
  reorderSeriesArticles,
  updateSeries
} from '@/api/inspire.js'
import {thumbOf} from '@/utils/media.js'

const router = useRouter()
const seriesList = ref([])
const listLoading = ref(false)
const listError = ref('')
const editingId = ref('')
const currentSeries = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
const reordering = ref(false)

const listState = computed(() => {
  if (listLoading.value && !seriesList.value.length) return 'loading'
  if (listError.value && !seriesList.value.length) return 'error'
  return seriesList.value.length ? 'ready' : 'empty'
})

const detailState = computed(() => {
  if (detailLoading.value && !currentSeries.value) return 'loading'
  if (detailError.value && !currentSeries.value) return 'error'
  return currentSeries.value ? 'ready' : 'empty'
})

const loadSeries = async () => {
  listLoading.value = true
  listError.value = ''
  try {
    const res = await getMySeries()
    if (res.code !== 200) throw new Error(res.msg || '系列加载失败')
    seriesList.value = res.data || []
  } catch (e) {
    listError.value = e?.response?.data?.msg || e?.message || '系列加载失败'
  } finally {
    listLoading.value = false
  }
}

const loadDetail = async () => {
  if (!editingId.value) return
  detailLoading.value = true
  detailError.value = ''
  try {
    const res = await getMySeriesDetail(editingId.value)
    if (res.code !== 200) throw new Error(res.msg || '系列加载失败')
    currentSeries.value = res.data || null
  } catch (e) {
    detailError.value = e?.response?.data?.msg || e?.message || '系列加载失败'
  } finally {
    detailLoading.value = false
  }
}

const openEditor = async (id) => {
  editingId.value = String(id)
  currentSeries.value = null
  await loadDetail()
}

const backToList = () => {
  editingId.value = ''
  currentSeries.value = null
  detailError.value = ''
  loadSeries()
}

const goBack = () => {
  if (editingId.value) {
    backToList()
    return
  }
  if (window.history.state?.back) router.back()
  else router.replace('/personal')
}

const metaDialogVisible = ref(false)
const savingMeta = ref(false)
const metaForm = ref({ id: '', name: '', description: '' })

const openCreate = () => {
  metaForm.value = { id: '', name: '', description: '' }
  metaDialogVisible.value = true
}

const openEdit = (series) => {
  metaForm.value = {
    id: String(series.id),
    name: series.name || '',
    description: series.description || ''
  }
  metaDialogVisible.value = true
}

const applySeries = (series) => {
  currentSeries.value = series
  const summary = {
    ...series,
    articles: undefined
  }
  const index = seriesList.value.findIndex(item => String(item.id) === String(series.id))
  if (index >= 0) seriesList.value[index] = summary
  else seriesList.value.unshift(summary)
}

const saveMeta = async () => {
  const name = metaForm.value.name.trim()
  if (!name) return ElMessage.warning('请输入系列名称')
  savingMeta.value = true
  try {
    const payload = { name, description: metaForm.value.description.trim() }
    const res = metaForm.value.id
      ? await updateSeries(metaForm.value.id, payload)
      : await createSeries(payload)
    if (res.code !== 200) throw new Error(res.msg || '系列保存失败')
    metaDialogVisible.value = false
    ElMessage.success(metaForm.value.id ? '系列已更新' : '系列已创建')
    if (metaForm.value.id && currentSeries.value) {
      applySeries(res.data)
    } else {
      await loadSeries()
      if (res.data?.id) await openEditor(res.data.id)
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '系列保存失败')
  } finally {
    savingMeta.value = false
  }
}

const confirmDelete = async (series) => {
  try {
    await ElMessageBox.confirm(
      `删除“${series.name}”后，文章会保留，但会移出这个系列。`,
      '删除系列',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteSeries(series.id)
    if (res.code !== 200) throw new Error(res.msg || '删除失败')
    seriesList.value = seriesList.value.filter(item => String(item.id) !== String(series.id))
    if (String(editingId.value) === String(series.id)) backToList()
    ElMessage.success('系列已删除')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.response?.data?.msg || e?.message || '删除失败')
  }
}

const moveArticle = async (index, delta) => {
  const target = index + delta
  const articles = currentSeries.value?.articles || []
  if (target < 0 || target >= articles.length || reordering.value) return
  const previous = [...articles]
  const next = [...articles]
  ;[next[index], next[target]] = [next[target], next[index]]
  currentSeries.value = { ...currentSeries.value, articles: next }
  reordering.value = true
  try {
    const res = await reorderSeriesArticles(currentSeries.value.id, next.map(item => item.id))
    if (res.code !== 200) throw new Error(res.msg || '排序失败')
    applySeries(res.data)
  } catch (e) {
    currentSeries.value = { ...currentSeries.value, articles: previous }
    ElMessage.error(e?.response?.data?.msg || e?.message || '排序失败')
  } finally {
    reordering.value = false
  }
}

const confirmRemove = async (item) => {
  try {
    await ElMessageBox.confirm(`将“${item.title || '无标题'}”移出当前系列？`, '移出系列', {
      confirmButtonText: '移出',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await removeSeriesArticle(currentSeries.value.id, item.id)
    if (res.code !== 200) throw new Error(res.msg || '移除失败')
    applySeries(res.data)
    ElMessage.success('已移出系列')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.response?.data?.msg || e?.message || '移除失败')
  }
}

const candidateDialogVisible = ref(false)
const candidateKeyword = ref('')
const candidates = ref([])
const candidateLoading = ref(false)
const candidateError = ref('')
const candidatePage = ref(1)
const candidateTotal = ref(0)
const candidateHasMore = ref(false)
const addingId = ref('')
const CANDIDATE_PAGE_SIZE = 20

const openCandidates = () => {
  candidateDialogVisible.value = true
  candidateKeyword.value = ''
  candidates.value = []
  loadCandidates(true)
}

const loadCandidates = async (reset = false) => {
  if (candidateLoading.value) return
  if (!reset && !candidateHasMore.value) return
  candidateLoading.value = true
  candidateError.value = ''
  const page = reset ? 1 : candidatePage.value + 1
  try {
    const res = await getSeriesCandidates(currentSeries.value.id, {
      keyword: candidateKeyword.value.trim() || undefined,
      page,
      size: CANDIDATE_PAGE_SIZE
    })
    if (res.code !== 200) throw new Error(res.msg || '加载失败')
    const rows = res.data?.records || []
    candidates.value = reset ? rows : [...candidates.value, ...rows]
    candidatePage.value = page
    candidateTotal.value = Number(res.data?.total || 0)
    candidateHasMore.value = candidates.value.length < candidateTotal.value
  } catch (e) {
    candidateError.value = e?.response?.data?.msg || e?.message || '加载失败'
  } finally {
    candidateLoading.value = false
  }
}

const handleAdd = async (item) => {
  addingId.value = String(item.id)
  try {
    const res = await addSeriesArticle(currentSeries.value.id, item.id)
    if (res.code !== 200) throw new Error(res.msg || '添加失败')
    applySeries(res.data)
    candidates.value = candidates.value.filter(row => String(row.id) !== String(item.id))
    candidateTotal.value = Math.max(0, candidateTotal.value - 1)
    ElMessage.success('已加入系列')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '添加失败')
  } finally {
    addingId.value = ''
  }
}

const viewPublic = () => {
  router.push(`/series/${currentSeries.value.id}`)
}

const coverStyle = (url) => {
  if (!url) return { background: 'linear-gradient(135deg,#dff4ef,#cce9e3)' }
  return {
    backgroundImage: `url("${thumbOf(url, 600)}")`,
    backgroundSize: 'cover',
    backgroundPosition: 'center'
  }
}

const articleCoverStyle = (item) => {
  const url = Array.isArray(item?.images) && item.images.length ? item.images[0] : item?.img
  return coverStyle(url)
}

const pad = (n) => String(n).padStart(2, '0')
const formatTime = (value) => {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return date.toLocaleDateString('zh-CN')
}

loadSeries()
</script>

<style scoped>
.sm-page { min-height:100vh; background:#f7fbfa; padding-bottom:44px; }
.sm-top { position:sticky; top:0; z-index:8; display:grid; grid-template-columns:40px 1fr 40px; align-items:center; padding:12px 14px; background:rgba(255,255,255,.96); backdrop-filter:blur(10px); border-bottom:1px solid #edf4f2; }
.sm-top b { text-align:center; color:#203b37; font-size:16px; }
.round { width:36px; height:36px; border:0; border-radius:50%; background:#fff; color:#315f5a; font-size:25px; line-height:1; cursor:pointer; box-shadow:0 2px 10px rgba(32,71,66,.08); }
.round.add { font-size:21px; color:#0f766e; }
.overview { padding:14px; display:grid; gap:12px; }
.create-card, .series-card { width:100%; border:1px solid #dcebe8; border-radius:18px; background:#fff; color:#294a45; font:inherit; text-align:left; cursor:pointer; }
.create-card { min-height:74px; padding:13px 14px; display:flex; align-items:center; gap:12px; border-style:dashed; color:#47756d; }
.create-icon { width:40px; height:40px; flex:0 0 auto; display:grid; place-items:center; border-radius:13px; background:#e8f6f2; color:#0f766e; font-size:23px; }
.create-card b, .series-card b { display:block; font-size:14px; color:#294a45; }
.create-card small, .series-card small { display:block; margin-top:4px; font-size:11.5px; color:#91a39f; }
.chev { margin-left:auto; color:#9db2ae; font-size:22px; }
.series-card { padding:12px; display:grid; grid-template-columns:76px minmax(0,1fr); gap:12px; }
.cover { width:76px; height:76px; border-radius:14px; background:#e8f4f1; }
.copy { min-width:0; align-self:center; }
.copy small { overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; line-height:1.45; }
.copy em { display:block; margin-top:7px; color:#0f766e; font-size:11px; font-style:normal; font-weight:700; }
.card-ops { grid-column:1/-1; display:flex; justify-content:flex-end; gap:8px; padding-top:9px; border-top:1px solid #eef4f2; }
.card-ops button { padding:6px 13px; border:1px solid #dcebe8; border-radius:999px; background:#f9fcfb; color:#4d7770; font:inherit; font-size:12px; cursor:pointer; }
.card-ops button.danger { color:#c2613a; border-color:#f0dcd3; background:#fff9f6; }
.editor-actions { display:flex; align-items:center; justify-content:space-between; padding:12px 14px 4px; }
.back-link { border:0; background:transparent; color:#4d7770; font:inherit; font-size:12.5px; cursor:pointer; padding:8px 0; }
.add-article { padding:8px 13px; border:0; border-radius:999px; background:#0f766e; color:#fff; font:inherit; font-size:12.5px; font-weight:700; cursor:pointer; }
.editor-hero { margin:10px 14px 14px; padding:15px; border-radius:20px; background:linear-gradient(160deg,#f5fbf9,#fff); border:1px solid #dcebe8; }
.hero-cover { display:block; height:170px; border-radius:16px; background:#e6f4f0; }
.hero-copy { padding:13px 2px 0; }
.hero-copy > span { color:#62938a; font-size:9px; letter-spacing:.14em; }
.hero-copy h1 { margin:6px 0; color:#24433e; font-size:20px; line-height:1.3; }
.hero-copy p { margin:0; color:#687d79; font-size:12px; line-height:1.65; }
.hero-copy small { display:block; margin-top:8px; color:#9aaba7; font-size:10.5px; }
.hero-ops { display:grid; grid-template-columns:1fr 1fr; gap:9px; margin-top:13px; }
.hero-ops button { height:36px; border:1px solid #cfe5e0; border-radius:11px; background:#fff; color:#3f7067; font:inherit; font-size:12.5px; cursor:pointer; }
.list-head { display:flex; align-items:center; justify-content:space-between; padding:0 17px 9px; }
.list-head b { color:#2f4a46; font-size:13.5px; }
.list-head span { color:#9aaba7; font-size:11px; }
.article-list { margin:0 14px; overflow:hidden; border:1px solid #e0ece9; border-radius:18px; background:#fff; }
.article-row { display:flex; align-items:center; gap:10px; min-height:70px; padding:10px 11px; border-bottom:1px solid #eef4f2; }
.article-row:last-child { border-bottom:0; }
.empty-articles { padding:38px 0; text-align:center; color:#93a5a1; font-size:12.5px; }
.order { width:27px; flex:0 0 auto; color:#8da29e; font-size:11px; font-weight:800; }
.article-thumb { width:45px; height:45px; flex:0 0 auto; border-radius:11px; background:#e8f4f1; }
.article-copy { flex:1; min-width:0; }
.article-copy b { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#2f4844; font-size:13px; }
.article-copy small { display:block; margin-top:5px; color:#98a7a4; font-size:10.5px; }
.move-ops { display:flex; align-items:center; gap:5px; }
.move-ops button { width:29px; height:29px; padding:0; border:1px solid #dcebe8; border-radius:9px; background:#f9fcfb; color:#4d7770; font:inherit; font-size:13px; cursor:pointer; }
.move-ops button:disabled { opacity:.35; cursor:not-allowed; }
.move-ops button.remove { width:auto; padding:0 9px; color:#c2613a; border-color:#f0dcd3; background:#fff9f6; font-size:11.5px; }
.order-foot { padding:14px; text-align:center; }
.order-foot button { border:0; background:transparent; color:#c2613a; font:inherit; font-size:12px; cursor:pointer; }
.meta-form { display:grid; gap:8px; padding:4px 0; }
.meta-form label { margin-top:8px; color:#657b77; font-size:12.5px; }
.candidate-search { display:grid; grid-template-columns:minmax(0,1fr) 62px; gap:8px; }
.candidate-search button { border:0; border-radius:10px; background:#0f766e; color:#fff; font:inherit; font-size:12.5px; cursor:pointer; }
.candidate-body { max-height:52vh; overflow:auto; padding-top:10px; }
.candidate-tip { padding:28px 0; text-align:center; color:#93a5a1; font-size:12.5px; }
.candidate-tip.error { color:#c2613a; }
.candidate-tip button { display:block; margin:10px auto 0; border:0; background:transparent; color:#0f766e; font:inherit; cursor:pointer; }
.candidate-row { width:100%; display:flex; align-items:center; gap:10px; padding:10px 2px; border:0; border-bottom:1px solid #eef4f2; background:transparent; text-align:left; cursor:pointer; }
.candidate-row:disabled { opacity:.55; cursor:wait; }
.candidate-add { width:30px; height:30px; display:grid; place-items:center; border-radius:50%; background:#e8f6f2; color:#0f766e; font-size:20px; }
.candidate-more { width:100%; margin-top:10px; padding:10px; border:1px solid #dcebe8; border-radius:11px; background:#f9fcfb; color:#4d7770; font:inherit; cursor:pointer; }
</style>
