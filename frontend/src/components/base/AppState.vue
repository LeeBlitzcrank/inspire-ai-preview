<!--
  文件：frontend/src/components/base/AppState.vue
  所属模块：可复用 Vue 组件
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <slot v-if="state === 'loading'" name="loading">
    <AppSkeleton :rows="rows" :variant="loadingVariant" />
  </slot>

  <div v-else-if="state === 'error'" class="st st--error">
    <slot name="error">
      <div class="st-ico">⚠️</div>
      <div class="st-text">{{ errorText || '加载失败，请稍后重试' }}</div>
      <button v-if="retryable" class="st-btn" type="button" @click="$emit('retry')">
        {{ retryText }}
      </button>
    </slot>
  </div>

  <div v-else-if="state === 'empty'" class="st">
    <slot name="empty">
      <AppEmpty :icon="emptyIcon" :text="emptyText">
        <template v-if="$slots['empty-action']" #action>
          <slot name="empty-action" />
        </template>
      </AppEmpty>
    </slot>
  </div>

  <slot v-else />
</template>

<script setup>
import AppSkeleton from './AppSkeleton.vue'
import AppEmpty from './AppEmpty.vue'

/**
 * 列表三态统一：loading（骨架屏）/ empty / error，都带默认文案与重试。
 * 用法：<AppState :state="listState" @retry="load" empty-text="还没有内容"><真实内容/></AppState>
 *   state 取值：loading | empty | error | ready
 */
defineProps({
  state: { type: String, default: 'ready' },
  rows: { type: Number, default: 3 },
  loadingVariant: { type: String, default: 'list' },
  emptyIcon: { type: String, default: '💭' },
  emptyText: { type: String, default: '暂无内容' },
  errorText: { type: String, default: '' },
  retryText: { type: String, default: '重新加载' },
  retryable: { type: Boolean, default: true }
})
defineEmits(['retry'])
</script>

<style scoped>
/* 空 / 出错 */
.st--error { text-align: center; padding: 56px 20px; }
.st-ico { font-size: 30px; margin-bottom: var(--sp-3); }
.st-text { font-size: var(--fs-md); color: var(--ui-text-3); line-height: 1.7; }
.st--error .st-text { color: var(--ui-danger); }
.st-btn {
  margin-top: var(--sp-4); padding: 8px 18px;
  border: 1px solid var(--ui-primary-line); border-radius: var(--radius-pill);
  background: var(--ui-surface); color: var(--ui-primary);
  font: inherit; font-size: var(--fs-sm); cursor: pointer;
}
.st-btn:hover { background: var(--ui-primary-weak); }
</style>
