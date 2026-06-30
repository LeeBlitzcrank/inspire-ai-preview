<template>
  <div>
    <h2 style="margin:0 0 20px">灵感管理</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索标题" clearable style="width:200px" @keyup.enter="load" />
      <el-select v-model="tag" placeholder="全部分类" clearable style="width:140px" @change="load">
        <el-option v-for="t in tags" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable style="width:140px" @change="load">
        <el-option label="已发布" :value="1" /><el-option label="草稿" :value="0" />
      </el-select>
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="title" label="标题" min-width="180" fixed="left" show-overflow-tooltip />
      <el-table-column prop="tag" label="分类" width="80" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{row}">{{ row.status===1?'已发布':'草稿' }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" class-name="hide-m" label="浏览" width="70" />
      <el-table-column prop="likeCount" class-name="hide-m" label="点赞" width="70" />
      <el-table-column prop="collectCount" class-name="hide-m" label="收藏" width="70" />
      <el-table-column prop="heat" class-name="hide-m" label="热度" width="70" />
      <el-table-column prop="deleted" label="下架" width="70">
        <template #default="{row}">{{ row.deleted===2?'已下架':'正常' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button v-if="row.deleted!==2" text type="danger" size="small" @click="doBlock(row.id)">下架</el-button>
          <el-button v-else text type="primary" size="small" @click="doUnblock(row.id)">上架</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination"><el-pagination background layout="prev,pager,next" :total="total" :page-size="size" @current-change="page=$event;load()" /></div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminInspireList, adminBlockInspire, adminUnblockInspire } from '@/api/inspire'
const list = ref([]); const total = ref(0); const loading = ref(false)
const keyword = ref(''); const tag = ref(''); const status = ref('')
const page = ref(1); const size = ref(20)
const tags = ['美食','运动','电影','穿搭','文案','旅游','摄影','其他']
const load = async () => {
  loading.value = true
  try {
    const res = await adminInspireList({ keyword: keyword.value, tag: tag.value, status: status.value || undefined, page: page.value, size: size.value })
    list.value = res.data?.list || []; total.value = res.data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}
const doBlock = async (id) => { try { await adminBlockInspire(id); ElMessage.success('已下架'); load() } catch (e) {} }
const doUnblock = async (id) => { try { await adminUnblockInspire(id); ElMessage.success('已上架'); load() } catch (e) {} }
onMounted(load)
</script>
<style scoped>
.toolbar { display:flex; gap:12px; margin-bottom:16px; align-items:center; }
.pagination { margin-top:16px; display:flex; justify-content:center; }

@media (max-width:768px) {
  .el-table .hide-m { display:none; }
}
</style>
