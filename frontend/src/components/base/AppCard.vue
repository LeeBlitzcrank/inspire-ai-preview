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
