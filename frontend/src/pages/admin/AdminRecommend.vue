<!--
  文件：frontend/src/pages/admin/AdminRecommend.vue
  所属模块：后台管理前端页面
  主要职责：推荐配置、人工推送和推荐效果管理
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="recommend-page">
    <div class="head">
      <div>
        <h3>推荐管理</h3>
        <p>管理推荐权重、向量召回实验和人工推送，不直接覆盖用户个性化结果。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="推荐配置" name="config">
        <el-table :data="configs" v-loading="loading" style="width:100%">
          <el-table-column prop="configKey" label="配置项" min-width="180" />
          <el-table-column label="配置值" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.configValue" />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220" />
        </el-table>
        <div class="actions"><el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button></div>
      </el-tab-pane>

      <el-tab-pane label="人工推送" name="push">
        <div class="toolbar">
          <span>人工推送有生效时间、权重、人群和每日频率限制。</span>
          <el-button type="primary" @click="openCreatePush">新建推送</el-button>
        </div>
        <el-table :data="pushes" v-loading="loading" style="width:100%">
          <el-table-column prop="title" label="灵感" min-width="220" show-overflow-tooltip />
          <el-table-column prop="targetType" label="目标人群" width="110" />
          <el-table-column prop="targetValue" label="目标值" min-width="110" />
          <el-table-column prop="weight" label="权重" width="80" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><span class="status" :class="row.status">{{ statusText(row.status) }}</span></template>
          </el-table-column>
          <el-table-column label="时间" min-width="220">
            <template #default="{ row }">{{ formatRange(row.startTime, row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEditPush(row)">编辑</el-button>
              <el-button v-if="row.status !== 'ACTIVE'" size="small" type="success" @click="changePushStatus(row, 'ACTIVE')">启用</el-button>
              <el-button v-else size="small" @click="changePushStatus(row, 'PAUSED')">暂停</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="效果数据" name="metrics">
        <el-table :data="metrics" v-loading="loading" style="width:100%">
          <el-table-column prop="title" label="灵感" min-width="220" show-overflow-tooltip />
          <el-table-column prop="impressions" label="曝光" width="90" />
          <el-table-column prop="clicks" label="点击" width="90" />
          <el-table-column prop="collects" label="收藏" width="90" />
          <el-table-column prop="skips" label="跳过" width="90" />
          <el-table-column prop="avg_dwell_ms" label="平均停留(ms)" width="130" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="pushDialog" :title="form.id ? '编辑人工推送' : '新建人工推送'" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="灵感 ID"><el-input v-model.number="form.inspireId" placeholder="请输入灵感 ID" /></el-form-item>
        <el-form-item label="目标人群">
          <el-select v-model="form.targetType" style="width:100%">
            <el-option label="全部用户" value="ALL" />
            <el-option label="分类偏好用户" value="TAG" />
            <el-option label="最近 7 天新用户" value="NEW_USER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'TAG'" label="目标分类">
          <el-input v-model="form.targetValue" placeholder="例如：家居" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="0.1" :max="10" :step="0.1" />
        </el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="推送说明"><el-input v-model="form.reason" maxlength="200" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pushDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePush">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
  adminRecommendConfig,
  adminRecommendMetrics,
  adminRecommendPushCreate,
  adminRecommendPushList,
  adminRecommendPushStatus,
  adminRecommendPushUpdate,
  adminRecommendUpdateConfig
} from '@/api/recommend.js'

const activeTab = ref('config')
const loading = ref(false)
const saving = ref(false)
const configs = ref([])
const pushes = ref([])
const metrics = ref([])
const pushDialog = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    id: null,
    inspireId: null,
    targetType: 'ALL',
    targetValue: '',
    weight: 1,
    status: 'ACTIVE',
    startTime: '',
    endTime: '',
    reason: ''
  }
}

const statusText = (status) => ({
  DRAFT: '草稿',
  ACTIVE: '生效中',
  PAUSED: '已暂停',
  ENDED: '已结束'
}[status] || status)

const formatRange = (start, end) => `${start || '立即'} ~ ${end || '长期'}`

const loadAll = async () => {
  loading.value = true
  try {
    const [configRes, pushRes, metricRes] = await Promise.all([
      adminRecommendConfig(),
      adminRecommendPushList(),
      adminRecommendMetrics()
    ])
    configs.value = configRes.data || []
    pushes.value = pushRes.data || []
    metrics.value = metricRes.data || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '推荐管理数据加载失败')
  } finally {
    loading.value = false
  }
}

const saveConfig = async () => {
  saving.value = true
  try {
    const res = await adminRecommendUpdateConfig(configs.value.map(item => ({
      configKey: item.configKey,
      configValue: String(item.configValue)
    })))
    if (res.code !== 200) throw new Error(res.msg || '保存失败')
    ElMessage.success('推荐配置已保存')
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const openCreatePush = () => {
  form.value = emptyForm()
  pushDialog.value = true
}

const openEditPush = (row) => {
  form.value = {
    id: row.id,
    inspireId: Number(row.inspireId),
    targetType: row.targetType || 'ALL',
    targetValue: row.targetValue || '',
    weight: Number(row.weight || 1),
    status: row.status || 'ACTIVE',
    startTime: row.startTime || '',
    endTime: row.endTime || '',
    reason: row.reason || ''
  }
  pushDialog.value = true
}

const savePush = async () => {
  if (!form.value.inspireId) return ElMessage.warning('请填写灵感 ID')
  if (form.value.targetType === 'TAG' && !form.value.targetValue.trim()) {
    return ElMessage.warning('请填写目标分类')
  }
  saving.value = true
  try {
    const payload = {
      inspireId: form.value.inspireId,
      targetType: form.value.targetType,
      targetValue: form.value.targetValue.trim(),
      weight: form.value.weight,
      status: form.value.status,
      startTime: form.value.startTime || null,
      endTime: form.value.endTime || null,
      reason: form.value.reason.trim()
    }
    const res = form.value.id
      ? await adminRecommendPushUpdate(form.value.id, payload)
      : await adminRecommendPushCreate(payload)
    if (res.code !== 200) throw new Error(res.msg || '保存失败')
    ElMessage.success(form.value.id ? '推送已更新' : '推送已创建')
    pushDialog.value = false
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const changePushStatus = async (row, status) => {
  try {
    const res = await adminRecommendPushStatus(row.id, status)
    if (res.code !== 200) throw new Error(res.msg || '状态更新失败')
    ElMessage.success(status === 'ACTIVE' ? '推送已启用' : '推送已暂停')
    await loadAll()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '状态更新失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.recommend-page { padding:4px 2px; }
.head { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:18px; }
.head h3 { margin:0; }
.head p { margin:6px 0 0; color:#8a94a6; font-size:12.5px; }
.actions { display:flex; justify-content:flex-end; margin-top:16px; }
.toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:14px; color:#8a94a6; font-size:12px; }
.status { display:inline-block; padding:3px 8px; border-radius:999px; background:#eef3f2; color:#5e716d; font-size:11px; }
.status.ACTIVE { background:#e6f6ec; color:#2f8a52; }
.status.PAUSED { background:#fff4dc; color:#ad7112; }
.status.ENDED { background:#f0f1f3; color:#7b828d; }
</style>
