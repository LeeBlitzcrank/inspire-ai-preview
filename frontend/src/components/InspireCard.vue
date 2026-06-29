<template>
  <div class="inspire-card" @click="goDetail">
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
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router'
const props = defineProps(['item'])
const emit = defineEmits(['collect'])
const router = useRouter()
const goDetail = () => { if (props.item.id) router.push({ name: 'InspireDetail', params: { id: props.item.id } }) }
</script>
<style scoped>
.inspire-card { border-radius:16px; border:1px solid #f0f3f9; background:#fff; padding:16px; cursor:pointer; transition:all 0.2s; }
.inspire-card:hover { border-color:#e1e6f0; box-shadow:0 2px 8px rgba(120,140,180,0.06); }
.card-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; }
.card-title { font-size:16px; font-weight:500; color:#1d1d1f; margin:0; }
.heat-tag { font-size:12px; color:#ff7d00; }
.card-desc { font-size:14px; color:#6e6e73; line-height:1.6; margin:0 0 14px; }
.card-footer { display:flex; gap:20px; font-size:13px; color:#86868b; align-items:center; justify-content:space-between; }
</style>
