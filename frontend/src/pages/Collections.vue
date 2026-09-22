<template>
  <div id="collections-page">
    <div class="head" id="c-head">
      <button class="back-btn" id="c-back" @click="$router.push('/personal')">←</button>
      <h2 id="c-title">我的收藏夹</h2>
      <button class="btn-new" id="c-new" @click="showCreate = true">＋ 新建</button>
    </div>

    <!-- 文件夹网格（与预览一致：两列卡片，右上角重命名） -->
    <AppState
      :state="folderState"
      :rows="2"
      loading-variant="grid"
      empty-icon="📁"
      empty-text="还没有收藏夹"
      error-text="收藏夹加载失败"
      @retry="loadFolders"
    >
    <div class="folder-grid" id="c-folder-grid">
      <!-- 未分类放第一个，和预览一致 -->
      <AppCard v-if="uncFolder" class="folder unc" id="c-folder-unc"
           padding="13px 12px"
           clickable
           :class="{ on: String(activeFolder) === String(uncFolder ? uncFolder.id : -1) }"
           @click="selectFolder(uncFolder ? uncFolder.id : -1)">
        <div class="top"><div class="ico">📂</div><div class="fn">未分类</div></div>
        <div class="fc">{{ uncFolder ? folderCount(uncFolder) + ' 条灵感' : '未归档的收藏' }}</div>
      </AppCard>
      <AppCard v-for="f in namedFolders" :key="f.id" class="folder" id="c-folder"
           padding="13px 12px"
           clickable
           :class="{ on: String(activeFolder) === String(f.id) }" @click="selectFolder(f.id)">
        <div class="top">
          <div class="ico">{{ f.icon || '📁' }}</div>
          <div class="fn">{{ f.name }}</div>
        </div>
        <div class="fc">{{ folderCount(f) }} 条灵感</div>
        <button class="edit" id="c-folder-edit" title="重命名" @click.stop="editFolder(f)">✎</button>
      </AppCard>
    </div>
    </AppState>

    <!-- 选中文件夹后的收藏列表 -->
    <div v-if="activeFolder !== null" class="list-head" id="c-list-head">
      <span class="t2">{{ activeFolderName }}</span>
      <span class="n">共 {{ collectedInspires.length }} 条</span>
    </div>
    <AppState
      :state="collectState"
      :rows="4"
      empty-icon="⭐"
      empty-text="该文件夹还没有收藏灵感"
      error-text="收藏内容加载失败"
      @retry="loadCollects"
    >
    <!-- 虚拟列表：只渲染可视区，滚到底继续放出一批（与预览一致） -->
    <div
      v-if="activeFolder !== null && collectedInspires.length"
      ref="listRef"
      class="virt"
      @scroll.passive="onListScroll"
    >
      <div class="spacer" :style="{ height: (visibleCount * ITEM_H) + 'px' }">
        <AppCard
          v-for="row in visibleRows"
          :key="row.data.id"
          class="ccard"
          id="c-card"
          padding="11px 12px"
          clickable
          :style="{ position: 'absolute', left: 0, right: 0, top: (row.index * ITEM_H) + 'px' }"
          @click="goDetail(row.data.id)"
        >
          <div class="cthumb" :style="thumbStyle(row.data)">
            <img v-if="row.data.img" :src="row.data.img" alt="">
          </div>
          <div class="ctxt">
            <div class="ct">{{ row.data.title || '无标题' }}</div>
            <div class="cm">
              <AppTag v-if="row.data.tag" class="ctag" tone="neutral">{{ row.data.tag }}</AppTag>
              <span v-if="row.data.tag">·</span>
              <span>{{ fmtDate(row.data.createTime) }}</span>
            </div>
          </div>
          <button class="move" id="c-move-btn" @click.stop="openMoveDialog(row.data.id)">📁 移动</button>
        </AppCard>
      </div>
    </div>
    <div v-if="activeFolder !== null && collectedInspires.length" class="virt-foot" id="c-foot">
      <template v-if="visibleCount < collectedInspires.length">
        滚到底继续加载… 已显示 {{ visibleCount }} / {{ collectedInspires.length }} 条
        <div class="bar"><i :style="{ width: Math.round(visibleCount / collectedInspires.length * 100) + '%' }"></i></div>
      </template>
      <template v-else>— 已经到底啦，共 {{ collectedInspires.length }} 条 —</template>
    </div>
    </AppState>
    <!-- 新建收藏夹：与预览一致（带字段标题 + 图标输入 + 胶囊按钮） -->
    <el-dialog v-model="showCreate" title="新建收藏夹" width="320px" id="c-dlg-create"
               class="mint-dialog" :show-close="false" append-to-body>
      <div class="fld">
        <label>名称</label>
        <input class="tin" id="c-new-name" v-model="newFolderName" placeholder="给收藏夹起个名字" maxlength="20">
      </div>
      <div class="fld">
        <label>图标</label>
        <input class="tin" id="c-new-icon" v-model="newFolderIcon" placeholder="📁" maxlength="4">
      </div>
      <div class="dlg-ops">
        <button class="dlg-ghost" id="c-create-cancel" @click="showCreate = false">取消</button>
        <button class="dlg-main" id="c-create-ok" @click="handleCreate">确定</button>
      </div>
    </el-dialog>
    <el-dialog v-model="showRename" title="重命名收藏夹" width="320px" id="c-dlg-rename"
               class="mint-dialog" :show-close="false" append-to-body>
      <div class="fld">
        <label>名称</label>
        <input class="tin" id="c-rename-name" v-model="renameName" placeholder="新名称" maxlength="20">
      </div>
      <div class="dlg-ops">
        <button class="dlg-ghost" @click="showRename = false">取消</button>
        <button class="dlg-main" @click="handleRename">确定</button>
      </div>
    </el-dialog>
      <!-- 移动到文件夹弹窗 -->
    <el-dialog v-model="showMoveDialog" title="移动到文件夹" width="320px"
               class="mint-dialog" :show-close="false" append-to-body>
      <div class="opt-list" id="c-move-list">
        <div v-for="f in namedFolders" :key="f.id" class="folder-option"
             id="c-move-option"
             :class="{selected: String(moveTargetFolder) === String(f.id)}"
             @click="moveTargetFolder = f.id">
          <span class="oi">{{ f.icon || '📁' }}</span>
          <span class="on">{{ f.name }}</span>
        </div>
      </div>
      <div style="margin-top:16px;text-align:right;">
        <el-button @click="showMoveDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmMove">移动到此处</el-button>
      </div>
    </el-dialog>

  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import InspireCard from '@/components/InspireCard.vue'
import { getCollectFolders, createCollectFolder, deleteCollectFolder, renameCollectFolder, getCollectListByFolder, collectInspire, moveCollectToFolder } from '@/api/inspire.js'
const router = useRouter()
const folders = ref([])
const activeFolder = ref(null)
const collectedInspires = ref([])
const showCreate = ref(false)
const newFolderName = ref('')
const newFolderIcon = ref('📁')
const showRename = ref(false)
const renameName = ref('')
const renamingFolder = ref(null)
const folderLoading = ref(true)
const folderError = ref('')
const collectLoading = ref(false)
const collectError = ref('')
const folderState = computed(() => {
  if (folderLoading.value) return 'loading'
  if (folderError.value) return 'error'
  return folders.value.length ? 'ready' : 'empty'
})
const collectState = computed(() => {
  if (collectLoading.value) return 'loading'
  if (collectError.value) return 'error'
  return collectedInspires.value.length ? 'ready' : 'empty'
})
const loadFolders = async () => {
  folderLoading.value = true
  folderError.value = ''
  try { const res = await getCollectFolders(); folders.value = res.data || []
    if (folders.value.length > 0 && !activeFolder.value) { activeFolder.value = folders.value[0].id; loadCollects() }
    loadFolderCounts()
  } catch (e) {
    folders.value = []
    folderError.value = e?.message || 'load folders failed'
  } finally {
    folderLoading.value = false
  }
}

/**
 * 收藏夹条数：接口不返回每个夹子的数量，这里逐个拉一次列表取 length。
 * （夹子数量很少，6 个左右，并发请求没有压力）
 */
const folderCounts = ref({})
const loadFolderCounts = async () => {
  const list = folders.value.filter(f => f && f.id != null)
  await Promise.all(list.map(async f => {
    try {
      const r = await getCollectListByFolder(f.id)
      folderCounts.value[f.id] = (r.data || []).length
    } catch (e) { folderCounts.value[f.id] = 0 }
  }))
}
const selectFolder = (id) => { activeFolder.value = id; loadCollects() }
const loadCollects = async () => {
  if (!activeFolder.value) { collectedInspires.value = []; return }
  // 换文件夹时回到第一批
  visibleCount.value = PAGE_STEP
  listScrollTop.value = 0
  collectLoading.value = true
  collectError.value = ''
  try { const res = await getCollectListByFolder(activeFolder.value); collectedInspires.value = res.data || [] }
  catch (e) {
    collectedInspires.value = []
    collectError.value = e?.message || 'load collects failed'
  } finally {
    collectLoading.value = false
  }
}

/* ===== 虚拟列表：只渲染可视区，滚到底再放出一批 ===== */
const ITEM_H = 96
const PAGE_STEP = 20
const visibleCount = ref(PAGE_STEP)
const listScrollTop = ref(0)
const listViewH = ref(360)
const listRef = ref(null)

const visibleRows = computed(() => {
  const total = Math.min(visibleCount.value, collectedInspires.value.length)
  if (!total) return []
  const start = Math.max(0, Math.floor(listScrollTop.value / ITEM_H) - 4)
  const end = Math.min(total, Math.ceil((listScrollTop.value + listViewH.value) / ITEM_H) + 4)
  const rows = []
  for (let i = start; i < end; i++) rows.push({ index: i, data: collectedInspires.value[i] })
  return rows
})

const onListScroll = (e) => {
  listScrollTop.value = e.target.scrollTop
  listViewH.value = e.target.clientHeight
  if (visibleCount.value < collectedInspires.value.length &&
      e.target.scrollTop + e.target.clientHeight >= e.target.scrollHeight - 200) {
    visibleCount.value = Math.min(visibleCount.value + PAGE_STEP, collectedInspires.value.length)
  }
}
const handleCreate = async () => {
  if (!newFolderName.value.trim()) return
  try {
    await createCollectFolder(newFolderName.value.trim(), newFolderIcon.value || '📁')
    ElMessage.success('创建成功')
    showCreate.value = false
    newFolderName.value = ''
    loadFolders()
  }
  catch (e) { ElMessage.error('创建失败') }
}
const editFolder = (f) => { renamingFolder.value = f; renameName.value = f.name; showRename.value = true }
const handleRename = async () => {
  if (!renameName.value.trim()) return
  try { await renameCollectFolder(renamingFolder.value.id, renameName.value.trim()); ElMessage.success('重命名成功'); showRename.value = false; loadFolders() }
  catch (e) { ElMessage.error('重命名失败') }
}
const removeFolder = async (f) => {
  try { await ElMessageBox.confirm('确定删除收藏夹「' + f.name + '」？'); await deleteCollectFolder(f.id); ElMessage.success('已删除')
    if (activeFolder.value === f.id) { activeFolder.value = null; collectedInspires.value = [] }; loadFolders()
  } catch (e) { console.error(e) }
}
const handleRemoveFromFolder = async (id) => {
  try { await collectInspire(id); ElMessage.success('已取消收藏'); loadCollects() }
  catch (e) { ElMessage.error('操作失败') }
}
const goDetail = (id) => { router.push({ name: 'InspireDetail', params: { id } }) }

/* 与预览一致的展示辅助 */
const folderCount = (f) => folderCounts.value[f.id] ?? f.count ?? f.inspireCount ?? 0
// 数据里本来就有一个名为「未分类」的文件夹，和页面顶部那张固定卡片重复。
// 这里把重名的过滤掉，只保留顶部那张，并且让它打开真实的「未分类」文件夹。
const uncFolder = computed(() => folders.value.find(f => f && f.name === '未分类'))
const namedFolders = computed(() => folders.value.filter(f => f && f.name !== '未分类'))
const activeFolderName = computed(() =>
  activeFolder.value === -1 ? '未分类' : (folders.value.find(f => f.id === activeFolder.value)?.name || ''))
const thumbStyle = (item) => item.img ? {} : { background: 'linear-gradient(135deg,#dff0e8,#cfe7dd)' }
const fmtDate = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return Number.isNaN(d.getTime()) ? '' : `${d.getMonth() + 1}月${d.getDate()}日`
}
onMounted(() => { loadFolders() })

const showMoveDialog = ref(false)
const moveTargetFolder = ref(null)
const movingInspireId = ref(null)

const openMoveDialog = (inspireId) => {
  movingInspireId.value = inspireId
  moveTargetFolder.value = folders.value.length > 0 ? folders.value[0].id : null
  showMoveDialog.value = true
}

const confirmMove = async () => {
  if (!movingInspireId.value || !moveTargetFolder.value) return
  try {
    await moveCollectToFolder(movingInspireId.value, moveTargetFolder.value)
    ElMessage.success('已移动')
    showMoveDialog.value = false
    loadCollects()
  } catch (e) { ElMessage.error('移动失败') }
}

</script>
<style scoped>
/* v2：整页换成站点薄荷绿色系 */
#collections-page {
  --ui-primary:#0f766e;
  --ui-primary-weak:#f2faf8;
  --ui-primary-soft:#e9f5f2;
  --ui-primary-line:#e6f2ef;
  --ui-primary-hover:#a8dcd2;
  width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px;
}
.head { display:flex; align-items:center; gap:10px; margin-bottom:18px; }
.head h2 { margin:0; font-size:17px; font-weight:700; }
.back-btn { width:34px; height:34px; border-radius:50%; border:none; background:#fff;
  color:#0f766e; font-size:17px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,.05); }
.btn-new { margin-left:auto; padding:8px 15px; border:none; border-radius:999px;
  background:#0f766e; color:#fff; font-size:12.5px; font-family:inherit; cursor:pointer; }

/* ===== 与预览完全一致的样式 ===== */
.folder-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:11px; margin-bottom:16px; }
.folder { border-radius:15px; cursor:pointer; position:relative; transition:.16s; }
.folder:hover { border-color:#d6e8e3; }
/* 选中态要明确区别于 hover，否则看起来「所有卡片都像选中」 */
.folder.on { border:2px solid #0f766e; background:#f2faf8; box-shadow:0 0 0 2px rgba(15,118,110,.10); }
.folder.unc { background:#fbfdfc; border-style:dashed; }
.folder .top { display:flex; align-items:center; gap:8px; margin-bottom:6px; }
.folder .ico { width:34px; height:34px; border-radius:10px; background:#f1f7f5; display:flex;
  align-items:center; justify-content:center; font-size:17px; flex:0 0 auto; }
.folder .fn { font-size:13px; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.folder .fc { font-size:11px; color:#9aa3b2; }
.folder .edit { position:absolute; right:9px; top:9px; border:none; background:none;
  color:#9bb5b0; font-size:13px; cursor:pointer; }
.folder .edit:hover { color:#0f766e; }

.list-head { display:flex; align-items:center; gap:8px; margin:4px 0 10px; }
.list-head .t2 { font-size:14px; font-weight:700; }
.list-head .n { font-size:11.5px; color:#9aa3b2; }

.virt { height:330px; overflow-y:auto; padding-right:2px; }
.spacer { position:relative; }
.ccard { display:flex; align-items:center; gap:11px; height:88px; margin-bottom:8px;
  box-sizing:border-box; border-radius:14px; cursor:pointer; transition:.16s; }
.ccard:hover { border-color:#a8dcd2; box-shadow:0 4px 14px rgba(30,90,80,.08); }
.cthumb { width:56px; height:56px; border-radius:11px; flex:0 0 auto; overflow:hidden; }
.cthumb img { width:100%; height:100%; object-fit:cover; display:block; }
.ctxt { min-width:0; flex:1; }
.ct { font-size:13.5px; font-weight:600; line-height:1.4; margin-bottom:5px;
  display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; }
.cm { display:flex; align-items:center; gap:6px; font-size:11.5px; color:#9aa3b2; }
.ctag { --ui-text-3:#4b7a72; }
.move { border:none; background:#f2f8f6; color:#0f766e; font-size:11.5px; padding:6px 10px;
  border-radius:999px; cursor:pointer; white-space:nowrap; font-family:inherit; }
.move:hover { background:#e6f3ef; }
.virt-foot { text-align:center; padding:11px 0; font-size:12px; color:#9aa3b2; }
.bar { height:4px; border-radius:99px; background:#eef4f2; overflow:hidden; margin-top:8px; }
.bar i { display:block; height:100%; background:#0f766e; transition:width .2s; }
.empty { text-align:center; padding:56px 0; color:#b6c2c0; font-size:13px; }

/* 移动弹窗：改成与预览一致的纵向选项列表 */
.fld { margin-bottom:12px; }
.fld label { display:block; font-size:12.5px; color:#5b6b68; margin-bottom:6px; }
/* 必须加 box-sizing，否则 width:100% 再加上左右各 12px 内边距会横向撑出弹窗 */
.tin { width:100%; height:38px; box-sizing:border-box; padding:0 12px;
  border:1px solid #e3ecea; border-radius:10px;
  font-family:inherit; font-size:13px; outline:none; }
.tin:focus { border-color:#0f766e; }
.dlg-ops { display:flex; gap:8px; margin-top:12px; }
.dlg-ops button { flex:1; height:40px; border-radius:11px; border:none;
  font-family:inherit; font-size:13.5px; cursor:pointer; }
.dlg-ghost { background:#f2f6f5; color:#5b6b68; }
.dlg-main { background:#0f766e; color:#fff; }

.opt-list { display:flex; flex-direction:column; gap:8px; margin-bottom:4px; }
.folder-option { display:flex; align-items:center; gap:9px; padding:10px 11px;
  border:1px solid #e6f2ef; border-radius:11px; cursor:pointer; font-size:13px;
  transition:.15s; background:#fff; }
.folder-option:hover { border-color:#d6e8e3; }
.folder-option.selected { border:2px solid #0f766e; background:#f2faf8; }
.folder-option .oi { font-size:17px; }
.folder-option .on { color:#1d1d1f; }
</style>

<!--
  弹窗外壳样式：el-dialog 默认 append-to-body（teleport 到 body），
  scoped 样式选不到它，所以这一段不加 scoped，用 .mint-dialog 限定作用范围。
  目的是让弹窗和预览一致：16px 圆角、无右上角关闭按钮、标题 15px、内边距 16px。
-->
<style>
.mint-dialog {
  border-radius: 16px !important;
  padding: 16px !important;
  overflow: hidden;
}
.mint-dialog .el-dialog__header {
  margin: 0 0 12px;
  padding: 0;
  border: none;
}
.mint-dialog .el-dialog__title {
  font-size: 15px;
  font-weight: 700;
  color: #1d1d1f;
  line-height: 1.4;
}
.mint-dialog .el-dialog__body {
  padding: 0;
  overflow: visible;
}
.mint-dialog .el-dialog__headerbtn {
  display: none;
}
</style>
