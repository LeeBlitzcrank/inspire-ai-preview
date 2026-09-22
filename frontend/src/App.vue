<template>
  <DeviceShell :enabled="$route.meta.deviceShell !== false">
  <div class="app-design">
    <router-view v-slot="{ Component, route }">
      <transition name="page">
        <div
          class="app-page"
          :class="{ 'app-page--fixed': route.meta.fixedViewport }"
          :key="route.fullPath"
        >
          <component :is="Component" />
        </div>
      </transition>
    </router-view>
  </div>
  <div v-if="!isOnline" class="offline-bar">📡 网络已断开，请检查网络连接</div>
  <div v-if="showTop" class="back-top" @click="scrollToTop">↑</div>
  </DeviceShell>
  <AppErrorDialog />
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import DeviceShell from '@/components/layout/DeviceShell.vue'
const showTop = ref(false)
const isOnline = ref(navigator.onLine)

const onScroll = () => { showTop.value = window.scrollY > 400 }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  window.addEventListener('online', () => isOnline.value = true)
  window.addEventListener('offline', () => isOnline.value = false)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.app-design { width: 100%; background: #fbfcfe; }
.offline-bar { position:fixed; top:0; left:0; right:0; z-index:9999; background:#f56c6c; color:#fff; text-align:center; padding:6px; font-size:13px; }
.back-top { position:fixed; right:16px; bottom:80px; width:38px; height:38px; border-radius:50%; background:rgba(255,255,255,.9); color:#909399; display:flex; align-items:center; justify-content:center; cursor:pointer; font-size:18px; z-index:999; transition:all 0.25s; backdrop-filter:blur(4px); box-shadow:0 1px 6px rgba(0,0,0,.08); border:1px solid rgba(0,0,0,.05); }
.back-top:hover { color:#606266; border-color:#c0c4cc; box-shadow:0 2px 10px rgba(0,0,0,.12); }
</style>

<style>
/* 页面过渡动画 */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.15s cubic-bezier(0.4, 0, 0.2, 1),
              transform 0.15s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(5px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}
</style>

<style>
/* 全局：消除浏览器默认边距与横向滚动条 */
html, body {
  margin: 0;
  padding: 0;
  width: 100%;
  overflow-x: hidden;
}
html { min-height: 100%; }
body {
  min-height: 100vh;
  min-height: 100dvh;
  overflow-y: auto;
  overscroll-behavior-y: auto;
  -webkit-overflow-scrolling: touch;
}
#app {
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
}

/* 所有路由（包括未来新增页面）统一使用动态视口高度和移动端安全区。
   不再用 transform 缩放页面，避免 iPhone 上文字变小、触控区域失准。 */
.app-design,
.app-page {
  width: 100%;
  min-width: 0;
  min-height: 100vh;
  min-height: 100dvh;
}
.app-page > * {
  min-width: 0;
  max-width: 100%;
}

.app-page--fixed {
  height: 100vh;
  height: 100dvh;
  min-height: 0;
  overflow: hidden;
}
#app .app-page--fixed > * {
  height: 100%;
  min-height: 0;
}

/* iOS Safari：表单控件字号必须 ≥16px，否则聚焦时会自动放大页面并卡在放大态 */
@media screen and (max-width: 620px) {
  input,
  textarea,
  select,
  .el-input__inner,
  .el-textarea__inner {
    font-size: 16px !important;
  }

  /* 直接给页面根节点留底部空间，背景色不会被透明外层破坏。
     消息等固定视口页面不参与这条规则。 */
  #app .app-page:not(.app-page--fixed) > * {
    padding-bottom: max(96px, calc(80px + env(safe-area-inset-bottom, 0px)));
  }
}
</style>

<!-- 路由进度条配色跟随站点主色（薄荷绿） -->
<style>
/* 全局盒模型 reset：避免「width:100% + padding」把元素横向撑出容器 */
*, *::before, *::after { box-sizing: border-box; }

/* 路由级过渡：淡入 + 轻微上移，比原来的硬切自然 */
.page-enter-active { transition: opacity .22s ease, transform .22s ease; }
.page-leave-active { transition: opacity .16s ease; }
.page-enter-from { opacity: 0; transform: translateY(6px); }
.page-leave-to { opacity: 0; }

#nprogress .bar {
  background: #4f8a48;
  height: 3px;
}
#nprogress .peg {
  box-shadow: 0 0 10px #4f8a48, 0 0 5px #4f8a48;
}
</style>
