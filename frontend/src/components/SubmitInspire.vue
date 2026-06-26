<template>
  <div class="index-page">
    <!-- 顶部导航栏：仅图标，右上角下拉菜单 -->
    <div class="nav-bar">
      <div class="nav-logo">灵感集</div>
      <div class="avatar-wrap" @click="showMenu = !showMenu">
        <span class="avatar-icon">👤</span>
        <!-- 下拉文字菜单 -->
        <div v-if="showMenu" class="dropdown-menu">
          <div class="menu-item" @click="goPersonal">个人中心</div>
          <div class="menu-item">我的收藏</div>
          <div class="menu-item">设置</div>
        </div>
      </div>
    </div>

    <!-- 灵感检索框（仅查询，无AI提交） -->
    <div class="search-box">
      <el-input
          v-model="searchText"
          placeholder="检索灵感关键词"
          @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">查询</el-button>
        </template>
      </el-input>
    </div>

    <!-- Apple风格单行热门标签自动轮播 -->
    <div class="carousel-wrap">
      <div class="carousel-scroll">
        <div
            class="carousel-item"
            v-for="slide in carouselList"
            :key="slide.id"
            @click="selectHotTag(slide)"
            :class="{active: currentHotTag.id === slide.id}"
        >
          {{ slide.name }}
        </div>
      </div>
    </div>

    <!-- 轮播下方：当前热门标签热度第一灵感 -->
    <div class="top-hot-card">
      <p class="hot-label">🔥 该分类最高热度灵感</p>
      <InspireCard :item="topHotItem" @refresh="getList" />
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <span @click="resetAll" class="root-tag">全部灵感</span>
      <span v-if="activeCategory" class="split">/</span>
      <span v-if="activeCategory" class="crumb-name">{{ activeCategory }}</span>
      <span v-if="activeSubItem" class="split">/</span>
      <span v-if="activeSubItem" class="crumb-name">{{ activeSubItem }}</span>
    </div>

    <!-- 一级分类区域 -->
    <transition name="slide-fade">
      <div v-if="!activeCategory" class="category-wrap">
        <div
            class="category-item"
            v-for="(item, idx) in categoryList"
            :key="item.id"
            @click="selectCategory(item)"
            :style="{'--idx': idx}"
        >
          <div class="item-icon">{{ item.icon }}</div>
          <div class="item-name">{{ item.name }}</div>
          <div class="item-count">{{ item.count }}条灵感</div>
        </div>
      </div>
    </transition>

    <!-- 二级标签区域 -->
    <transition name="slide-fade">
      <div v-if="activeCategory && !activeSubItem" class="sub-wrap" key="sub">
        <div class="back-btn" @click="resetCategory">← 返回全部分类</div>
        <div class="sub-tag-list">
          <div
              class="sub-tag"
              v-for="item in currentSubList"
              :key="item.id"
              @click="selectSubItem(item)"
          >
            {{ item.name }}
          </div>
        </div>
      </div>
    </transition>

    <!-- 灵感详情列表 -->
    <transition name="slide-fade">
      <div v-if="activeSubItem" class="detail-wrap" key="detail">
        <div class="back-btn" @click="resetSubItem">← 返回{{ activeCategory }}</div>
        <div class="list-wrap">
          <InspireCard
              v-for="(item, idx) in detailList"
              :key="item.id"
              :item="item"
              @refresh="getList"
              :style="{'--idx': idx}"
          />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
const router = useRouter()

// 导航下拉菜单状态
const showMenu = ref(false)
// 检索关键词
const searchText = ref('')
// 当前选中轮播热门标签
const currentHotTag = ref({ id:1, name:'美食' })
// 轮播热门标签数据
const carouselList = ref([
  { id:1, name:'美食' },
  { id:2, name:'运动' },
  { id:3, name:'电影' },
  { id:4, name:'穿搭' },
  { id:5, name:'文案' },
  { id:6, name:'甜品' },
  { id:7, name:'书单' },
])

// 层级状态
const activeCategory = ref('')
const activeSubItem = ref('')

// 一级分类
const categoryList = ref([
  { id:1, name:'美食', icon:'🍜', count:268 },
  { id:2, name:'运动', icon:'🏃', count:135 },
  { id:3, name:'电影', icon:'🎬', count:96 },
  { id:4, name:'穿搭', icon:'👗', count:182 },
  { id:5, name:'文案', icon:'✍️', count:321 }
])

// 二级分类
const subData = {
  美食: [{id:101,name:'鸡腿'},{id:102,name:'火锅'},{id:103,name:'烧烤'}],
  运动: [{id:201,name:'跑步'},{id:202,name:'健身'},{id:203,name:'篮球'}],
  电影: [{id:301,name:'悬疑片'},{id:302,name:'纪录片'},{id:303,name:'喜剧片'}],
  穿搭: [{id:401,name:'夏日穿搭'},{id:402,name:'通勤穿搭'}],
  文案: [{id:501,name:'短视频文案'},{id:502,name:'朋友圈文案'}]
}

// 详情内容
const detailData = {
  鸡腿: [{
    id:1,
    title:"家常鸡腿做法大全",
    content:"包含清炖、红烧、油炸、卤制多种做法，食材简单易获取，新手也能一次成功。",
    viewCount:1234,
    collectCount:233,
    heatScore:987
  }],
  火锅: [{
    id:2,
    title:"家庭火锅底料搭配技巧",
    content:"番茄、牛油、清汤三种锅底调配，蘸料搭配完整方案。",
    viewCount:889,
    collectCount:156,
    heatScore:660
  }],
  烧烤: [{
    id:3,
    title:"户外烧烤腌制配方",
    content:"荤素通用腌制料，火候把控小技巧，摆摊家用都合适。",
    viewCount:756,
    collectCount:122,
    heatScore:520
  }],
  跑步: [{id:4,title:"科学跑步减脂计划",content:"不伤膝盖慢跑节奏，完整热身拉伸流程。",viewCount:668,collectCount:98,heatScore:440}],
  健身: [{id:5,title:"居家无器械健身教程",content:"全套徒手塑形动作，早晚15分钟即可。",viewCount:992,collectCount:188,heatScore:710}],
  篮球: [{id:6,title:"新手篮球基础技巧",content:"运球、投篮零基础入门教学。",viewCount:556,collectCount:76,heatScore:380}],
  悬疑片: [{id:7,title:"高分悬疑电影推荐",content:"反转细腻、逻辑完整优质悬疑片合集。",viewCount:1123,collectCount:210,heatScore:890}],
  纪录片: [{id:8,title:"人文治愈纪录片合集",content:"拓宽眼界、舒缓情绪优质纪实影片。",viewCount:665,collectCount:132,heatScore:510}],
  喜剧片: [{id:9,title:"轻松解压喜剧片单",content:"无压抑剧情，适合休闲放松观看。",viewCount:886,collectCount:167,heatScore:620}],
  夏日穿搭: [{id:10,title:"清爽夏日穿搭灵感",content:"低饱和简约搭配，学生通勤通用。",viewCount:988,collectCount:166,heatScore:760}],
  通勤穿搭: [{id:11,title:"低调职场穿搭方案",content:"简约高级，日常上班不出错搭配。",viewCount:774,collectCount:124,heatScore:530}],
  短视频文案: [{id:12,title:"短视频万能文案模板",content:"美食、生活、好物通用高完播文案。",viewCount:2241,collectCount:532,heatScore:1250}],
  朋友圈文案: [{id:13,title:"小众高级朋友圈短句",content:"温柔简约，告别土味文案。",viewCount:1567,collectCount:389,heatScore:940}]
}

// 当前轮播标签下热度最高灵感
const topHotItem = ref(detailData['鸡腿'][0])
const currentSubList = ref([])
const detailList = ref([])

// 切换轮播热门标签，更新顶部最热卡片
const selectHotTag = (tag) => {
  currentHotTag.value = tag
  const subKey = tag.name
  const firstSub = subData[subKey][0].name
  topHotItem.value = detailData[firstSub][0]
}

// 检索查询逻辑
const handleSearch = () => {
  if (!searchText.value.trim()) return
  alert(`执行检索：${searchText.value}`)
}

// 跳转个人中心
const goPersonal = () => {
  router.push('/personal')
  showMenu.value = false
}

const selectCategory = (item) => {
  activeCategory.value = item.name
  currentSubList.value = subData[item.name]
}
const selectSubItem = (item) => {
  activeSubItem.value = item.name
  detailList.value = detailData[item.name] || []
}
const resetAll = () => {
  activeCategory.value = ''
  activeSubItem.value = ''
}
const resetCategory = () => activeCategory.value = ''
const resetSubItem = () => activeSubItem.value = ''

const getList = async () => {}
onMounted(getList)
</script>

<style scoped>
.index-page {
  width: 94%;
  max-width: 620px;
  margin: 0 auto;
  padding: 16px 0 80px;
  background: #fbfcfe;
  min-height: 100vh;
}

/* 顶部导航栏：仅图标，右上角下拉 */
.nav-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0 18px;
  position: relative;
}
.nav-logo {
  font-size: 20px;
  font-weight: 500;
  color: #1d1d1f;
}
.avatar-wrap {
  position: relative;
}
.avatar-icon {
  font-size: 24px;
  cursor: pointer;
}
.dropdown-menu {
  position: absolute;
  right: 0;
  top: 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  width: 120px;
  overflow: hidden;
  z-index: 99;
}
.menu-item {
  padding: 12px 16px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}
.menu-item:hover {
  background: #f4f7fd;
}

/* 检索框 */
.search-box {
  margin-bottom: 24px;
}

/* 单行自动轮播热门标签 */
.carousel-wrap {
  overflow: hidden;
  border-radius: 16px;
  background: #fff;
  padding: 14px 0;
  margin-bottom: 20px;
}
.carousel-scroll {
  display: flex;
  gap: 24px;
  animation: scrollLoop 22s linear infinite;
  width: max-content;
}
.carousel-item {
  flex-shrink: 0;
  padding: 6px 18px;
  font-size: 14px;
  color: #666;
  white-space: nowrap;
  cursor: pointer;
  border-radius: 30px;
  transition: 0.25s;
}
.carousel-item.active {
  background: #409eff;
  color: #fff;
}
@keyframes scrollLoop {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* 轮播下方最高热度灵感卡片 */
.top-hot-card {
  margin-bottom: 32px;
}
.hot-label {
  font-size: 14px;
  color: #ff7d00;
  margin: 0 0 10px;
}

/* 面包屑下划线动画 */
.breadcrumb {
  font-size: 14px;
  color: #86868b;
  margin-bottom: 26px;
}
.root-tag, .crumb-name {
  position: relative;
  color: #1d1d1f;
  cursor: pointer;
}
.root-tag::after, .crumb-name::after {
  content: "";
  position: absolute;
  left: 50%;
  right: 50%;
  bottom: -1px;
  height: 1px;
  background: #1d1d1f;
  transition: 0.3s ease;
}
.root-tag:hover::after, .crumb-name:hover::after {
  left: 0;
  right: 0;
}
.split {
  margin: 0 8px;
}

/* 页面切换滑入淡出 */
.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(14px);
}
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-14px);
}
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.28s ease-out;
}

/* 一级分类两栏卡片 */
.category-wrap {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.category-item {
  --idx: 0;
  padding: 30px 12px;
  background: #ffffff;
  border-radius: 18px;
  text-align: center;
  cursor: pointer;
  transition: all 0.26s ease;
  animation: fadeUp 0.4s ease forwards;
  animation-delay: calc(var(--idx) * 65ms);
  opacity: 0;
  border: 1px solid #f0f3f9;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.category-item:active {
  transform: scale(0.96);
}
.category-item:hover {
  border-color: #e1e6f0;
  box-shadow: 0 6px 18px rgba(120,140,180,0.06);
  transform: translateY(-2px);
}
.item-icon {
  font-size: 30px;
  margin-bottom: 12px;
}
.item-name {
  font-size: 16px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 4px;
}
.item-count {
  font-size: 12px;
  color: #86868b;
}

.back-btn {
  font-size: 14px;
  color: #515154;
  cursor: pointer;
  margin-bottom: 20px;
}
.back-btn:hover {
  color: #1d1d1f;
}

/* 二级标签 */
.sub-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}
.sub-tag {
  padding: 9px 20px;
  background: #f4f7fd;
  border-radius: 99px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: 0.24s;
}
.sub-tag:active {
  transform: scale(0.95);
}
.sub-tag:hover {
  background: #dde6fb;
}

.list-wrap {
  margin-top: 6px;
}
</style>