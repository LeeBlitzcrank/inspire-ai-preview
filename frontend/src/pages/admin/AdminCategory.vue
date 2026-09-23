<template>
  <div class="category-page">
    <div class="head">
      <h3>灵感分类</h3>
      <el-button type="primary" @click="openCreate(null)">新增一级分类</el-button>
    </div>
    <div class="tip">
      这里的分类同时用于两处：前台首页的「灵感分类」，以及创建灵感时的「所属分类」。
      <b>与 AI 探索的推荐词（词云）互相独立</b>，推荐词请去「词云管理」维护。
    </div>

    <div v-loading="loading">
      <div v-for="p in tree" :key="p.id" class="parent-card">
        <div class="row parent-row">
          <span class="icon">{{ p.icon || '📁' }}</span>
          <span class="name">{{ p.name }}</span>
          <span class="tag" :class="{ off: p.status !== 1 }">{{ p.status === 1 ? '启用' : '停用' }}</span>
          <span class="sort">排序 {{ p.sortOrder }}</span>
          <div class="ops">
            <el-button size="small" @click="openCreate(p)">加子类</el-button>
            <el-button size="small" @click="openEdit(p, 0)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeItem(p, 0)">删除</el-button>
          </div>
        </div>

        <div v-if="p.children && p.children.length" class="children">
          <div v-for="c in p.children" :key="c.id" class="row child-row">
            <span class="dot">·</span>
            <span class="name">{{ c.name }}</span>
            <span class="tag" :class="{ off: c.status !== 1 }">{{ c.status === 1 ? '启用' : '停用' }}</span>
            <span class="sort">排序 {{ c.sortOrder }}</span>
            <div class="ops">
              <el-button size="small" @click="openEdit(c, p.id)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeItem(c, 1)">删除</el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-child">暂无子分类</div>
      </div>
      <div v-if="!loading && !tree.length" class="empty">还没有分类，先新增一个吧</div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="360px" append-to-body>
      <div v-if="parentName" class="parent-tip">所属一级分类：{{ parentName }}</div>
      <el-input v-model="form.name" placeholder="分类名称" />
      <el-input v-if="!form.parentId" v-model="form.icon" placeholder="图标（emoji，可留空）" style="margin-top:10px" />
      <el-input v-model.number="form.sortOrder" placeholder="排序（数字越小越靠前）" style="margin-top:10px" />
      <div class="status-line">
        <span>状态</span>
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {ElMessageBox} from 'element-plus'
import {ElMessage} from '@/utils/uiFeedback.js'
import {adminCategoryList, adminCreateCategory, adminDeleteCategory, adminUpdateCategory} from '@/api/inspire.js'
import {clearGetCache} from '@/utils/request.js'

const tree = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const parentName = ref('')
const form = ref({ id: null, parentId: 0, name: '', icon: '', sortOrder: 0, status: 1 })

const load = async () => {
  loading.value = true
  try {
    const res = await adminCategoryList()
    tree.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openCreate = (parent) => {
  parentName.value = parent ? parent.name : ''
  form.value = {
    id: null,
    parentId: parent ? parent.id : 0,
    name: '',
    icon: '',
    sortOrder: 0,
    status: 1
  }
  dialogVisible.value = true
}

const openEdit = (item, parentId) => {
  parentName.value = parentId ? '' : ''
  form.value = {
    id: item.id,
    parentId: item.parentId || parentId,
    name: item.name,
    icon: item.icon || '',
    sortOrder: item.sortOrder || 0,
    status: item.status == null ? 1 : item.status
  }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.name || !form.value.name.trim()) return ElMessage.warning('请填写分类名称')
  saving.value = true
  try {
    const payload = { ...form.value, name: form.value.name.trim() }
    const res = form.value.id ? await adminUpdateCategory(payload) : await adminCreateCategory(payload)
    if (res.code === 200) {
      ElMessage.success(form.value.id ? '已更新' : '已新增')
      dialogVisible.value = false
      clearGetCache()
      await load()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const removeItem = async (item, level) => {
  const tip = level === 0
    ? `删除一级分类「${item.name}」会同时删除它的子分类，确定继续？`
    : `确定删除子分类「${item.name}」？`
  try {
    await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const res = await adminDeleteCategory(item.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
      clearGetCache()
      await load()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.category-page { padding: 4px 2px; }
.head { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; }
.tip { font-size:12.5px; color:#8a94a6; line-height:1.7; margin:-8px 0 16px; }
.head h3 { margin:0; }
.parent-card { background:#fff; border-radius:14px; padding:16px 18px; margin-bottom:14px; }
.row { display:flex; align-items:center; gap:12px; padding:8px 0; }
.parent-row { border-bottom:1px dashed #eef1f6; }
.child-row { padding-left:14px; }
.icon { font-size:20px; }
.dot { color:#c8cfdb; }
.name { font-size:14px; font-weight:600; color:#1d1d1f; }
.sort { font-size:12px; color:#8a94a6; }
.tag { font-size:11px; padding:2px 8px; border-radius:999px; background:#e8f5e9; color:#2e7d32; }
.tag.off { background:#f1f3f5; color:#8a94a6; }
.ops { margin-left:auto; display:flex; gap:6px; }
.children { padding-top:4px; }
.empty-child, .empty { color:#98a2b3; font-size:13px; padding:10px 0 4px 14px; }
.parent-tip { margin-bottom:10px; font-size:13px; color:#6b7280; }
.status-line { display:flex; align-items:center; justify-content:space-between; margin-top:14px; font-size:13px; color:#374151; }
</style>
