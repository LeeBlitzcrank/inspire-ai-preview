<template>
  <div class="wc-page">
    <div class="head">
      <h3>AI 探索推荐词</h3>
      <el-button type="primary" @click="openCreate">新增词条</el-button>
    </div>
    <div class="tip">
      这里维护的是「推荐词 / 探索方向」，显示在前台「创建灵感 · AI 探索」的流动词云里，用户点一个词就围绕它往下探索，
      <b>与「所属分类」互相独立</b>（分类请去「分类管理」维护）。权重越大字号越大。
    </div>

    <el-table :data="list" v-loading="loading" style="width:100%">
      <el-table-column prop="word" label="词条" min-width="120" />
      <el-table-column prop="weight" label="权重" width="80" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <span class="tag" :class="{ off: row.status !== 1 }">{{ row.status === 1 ? '启用' : '停用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeItem(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑词条' : '新增词条'" width="340px">
      <el-input v-model="form.word" placeholder="词条内容，例如：旅行" />
      <el-input v-model.number="form.weight" placeholder="权重（1-5，越大字号越大）" style="margin-top:10px" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminWordCloudList, adminCreateWord, adminUpdateWord, adminDeleteWord } from '@/api/inspire.js'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const form = ref({ id: null, word: '', weight: 1, sortOrder: 0, status: 1 })

const load = async () => {
  loading.value = true
  try {
    const res = await adminWordCloudList()
    list.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  form.value = { id: null, word: '', weight: 1, sortOrder: list.value.length + 1, status: 1 }
  dialogVisible.value = true
}

const openEdit = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const save = async () => {
  if (!form.value.word || !form.value.word.trim()) return ElMessage.warning('请填写词条内容')
  saving.value = true
  try {
    const payload = { ...form.value, word: form.value.word.trim() }
    const res = form.value.id ? await adminUpdateWord(payload) : await adminCreateWord(payload)
    if (res.code === 200) {
      ElMessage.success(form.value.id ? '已更新' : '已新增')
      dialogVisible.value = false
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

const removeItem = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除词条「${row.word}」？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    const res = await adminDeleteWord(row.id)
    if (res.code === 200) {
      ElMessage.success('已删除')
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
.wc-page { padding: 4px 2px; }
.head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.head h3 { margin:0; }
.tip { font-size:12.5px; color:#8a94a6; margin-bottom:14px; }
.tag { font-size:11px; padding:2px 8px; border-radius:999px; background:#e8f5e9; color:#2e7d32; }
.tag.off { background:#f1f3f5; color:#8a94a6; }
.status-line { display:flex; align-items:center; justify-content:space-between; margin-top:14px; font-size:13px; color:#374151; }
</style>
