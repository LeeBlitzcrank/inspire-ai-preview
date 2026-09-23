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
        <h2 id="p-name">{{ userInfo.nickname || userInfo.username || '灵感爱好者' }}</h2>
        <div class="mail" id="p-account">@{{ userInfo.username || 'user' }}</div>
        <div v-if="userInfo.city" id="p-city"><span class="tagchip">📍 {{ userInfo.city }}</span></div>
      </div>
      <div class="banner-ops" id="p-ops">
        <button id="p-btn-edit" @click="showProfileDialog = true">编辑资料</button>
        <button id="p-btn-pwd" @click="showPwdDialog = true">修改密码</button>
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
                <div class="thumb" :style="thumbStyle(row.data, row.index)"></div>
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
            <span class="bk" @click="activeFolder = null">← 收藏夹</span>
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
                <div class="thumb" :style="thumbStyle(item, idx)"></div>
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
    <el-dialog v-model="showProfileDialog" title="编辑资料" width="90%" append-to-body>
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
          <el-cascader v-model="cityPath" :options="cityOptions" placeholder="搜索或选择城市" clearable filterable
                       :props="{ expandTrigger: 'hover' }" style="width:100%" popper-class="city-popper" @change="onCityChange" />
          <div v-if="autoDetecting" class="detect-hint">⏳ 正在自动定位...</div>
          <div v-if="detectCity && !cityPath.length" class="detect-hint">📍 检测到您可能在
            <el-link type="primary" @click="applyDetectedCity">{{ detectCity }}</el-link>，点击使用</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPwdDialog" title="修改密码" width="90%" append-to-body>
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
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {
  changePassword,
  getCollectFolders,
  getCollectListByFolder,
  getFollowing,
  getMyCollects,
  getMyDrafts,
  getMyInspires,
  getUserInfo,
  uncollectInspire,
  updateUserInfo
} from '@/api/inspire.js'
import {cityOptions, findCityPath} from '@/utils/cityData.js'
import {randomNickname} from '@/utils/nickname.js'
import {thumbOf} from '@/utils/media.js'
import {useAuthStore} from '@/stores/auth'
import {applyTheme, currentTheme, THEMES} from '@/utils/theme.js'

const auth = useAuthStore()

const router = useRouter()
const userInfo = ref({})
const publishedList = ref([])

// ===== 我的发布：虚拟列表 =====
// 500 条灵感如果全部渲染成 DOM，滚动到底部时浏览器要维护 5000+ 节点；
// 这里只渲染可视区 + 上下各 4 条的缓冲，滚动时复用同一批 DOM。
const PUB_ITEM_H = 86         // 卡片高 76 + 间距 10（与预览一致）
const PUB_OVERSCAN = 4
// 列表可视高度用固定常量，和 .virt-scroll 的 CSS 高度保持一致。
// 之前是运行时读 clientHeight，首次挂载时样式还没生效、量到 0，
// 结果只渲染 4 条（ceil(0/86)+4）——现在彻底不依赖测量。
const PUB_VIEW_H = 340
const pubScrollTop = ref(0)

const pubVisible = computed(() => {
  const total = publishedList.value.length
  if (!total) return []
  const start = Math.max(0, Math.floor(pubScrollTop.value / PUB_ITEM_H) - PUB_OVERSCAN)
  const end = Math.min(total, Math.ceil((pubScrollTop.value + PUB_VIEW_H) / PUB_ITEM_H) + PUB_OVERSCAN)
  const rows = []
  for (let i = start; i < end; i++) rows.push({ index: i, data: publishedList.value[i] })
  return rows
})

const onPubScroll = (e) => {
  pubScrollTop.value = e.target.scrollTop
  // 无限滚动：离底部还有 240px 就补下一页
  const el = e.target
  if (!pubLoading.value && pubHasMore.value &&
      el.scrollTop + el.clientHeight >= el.scrollHeight - 240) {
    loadPublished(false)
  }
}

const setPubContainer = (el) => {
  if (!el) return
  // 可视高度用常量 PUB_VIEW_H，这里只需要把滚动位置同步过来
  pubScrollTop.value = el.scrollTop
}
const draftList = ref([])
const collectList = ref([])
const loading = ref(false)
const pageError = ref('')
const publishedError = ref('')
const draftError = ref('')
const foldersError = ref('')
const folderCollectsError = ref('')
const activeTab = ref('published')

// 分页状态
const pageSize = ref(5)
const pubPage = ref(1); const pubTotal = ref(0)
const draftPage = ref(1); const draftTotal = ref(0)
const collPage = ref(1); const collTotal = ref(0)

// 收藏夹（对应 collect_folder 表）：先展示文件夹，点进去才看该夹下的灵感
const folders = ref([])
const folderCounts = ref({})
const activeFolder = ref(null)
const folderCollects = ref([])
const folderLoading = ref(false)
const folderPage = ref(1)
const folderTotal = ref(0)
const folderHasMore = ref(false)
const folderLoadingMore = ref(false)

const personalEmptyText = computed(() => {
  if (activeTab.value === 'published') return '还没有发布过灵感'
  if (activeTab.value === 'drafts') return '还没有草稿'
  return activeFolder.value ? '该收藏夹还没有灵感' : '还没有收藏夹'
})
const personalState = computed(() => {
  if (loading.value) return 'loading'
  if (pageError.value) return 'error'
  if (activeTab.value === 'published') {
    if (publishedError.value && !publishedList.value.length) return 'error'
    return publishedList.value.length ? 'ready' : 'empty'
  }
  if (activeTab.value === 'drafts') {
    if (draftError.value && !draftList.value.length) return 'error'
    return draftList.value.length ? 'ready' : 'empty'
  }
  if (activeFolder.value) {
    if (folderLoading.value) return 'loading'
    if (folderCollectsError.value && !folderCollects.value.length) return 'error'
    return folderCollects.value.length ? 'ready' : 'empty'
  }
  if (foldersError.value && !folders.value.length) return 'error'
  // 收藏夹为空时也要保留「管理收藏夹」入口，不能落进 empty 插槽把入口隐藏。
  return 'ready'
})
const reloadActiveTab = async () => {
  pageError.value = ''
  if (activeTab.value === 'published') {
    publishedError.value = ''
    await loadPublished(true)
  } else if (activeTab.value === 'drafts') {
    draftError.value = ''
    await loadDrafts(1)
  } else if (activeFolder.value) {
    folderCollectsError.value = ''
    await openFolder(activeFolder.value)
  } else {
    foldersError.value = ''
    await loadFolders()
  }
}

// Banner 头像：优先 emoji，否则取昵称首字
const avatarText = computed(() => {
  const a = userInfo.value.avatar
  if (a) return a
  const n = userInfo.value.nickname || userInfo.value.username
  return n ? n[0] : '👤'
})

// 分页（仅「我的发布」「我的草稿」使用）
const pagerTotal = computed(() => activeTab.value === 'drafts' ? draftTotal.value : pubTotal.value)
const pagerPage = computed(() => activeTab.value === 'drafts' ? draftPage.value : pubPage.value)
const totalPages = computed(() => Math.max(1, Math.ceil(pagerTotal.value / pageSize.value)))

const goPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  if (activeTab.value === 'drafts') loadDrafts(p)
  else loadPublished(p)
}

const goDetail = (id) => {
  if (id !== null && id !== undefined && String(id).trim()) {
    router.push({ name: 'InspireDetail', params: { id: String(id) } })
  }
}

// 标签补一个 emoji，和创建页的分类图标保持一致
const TAG_ICON = {
  '家居':'🏠','美食':'🍜','旅行':'🏕','摄影':'📷','穿搭':'👗',
  '运动':'🏃','文案':'✍️','电影':'🎬','生活':'🌿','手作':'🧶','其他':'✨'
}
const tagText = (t) => t ? ((TAG_ICON[t] ? TAG_ICON[t] + ' ' : '') + t) : ''

// 缩略图：有图用真图，没有就用渐变占位（与设计稿一致）
const GRADS = [
  'linear-gradient(135deg,#dbeafe,#ede9fe)',
  'linear-gradient(135deg,#fef3c7,#fde68a)',
  'linear-gradient(135deg,#dcfce7,#bbf7d0)',
  'linear-gradient(135deg,#cffafe,#a5f3fc)',
  'linear-gradient(135deg,#fee2e2,#fecaca)',
  'linear-gradient(135deg,#e0e7ff,#c7d2fe)'
]
const firstImage = (item) => {
  if (Array.isArray(item?.images) && item.images.length) return item.images[0]
  if (typeof item?.images === 'string' && item.images.trim()) {
    try {
      const arr = JSON.parse(item.images)
      if (Array.isArray(arr) && arr.length) return arr[0]
    } catch (e) { /* 非 JSON 就当作单张地址 */ }
    return item.images
  }
  return item?.img || ''
}
const thumbStyle = (item, idx) => {
  const url = firstImage(item)
  if (url) {
    const displayUrl = thumbOf(url, 200)
    return {
      backgroundImage: `url("${displayUrl}")`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return { background: GRADS[idx % GRADS.length] }
}

/**
 * 我的发布：改成「分批追加 + 无限滚动」，配合上面的虚拟列表。
 * reset=true 时回到第一页（切换 tab / 刷新），否则追加下一页。
 */
const PUB_PAGE_SIZE = 20
const pubLoadedPage = ref(0)
const pubHasMore = ref(true)
const pubLoading = ref(false)
const pubCursor = ref('')

const loadPublished = async (reset = false) => {
  if (pubLoading.value) return
  if (!reset && !pubHasMore.value) return
  pubLoading.value = true
  publishedError.value = ''
  const nextPage = reset ? 1 : pubLoadedPage.value + 1
  try {
    const json = await getMyInspires(nextPage, PUB_PAGE_SIZE, reset ? '' : pubCursor.value)
    if (json.code !== 200) throw new Error(json.msg || 'load published failed')
    const rows = json.data?.records || []
    publishedList.value = reset ? rows : [...publishedList.value, ...rows]
    if (reset || Number(json.data?.total || 0) > 0) pubTotal.value = json.data?.total || 0
    pubLoadedPage.value = nextPage
    pubCursor.value = json.data?.nextCursor || ''
    pubHasMore.value = json.data?.hasMore !== undefined
      ? Boolean(json.data.hasMore)
      : rows.length >= PUB_PAGE_SIZE
  } catch (e) {
    console.error(e)
    publishedError.value = e?.message || 'load published failed'
  } finally { pubLoading.value = false }
}

const loadDrafts = async (page) => {
  if (page !== undefined) draftPage.value = page
  draftError.value = ''
  try {
    const json = await getMyDrafts(draftPage.value, pageSize.value)
    if (json.code !== 200) throw new Error(json.msg || 'load drafts failed')
    draftList.value = json.data?.records || []
    draftTotal.value = json.data?.total || 0
  } catch (e) {
    console.error(e)
    draftError.value = e?.message || 'load drafts failed'
  }
}

const loadCollects = async (page) => {
  if (page !== undefined) collPage.value = page
  try {
    const json = await getMyCollects(collPage.value, pageSize.value)
    if (json.code !== 200) throw new Error(json.msg || 'load collects failed')
    collectList.value = json.data?.records || []
    collTotal.value = json.data?.total || 0
  } catch (e) { console.error(e) }
}

// 拉取收藏夹列表，并并行统计每个夹里的灵感条数
const loadFolders = async () => {
  foldersError.value = ''
  try {
    const res = await getCollectFolders()
    folders.value = res.data || []
    folderCounts.value = Object.fromEntries(
      folders.value.map(f => [f.id, Number(f.count || 0)])
    )
  } catch (e) {
    folders.value = []
    folderCounts.value = {}
    foldersError.value = e?.message || 'load folders failed'
  }
}

const FOLDER_PAGE_SIZE = 20

// 进入某个收藏夹：接口真正分页，首屏只拿 20 条
const openFolder = async (f, append = false) => {
  if (!append) {
    activeFolder.value = f
    folderCollects.value = []
    folderPage.value = 1
    folderTotal.value = 0
    folderHasMore.value = false
    folderLoading.value = true
  } else {
    folderLoadingMore.value = true
  }
  folderCollectsError.value = ''
  try {
    const res = await getCollectListByFolder(f.id, folderPage.value, FOLDER_PAGE_SIZE)
    const rows = res.data?.records || []
    folderCollects.value = append ? [...folderCollects.value, ...rows] : rows
    folderTotal.value = Number(res.data?.total || 0)
    folderHasMore.value = folderCollects.value.length < folderTotal.value
    if (folderHasMore.value) folderPage.value += 1
  } catch (e) {
    if (!append) folderCollects.value = []
    folderCollectsError.value = e?.message || 'load folder collects failed'
  } finally {
    folderLoading.value = false
    folderLoadingMore.value = false
  }
}

const loadMoreFolder = () => {
  if (activeFolder.value && folderHasMore.value && !folderLoadingMore.value) {
    openFolder(activeFolder.value, true)
  }
}

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
// 编辑资料
const showProfileDialog = ref(false)
const savingProfile = ref(false)
const editForm = ref({ nickname: '', city: '', avatar: '' })

// When dialog opens, populate form from userInfo
watch(showProfileDialog, (val) => {
  if (val) {
    editForm.value.nickname = userInfo.value.nickname || ''
    editForm.value.city = userInfo.value.city || ''
    editForm.value.avatar = userInfo.value.avatar || ''
    const existingPath = findCityPath(userInfo.value.city)
    cityPath.value = existingPath
    detectLocation()
    // 96 个头像要滚动，打开时主动定位到当前选中的那个（没有选中就回到顶部）
    nextTick(() => {
      const grid = document.querySelector('.avatar-grid')
      if (!grid) return
      const selected = grid.querySelector('.avatar-option.selected')
      if (selected) selected.scrollIntoView({ block: 'nearest' })
      else grid.scrollTop = 0
    })
  }
})
const cityPath = ref([])
const autoDetecting = ref(false)
const detectCity = ref('')

const onCityChange = (val) => {
  // val = ['浙江', '杭州'] 或 直辖市: ['北京', '北京']
  if (val && val.length >= 2) editForm.value.city = val[1]
  else if (val && val.length === 1) editForm.value.city = val[0]
  else editForm.value.city = ''
}

// 英文 -> 中文城市名映射
const enToCn = {
  'beijing': '北京', 'shanghai': '上海', 'tianjin': '天津', 'chongqing': '重庆',
  'hongkong': '香港', 'macau': '澳门', 'macao': '澳门', 'taipei': '台北', 'kaohsiung': '高雄', 'taichung': '台中', 'tainan': '台南', 'newtaipei': '新北', 'taoyuan': '桃园', 'keelung': '基隆',
  'guangzhou': '广州', 'shenzhen': '深圳', 'zhuhai': '珠海', 'shantou': '汕头', 'foshan': '佛山', 'shaoguan': '韶关', 'zhanjiang': '湛江', 'zhaoqing': '肇庆', 'jiangmen': '江门',
  'maoming': '茂名', 'huizhou': '惠州', 'meizhou': '梅州', 'shanwei': '汕尾', 'heyuan': '河源', 'yangjiang': '阳江', 'qingyuan': '清远', 'dongguan': '东莞', 'zhongshan': '中山',
  'chaozhou': '潮州', 'jieyang': '揭阳', 'yunfu': '云浮', 'hangzhou': '杭州', 'ningbo': '宁波', 'wenzhou': '温州', 'jiaxing': '嘉兴', 'huzhou': '湖州', 'shaoxing': '绍兴',
  'jinhua': '金华', 'quzhou': '衢州', 'zhoushan': '舟山', 'taizhou': '台州', 'lishui': '丽水', 'nanjing': '南京', 'wuxi': '无锡', 'xuzhou': '徐州', 'changzhou': '常州',
  'suzhou': '苏州', 'nantong': '南通', 'lianyungang': '连云港', 'huai_an': '淮安', 'yancheng': '盐城', 'yangzhou': '扬州', 'zhenjiang': '镇江', 'taizhou_j': '泰州', 'suqian': '宿迁',
  'jinan': '济南', 'qingdao': '青岛', 'zibo': '淄博', 'zaozhuang': '枣庄', 'dongying': '东营', 'yantai': '烟台', 'weifang': '潍坊', 'jining': '济宁', 'tai_an': '泰安',
  'weihai': '威海', 'rizhao': '日照', 'linyi': '临沂', 'dezhou': '德州', 'liaocheng': '聊城', 'binzhou': '滨州', 'heze': '菏泽', 'chengdu': '成都', 'zigong': '自贡', 'panzhihua': '攀枝花',
  'luzhou': '泸州', 'deyang': '德阳', 'mianyang': '绵阳', 'guangyuan': '广元', 'suining': '遂宁', 'neijiang': '内江', 'leshan': '乐山', 'nanchong': '南充', 'meishan': '眉山',
  'yibin': '宜宾', 'guang_an': '广安', 'dazhou': '达州', 'ya_an': '雅安', 'bazhong': '巴中', 'ziyang': '资阳', 'wuhan': '武汉', 'huangshi': '黄石', 'shiyan': '十堰', 'yichang': '宜昌',
  'xiangyang': '襄阳', 'ezhou': '鄂州', 'jingmen': '荆门', 'xiaogan': '孝感', 'jingzhou': '荆州', 'huanggang': '黄冈', 'xianning': '咸宁', 'suizhou': '随州', 'changsha': '长沙',
  'zhuzhou': '株洲', 'xiangtan': '湘潭', 'hengyang': '衡阳', 'shaoyang': '邵阳', 'yueyang': '岳阳', 'changde': '常德', 'zhangjiajie': '张家界', 'yiyang': '益阳', 'chenzhou': '郴州',
  'yongzhou': '永州', 'huaihua': '怀化', 'loudi': '娄底', 'fuzhou': '福州', 'xiamen': '厦门', 'putian': '莆田', 'sanming': '三明', 'quanzhou': '泉州', 'zhangzhou': '漳州',
  'nanping': '南平', 'longyan': '龙岩', 'ningde': '宁德', 'zhengzhou': '郑州', 'kaifeng': '开封', 'luoyang': '洛阳', 'pingdingshan': '平顶山', 'anyang': '安阳', 'hebi': '鹤壁',
  'xinxiang': '新乡', 'jiaozuo': '焦作', 'puyang': '濮阳', 'xuchang': '许昌', 'luohe': '漯河', 'sanmenxia': '三门峡', 'nanyang': '南阳', 'shangqiu': '商丘', 'xinyang': '信阳',
  'zhoukou': '周口', 'zhumadian': '驻马店', 'hefei': '合肥', 'wuhu': '芜湖', 'bengbu': '蚌埠', 'huainan': '淮南', 'ma_anshan': '马鞍山', 'huaibei': '淮北', 'tongling': '铜陵',
  'anqing': '安庆', 'huangshan': '黄山', 'chuzhou': '滁州', 'fuyang': '阜阳', 'suzhou_a': '宿州', 'lu_an': '六安', 'bozhou': '亳州', 'chizhou': '池州', 'xuancheng': '宣城',
  'shijiazhuang': '石家庄', 'tangshan': '唐山', 'qinhuangdao': '秦皇岛', 'handan': '邯郸', 'xingtai': '邢台', 'baoding': '保定', 'zhangjiakou': '张家口', 'chengde': '承德',
  'cangzhou': '沧州', 'langfang': '廊坊', 'hengshui': '衡水', 'xi_an': '西安', 'tongchuan': '铜川', 'baoji': '宝鸡', 'xianyang': '咸阳', 'weinan': '渭南', 'yan_an': '延安',
  'hanzhong': '汉中', 'yulin': '榆林', 'ankang': '安康', 'shangluo': '商洛', 'taiyuan': '太原', 'datong': '大同', 'yangquan': '阳泉', 'changzhi': '长治', 'jincheng': '晋城',
  'shuozhou': '朔州', 'jinzhong': '晋中', 'yuncheng': '运城', 'xinzhou': '忻州', 'linfen': '临汾', 'lvliang': '吕梁', 'shenyang': '沈阳', 'dalian': '大连', 'anshan': '鞍山',
  'fushun': '抚顺', 'benxi': '本溪', 'dandong': '丹东', 'jinzhou': '锦州', 'yingkou': '营口', 'fuxin': '阜新', 'liaoyang': '辽阳', 'panjin': '盘锦', 'tieling': '铁岭',
  'chaoyang': '朝阳', 'huludao': '葫芦岛', 'changchun': '长春', 'jilin': '吉林', 'siping': '四平', 'liaoyuan': '辽源', 'tonghua': '通化', 'baishan': '白山', 'songyuan': '松原',
  'baicheng': '白城', 'harbin': '哈尔滨', 'qiqihar': '齐齐哈尔', 'jixi': '鸡西', 'hegang': '鹤岗', 'shuangyashan': '双鸭山', 'daqing': '大庆', 'yichun': '伊春', 'jiamusi': '佳木斯',
  'qitaihe': '七台河', 'mudanjiang': '牡丹江', 'heihe': '黑河', 'suihua': '绥化', 'nanchang': '南昌', 'jingdezhen': '景德镇', 'pingxiang': '萍乡', 'jiujiang': '九江', 'xinyu': '新余',
  'yingtan': '鹰潭', 'ganzhou': '赣州', 'ji_an': '吉安', 'yichun_j': '宜春', 'fuzhou_j': '抚州', 'shangrao': '上饶', 'nanning': '南宁', 'liuzhou': '柳州', 'guilin': '桂林',
  'wuzhou': '梧州', 'beihai': '北海', 'fangchenggang': '防城港', 'qinzhou': '钦州', 'guigang': '贵港', 'yulin_g': '玉林', 'baise': '百色', 'hezhou': '贺州', 'hechi': '河池',
  'laibin': '来宾', 'chongzuo': '崇左', 'kunming': '昆明', 'qujing': '曲靖', 'yuxi': '玉溪', 'baoshan': '保山', 'zhaotong': '昭通', 'lijiang': '丽江', 'pu_er': '普洱', 'lincang': '临沧',
  'chuxiong': '楚雄', 'honghe': '红河', 'wenshan': '文山', 'xishuangbanna': '西双版纳', 'dali': '大理', 'dehong': '德宏', 'nujiang': '怒江', 'diqing': '迪庆',
  'guiyang': '贵阳', 'liupanshui': '六盘水', 'zunyi': '遵义', 'anshun': '安顺', 'bijie': '毕节', 'tongren': '铜仁', 'qianxinan': '黔西南', 'qiandongnan': '黔东南', 'qiannan': '黔南',
  'lanzhou': '兰州', 'jiayuguan': '嘉峪关', 'jinchang': '金昌', 'baiyin': '白银', 'tianshui': '天水', 'wuwei': '武威', 'zhangye': '张掖', 'pingliang': '平凉', 'jiuquan': '酒泉',
  'qingyang': '庆阳', 'dingxi': '定西', 'longnan': '陇南', 'hohhot': '呼和浩特', 'baotou': '包头', 'wuhai': '乌海', 'chifeng': '赤峰', 'tongliao': '通辽', 'ordos': '鄂尔多斯',
  'hulunbuir': '呼伦贝尔', 'binyan': '巴彦淖尔', 'wulanqab': '乌兰察布', 'urumqi': '乌鲁木齐', 'karamay': '克拉玛依', 'turpan': '吐鲁番', 'hami': '哈密',
  'haikou': '海口', 'sanya': '三亚', 'sansha': '三沙', 'danzhou': '儋州',
  'lhasa': '拉萨', 'shigatse': '日喀则', 'xining': '西宁', 'yinchuan': '银川', 'shizuishan': '石嘴山', 'wuzhong': '吴忠', 'guyuan': '固原', 'zhongwei': '中卫'
}
const toCn = (s) => enToCn[(s || '').toLowerCase().replace(/[\s'_-]/g, '')] || s

const detectLocation = async () => {
  autoDetecting.value = true
  try {
    const res = await fetch('/api/auth/ip-location', {
      headers: { 'Authorization': 'Bearer ' + (sessionStorage.getItem('token') || '') }
    })
    if (res.ok) {
      const body = await res.json()
      if (body && body.code === 200 && body.data) {
        const d = body.data
        const city = toCn(d.city || d.region)
        const path = findCityPath(city)
        if (path.length > 0) {
          cityPath.value = path
          onCityChange(path)
          detectCity.value = path.join(' / ')
        }
      }
    }
  } catch (e) {
    console.error(e)
  }
  finally { autoDetecting.value = false }
}

const applyDetectedCity = () => {
  if (cityPath.value.length > 0) {
    onCityChange(cityPath.value)
    detectCity.value = ''
  }
}

// 修改密码
const showPwdDialog = ref(false)
const savingPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

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
  document.addEventListener('visibilitychange', handleVisibilityRefresh)
})

let personalRefreshPromise = null
const refreshPersonalData = () => {
  if (personalRefreshPromise || loading.value) return personalRefreshPromise
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
    personalRefreshPromise = null
  })
  return personalRefreshPromise
}

const handleVisibilityRefresh = () => {
  if (!document.hidden) refreshPersonalData()
}

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshPersonalData)
  document.removeEventListener('visibilitychange', handleVisibilityRefresh)
})

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

const handleSaveProfile = async () => {
  savingProfile.value = true
  try {
    const res = await updateUserInfo(editForm.value)
    if (res.code === 200) {
      userInfo.value.nickname = editForm.value.nickname
      userInfo.value.city = editForm.value.city
      userInfo.value.avatar = editForm.value.avatar
      if (editForm.value.avatar) sessionStorage.setItem('userAvatar', editForm.value.avatar)
      if (editForm.value.nickname) sessionStorage.setItem('userAccount', editForm.value.nickname)
      ElMessage.success('资料已更新')
      showProfileDialog.value = false
    } else {
      ElMessage.error(res.msg || '资料更新失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '资料更新失败')
  }
  finally { savingProfile.value = false }
}

const handleChangePassword = async () => {
  if (!pwdForm.value.oldPassword) return ElMessage.warning('请输入旧密码')
  if (!pwdForm.value.newPassword) return ElMessage.warning('请输入新密码')
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) return ElMessage.warning('两次密码不一致')
  savingPwd.value = true
  try {
    const res = await changePassword(pwdForm.value)
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      showPwdDialog.value = false
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || '密码修改失败')
  }
  finally { savingPwd.value = false }
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

<style scoped>
/* ============ 方案 D：通栏 Banner + 薄荷绿 + 卡片（与「灵感录入」页统一） ============ */
.personal-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#f7fbfa; min-height:100vh; }

/* ---------- 顶部栏 ---------- */
.topbar { display:flex; align-items:center; justify-content:space-between; padding:0 4px 12px; }
.topbar .ico { width:38px; height:38px; border-radius:50%; background:#fff; display:flex; align-items:center;
  justify-content:center; font-size:17px; border:1px solid var(--c-primary-line); box-shadow:0 1px 5px rgba(0,0,0,.04);
  cursor:pointer; transition:.15s; }
.topbar .ico:hover { background:var(--c-primary-weak); }
.topbar .right { display:flex; gap:8px; }

/* ---------- 通栏 Banner ---------- */
/* v2：头像与昵称一行，右侧放两个操作按钮 */
.banner { display:flex; align-items:center; gap:12px; border-radius:18px; padding:16px;
  text-align:left; background:linear-gradient(160deg,#f3fbf9,#fff); border:1px solid var(--c-primary-line); }
.banner .av { width:60px; height:60px; flex:0 0 auto; border-radius:50%;
  background:#e6f2e4; color:#4f8a48; display:flex; align-items:center; justify-content:center;
  font-size:24px; font-weight:700; }
.banner .who { min-width:0; }
.banner h2 { margin:0; font-size:16.5px; font-weight:700; color:#134e4a; }
.banner .mail { margin-top:3px; font-size:12px; color:#3f7268; }
.banner .tagchip { display:inline-flex; align-items:center; gap:4px; margin-top:7px;
  padding:2px 9px; border-radius:999px; background:#f3f8f6; color:#d98200; font-size:11.5px; }
.banner-ops { margin-left:auto; display:flex; flex-direction:column; gap:6px; flex:0 0 auto; }
.banner-ops button { padding:6px 12px; border-radius:999px; border:1px solid var(--c-primary-line);
  background:#fff; color:var(--c-primary); font-size:12px; font-family:inherit; cursor:pointer; white-space:nowrap; }
.banner-ops button:hover { background:var(--c-primary-weak); }

/* ---------- 悬浮统计 ---------- */
.stats { position:relative; z-index:2; display:grid; grid-template-columns:repeat(4,1fr);
  margin:12px 8px 12px; padding:12px 6px; background:#fff; border-radius:16px;
  box-shadow:0 6px 16px rgba(30,90,80,.08); }
.stat { position:relative; text-align:center; }
.stat + .stat:before { content:""; position:absolute; left:0; top:8px; bottom:8px; width:1px; background:#e8f3f0; }
.stat .n { font-size:17px; font-weight:700; color:var(--c-primary); }
.stat .l { margin-top:3px; font-size:11px; color:#6b8b85; }

/* ---------- 我的系列入口 ---------- */
.series-manage-entry { width:100%; display:flex; align-items:center; gap:11px; margin:0 0 12px;
  padding:11px 13px; border:1px solid var(--c-primary-line); border-radius:15px; background:#fff;
  color:#28504a; font-family:inherit; text-align:left; cursor:pointer; transition:.15s; }
.series-manage-entry:hover { border-color:var(--c-primary-hover); background:#fbfefd; }
.series-manage-entry:active { transform:scale(.99); }
.series-manage-icon { width:36px; height:36px; flex:0 0 auto; display:grid; place-items:center;
  border-radius:11px; background:#eef7f5; font-size:18px; }
.series-manage-copy { min-width:0; }
.series-manage-copy b { display:block; font-size:13.5px; color:#28504a; }
.series-manage-arrow { margin-left:auto; color:#9cb0ac; font-size:22px; }

/* ---------- 快捷操作 ---------- */
.quick { display:grid; grid-template-columns:1fr 1fr; gap:10px; margin-bottom:12px; }
.quick .q { display:flex; align-items:center; gap:10px; padding:12px; background:#fff;
  border:1px solid var(--c-primary-line); border-radius:14px; font-size:13.5px; font-weight:500; color:#28504a;
  cursor:pointer; transition:.15s; }
.quick .q:hover { border-color:var(--c-primary-hover); background:#fbfefd; }
.quick .q i { width:30px; height:30px; border-radius:50%; background:#eef7f5; color:var(--c-primary);
  display:flex; align-items:center; justify-content:center; font-style:normal; }

/* ---------- 选项卡 ---------- */
.tabs { display:flex; gap:6px; margin-bottom:12px; padding:4px; background:var(--c-primary-soft); border-radius:12px; }
.tabs button { flex:1; padding:9px 0; border:none; border-radius:9px; background:transparent;
  font-family:inherit; font-size:13.5px; color:#5f807a; cursor:pointer; transition:.15s; }
.tabs button.on { background:#fff; color:var(--c-primary); font-weight:700; box-shadow:0 1px 4px rgba(0,0,0,.06); }
.tabs .cnt { margin-left:2px; font-size:11px; color:#9bb5b0; }
.tabs button.on .cnt { color:var(--c-primary); }

/* ---------- 列表卡片 ---------- */
.list { min-height:200px; }
/* 虚拟列表容器：列表自身滚动，只挂可视区卡片 */
.virt-scroll { height: 340px; overflow-y: auto; padding-right: 2px; }
.virt-foot { text-align:center; padding:10px 0; font-size:12px; color:var(--c-text-3); }
.virt-foot .bar { height:4px; border-radius:99px; background:var(--c-bg); overflow:hidden; margin-top:8px; }
.virt-foot .bar i { display:block; height:100%; background:var(--c-primary); transition:width .2s; }
.stat.clickable { cursor:pointer; }
.stat.clickable .l { color:var(--c-primary); }
.virt-spacer { position: relative; width: 100%; }
/* 卡片改成等高（76px，与预览一致），虚拟列表才能精确算偏移 */
.card { height:76px; margin-bottom:10px; box-sizing:border-box; overflow:hidden;
  border-radius:16px; cursor:pointer; transition:.16s; }
.item { align-items:center; }
.item .thumb { width:54px; height:54px; border-radius:11px; }
.item .t { font-size:13.5px; margin-bottom:5px; }
.item .m { font-size:11.5px; }
.item .txt { min-width: 0; }
.item .txt .t {
  display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;
}
.card:hover { border-color:var(--c-primary-hover); box-shadow:0 4px 14px rgba(30,90,80,.08); }
.card:active { transform:scale(.985); }
.item { display:flex; gap:12px; align-items:flex-start; }
.thumb { width:64px; height:64px; border-radius:12px; flex:0 0 auto; background:#f1f5f4; }
.item .t { margin-bottom:7px; font-size:14.5px; font-weight:600; line-height:1.45; color:var(--c-text); }
.item .m { display:flex; align-items:center; gap:6px; flex-wrap:wrap; font-size:12px; color:var(--c-text-3); }
.sep { color:#dfe6e4; }
.uncollect { color:#d98200; cursor:pointer; }
.uncollect:hover { text-decoration:underline; }

/* ---------- 草稿卡片 ---------- */
.draft { margin-bottom:10px; border-radius:14px; cursor:pointer; transition:.16s; }
.draft:hover { border-color:var(--c-primary-hover); box-shadow:0 4px 14px rgba(30,90,80,.08); }
.draft:active { transform:scale(.985); }
.draft .dt { margin-bottom:6px; font-size:14.5px; font-weight:600; line-height:1.45; color:var(--c-text); }
.draft .dm { display:flex; align-items:center; gap:6px; font-size:12px; color:var(--c-text-3); }

/* ---------- 收藏夹 ---------- */
.grid-hint { grid-column:1/-1; margin-bottom:2px; font-size:12px; color:var(--c-text-3); }
.folders { display:grid; grid-template-columns:1fr 1fr; gap:11px; }
.folder { display:flex; align-items:center; gap:10px; height:68px; box-sizing:border-box;
  border-radius:15px; cursor:pointer; transition:.16s; }
.folder:hover { border-color:var(--c-primary-hover); box-shadow:0 4px 14px rgba(30,90,80,.08); }
.folder:active { transform:scale(.985); }
.folder .ico { width:38px; height:38px; border-radius:11px; background:#f1f7f5; display:flex;
  align-items:center; justify-content:center; font-size:18px; flex:0 0 auto; }
.folder .fn { margin-bottom:2px; font-size:13px; font-weight:600; color:var(--c-text);
  overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.folder .fc { font-size:11.5px; color:var(--c-text-3); }
/* 与普通文件夹卡片严格等高：高度完全由 .folder 的 height 决定，这里不再单独设 min-height */
.folder.add { justify-content:center; gap:4px; border-style:dashed; color:#7b9891; font-size:12.5px; }
.folder.add .plus { font-size:22px; line-height:1; }

.backrow { display:flex; align-items:center; gap:8px; margin-bottom:12px; }
.backrow .bk { display:inline-flex; align-items:center; gap:4px; padding:4px 11px; background:#fff;
  border:1px solid var(--c-primary-line); border-radius:999px; font-size:12.5px; color:#4b7a72;
  cursor:pointer; transition:.15s; }
.backrow .bk:hover { border-color:var(--c-primary-hover); background:#fbfefd; }
.backrow .ttl { font-size:13.5px; font-weight:600; color:#28504a; }
.backrow .c { margin-left:auto; font-size:12px; color:var(--c-text-3); }

/* ---------- 分页 ---------- */
.pager { display:flex; justify-content:center; align-items:center; gap:6px; margin:18px 0 4px; }
.pager button { min-width:30px; height:30px; padding:0 6px; border:1px solid var(--c-primary-line); border-radius:9px;
  background:#fff; font-family:inherit; font-size:13px; color:#4b7a72; cursor:pointer; transition:.15s; }
.pager button:hover:not(:disabled) { border-color:var(--c-primary-hover); }
.pager button.on { background:var(--c-primary); border-color:var(--c-primary); color:#fff; font-weight:700; }
.pager button:disabled { opacity:.4; cursor:not-allowed; }
.foot-note { margin-top:6px; text-align:center; font-size:12.5px; color:#7b8a88; }

/* ---------- 空态 / 骨架 ---------- */
.empty { padding:48px 0; text-align:center; font-size:13.5px; color:#93a5a1; }
.s-line { height:14px; margin-bottom:12px; border-radius:8px;
  background:linear-gradient(90deg,#eef2f1 25%,#e4ecea 50%,#eef2f1 75%);
  background-size:200px 100%; animation:shimmer 1.5s infinite; }
.s-w-40 { width:40%; } .s-w-60 { width:60%; } .s-w-90 { width:90%; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }

/* ---------- 退出登录 ---------- */
/* 与预览一致的退出按钮：整条描边胶囊按钮，不再是纯文字链接 */
.theme-row { display:flex; align-items:center; gap:8px; margin:16px 0 4px; }
.theme-label { font-size:var(--fs-sm); color:var(--c-text-3); }
.theme-chip {
  padding:5px 12px; border:1px solid var(--c-primary-line); border-radius:var(--radius-pill);
  background:var(--c-surface); color:var(--c-text-2); font:inherit; font-size:var(--fs-xs); cursor:pointer;
}
.theme-chip.on { background:var(--c-primary); border-color:var(--c-primary); color:#fff; font-weight:600; }
.logout { display:block; width:100%; margin:18px 0 6px; height:40px;
  border-radius:12px; border:1px solid #f0dcd3; background:#fff;
  color:#c2613a; font-family:inherit; font-size:13.5px; cursor:pointer; transition:.15s; }
.logout:hover { background:#fff8f5; }

/* ---------- 弹窗表单 ---------- */
.dialog-form { padding:4px 0; }
.form-row { margin-bottom:16px; }
.form-row label { display:block; margin-bottom:6px; font-size:13px; color:var(--c-text-2); }
.nick-row { display:flex; gap:8px; }
.nick-row :deep(.el-input) { flex:1; }
.dice { width:38px; height:38px; flex:0 0 auto; border:1px solid var(--c-primary-line); border-radius:10px;
  background:#fbfefd; font-size:16px; cursor:pointer; transition:.15s; }
.dice:hover { border-color:var(--c-primary-hover); background:var(--c-primary-weak); }
.dice:active { transform:scale(.92); }
.detect-hint { margin-top:4px; font-size:12px; color:#909399; }

/* 96 个头像：8 列自适应 + 限高滚动，避免弹窗被撑过长 */
.avatar-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:6px;
  max-height:240px; overflow-y:auto; padding:2px 8px 2px 2px; }
.avatar-grid::-webkit-scrollbar { width:6px; }
.avatar-grid::-webkit-scrollbar-thumb { background:#d5e9e4; border-radius:3px; }
.avatar-option { width:100%; aspect-ratio:1; border-radius:50%; display:flex; align-items:center;
  justify-content:center; font-size:17px; line-height:1; cursor:pointer;
  border:2px solid transparent; transition:.15s; }
.avatar-option:hover { border-color:#cde9e3; }
.avatar-option.selected { border-color:var(--c-primary-hover); background:#eef7f5; }

:deep(.city-popper) { --el-cascader-menu-min-width: 100px; }
</style>
