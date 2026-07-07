<template>
  <div id="collections-page" style="width:94%;max-width:620px;margin:0 auto;padding:16px 0 80px;">
    <div style="display:flex;align-items:center;gap:10px;margin-bottom:20px;">
      <button class="back-btn" @click="$router.push('/personal')" style="font-size:20px;border:none;background:none;cursor:pointer;">&larr;</button>
      <h2 style="margin:0;font-size:20px;">我的收藏夹</h2>
      <button class="btn-primary" @click="showCreate = true" style="margin-left:auto;padding:8px 16px;border:none;border-radius:10px;background:#409eff;color:#fff;font-size:13px;cursor:pointer;">+ 新建</button>
    </div>
    <div class="folder-grid" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(140px,1fr));gap:12px;margin-bottom:24px;">
      <div v-for="f in folders" :key="f.id" class="folder-card"
           :class="{active: activeFolder === f.id}"
           @click="selectFolder(f.id)"
           style="background:#fff;border-radius:14px;padding:16px;text-align:center;cursor:pointer;border:2px solid transparent;transition:0.2s;">
        <div style="font-size:32px;margin-bottom:6px;">{{ f.icon || "📁" }}</div>
        <div style="font-size:14px;font-weight:500;color:#1d1d1f;">{{ f.name }}</div>
        <div style="font-size:11px;color:#999;margin-top:4px;">
          <button v-if="activeFolder === f.id" @click.stop="editFolder(f)" style="border:none;background:none;color:#409eff;cursor:pointer;font-size:11px;">重命名</button>
          <button v-if="activeFolder === f.id" @click.stop="removeFolder(f)" style="border:none;background:none;color:#ff4d4f;cursor:pointer;font-size:11px;margin-left:8px;">删除</button>
        </div>
      </div>
      <div class="folder-card uncategorized"
           :class="{active: activeFolder === -1}"
           @click="selectFolder(-1)"
           style="background:#f9fafb;border-radius:14px;padding:16px;text-align:center;cursor:pointer;border:2px dashed #d0d5dd;transition:0.2s;">
        <div style="font-size:32px;margin-bottom:6px;opacity:0.5;">📂</div>
        <div style="font-size:14px;font-weight:500;color:#666;">未分类</div>
      </div>
    </div>
    <div v-if="collectedInspires.length > 0" class="feed-list" style="display:flex;flex-direction:column;gap:16px;">
      <div v-for="item in collectedInspires" :key="item.id" class="collect-card-wrap">
        <InspireCard :item="item" @click-card="goDetail" @collect="handleRemoveFromFolder" />
        <div v-if="folders.length > 0" style="text-align:right;margin-top:-8px;margin-bottom:4px;">
          <button @click="openMoveDialog(item.id)" style="border:none;background:none;color:#409eff;font-size:12px;cursor:pointer;">📁 移动到文件夹</button>
        </div>
      </div>
</div>
    <div v-else-if="folders.length > 0" class="empty-sub" style="text-align:center;padding:40px 0;color:#999;">该文件夹还没有收藏灵感</div>
    <div v-else class="empty-sub" style="text-align:center;padding:40px 0;color:#999;">还没有收藏夹，点击上方新建</div>
    <el-dialog v-model="showCreate" title="新建收藏夹" width="320px">
      <el-input v-model="newFolderName" placeholder="文件夹名称" maxlength="20" />
      <div style="margin-top:12px;text-align:right;">
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </div>
    </el-dialog>
    <el-dialog v-model="showRename" title="重命名" width="320px">
      <el-input v-model="renameName" placeholder="新名称" maxlength="20" />
      <div style="margin-top:12px;text-align:right;">
        <el-button @click="showRename = false">取消</el-button>
        <el-button type="primary" @click="handleRename">确定</el-button>
      </div>
    </el-dialog>
      <!-- 移动到文件夹弹窗 -->
    <el-dialog v-model="showMoveDialog" title="移动到文件夹" width="320px">
      <div style="display:flex;flex-wrap:wrap;gap:10px;margin-bottom:16px;">
        <div v-for="f in folders" :key="f.id" class="folder-option"
             :class="{selected: moveTargetFolder === f.id}"
             @click="moveTargetFolder = f.id"
             style="flex:1;min-width:100px;padding:12px;border-radius:12px;border:2px solid #e4e7ed;text-align:center;cursor:pointer;">
          <div style="font-size:24px;">{{ f.icon || '📁' }}</div>
          <div style="font-size:13px;margin-top:4px;color:#1d1d1f;">{{ f.name }}</div>
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
import { ref, onMounted } from 'vue'
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
const showRename = ref(false)
const renameName = ref('')
const renamingFolder = ref(null)
const loadFolders = async () => {
  try { const res = await getCollectFolders(); folders.value = res.data || []
    if (folders.value.length > 0 && !activeFolder.value) { activeFolder.value = folders.value[0].id; loadCollects() }
  } catch (e) { folders.value = [] }
}
const selectFolder = (id) => { activeFolder.value = id; loadCollects() }
const loadCollects = async () => {
  if (!activeFolder.value) { collectedInspires.value = []; return }
  try { const res = await getCollectListByFolder(activeFolder.value); collectedInspires.value = res.data || [] }
  catch (e) { collectedInspires.value = [] }
}
const handleCreate = async () => {
  if (!newFolderName.value.trim()) return
  try { await createCollectFolder(newFolderName.value.trim()); ElMessage.success('创建成功'); showCreate.value = false; newFolderName.value = ''; loadFolders() }
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
  } catch (e) {}
}
const handleRemoveFromFolder = async (id) => {
  try { await collectInspire(id); ElMessage.success('已取消收藏'); loadCollects() }
  catch (e) { ElMessage.error('操作失败') }
}
const goDetail = (id) => { router.push({ name: 'InspireDetail', params: { id } }) }
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
.folder-card.active { border-color:#409eff; background:#f0f8ff; }
.folder-card:hover { border-color:#409eff44; }
.folder-option.selected { border-color: #409eff; background: #f0f8ff; }
.folder-option:hover { border-color: #409eff44; }
.uncategorized.active { border-color: #409eff; background: #f0f8ff; border-style: solid; }
</style>