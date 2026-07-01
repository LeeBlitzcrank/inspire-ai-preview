<template>
  <router-view v-slot="{ Component, route }">
    <component :is="Component" :key="route.fullPath" />
  </router-view>
  <div v-if="!isOnline" class="offline-bar">📡 网络已断开，请检查网络连接</div>
  <div v-if="showTop" class="back-top" @click="scrollToTop">↑</div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
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
.offline-bar { position:fixed; top:0; left:0; right:0; z-index:9999; background:#f56c6c; color:#fff; text-align:center; padding:6px; font-size:13px; }
.back-top { position:fixed; right:16px; bottom:80px; width:38px; height:38px; border-radius:50%; background:rgba(255,255,255,.9); color:#909399; display:flex; align-items:center; justify-content:center; cursor:pointer; font-size:18px; z-index:999; transition:all 0.25s; backdrop-filter:blur(4px); box-shadow:0 1px 6px rgba(0,0,0,.08); border:1px solid rgba(0,0,0,.05); }
.back-top:hover { color:#606266; border-color:#c0c4cc; box-shadow:0 2px 10px rgba(0,0,0,.12); }
</style>
