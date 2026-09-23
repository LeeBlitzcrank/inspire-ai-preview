<template>
  <el-dialog
    class="collect-folder-dialog"
    :model-value="modelValue"
    width="340px"
    align-center
    append-to-body
    :show-close="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="collect-head">
      <div class="collect-head-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M6 3h12v18l-6-4-6 4Z" />
        </svg>
      </div>
      <div>
        <h3>{{ title }}</h3>
        <p>选择一个收藏夹，方便之后快速找到这篇灵感。</p>
      </div>
      <button class="dialog-close" type="button" aria-label="关闭" @click="close">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M18 6 6 18" />
          <path d="m6 6 12 12" />
        </svg>
      </button>
    </div>

    <div class="folder-list">
      <div v-if="loading" class="folder-loading">正在加载收藏夹...</div>
      <template v-else>
        <button
          v-for="folder in folders"
          :key="folder.id"
          class="folder-item"
          :class="{ selected: String(selectedId) === String(folder.id) }"
          type="button"
          @click="selectedId = folder.id"
        >
          <span class="folder-item-icon">{{ folder.icon || '📁' }}</span>
          <span class="folder-item-copy">
            <b>{{ folder.name }}</b>
            <small>收藏夹</small>
          </span>
          <span class="folder-check">✓</span>
        </button>
        <div v-if="!folders.length" class="folder-empty">
          还没有收藏夹，创建一个后再收藏。
        </div>
      </template>
    </div>

    <button class="create-toggle" type="button" @click="createOpen = !createOpen">
      <span>{{ createOpen ? '取消新建' : '新建收藏夹' }}</span>
      <span class="create-plus">{{ createOpen ? '−' : '+' }}</span>
    </button>

    <div v-if="createOpen" class="create-row">
      <input
        v-model="newFolderName"
        maxlength="20"
        placeholder="输入收藏夹名称"
        @keydown.enter.prevent="createFolder"
      >
      <button type="button" :disabled="creating || !newFolderName.trim()" @click="createFolder">
        {{ creating ? '创建中' : '创建' }}
      </button>
    </div>

    <div class="collect-actions">
      <button class="ghost" type="button" :disabled="saving" @click="close">取消</button>
      <button
        class="primary"
        type="button"
        :disabled="saving || !selectedId"
        @click="confirmCollect"
      >
        {{ saving ? '收藏中...' : '收藏到此' }}
      </button>
    </div>
  </el-dialog>
</template>

<script setup>
import {ref, watch} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {collectToFolder, createCollectFolder, getCollectFolders} from '@/api/inspire.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  targetId: { type: [String, Number], default: '' },
  title: { type: String, default: '收藏到' }
})

const emit = defineEmits(['update:modelValue', 'collected'])

const folders = ref([])
const selectedId = ref(null)
const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const createOpen = ref(false)
const newFolderName = ref('')

const loadFolders = async () => {
  loading.value = true
  try {
    const res = await getCollectFolders()
    folders.value = res.data || []
    const currentExists = folders.value.some(item => String(item.id) === String(selectedId.value))
    if (!currentExists) selectedId.value = folders.value[0]?.id ?? null
  } catch (e) {
    ElMessage.error('收藏夹加载失败')
  } finally {
    loading.value = false
  }
}

const close = () => {
  if (saving.value) return
  emit('update:modelValue', false)
}

const createFolder = async () => {
  const name = newFolderName.value.trim()
  if (!name || creating.value) return
  creating.value = true
  try {
    const res = await createCollectFolder(name, '📁')
    const created = res.data || res
    await loadFolders()
    selectedId.value = created?.id ?? folders.value.at(-1)?.id ?? null
    newFolderName.value = ''
    createOpen.value = false
    ElMessage.success('收藏夹已创建')
  } catch (e) {
    ElMessage.error('创建收藏夹失败')
  } finally {
    creating.value = false
  }
}

const confirmCollect = async () => {
  if (!props.targetId || !selectedId.value || saving.value) return
  saving.value = true
  try {
    await collectToFolder(props.targetId, selectedId.value)
    ElMessage.success('收藏成功')
    emit('collected', String(props.targetId))
    emit('update:modelValue', false)
  } catch (e) {
    ElMessage.error('收藏失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    createOpen.value = false
    newFolderName.value = ''
    loadFolders()
  }
)
</script>

<style scoped>
.collect-head {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding-right: 28px;
}

.collect-head-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ui-primary);
  background: var(--ui-primary-weak);
}

.collect-head-icon svg { width: 20px; height: 20px; }
.collect-head h3 { margin: 1px 0 4px; font-size: 17px; color: var(--ui-text); }
.collect-head p { margin: 0; color: var(--ui-text-3); font-size: 12px; line-height: 1.6; }

.dialog-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--ui-text-3);
  cursor: pointer;
}

.dialog-close:hover { background: var(--ui-primary-soft); color: var(--ui-primary); }
.dialog-close svg { width: 17px; height: 17px; margin: auto; }

.folder-list {
  max-height: 270px;
  overflow-y: auto;
  margin-top: 16px;
}

.folder-loading,
.folder-empty {
  padding: 28px 8px;
  text-align: center;
  color: var(--ui-text-3);
  font-size: 12.5px;
}

.folder-item {
  width: 100%;
  min-height: 58px;
  padding: 9px 11px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--ui-primary-line);
  border-radius: 14px;
  background: #fff;
  color: var(--ui-text);
  text-align: left;
  cursor: pointer;
  transition: border-color .16s, background .16s, box-shadow .16s;
}

.folder-item + .folder-item { margin-top: 8px; }
.folder-item:hover { border-color: var(--ui-primary-hover); }
.folder-item.selected {
  border-color: var(--ui-primary);
  background: var(--ui-primary-weak);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--ui-primary) 10%, transparent);
}

.folder-item-icon {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ui-primary-soft);
  font-size: 19px;
}

.folder-item-copy { flex: 1; min-width: 0; }
.folder-item-copy b { display: block; font-size: 13.5px; }
.folder-item-copy small { display: block; margin-top: 2px; color: var(--ui-text-3); font-size: 10.5px; }

.folder-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: transparent;
  background: var(--ui-primary-soft);
  font-size: 12px;
  font-weight: 800;
}

.folder-item.selected .folder-check {
  color: #fff;
  background: var(--ui-primary);
}

.create-toggle {
  width: 100%;
  margin-top: 12px;
  padding: 9px 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  border-top: 1px dashed var(--ui-primary-line);
  background: transparent;
  color: var(--ui-primary);
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}

.create-plus { font-size: 19px; line-height: 1; }

.create-row { display: flex; gap: 8px; margin-top: 8px; }
.create-row input {
  flex: 1;
  min-width: 0;
  height: 38px;
  padding: 0 11px;
  border: 1px solid var(--ui-primary-line);
  border-radius: 11px;
  outline: none;
  font: inherit;
  font-size: 12.5px;
}
.create-row input:focus { border-color: var(--ui-primary); }
.create-row button {
  padding: 0 13px;
  border: 0;
  border-radius: 11px;
  background: var(--ui-primary-soft);
  color: var(--ui-primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.create-row button:disabled { opacity: .5; cursor: default; }

.collect-actions { display: flex; gap: 9px; margin-top: 16px; }
.collect-actions button {
  flex: 1;
  height: 42px;
  border: 0;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.collect-actions .ghost { background: var(--ui-primary-soft); color: var(--ui-text-2); }
.collect-actions .primary { background: var(--ui-primary); color: #fff; }
.collect-actions button:disabled { opacity: .5; cursor: default; }
</style>
