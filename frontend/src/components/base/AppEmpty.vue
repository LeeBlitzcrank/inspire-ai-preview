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
