<template>
  <div>
    <h2 style="margin:0 0 20px">数据看板</h2>

    <!-- 核心指标 -->
    <div class="stat-row">
      <div class="stat-card primary"><div class="num">{{ d.dau || '-' }}</div><div class="lbl">日活用户 <span class="sub">今日操作</span></div></div>
      <div class="stat-card"><div class="num">{{ d.inspireTotal || 0 }}</div><div class="lbl">灵感总数</div></div>
      <div class="stat-card"><div class="num">{{ d.userTotal || 0 }}</div><div class="lbl">用户总数</div></div>
      <div class="stat-card"><div class="num">{{ d.aiCallCount || 0 }}</div><div class="lbl">AI调用</div></div>
    </div>

    <!-- 今日数据 -->
    <h3 style="margin:20px 0 12px;font-size:15px;color:#606266;">📅 今日数据</h3>
    <div class="stat-row">
      <div class="stat-card sm"><div class="num sm">{{ d.todayPublish || 0 }}</div><div class="lbl">发布灵感</div></div>
      <div class="stat-card sm"><div class="num sm">{{ d.todayRegister || 0 }}</div><div class="lbl">新注册</div></div>
      <div class="stat-card sm"><div class="num sm">{{ d.todayLikes || 0 }}</div><div class="lbl">点赞</div></div>
      <div class="stat-card sm"><div class="num sm">{{ d.todayCollects || 0 }}</div><div class="lbl">收藏</div></div>
    </div>

    <!-- 累计数据 -->
    <h3 style="margin:20px 0 12px;font-size:15px;color:#606266;">📊 累计数据</h3>
    <div class="stat-row">
      <div class="stat-card sm"><div class="num sm">{{ d.totalLikes || 0 }}</div><div class="lbl">累计点赞</div></div>
      <div class="stat-card sm"><div class="num sm">{{ d.totalCollects || 0 }}</div><div class="lbl">累计收藏</div></div>
    </div>

    <!-- 分类 & 城市 -->
    <div class="row">
      <div class="panel"><h3>📍 分类热度</h3><div v-if="d.tagHeat && Object.keys(d.tagHeat).length">
        <div v-for="(v,k) in d.tagHeat" :key="k" class="tag-row"><span>{{ k }}</span><span class="tag-val">{{ v }}条</span></div>
      </div><div v-else class="empty-panel">暂无数据</div></div>
      <div class="panel"><h3>🏠 城市分布</h3><div v-if="d.cityStats && Object.keys(d.cityStats).length">
        <div v-for="(v,k) in d.cityStats" :key="k" class="tag-row"><span>{{ k }}</span><span class="tag-val">{{ v }}条</span></div>
      </div><div v-else class="empty-panel">暂无数据</div></div>
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
.stat-row { display:flex; gap:12px; margin-bottom:12px; flex-wrap:wrap; }
.stat-card { flex:1; min-width:140px; background:#fff; border-radius:14px; padding:20px; text-align:center; box-shadow:0 1px 4px rgba(0,0,0,0.04); }
.stat-card.primary { background:linear-gradient(135deg,#667eea,#764ba2); }
.stat-card.primary .num { color:#fff; }
.stat-card.primary .lbl { color:rgba(255,255,255,.85); }
.stat-card.primary .sub { font-size:11px; opacity:.7; }
.stat-card.sm { min-width:100px; padding:16px; }
.num { font-size:30px; font-weight:600; color:#409eff; }
.num.sm { font-size:24px; }
.lbl { font-size:13px; color:#86868b; margin-top:4px; }
.row { display:flex; gap:16px; margin-top:16px; }
.panel { flex:1; background:#fff; border-radius:14px; padding:20px; box-shadow:0 1px 4px rgba(0,0,0,0.04); }
.panel h3 { margin:0 0 16px; font-size:15px; color:#1d1d1f; }
.tag-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #f5f5f5; font-size:14px; }
.tag-row:last-child { border:none; }
.tag-val { color:#409eff; font-weight:500; }
.empty-panel { text-align:center; color:#c0c4cc; font-size:13px; padding:20px 0; }
</style>
