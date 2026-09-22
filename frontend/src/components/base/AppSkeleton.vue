<template>
  <div class="skeleton" :class="`skeleton--${variant}`">
    <div v-for="n in rows" :key="n" class="skeleton-row">
      <div v-if="variant !== 'text'" class="skeleton-thumb"></div>
      <div class="skeleton-lines">
        <div class="skeleton-line w70"></div>
        <div v-if="variant !== 'grid'" class="skeleton-line w40"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
  variant: { type: String, default: 'list' }
})
</script>

<style scoped>
.skeleton-row {
  display: flex; align-items: center; gap: var(--sp-3);
  padding: 11px 12px; margin-bottom: var(--sp-2);
  background: var(--ui-surface);
  border: 1px solid var(--ui-primary-line);
  border-radius: var(--radius-lg);
}
.skeleton-thumb {
  width: 54px; height: 54px; border-radius: var(--radius-md); flex: 0 0 auto;
  background: var(--ui-primary-soft);
  animation: sk-pulse 1.2s ease-in-out infinite;
}
.skeleton-lines { flex: 1; min-width: 0; }
.skeleton-line {
  height: 12px; border-radius: 6px; margin-bottom: 8px;
  background: var(--ui-primary-soft);
  animation: sk-pulse 1.2s ease-in-out infinite;
}
.skeleton-line.w70 { width: 70%; }
.skeleton-line.w40 { width: 40%; margin-bottom: 0; }
.skeleton--text .skeleton-row { padding: 8px 0; background: transparent; border: 0; }
.skeleton--text .skeleton-line { width: 100% !important; height: 14px; }
.skeleton--grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--sp-3); }
.skeleton--grid .skeleton-row { display: block; margin-bottom: 0; }
.skeleton--grid .skeleton-thumb { width: 100%; height: 88px; margin-bottom: var(--sp-3); }
@keyframes sk-pulse { 0%, 100% { opacity: .55 } 50% { opacity: 1 } }
</style>
