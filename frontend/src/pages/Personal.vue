<!--
  文件：frontend/src/pages/Personal.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="personal-page" :data-theme="currentTheme">
    <div class="topbar" id="p-topbar">
      <div class="ico" id="p-logo" @click="$router.push('/')">🍎</div>
      <div class="right">
        <div class="ico" id="p-icon-create" @click="$router.push('/create')">✨</div>
        <div class="ico" id="p-icon-search" @click="$router.push('/search')">🔍</div>
        <div class="ico" id="p-icon-message" @click="$router.push('/messages')">💬</div>
      </div>
    </div>

    <!-- 个人信息：头像/昵称一行，右侧编辑资料与修改密码（v2 版式） -->
    <div class="banner" id="p-banner">
      <div class="av" id="p-avatar">{{ avatarText }}</div>
      <div class="who" id="p-who">
        <h2 id="p-name">{{ userInfo.nickname || '灵感爱好者' }}</h2>
        <div v-if="userInfo.city" id="p-city"><span class="tagchip">📍 {{ userInfo.city }}</span></div>
      </div>
      <div class="banner-ops" id="p-ops">
        <button id="p-btn-edit" @click="openProfileDialog">编辑资料</button>
        <button id="p-btn-pwd" @click="openPasswordDialog">修改密码</button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats" id="p-stats">
      <div
        class="stat"
        id="p-stat"
        :class="{ clickable: !!item.to }"
        v-for="item in statList"
        :key="item.id"
        @click="item.to && $router.push(item.to)"
      >
        <div class="n">{{ item.num }}</div>
        <div class="l">{{ item.label }}<template v-if="item.to"> ›</template></div>
      </div>
    </div>

    <button class="series-manage-entry" type="button" @click="$router.push('/series/manage')">
      <span class="series-manage-icon">📚</span>
      <span class="series-manage-copy">
        <b>我的系列</b>
      </span>
      <span class="series-manage-arrow">›</span>
    </button>

    <!-- 选项卡 -->
    <div class="tabs" id="p-tabs">
      <button :class="{ on: activeTab === 'published' }" @click="switchTab('published')">
        我的发布<span class="cnt">{{ pubTotal }}</span>
      </button>
      <button :class="{ on: activeTab === 'drafts' }" @click="switchTab('drafts')">
        我的草稿<span class="cnt">{{ draftTotal }}</span>
      </button>
        <button :class="{ on: activeTab === 'collects' }" @click="switchTab('collects')">
        我的收藏夹<span class="cnt">{{ collTotal }}</span>
      </button>
    </div>

    <div class="list">
      <AppState
        :state="personalState"
        :rows="4"
        empty-icon="✍️"
        :empty-text="personalEmptyText"
        error-text="内容加载失败"
        @retry="reloadActiveTab"
      >

      <!-- 我的发布 -->
      <template v-if="activeTab === 'published'">
        <!-- 虚拟列表：500 条也只渲染可视区那十几张卡片 -->
        <div
          v-if="publishedList.length"
          :ref="setPubContainer"
          class="virt-scroll"
          id="p-virt"
          @scroll.passive="onPubScroll"
        >
          <div class="virt-spacer" :style="{ height: (publishedList.length * PUB_ITEM_H) + 'px' }">
            <AppCard
              v-for="row in pubVisible"
              :key="row.data.id"
              class="card"
              padding="11px 12px"
              clickable
              :style="{ position: 'absolute', left: 0, right: 0, top: (row.index * PUB_ITEM_H) + 'px' }"
              @click="goDetail(row.data.id)"
            >
              <div class="item">
                <div class="thumb" :style="thumbFallbackStyle(row.index)">
                  <img
                    v-if="thumbUrl(row.data)"
                    :src="thumbUrl(row.data)"
                    alt=""
                    width="54"
                    height="54"
                    loading="lazy"
                    decoding="async"
                    fetchpriority="low"
                    @error="hideBrokenImage"
                  >
                </div>
                <div class="txt">
                  <div class="t">{{ row.data.title || '无标题' }}</div>
                  <div class="m">
                    <AppTag v-if="row.data.tag" class="tag" tone="neutral">{{ tagText(row.data.tag) }}</AppTag>
                    <span class="sep" v-if="row.data.tag">·</span>
                    <span>{{ formatTime(row.data.createTime) }}</span>
                    <span class="sep">·</span>
                    <span>👁 {{ row.data.viewCount || 0 }}</span>
                  </div>
                </div>
              </div>
            </AppCard>
          </div>
        </div>
        <div v-if="publishedList.length" class="virt-foot" id="p-virt-foot">
          <template v-if="pubHasMore">
            滚到底自动加载… 已显示 {{ publishedList.length }} / {{ pubTotal }} 条
            <div class="bar"><i :style="{ width: Math.round(publishedList.length / (pubTotal || 1) * 100) + '%' }"></i></div>
          </template>
          <template v-else>— 已经到底啦，共 {{ pubTotal }} 条 —</template>
        </div>
      </template>

      <!-- 我的草稿 -->
      <template v-else-if="activeTab === 'drafts'">
        <AppCard
          v-for="item in draftList"
          :key="item.id"
          class="draft"
          padding="13px 14px"
          clickable
          @click="$router.push('/edit/' + item.id)"
        >
          <div class="dt">{{ item.title || '无标题' }}</div>
          <div class="dm">
            <AppTag class="badge" tone="warning">草稿</AppTag>
            <AppTag v-if="item.tag" class="tag" tone="neutral">{{ tagText(item.tag) }}</AppTag>
            <span class="sep" v-if="item.tag">·</span>
            <span>{{ formatTime(item.createTime) }}</span>
          </div>
        </AppCard>
      </template>

      <!-- 我的收藏夹：先看文件夹，点进去才看该夹下的灵感 -->
      <template v-else>
        <div v-if="!activeFolder" class="folders" id="p-folders">
          <div class="grid-hint" id="p-folders-hint">
            {{ folders.length ? '点文件夹查看其中灵感' : '还没有收藏夹，先进入管理收藏夹创建' }}
          </div>
          <AppCard v-for="f in folders" :key="f.id" class="folder" id="p-folder"
               padding="14px 12px"
               clickable
               :class="{ on: String(activeFolder?.id) === String(f.id) }" @click="openFolder(f)">
            <div class="ico">{{ f.icon || '📁' }}</div>
            <div>
              <div class="fn">{{ f.name }}</div>
              <div class="fc">{{ folderCounts[f.id] || 0 }} 条灵感</div>
            </div>
          </AppCard>
          <AppCard class="folder add" id="p-folder-add" padding="14px 12px" clickable @click="$router.push('/collections')">
            <span class="plus">＋</span><span>管理收藏夹</span>
          </AppCard>
        </div>

        <template v-else>
          <div class="backrow">
            <span class="bk" @click="closeFolder">← 收藏夹</span>
            <span class="ttl">{{ activeFolder.icon }} {{ activeFolder.name }}</span>
            <span class="c">共 {{ folderTotal }} 条</span>
          </div>
          <div class="folder-list">
            <AppCard
              v-for="(item, idx) in folderCollects"
              :key="item.id"
              class="card"
              padding="11px 12px"
              clickable
              @click="goDetail(item.id)"
            >
              <div class="item">
                <div class="thumb" :style="thumbFallbackStyle(idx)">
                  <img
                    v-if="thumbUrl(item)"
                    :src="thumbUrl(item)"
                    alt=""
                    width="54"
                    height="54"
                    loading="lazy"
                    decoding="async"
                    fetchpriority="low"
                    @error="hideBrokenImage"
                  >
                </div>
                <div>
                  <div class="t">{{ item.title || '无标题' }}</div>
                  <div class="m">
                    <AppTag v-if="item.tag" class="tag" tone="neutral">{{ tagText(item.tag) }}</AppTag>
                    <span class="sep" v-if="item.tag">·</span>
                    <span class="uncollect" @click.stop="handleUncollect(item.id)">取消收藏</span>
                  </div>
                </div>
              </div>
            </AppCard>
            <div v-if="folderHasMore" class="virt-foot" @click="loadMoreFolder">
              {{ folderLoadingMore ? '加载中…' : `加载更多 · 已显示 ${folderCollects.length}/${folderTotal}` }}
            </div>
          </div>
        </template>
      </template>
      </AppState>
    </div>

    <!-- 分页（我的发布 / 我的草稿） -->
    <!-- 我的发布改成无限滚动，不再需要页码；草稿量小，保留分页 -->
    <template v-if="activeTab === 'drafts' && totalPages > 1">
      <div class="pager">
        <button :disabled="pagerPage <= 1" @click="goPage(pagerPage - 1)">‹</button>
        <button v-for="p in totalPages" :key="p" :class="{ on: p === pagerPage }" @click="goPage(p)">{{ p }}</button>
        <button :disabled="pagerPage >= totalPages" @click="goPage(pagerPage + 1)">›</button>
      </div>
      <div class="foot-note">共 {{ pagerTotal }} 条 · 第 {{ pagerPage }}/{{ totalPages }} 页</div>
    </template>
    <div class="foot-note" v-else-if="activeTab === 'collects' && !activeFolder && folders.length">
      共 {{ folders.length }} 个收藏夹
    </div>

    <!-- 主题切换：色值全在 styles/tokens.css 里，这里只切换 data-theme -->
    <div class="theme-row" id="p-theme">
      <span class="theme-label">主题</span>
      <button
        v-for="t in THEMES"
        :key="t.key"
        class="theme-chip"
        :class="{ on: currentTheme === t.key }"
        @click="applyTheme(t.key)"
      >{{ t.label }}</button>
    </div>
    <button class="logout" id="p-logout" @click="handleLogout">退出登录</button>

    <!-- 编辑资料对话框 -->
    <el-dialog :model-value="showProfileDialog" title="编辑资料" width="90%" append-to-body
               @update:model-value="setProfileDialog">
      <div class="dialog-form">
        <div class="form-row">
          <label>头像</label>
          <div class="avatar-grid">
            <div v-for="a in avatarList" :key="a" class="avatar-option"
                 :class="{ selected: editForm.avatar === a }" @click="editForm.avatar = a">{{ a }}</div>
          </div>
        </div>
        <div class="form-row">
          <label>昵称</label>
          <div class="nick-row">
            <el-input v-model="editForm.nickname" placeholder="输入昵称" maxlength="20" />
            <button class="dice" title="随机昵称" @click="editForm.nickname = randomNickname()">🎲</button>
          </div>
        </div>
        <div class="form-row">
          <label>城市</label>
          <el-cascader :model-value="cityPath" :options="cityOptions" placeholder="搜索或选择城市" clearable filterable
                       @update:model-value="setCityPath"
                       :props="{ expandTrigger: 'hover' }" style="width:100%" popper-class="city-popper" @change="onCityChange" />
          <div v-if="autoDetecting" class="detect-hint">⏳ 正在自动定位...</div>
          <div v-if="detectCity && !cityPath.length" class="detect-hint">📍 检测到您可能在
            <el-link type="primary" @click="applyDetectedCity">{{ detectCity }}</el-link>，点击使用</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="setProfileDialog(false)">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog :model-value="showPwdDialog" title="修改密码" width="90%" append-to-body
               @update:model-value="setPasswordDialog">
      <div class="dialog-form">
        <div class="form-row">
          <label>旧密码</label>
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="输入旧密码" show-password />
        </div>
        <div class="form-row">
          <label>新密码</label>
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="输入新密码" show-password />
        </div>
        <div class="form-row">
          <label>确认新密码</label>
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </div>
      </div>
      <template #footer>
        <el-button @click="setPasswordDialog(false)">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getFollowing, getMyCollects, getMyDrafts, getMyInspires, getUserInfo, uncollectInspire,} from '@/api/inspire.js'
import {cityOptions} from '@/utils/cityData.js'
import {randomNickname} from '@/utils/nickname.js'
import {useAuthStore} from '@/stores/auth'
import {usePersonalContent} from './personal/composables/usePersonalContent.js'
import {usePersonalProfile} from './personal/composables/usePersonalProfile.js'
import {useVisibilityRefresh} from '@/composables/useVisibilityRefresh.js'
import {applyTheme, currentTheme, THEMES} from '@/utils/theme.js'

const auth = useAuthStore()

const router = useRouter()
const userInfo = ref({})
const {
  publishedList,
  PUB_ITEM_H,
  pubVisible,
  onPubScroll,
  setPubContainer,
  draftList,
  collectList,
  loading,
  pageError,
  publishedError,
  draftError,
  foldersError,
  folderCollectsError,
  activeTab,
  pageSize,
  pubPage,
  pubTotal,
  draftPage,
  draftTotal,
  collPage,
  collTotal,
  folders,
  folderCounts,
  activeFolder,
  folderCollects,
  folderLoading,
  folderPage,
  folderTotal,
  folderHasMore,
  folderLoadingMore,
  personalEmptyText,
  personalState,
  reloadActiveTab,
  avatarText,
  pagerTotal,
  pagerPage,
  totalPages,
  goPage,
  goDetail,
  tagText,
  thumbUrl,
  thumbFallbackStyle,
  hideBrokenImage,
  PUB_PAGE_SIZE,
  pubLoadedPage,
  pubHasMore,
  pubLoading,
  pubCursor,
  loadPublished,
  loadDrafts,
  loadCollects,
  loadFolders,
  openFolder,
  loadMoreFolder
} = usePersonalContent({router, userInfo})

const statList = ref([
  { id: 1, num: 0, label: '总发布' },
  { id: 2, num: 0, label: '总收藏' },
  { id: 3, num: 0, label: '总浏览量' },
  // 新增：可点击跳「我的关注」
  { id: 4, num: 0, label: '关注', to: '/following' }
])

// 96 个可选头像：表情 / 动物 / 自然 / 食物 / 物件
const avatarList = [
  '😊','😀','😃','😄','😁','😆','😅','😂','🤣','😇','🙂','😉',
  '😍','🥰','😘','😋','😜','🤪','🤩','🥳','😎','🤠','🥺','😴',
  '🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🦁','🐮',
  '🐷','🐸','🐵','🐔','🐧','🐦','🦄','🐝','🐢','🐙','🦋','🐳',
  '🌈','🌸','🌺','🌻','🌼','🌷','🌵','🌴','🌿','🍀','🍁','🌙',
  '🍎','🍊','🍋','🍉','🍇','🍓','🍒','🥑','🍞','🧁','🍩','🍪',
  '🍰','🍜','🍵','🍺','🔥','⭐','🌟','✨','⚡','💡','💻','🎨',
  '🎬','📷','🎧','🎮','🏀','⚽','🎸','🚀','🛸','🎈','🎁','☕'
]
const {
  showProfileDialog,
  savingProfile,
  editForm,
  cityPath,
  autoDetecting,
  detectCity,
  onCityChange,
  detectLocation,
  applyDetectedCity,
  showPwdDialog,
  savingPwd,
  pwdForm,
  handleSaveProfile,
  handleChangePassword
} = usePersonalProfile(userInfo)

const openProfileDialog = () => { showProfileDialog.value = true }
const setProfileDialog = (value) => { showProfileDialog.value = value }
const openPasswordDialog = () => { showPwdDialog.value = true }
const setPasswordDialog = (value) => { showPwdDialog.value = value }
const setCityPath = (value) => { cityPath.value = value }
const closeFolder = () => { activeFolder.value = null }

onMounted(async () => {
  loading.value = true
  const foldersPromise = loadFolders()
  try {
    const [userRes, pubRes, draftRes, colRes, folRes] = await Promise.all([
      getUserInfo(), getMyInspires(1, PUB_PAGE_SIZE), getMyDrafts(1, pageSize.value),
      getMyCollects(1, pageSize.value),
      getFollowing().catch(() => ({ data: [] })), foldersPromise
    ])
    userInfo.value = userRes.data || {}
    if (userRes.data?.avatar) sessionStorage.setItem('userAvatar', userRes.data.avatar)
    publishedList.value = pubRes.data?.records || []
    pubTotal.value = pubRes.data?.total || 0
    pubLoadedPage.value = 1
    pubCursor.value = pubRes.data?.nextCursor || ''
    pubHasMore.value = pubRes.data?.hasMore !== undefined
      ? Boolean(pubRes.data.hasMore)
      : (pubRes.data?.records || []).length >= PUB_PAGE_SIZE
    draftList.value = draftRes.data?.records || []
    draftTotal.value = draftRes.data?.total || 0
    statList.value[3].num = (folRes.data || []).length
    collectList.value = colRes.data?.records || []
    collTotal.value = colRes.data?.total || 0
    statList.value[0].num = pubTotal.value
    statList.value[1].num = collTotal.value
    statList.value[2].num = publishedList.value.reduce((s, i) => s + (i.viewCount || 0), 0)
  } catch (e) {
    console.error(e)
    pageError.value = e?.message || 'load personal failed'
  }
  finally { loading.value = false }
  window.addEventListener('focus', refreshPersonalData)
})

let personalRefreshPromise = null
let lastPersonalRefreshAt = Date.now()
const PERSONAL_REFRESH_COOLDOWN = 15_000
const refreshPersonalData = () => {
  if (personalRefreshPromise || loading.value) return personalRefreshPromise
  if (Date.now() - lastPersonalRefreshAt < PERSONAL_REFRESH_COOLDOWN) {
    return Promise.resolve()
  }
  personalRefreshPromise = Promise.all([
    getMyInspires(1, PUB_PAGE_SIZE),
    getMyDrafts(1, pageSize.value),
    getMyCollects(1, pageSize.value),
    getFollowing().catch(() => ({ data: [] }))
  ]).then(([pubRes, draftRes, colRes, folRes]) => {
    if (pubRes?.data) {
      publishedList.value = pubRes.data.records || []
      pubTotal.value = pubRes.data.total || 0
      pubLoadedPage.value = 1
      pubCursor.value = pubRes.data.nextCursor || ''
      pubHasMore.value = pubRes.data.hasMore !== undefined
        ? Boolean(pubRes.data.hasMore)
        : (pubRes.data.records || []).length >= PUB_PAGE_SIZE
      statList.value[0].num = pubTotal.value
      statList.value[2].num = publishedList.value.reduce((sum, item) => sum + (item.viewCount || 0), 0)
    }
    if (draftRes?.data) {
      draftList.value = draftRes.data.records || []
      draftTotal.value = draftRes.data.total || 0
    }
    if (colRes?.data) {
      collTotal.value = colRes.data.total || 0
      statList.value[1].num = collTotal.value
    }
    if (folRes?.data) statList.value[3].num = folRes.data.length
    return loadFolders()
  }).catch(e => {
    console.error('[personal refresh]', e)
  }).finally(() => {
    lastPersonalRefreshAt = Date.now()
    personalRefreshPromise = null
  })
  return personalRefreshPromise
}

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshPersonalData)
})

useVisibilityRefresh(refreshPersonalData)

const switchTab = async (tab) => {
  activeTab.value = tab
  activeFolder.value = null          // 切 Tab 时回到收藏夹列表层
  folderCollects.value = []
  if (tab === 'published') {
    pubPage.value = 1
    await loadPublished(1)
  } else if (tab === 'collects') {
    // 收藏夹 Tab 改为展示「收藏夹列表 → 进入某个夹」
    await loadFolders()
  } else if (tab === 'drafts') {
    draftPage.value = 1
    await loadDrafts(1)
  }
}

const handleUncollect = async (id) => {
  try {
    await uncollectInspire(id)
    collectList.value = collectList.value.filter(i => i.id !== id)
    folderCollects.value = folderCollects.value.filter(i => i.id !== id)
    if (activeFolder.value) {
      folderCounts.value[activeFolder.value.id] = Math.max(0, (folderCounts.value[activeFolder.value.id] || 1) - 1)
    }
    collTotal.value = Math.max(0, collTotal.value - 1)
    statList.value[1].num = collTotal.value
    ElMessage.success('已取消收藏')
  } catch (e) { console.error(e) }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN')
}

const handleLogout = () => {
  sessionStorage.removeItem('token'); sessionStorage.removeItem('isLogin')
  sessionStorage.removeItem('userAccount'); sessionStorage.removeItem('userId')
  sessionStorage.removeItem('adminToken'); sessionStorage.removeItem('adminUser')
  auth.setLoggedOut()
  ElMessage.success('已退出登录'); router.push('/login')
}

</script>

<style scoped src="./personal/styles/personal.css"></style>
