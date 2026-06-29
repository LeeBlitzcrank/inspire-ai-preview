<template>
  <div class="create-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/personal')">👤</div>
      </div>
    </div>
    <div class="input-box">
      <h2 class="page-title">录入新灵感</h2>
      <p class="page-desc">填写创意内容，保存到灵感库</p>

      <!-- AI 生成区域 -->
      <div class="ai-section">
        <div class="ai-row">
          <el-input v-model="aiKeyword" placeholder="输入关键词，AI帮你生成灵感" size="large" @keyup.enter="handleAiGenerate">
            <template #append><el-button type="warning" :loading="aiLoading" @click="handleAiGenerate">AI生成</el-button></template>
          </el-input>
        </div>
        <!-- AI 候选卡片 -->
        <div v-if="candidates.length > 0" class="candidate-wrap">
          <div class="candidate-card" v-for="c in candidates" :key="c.id" :class="{selected: selectedId === c.id}"
            @click="selectCandidate(c)">
            <div class="candidate-title">{{ c.title }}</div>
            <div class="candidate-summary">{{ c.summary }}</div>
            <div class="candidate-tag">#{{ c.tag }}</div>
          </div>
        </div>
      </div>

      <div class="row"><label>灵感标题</label><el-input v-model="form.title" placeholder="输入简短标题"></el-input></div>
      <div class="row">
        <label>所属分类</label>
        <el-select v-model="form.tag" placeholder="选择分类" style="width:100%">
          <el-option v-for="t in tags" :key="t" :label="t" :value="t" />
        </el-select>
      </div>
      <div class="row"><label>灵感详情</label><el-input v-model="form.content" type="textarea" :rows="5" placeholder="详细描述你的创意"></el-input></div>
      <el-button class="submit-btn" type="primary" :loading="loading" @click="submit">保存灵感</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { aiGenerate, createInspire } from '@/api/inspire'
const router = useRouter()
const tags = ['美食','运动','电影','穿搭','文案','旅游','摄影','其他']
const loading = ref(false); const aiLoading = ref(false)
const aiKeyword = ref(''); const candidates = ref([]); const selectedId = ref(null)
const form = ref({ title: '', tag: '', content: '' })

const handleAiGenerate = async () => {
  if (!aiKeyword.value.trim()) return ElMessage.warning('请输入关键词')
  aiLoading.value = true; candidates.value = []; selectedId.value = null
  try {
    const res = await aiGenerate({ keyword: aiKeyword.value })
    candidates.value = res.data?.candidates || []
  } catch (e) {} finally { aiLoading.value = false }
}
const selectCandidate = (c) => {
  selectedId.value = c.id; form.value.title = c.title; form.value.tag = c.tag
}
const submit = async () => {
  if (!form.value.title) return ElMessage.warning('请填写标题')
  if (!form.value.tag) return ElMessage.warning('请选择分类')
  if (!form.value.content) return ElMessage.warning('请填写灵感详情')
  loading.value = true
  try {
    const res = await createInspire({ ...form.value, status: 1 })
    ElMessage.success('灵感发布成功')
    router.push('/')
  } catch (e) {} finally { loading.value = false }
}
</script>
<style scoped>
.create-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 0 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.input-box { background:#fff; border-radius:20px; padding:30px 24px; margin-top:20px; }
.page-title { font-size:24px; font-weight:500; text-align:center; margin:0 0 6px; color:#1d1d1f; }
.page-desc { text-align:center; font-size:14px; color:#86868b; margin-bottom:28px; }
.ai-section { margin-bottom:24px; background:#fef9ef; border-radius:16px; padding:16px; }
.ai-row { margin-bottom:12px; }
.candidate-wrap { display:flex; gap:12px; }
.candidate-card { flex:1; background:#fff; border:2px solid #eee; border-radius:14px; padding:16px; cursor:pointer; transition:0.25s; }
.candidate-card:hover { border-color:#409eff; }
.candidate-card.selected { border-color:#409eff; background:#f0f7ff; }
.candidate-title { font-size:15px; font-weight:500; color:#1d1d1f; margin-bottom:6px; }
.candidate-summary { font-size:13px; color:#6e6e73; margin-bottom:8px; line-height:1.4; }
.candidate-tag { font-size:12px; color:#409eff; }
.row { margin-bottom:18px; }
.row label { display:block; font-size:14px; color:#1d1d1f; margin-bottom:8px; }
.submit-btn { width:100%; height:46px; border-radius:14px; font-size:16px; background:#409eff; border:none; margin-top:10px; }
</style>
