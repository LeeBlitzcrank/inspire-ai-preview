<template>
  <div class="config-page">
    <div class="section"><h3>推荐权重配置</h3>
      <div v-for="cfg in configs" :key="cfg.id" class="cfg-row">
        <div class="cfg-info"><div class="cfg-key">{{ cfg.configKey }}</div><div class="cfg-desc">{{ cfg.desc }}</div></div>
        <el-input v-model="cfg.configValue" style="width:120px" size="small" />
        <el-button size="small" type="primary" @click="doUpdate(cfg)">保存</el-button>
      </div>
    </div>
    <div class="section"><h3>手动推送</h3>
      <el-input v-model="push.title" placeholder="推送标题" class="field" />
      <el-input v-model="push.content" placeholder="推送内容" class="field" />
      <el-input v-model="push.city" placeholder="目标城市（留空则推送到全部）" class="field" />
      <el-button type="warning" :loading="pushing" @click="doPush">发送推送</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminConfigList, adminUpdateConfig, adminManualPush } from '@/api/inspire'
const configs = ref([]); const pushing = ref(false)
const push = ref({ title: '', content: '', city: '' })
onMounted(async () => {
  try { const res = await adminConfigList(); configs.value = res.data || [] } catch (e) {}
})
const doUpdate = async (cfg) => {
  try { await adminUpdateConfig({ id: cfg.id, value: cfg.configValue }); ElMessage.success('更新成功') } catch (e) {}
}
const doPush = async () => {
  if (!push.value.title) return ElMessage.warning('请输入标题')
  pushing.value = true
  try { await adminManualPush(push.value); ElMessage.success('推送已发送'); push.value = { title: '', content: '', city: '' } } catch (e) {}
  finally { pushing.value = false }
}
</script>
<style scoped>
.section { background:#fff; border-radius:14px; padding:20px; margin-bottom:20px; }
.section h3 { margin:0 0 16px; }
.cfg-row { display:flex; align-items:center; gap:12px; margin-bottom:12px; padding-bottom:12px; border-bottom:1px solid #f5f5f5; }
.cfg-row:last-child { border:none; margin-bottom:0; padding-bottom:0; }
.cfg-info { flex:1; }
.cfg-key { font-size:14px; font-weight:500; }
.cfg-desc { font-size:12px; color:#86868b; margin-top:2px; }
.field { margin-bottom:12px; }
</style>
