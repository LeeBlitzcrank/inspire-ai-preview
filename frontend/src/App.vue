<template>
  <div ref="designRoot" class="app-design">
    <router-view v-slot="{ Component, route }">
      <transition name="page">
        <component :is="Component" :key="route.fullPath" />
      </transition>
    </router-view>
  </div>
  <div v-if="!isOnline" class="offline-bar">📡 网络已断开，请检查网络连接</div>
  <div v-if="showTop" class="back-top" @click="scrollToTop">↑</div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
const showTop = ref(false)
const isOnline = ref(navigator.onLine)
const designRoot = ref(null)
const DESIGN_WIDTH = 620   // 设计基准宽度（与各页面 max-width 一致）

const onScroll = () => { showTop.value = window.scrollY > 400 }
const scrollToTop = () => { window.scrollTo({ top: 0, behavior: 'smooth' }) }

// 方案A（zoom 版）：zoom 会重排布局，缩放后元素实际占位 = 620×zoom，
// 不会像 transform 那样撑出横向滚动条。
const applyScale = () => {
  const vw = document.documentElement.clientWidth || window.innerWidth
  const scale = Math.min(vw / DESIGN_WIDTH, 1)
  if (designRoot.value) {
    designRoot.value.style.zoom = scale
    // 缩放后仍撑满视口高度（min-height 处于缩放坐标系，需除以 scale）
    designRoot.value.style.minHeight = (window.innerHeight / scale) + 'px'
  }
}

onMounted(() => {
  applyScale()
  window.addEventListener('resize', applyScale)
  window.addEventListener('scroll', onScroll)
  window.addEventListener('online', () => isOnline.value = true)
  window.addEventListener('offline', () => isOnline.value = false)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', applyScale)
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.app-design {
  width: 620px;
  margin: 0 auto;
  background: #fbfcfe;
}
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
#app { overflow-x: hidden; }
</style>
