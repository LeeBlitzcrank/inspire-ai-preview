<template>
  <div>
    <h2 style="margin:0 0 20px">监控大屏</h2>
    <div class="stats">
      <div class="stat-card"><div class="num">{{ d.inspireTotal || 0 }}</div><div class="lbl">灵感总数</div></div>
      <div class="stat-card"><div class="num">{{ d.userTotal || 0 }}</div><div class="lbl">用户总数</div></div>
      <div class="stat-card"><div class="num">{{ d.aiCallCount || 0 }}</div><div class="lbl">AI调用次数</div></div>
    </div>
    <div class="row">
      <div class="panel"><h3>分类热度</h3><div v-for="(v,k) in d.tagHeat" :key="k" class="tag-row"><span>{{ k }}</span><span>{{ v }}条</span></div></div>
      <div class="panel"><h3>城市分布</h3><div v-for="(v,k) in d.cityStats" :key="k" class="tag-row"><span>{{ k }}</span><span>{{ v }}条</span></div></div>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { getAdminDashboard } from '@/api/inspire'
const d = ref({})
onMounted(async () => {
  try { const res = await getAdminDashboard(); d.value = res.data || {} } catch (e) {}
})
</script>
<style scoped>
.stats { display:flex; gap:16px; margin-bottom:24px; }
.stat-card { flex:1; background:#fff; border-radius:14px; padding:24px; text-align:center; }
.num { font-size:32px; font-weight:600; color:#409eff; }
.lbl { font-size:14px; color:#86868b; margin-top:6px; }
.row { display:flex; gap:16px; }
.panel { flex:1; background:#fff; border-radius:14px; padding:20px; }
.panel h3 { margin:0 0 16px; font-size:16px; }
.tag-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #f5f5f5; font-size:14px; }
.tag-row:last-child { border:none; }
</style>
