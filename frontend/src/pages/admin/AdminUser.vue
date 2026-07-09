<template>
  <div>
    <h2 style="margin:0 0 20px">用户查询</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱/昵称" clearable style="width:280px" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" style="width:100%">
      <el-table-column label="头像" width="60">
        <template #default="{row}"><span style="font-size:20px">{{ row.avatar || (row.nickname ? row.nickname[0] : '\U0001f464') }}</span></template>
      </el-table-column>
      <el-table-column prop="id" label="用户ID" width="160" class-name="hide-on-mobile" />
      <el-table-column prop="username" label="用户名" width="120" class-name="hide-on-mobile" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="createTime" label="注册时间" width="160" />
      <el-table-column label="操作" width="80">
        <template #default="{row}"><el-button text size="small" @click="showDetail(row)">详情</el-button></template>
      </el-table-column>
    </el-table>
    <div class="pagination"><el-pagination background layout="prev,pager,next" :total="total" :page-size="size" @current-change="page=$event;load()" /></div>

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="90%" max-width="400px">
      <div v-if="detailUser" class="detail-wrap">
        <div class="detail-avatar">{{ detailUser.avatar || (detailUser.nickname ? detailUser.nickname[0] : '\U0001f464') }}</div>
        <div class="detail-row"><span class="dl">用户ID</span><span>{{ detailUser.id }}</span></div>
        <div class="detail-row"><span class="dl">用户名</span><span>{{ detailUser.username }}</span></div>
        <div class="detail-row"><span class="dl">昵称</span><span>{{ detailUser.nickname || '-' }}</span></div>
        <div class="detail-row"><span class="dl">邮箱</span><span>{{ detailUser.email || '-' }}</span></div>
        <div class="detail-row"><span class="dl">城市</span><span>{{ detailUser.city || '-' }}</span></div>
        <div class="detail-row"><span class="dl">头像</span><span style="font-size:24px">{{ detailUser.avatar || '-' }}</span></div>
        <div class="detail-row"><span class="dl">注册时间</span><span>{{ detailUser.createTime || '-' }}</span></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminSearchUser, adminUserDetail } from '@/api/inspire.js'
const list = ref([]); const total = ref(0); const loading = ref(false)
const keyword = ref(''); const page = ref(1); const size = ref(20)
const load = async () => {
  loading.value = true
  try {
    const res = await adminSearchUser({ keyword: keyword.value, page: page.value, size: size.value })
    list.value = res.data?.list || []; total.value = res.data?.total || 0
  } catch (e) { list.value = [] } finally { loading.value = false }
}
onMounted(load)

const detailVisible = ref(false)
const detailUser = ref(null)
const showDetail = async (row) => {
  detailVisible.value = true
  detailUser.value = row
  try {
    const res = await adminUserDetail(row.id)
    if (res.code === 200 && res.data) detailUser.value = res.data
  } catch (e) { console.error(e) }
}
</script>

<style scoped>
.toolbar { display:flex; gap:12px; margin-bottom:16px; }
.pagination { margin-top:16px; display:flex; justify-content:center; }
.detail-wrap { padding:8px 0; }
.detail-avatar { width:64px; height:64px; border-radius:50%; background:linear-gradient(135deg,#667eea,#764ba2); color:#fff; display:flex; align-items:center; justify-content:center; font-size:24px; margin:0 auto 20px; }
.detail-row { display:flex; align-items:center; padding:8px 0; border-bottom:1px solid #f5f5f5; font-size:14px; }
.detail-row .dl { width:80px; color:#909399; flex-shrink:0; }
@media (max-width:768px) { :deep(.hide-on-mobile) { display:none !important; } }
</style>
