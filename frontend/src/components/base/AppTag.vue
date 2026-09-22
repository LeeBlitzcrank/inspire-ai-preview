<template>
  <span
    class="app-tag"
    :class="[`app-tag--${tone}`, `app-tag--${size}`, { 'is-soft': soft }]"
  >
    <slot />
  </span>
</template>

<script setup>
/** 通用小标签：统一圆角、字号、颜色语义。 */
defineProps({
  soft: { type: Boolean, default: true },
  tone: { type: String, default: 'primary' },
  size: { type: String, default: 'sm' }
})
</script>

<style scoped>
.app-tag {
  display: inline-flex; align-items: center; gap: var(--sp-1);
  border-radius: var(--radius-pill);
  line-height: 1.6;
  background: color-mix(in srgb, var(--tag-color) 12%, white);
  color: var(--tag-color);
}
.app-tag:not(.is-soft) {
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--tag-color) 34%, white);
}
.app-tag--sm { padding: 1px 7px; font-size: var(--fs-xs); }
.app-tag--md { padding: 3px 10px; font-size: var(--fs-sm); }
.app-tag--primary { --tag-color: var(--ui-primary); }
.app-tag--success { --tag-color: var(--ui-success); }
.app-tag--warning { --tag-color: var(--ui-warning); }
.app-tag--danger { --tag-color: var(--ui-danger); }
.app-tag--neutral { --tag-color: var(--ui-text-3); }
</style>
