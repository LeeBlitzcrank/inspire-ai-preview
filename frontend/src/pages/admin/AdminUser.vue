<template>
  <div>
    <h2 style="margin:0 0 20px">用户查询</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱/昵称" clearable style="width:280px" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="用户ID" width="160" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="email" label="邮箱" width="200" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="createTime" label="注册时间" width="160" />
    </el-table>
    <div class="pagination"><el-pagination background layout="prev,pager,next" :total="total" :page-size="size" @current-change="page=$event;load()" /></div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { adminSearchUser } from '@/api/inspire'
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
</script>
<style scoped>
.toolbar { display:flex; gap:12px; margin-bottom:16px; }
.pagination { margin-top:16px; display:flex; justify-content:center; }
</style>
