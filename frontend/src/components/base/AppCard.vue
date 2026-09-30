<!--
  文件：frontend/src/components/base/AppCard.vue
  所属模块：可复用 Vue 组件
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <component
    :is="as"
    class="app-card"
    :class="[
      `app-card--${variant}`,
      { 'is-hover': clickable, 'is-selected': selected }
    ]"
    :style="{ '--card-pad': padding }"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="clickable && $emit('click')"
    @keydown.enter.prevent="clickable && $emit('click')"
    @keydown.space.prevent="clickable && $emit('click')"
  >
    <slot />
  </component>
</template>

<script setup>
/**
 * 通用卡片：统一圆角 / 描边 / hover 态。
 * 用法：<AppCard clickable @click="...">内容</AppCard>
 */
defineProps({
  as: { type: String, default: 'div' },
  clickable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
  variant: { type: String, default: 'outline' },
  padding: { type: String, default: '14px' }
})
defineEmits(['click'])
</script>

<style scoped>
.app-card {
  padding: var(--card-pad);
  background: var(--ui-surface);
  border: 1px solid var(--ui-primary-line);
  border-radius: var(--radius-lg);
  transition: border-color .16s, box-shadow .16s, background .16s;
}
.app-card--flat { border-color: transparent; box-shadow: var(--shadow-sm); }
.app-card--plain { border-color: transparent; background: transparent; }
.app-card.is-hover { cursor: pointer; }
.app-card.is-hover:hover {
  border-color: var(--ui-primary-hover);
  box-shadow: var(--shadow-md);
}
.app-card.is-selected {
  border-color: var(--ui-primary);
  background: var(--ui-primary-weak);
}
.app-card.is-hover:focus-visible {
  outline: 2px solid var(--ui-primary);
  outline-offset: 2px;
}
</style>
