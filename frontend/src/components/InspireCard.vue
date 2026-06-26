<template>
  <div class="inspire-card">
    <div class="card-header">
      <h3 class="card-title">{{ item.title }}</h3>
      <span class="heat-tag">{{ item.heatScore }} 热度</span>
    </div>
    <p class="card-desc">{{ item.content }}</p>
    <div class="card-footer">
      <span>👁️ {{ item.viewCount }}</span>
      <span>⭐ {{ item.collectCount }}</span>
      <el-button text size="small" @click="$emit('collect', item.id)">收藏</el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps(['item'])
const emit = defineEmits(['collect'])
</script>

<style scoped>
.inspire-card {
  border-radius: 16px;
  border: 1px solid #f0f3f9;
  background: #fff;
  padding: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d1d1f;
  margin: 0;
}
.heat-tag {
  font-size: 12px;
  color: #ff7d00;
}
.card-desc {
  font-size: 14px;
  color: #6e6e73;
  line-height: 1.6;
  margin: 0 0 14px;
}
.card-footer {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #86868b;
  align-items: center;
  justify-content: space-between;
}
</style>