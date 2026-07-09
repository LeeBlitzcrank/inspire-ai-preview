<template>
  <div>
    <h2 style="margin:0 0 20px">灵感管理</h2>
    
    <!-- 选项卡：全部 / 待审核 -->
    <div class="sub-tabs" style="display:flex; gap:12px; margin-bottom:16px;">
      <el-radio-group v-model="activeTab" @change="switchTab">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="pending">待审核<el-tag v-if="pendingCount>0" size="small" type="danger" style="margin-left:4px">{{ pendingCount }}</el-tag></el-radio-button>
      </el-radio-group>
    </div>

    <!-- 全部列表 -->
    <div v-if="activeTab==='all'">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索标题" clearable style="width:200px" @keyup.enter="loadAll" />
        <el-select v-model="tag" placeholder="全部分类" clearable style="width:140px" @change="loadAll">
          <el-option v-for="t in tags" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable style="width:140px" @change="loadAll">
          <el-option label="已发布" :value="1" /><el-option label="草稿" :value="0" /><el-option label="待审核" :value="2" /><el-option label="已拒绝" :value="3" />
        </el-select>
        <el-button type="primary" @click="loadAll">搜索</el-button>
      </div>
      <el-table :data="allList" border stripe v-loading="allLoading" style="width:100%">
        <el-table-column prop="title" label="标题" min-width="180" fixed="left" show-overflow-tooltip />
        <el-table-column prop="tag" label="分类" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">{{ {0:'草稿',1:'已发布',2:'待审核',3:'已拒绝'}[row.status] || row.status }}</template>
        </el-table-column>
        <el-table-column prop="viewCount" class-name="hide-m" label="浏览" width="70" />
        <el-table-column prop="likeCount" class-name="hide-m" label="点赞" width="70" />
        <el-table-column prop="collectCount" class-name="hide-m" label="收藏" width="70" />
        <el-table-column prop="heat" class-name="hide-m" label="热度" width="70" />
        <el-table-column prop="deleted" label="下架" width="70">
          <template #default="{row}">{{ row.deleted===2?'已下架':'正常' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button v-if="row.deleted!==2" text type="danger" size="small" @click="doBlock(row.id)">下架</el-button>
            <el-button v-else text type="primary" size="small" @click="doUnblock(row.id)">上架</el-button>
            <el-button v-if="row.status===2" text type="success" size="small" @click="doApprove(row.id)">通过</el-button>
            <el-button v-if="row.status===2" text type="warning" size="small" @click="doReject(row.id)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="prev,pager,next" :total="allTotal" :page-size="size" @current-change="page=$event;loadAll()" /></div>
    </div>

    <!-- 待审核列表 -->
    <div v-if="activeTab==='pending'">
      <el-table :data="pendingList" border stripe v-loading="pendingLoading" style="width:100%">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="tag" label="分类" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">{{ {0:'草稿',1:'已发布',2:'待审核',3:'已拒绝'}[row.status] || row.status }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{row}">
            <el-button text type="success" size="small" @click="doApprove(row.id)">通过</el-button>
            <el-button text type="warning" size="small" @click="doReject(row.id)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination"><el-pagination background layout="prev,pager,next" :total="pendingTotal" :page-size="size" @current-change="pPage=$event;loadPending()" /></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminInspireList, adminBlockInspire, adminUnblockInspire,
         adminPendingList, adminApproveInspire, adminRejectInspire } from '@/api/inspire.js'

const activeTab = ref('all')
const tags = ['美食','运动','电影','穿搭','文案','旅游','摄影','其他']
const size = ref(20)

// 全部列表
const allList = ref([]); const allTotal = ref(0); const allLoading = ref(false)
const keyword = ref(''); const tag = ref(''); const status = ref(''); const page = ref(1)
const loadAll = async () => {
  allLoading.value = true
  try {
    const res = await adminInspireList({ keyword: keyword.value, tag: tag.value, status: status.value || undefined, page: page.value, size: size.value })
    allList.value = res.data?.list || []; allTotal.value = res.data?.total || 0
  } catch (e) { allList.value = [] } finally { allLoading.value = false }
}

// 待审核列表
const pendingList = ref([]); const pendingTotal = ref(0); const pendingLoading = ref(false)
const pendingCount = ref(0); const pPage = ref(1)
const loadPending = async () => {
  pendingLoading.value = true
  try {
    const res = await adminPendingList({ page: pPage.value, size: size.value })
    pendingList.value = res.data?.list || []; pendingTotal.value = res.data?.total || 0
    pendingCount.value = pendingTotal.value
  } catch (e) { pendingList.value = [] } finally { pendingLoading.value = false }
}

// 操作
const doBlock = async (id) => { try { await adminBlockInspire(id); ElMessage.success('已下架'); loadAll() } catch (e) { console.error(e) } }
const doUnblock = async (id) => { try { await adminUnblockInspire(id); ElMessage.success('已上架'); loadAll() } catch (e) { console.error(e) } }
const doApprove = async (id) => { try { await adminApproveInspire(id); ElMessage.success('审核通过'); loadAll(); loadPending() } catch (e) { console.error(e) } }
const doReject = async (id) => { try { await adminRejectInspire(id); ElMessage.success('已拒绝'); loadAll(); loadPending() } catch (e) { console.error(e) } }

const switchTab = (tab) => { if (tab === 'pending') loadPending() }

onMounted(loadAll)
</script>

<style scoped>
.toolbar { display:flex; gap:12px; margin-bottom:16px; align-items:center; flex-wrap:wrap; }
.pagination { margin-top:16px; display:flex; justify-content:center; }
@media (max-width:768px) { .el-table .hide-m { display:none; } }
</style>