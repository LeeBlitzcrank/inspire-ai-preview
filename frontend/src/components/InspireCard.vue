<!--
  文件：frontend/src/components/InspireCard.vue
  所属模块：可复用 Vue 组件
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <AppCard class="inspire-card" padding="16px" clickable @click="goDetail">
    <div class="card-header">
      <h3 class="card-title">{{ item.title }}</h3>
      <span class="heat-tag">{{ item.heat }} 热度</span>
    </div>
    <p class="card-desc">{{ item.content }}</p>
    <div class="card-footer">
      <span>{{ item.viewCount }} 浏览</span>
      <span>{{ item.collectCount }} 收藏</span>
      <el-button text size="small" @click.stop="$emit('collect', item.id)">收藏</el-button>
    </div>
  </AppCard>
</template>
<script setup>
import AppCard from '@/components/base/AppCard.vue'
import {useRouter} from 'vue-router'

const props = defineProps(['item'])
const emit = defineEmits(['collect'])
const router = useRouter()
const goDetail = () => {
  if (props.item.id !== null && props.item.id !== undefined && String(props.item.id).trim()) {
    router.push({ name: 'InspireDetail', params: { id: String(props.item.id) } })
  }
}
</script>
<style scoped>
.inspire-card { border-radius:16px; transition:all 0.2s; }
.inspire-card:hover { border-color:#e1e6f0; box-shadow:0 2px 8px rgba(120,140,180,0.06); }
.card-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; }
.card-title { font-size:16px; font-weight:500; color:#1d1d1f; margin:0; }
.heat-tag { font-size:12px; color:#ff7d00; }
.card-desc { font-size:14px; color:#6e6e73; line-height:1.6; margin:0 0 14px; }
.card-footer { display:flex; gap:20px; font-size:13px; color:#86868b; align-items:center; justify-content:space-between; }
</style>
