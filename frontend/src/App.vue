<template>
  <router-view v-slot="{ Component, route }">
    <!-- transition name 改为 slide-top，自定义上下动画 -->
    <transition name="slide-top" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </transition>
  </router-view>
</template>

<style scoped>
/* 全局路由动画：新页面从上滑入，旧页面向下滑出 */
:deep(.slide-top-enter-from) {
  transform: translateY(-100%);
  opacity: 0;
}
:deep(.slide-top-enter-active) {
  transition: all 0.3s ease;
}
:deep(.slide-top-leave-to) {
  transform: translateY(100%);
  opacity: 0;
}
:deep(.slide-top-leave-active) {
  transition: all 0.3s ease;
}
</style>