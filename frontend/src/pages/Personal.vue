<template>
  <div class="personal-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/personal')">👤</div>
      </div>
    </div>
    <div class="user-header">
      <div class="avatar">👤</div>
      <div class="user-info">
        <h2>{{ userInfo.username || '灵感爱好者' }}</h2>
        <p v-if="userInfo.email">{{ userInfo.email }}</p>
      </div>
    </div>
    <div class="stat-wrap">
      <div class="stat-card" v-for="item in statList" :key="item.id">
        <div class="stat-num">{{ item.num }}</div>
        <div class="stat-line"></div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 选项卡 -->
    <div class="tab-bar">
      <div class="tab-item" :class="{active: activeTab==='published'}" @click="activeTab='published'">我的发布</div>
      <div class="tab-item" :class="{active: activeTab==='collects'}" @click="switchToCollects">我的收藏</div>
    </div>

    <div v-if="loading" class="empty-sub"><div v-for="n in 3" :key="n" class="inspire-card skeleton-card" style="padding:20px"><div class="s-line s-w-60"></div><div class="s-line s-w-90"></div><div class="s-line s-w-40"></div></div></div>
    <div class="list-wrap" v-else-if="activeTab==='published'">
      <InspireCard v-for="item in publishedList" :key="item.id" :item="item" />
      <div v-if="publishedList.length === 0" class="empty-sub">✍️ 还没有发布过灵感</div>
    </div>
    <div class="list-wrap" v-else>
      <InspireCard v-for="item in collectList" :key="item.id" :item="item" @collect="handleUncollect" />
      <div v-if="collectList.length === 0" class="empty-sub">⭐ 还没有收藏过灵感</div>
    </div>

    <div class="logout-wrap">
      <el-button type="text" class="logout-btn" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, getMyInspires, getMyCollects, uncollectInspire } from '@/api/inspire'
const router = useRouter()
const userInfo = ref({})
const publishedList = ref([])
const collectList = ref([])
const loading = ref(false)
const activeTab = ref('published')
const statList = ref([{ id:1, num:0, label:'总发布' }, { id:2, num:0, label:'总收藏' }, { id:3, num:0, label:'总浏览量' }])

onMounted(async () => {
  loading.value = true
  try {
    const [userRes, pubRes, colRes] = await Promise.all([
      getUserInfo(), getMyInspires(), getMyCollects()
    ])
    userInfo.value = userRes.data || {}
    publishedList.value = pubRes.data || []
    collectList.value = colRes.data || []
    statList.value[0].num = publishedList.value.length
    statList.value[1].num = collectList.value.length
    statList.value[2].num = publishedList.value.reduce((s, i) => s + (i.viewCount || 0), 0)
  } catch (e) {} finally { loading.value = false }
})

const switchToCollects = async () => {
  activeTab.value = 'collects'
  try {
    const res = await getMyCollects()
    collectList.value = res.data || []
    statList.value[1].num = collectList.value.length
  } catch (e) {}
}
const handleUncollect = async (id) => {
  try {
    await uncollectInspire(id)
    collectList.value = collectList.value.filter(i => i.id !== id)
    statList.value[1].num = collectList.value.length
    ElMessage.success('已取消收藏')
  } catch (e) {}
}

const handleLogout = () => {
  localStorage.removeItem('token'); localStorage.removeItem('isLogin')
  localStorage.removeItem('userAccount'); localStorage.removeItem('userId')
  ElMessage.success('已退出登录'); router.push('/login')
}
</script>
<style scoped>
.personal-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.user-header { display:flex; align-items:center; gap:16px; margin-bottom:30px; }
.avatar { width:64px; height:64px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:28px; }
.user-info h2 { margin:0; font-size:20px; color:#1d1d1f; }
.user-info p { margin:4px 0 0; font-size:14px; color:#86868b; }
.stat-wrap { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-bottom:24px; }
.stat-card { background:#fff; border-radius:14px; padding:20px 10px; text-align:center; }
.stat-num { font-size:24px; font-weight:500; color:#1d1d1f; }
.stat-line { width:30px; height:2px; background:#409eff; margin:10px auto; }
.stat-label { font-size:13px; color:#86868b; }
.tab-bar { display:flex; background:#f4f7fd; border-radius:12px; padding:4px; margin-bottom:20px; }
.tab-item { flex:1; text-align:center; padding:10px; font-size:15px; color:#666; cursor:pointer; border-radius:10px; transition:0.25s; }
.tab-item.active { background:#fff; color:#409eff; font-weight:500; box-shadow:0 1px 4px rgba(0,0,0,0.06); }
.list-wrap { display:flex; flex-direction:column; gap:14px; }
.logout-wrap { margin-top:40px; text-align:center; }
.logout-btn { color:#f56c6c; font-size:14px; }
.empty-sub { text-align:center; color:#999; font-size:14px; padding:20px 0; }
.skeleton-card { border-radius:16px; border:1px solid #f0f3f9; background:#fff; }
.s-line { height:14px; border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; margin-bottom:12px; }
.s-w-40 { width:40%; } .s-w-60 { width:60%; } .s-w-90 { width:90%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }
</style>
