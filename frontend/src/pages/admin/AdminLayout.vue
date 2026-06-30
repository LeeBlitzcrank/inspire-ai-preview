<template>
  <div class="admin-wrap">
    <div class="mobile-bar">
      <div class="mobile-logo">🍎 灵思集</div>
      <div class="hamburger" @click="showSidebar = !showSidebar">☰</div>
    </div>
    <div class="sidebar" :class="{open: showSidebar}">
      <div class="logo">🍎 灵思集</div>
      <div class="nav">
        <div v-for="item in navs" :key="item.path" class="nav-item"
          :class="{active: $route.path === item.path}" @click="go(item.path)">
          <span class="nav-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </div>
      </div>
      <div class="sidebar-footer">
        <span class="admin-name">{{ adminName }}</span>
        <el-button text size="small" @click="doLogout">退出</el-button>
      </div>
    </div>
    <div class="content" @click="showSidebar=false"><router-view /></div>
  </div>
</template>
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()
const adminName = ref(localStorage.getItem('adminUser') || '管理员')
const showSidebar = ref(window.innerWidth > 768)
const navs = [
  { path: '/admin/dashboard', label: '监控大屏', icon: '📊' },
  { path: '/admin/inspire', label: '灵感管理', icon: '💡' },
  { path: '/admin/user', label: '用户查询', icon: '👤' },
  { path: '/admin/config', label: '推送配置', icon: '⚙️' },
]
const go = (path) => { router.push(path); if (window.innerWidth <= 768) showSidebar.value = false }
const doLogout = () => { localStorage.removeItem('adminToken'); localStorage.removeItem('adminUser'); router.push('/admin/login') }
const onResize = () => { if (window.innerWidth > 768) showSidebar.value = true }
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>
<style scoped>
.admin-wrap { display:flex; min-height:100vh; background:#f5f7fa; }
.mobile-bar { display:none; position:fixed; top:0; left:0; right:0; height:48px; background:#fff; z-index:100; align-items:center; justify-content:space-between; padding:0 16px; border-bottom:1px solid #eee; }
.mobile-logo { font-size:16px; font-weight:600; }
.hamburger { font-size:22px; cursor:pointer; }
.sidebar { width:200px; background:#fff; border-right:1px solid #eee; display:flex; flex-direction:column; flex-shrink:0; transition:transform 0.25s; }
.logo { padding:20px; font-size:18px; font-weight:600; border-bottom:1px solid #f0f0f0; }
.nav { flex:1; padding:8px; }
.nav-item { padding:12px 16px; border-radius:10px; cursor:pointer; margin-bottom:4px; font-size:15px; color:#333; }
.nav-item:hover { background:#f4f7fd; }
.nav-item.active { background:#409eff; color:#fff; }
.nav-icon { margin-right:8px; }
.sidebar-footer { padding:16px; border-top:1px solid #f0f0f0; display:flex; align-items:center; justify-content:space-between; }
.admin-name { font-size:13px; color:#86868b; }
.content { flex:1; padding:24px; overflow-x:auto; }
@media (max-width:768px) {
  .admin-wrap { padding-top:48px; }
  .mobile-bar { display:flex; }
  .sidebar { position:fixed; top:48px; left:0; bottom:0; z-index:99; transform:translateX(-100%); }
  .sidebar.open { transform:translateX(0); }
  .content { padding:12px; }
}
</style>
