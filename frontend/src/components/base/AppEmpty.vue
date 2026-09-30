<!--
  文件：frontend/src/components/base/AppEmpty.vue
  所属模块：可复用 Vue 组件
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-text">{{ text }}</div>
    <div v-if="$slots.action" class="empty-action"><slot name="action" /></div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '💭' },
  text: { type: String, default: '暂无内容' }
})
</script>

<style scoped>
.empty-state { text-align: center; padding: 56px 20px; }
.empty-icon { font-size: 30px; margin-bottom: var(--sp-3); }
.empty-text { font-size: var(--fs-md); color: var(--ui-text-3); line-height: 1.7; }
.empty-action { margin-top: var(--sp-4); }
</style>
