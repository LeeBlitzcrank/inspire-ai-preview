<template>
  <div id="detail-root" class="detail-page">
    <div id="detail-top-nav" class="top-nav">
      <div id="detail-back-btn" class="left-logo" @click="$router.back()">←</div>
      <div id="detail-title">灵感详情</div>
      <div class="placeholder"></div>
    </div>
    <div v-if="detail.id" id="detail-main-card" class="main-card">
      <div class="img-box"><img :src="detail.img || 'https://picsum.photos/id/102/300/160'" /></div>
      <h2 class="title">{{ detail.title }}</h2>
      <p class="desc">{{ detail.content || detail.title }}</p>
      <div class="stat-row">
        <span>分类: {{ detail.tag }}</span>
        <span>浏览 {{ detail.viewCount }}</span>
        <span>热度 {{ detail.heat }}</span>
        <span>{{ detail.publishCity }}</span>
      </div>
      <div class="action-row">
        <el-button :type="liked ? 'primary' : ''" @click="handleLike">{{ liked ? '取消点赞' : '点赞' }} {{ detail.likeCount || '' }}</el-button>
        <el-button :type="collected ? 'primary' : ''" @click="handleCollect">{{ collected ? '取消收藏' : '收藏' }} {{ detail.collectCount || '' }}</el-button>
      </div>
    </div>
    <div v-else class="loading-tip">{{ loadErr || '加载中...' }}</div>
  </div>
</template>
<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInspireDetail, collectInspire, uncollectInspire, likeInspire, unlikeInspire } from '@/api/inspire'
const route = useRoute(); const router = useRouter()
const detail = ref({}); const liked = ref(false); const collected = ref(false); const loadErr = ref('')

const loadData = async (id) => {
  try {
    const res = await getInspireDetail(id)
    detail.value = res.data || {}
    liked.value = !!detail.value.liked
    collected.value = !!detail.value.collected
  } catch (e) { loadErr.value = '灵感不存在或已删除' }
}
const handleLike = async () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); window.location.href = '/login'; return }
  try {
    if (liked.value) {
      const res = await unlikeInspire(detail.value.id)
      if (res.code === 200) { liked.value = false; detail.value.likeCount-- }
    } else {
      const res = await likeInspire(detail.value.id)
      if (res.code === 200) { liked.value = true; detail.value.likeCount++ }
    }
  } catch (e) {}
}
const handleCollect = async () => {
  if (!localStorage.getItem('isLogin')) { ElMessage.warning('请先登录'); window.location.href = '/login'; return }
  try {
    if (collected.value) {
      const res = await uncollectInspire(detail.value.id)
      if (res.code === 200) { collected.value = false; detail.value.collectCount-- }
    } else {
      const res = await collectInspire(detail.value.id)
      if (res.code === 200) { collected.value = true; detail.value.collectCount++ }
    }
  } catch (e) {}
}
watch(() => route.params.id, (id) => { if (id) loadData(id) }, { immediate: true })
</script>
<style scoped>
.detail-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; align-items:center; justify-content:space-between; padding:8px 0 24px; }
.left-logo { width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); cursor:pointer; font-size:20px; }
#detail-title { font-size:18px; font-weight:600; }
.placeholder { width:40px; }
.main-card { background:#fff; border-radius:20px; padding:20px; margin-bottom:30px; }
.img-box { width:100%; height:200px; border-radius:16px; overflow:hidden; margin-bottom:16px; }
.img-box img { width:100%; height:100%; object-fit:cover; }
.title { font-size:20px; margin:0 0 12px; color:#1d1d1f; }
.desc { font-size:15px; color:#666; line-height:1.6; margin-bottom:16px; }
.stat-row { display:flex; gap:20px; font-size:14px; color:#888; margin-bottom:20px; flex-wrap:wrap; }
.action-row { display:flex; gap:16px; }
.loading-tip { text-align:center; padding:60px 0; color:#999; font-size:15px; }
</style>
