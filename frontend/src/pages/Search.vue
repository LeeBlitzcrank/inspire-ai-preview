<template>
  <div id="search-root" class="search-page">
    <!-- 顶部导航 -->
    <div id="search-top-nav" class="top-nav">
      <div id="search-nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="search-nav-icon-group" class="right-icons">
        <div id="search-icon-search" class="icon-item active" @click="$router.push('/search')">🔍</div>
        <div id="search-icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>

    <!-- 页面头部标题 -->
    <div id="search-page-head" class="page-head">
      <h2>灵感检索</h2>
      <p>搜索你感兴趣的灵感内容</p>
    </div>

    <!-- 搜索框区域 -->
    <div id="search-input-wrap" class="search-input-wrap">
      <el-input
          v-model="keyword"
          placeholder="输入关键词查找灵感"
          clearable
          size="large"
          @keyup.enter="doSearch"
      >
        <template #append>
          <el-button type="primary" size="large" @click="doSearch">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 热门分类轮播 -->
    <div id="search-tag-carousel" class="tag-carousel" @mouseenter="pauseAuto" @mouseleave="startAuto">
      <p class="sub-title">热门分类</p>
      <div class="tag-scroll">
        <div
            class="tag-item"
            v-for="slide in carouselList"
            :key="slide.id"
            @click="selectHotTag(slide)"
            :class="{active: currentHotTag.id === slide.id}"
        >
          {{ slide.name }}
        </div>
      </div>
    </div>

    <!-- 热门灵感卡片区块 -->
    <div id="search-hot-block" class="hot-block">
      <p class="hot-tip">🔥 {{ currentHotTag.name }}分类热门灵感</p>
      <div class="hot-card-list">
        <div class="hot-card" v-for="item in hotCardList" :key="item.id" @click="$router.push('/detail/' + item.id)">
          <div class="card-img">
            <img :src="item.img" alt="配图" />
          </div>
          <div class="card-content">
            <div class="card-top">
              <h3 class="card-title">{{ item.title }}</h3>
              <span class="heat">{{ item.heatScore }} 热度</span>
            </div>
            <p class="card-desc">{{ item.content }}</p>
            <div class="card-footer">
              <span>👁 {{ item.viewCount }}</span>
              <span>⭐ {{ item.collectCount }}</span>
              <el-button text size="small" @click.stop="handleCollect(item.id)">收藏</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div id="search-result-block" class="result-block" v-if="searched">
      <p class="result-tip">
        <template v-if="searchResult.length > 0">共找到 {{ searchResult.length }} 条匹配灵感</template>
        <template v-else>暂无匹配内容，请更换关键词</template>
      </p>
      <div id="inspire-card-list" class="result-list" v-if="searchResult.length > 0">
        <InspireCard
            v-for="item in searchResult"
            :key="item.id"
            :item="item"
            @click-card="goDetail"
            @collect="handleCollect"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()

// 跳转灵感详情页
const goDetail = (id) => {
  router.push({ name: 'InspireDetail', params: { id } })
}

const goPersonal = () => {
  if (!localStorage.getItem('isLogin')) {
    ElMessage.warning('请先登录账号')
    router.push('/login')
    return
  }
  router.push('/personal')
}

const carouselList = ref([
  { id:1, name:'美食' },
  { id:2, name:'运动' },
  { id:3, name:'电影' },
  { id:4, name:'穿搭' },
  { id:5, name:'文案' },
])
let autoTimer = null
const currentTagIndex = ref(0)
const currentHotTag = ref(carouselList.value[currentTagIndex.value])
const startAuto = () => {
  clearInterval(autoTimer)
  autoTimer = setInterval(() => {
    currentTagIndex.value = (currentTagIndex.value + 1) % carouselList.value.length
    currentHotTag.value = carouselList.value[currentTagIndex.value]
    hotCardList.value = allHotData[currentHotTag.value.name]
  }, 4000)
}
const pauseAuto = () => clearInterval(autoTimer)
const selectHotTag = (tag) => {
  clearInterval(autoTimer)
  currentHotTag.value = tag
  hotCardList.value = allHotData[tag.name]
  setTimeout(() => startAuto(), 2000)
}

const allHotData = {
  美食: [
    {id:1,img:"https://picsum.photos/id/102/300/160",title:"家常鸡腿做法大全",content:"包含清炖、红烧、油炸、卤制多种做法，食材简单易获取，新手也能一次成功。",viewCount:1234,collectCount:233,heatScore:987},
    {id:2,img:"https://picsum.photos/id/103/300/160",title:"家庭火锅底料搭配技巧",content:"番茄、牛油、清汤三种锅底调配，蘸料搭配完整方案。",viewCount:889,collectCount:156,heatScore:860},
  ],
  运动: [
    {id:6,img:"https://picsum.photos/id/202/300/160",title:"科学跑步减脂计划",content:"不伤膝盖慢跑节奏，完整热身拉伸流程。",viewCount:668,collectCount:98,heatScore:820},
  ],
  电影: [
    {id:11,img:"https://picsum.photos/id/302/300/160",title:"高分悬疑电影推荐",content:"反转细腻、逻辑完整优质悬疑片合集。",viewCount:1123,collectCount:210,heatScore:940},
  ],
  穿搭: [
    {id:16,img:"https://picsum.photos/id/402/300/160",title:"清爽夏日穿搭灵感",content:"低饱和简约搭配，学生通勤通用。",viewCount:988,collectCount:166,heatScore:880},
  ],
  文案: [
    {id:21,img:"https://picsum.photos/id/502/300/160",title:"短视频万能文案模板",content:"美食、生活、好物通用高完播文案。",viewCount:2241,collectCount:532,heatScore:990},
  ]
}
const hotCardList = ref(allHotData[currentHotTag.value.name])

const keyword = ref('')
const searched = ref(false)
const searchResult = ref([])
const allData = Object.values(allHotData).flat()
const doSearch = () => {
  if (!keyword.value.trim()) return ElMessage.warning('请输入搜索关键词')
  searched.value = true
  const key = keyword.value.toLowerCase()
  searchResult.value = allData.filter(item =>
      item.title.toLowerCase().includes(key) || item.content.toLowerCase().includes(key)
  )
}

const getCollectList = () => localStorage.getItem('collectList') ? JSON.parse(localStorage.getItem('collectList')) : []
const handleCollect = (targetId) => {
  const list = getCollectList()
  if(list.includes(targetId)) return ElMessage.info('该灵感已收藏')
  list.push(targetId)
  localStorage.setItem('collectList', JSON.stringify(list))
  ElMessage.success('收藏成功')
}

onMounted(() => startAuto())
onBeforeUnmount(() => clearInterval(autoTimer))
</script>

<style scoped>
.search-page {
  width: 94%;
  max-width: 620px;
  margin: 0 auto;
  padding: 16px 0 80px;
  background: #fbfcfe;
  min-height: 100vh;
}
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 28px;
}
.left-logo {
  font-size: 26px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
}
.right-icons {
  display: flex;
  gap: 20px;
}
.icon-item {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
}
.icon-item.active {
  background: #409eff;
  color: #fff;
}
.page-head {
  text-align: center;
  margin-bottom: 28px;
}
.page-head h2 {
  font-size: 26px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px;
}
.page-head p {
  font-size: 14px;
  color: #86868b;
}
.search-input-wrap {
  margin-bottom: 32px;
}
.tag-carousel {
  background: #fff;
  border-radius: 20px;
  padding: 20px 16px;
  margin-bottom: 24px;
}
.sub-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d1d1f;
  margin: 0 0 16px;
}
.tag-scroll {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.tag-scroll::-webkit-scrollbar {
  height: 4px;
}
.tag-item {
  flex-shrink: 0;
  padding: 8px 20px;
  font-size: 14px;
  color: #666;
  background: #f7f8fa;
  border-radius: 30px;
  cursor: pointer;
  transition: 0.25s;
}
.tag-item.active {
  background: #409eff;
  color: #fff;
}
.hot-block {
  background: #fff;
  border-radius: 20px;
  padding: 20px 16px;
  margin-bottom: 32px;
}
.hot-tip {
  font-size: 16px;
  font-weight: 500;
  color: #ff7d00;
  margin: 0 0 18px;
}
.hot-card-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.hot-card {
  display: flex;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}
.hot-card:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.card-img {
  width: 130px;
  height: 86px;
  flex-shrink: 0;
  border-radius: 14px;
  overflow: hidden;
}
.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}
.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d1d1f;
  margin: 0;
}
.heat {
  font-size: 13px;
  color: #ff7d00;
}
.card-desc {
  font-size: 13px;
  color: #6e6e73;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}
.card-footer {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #86868b;
  align-items: center;
  margin-top: 10px;
}
.result-block {
  background: #fff;
  border-radius: 20px;
  padding: 20px 16px;
}
.result-tip {
  font-size: 15px;
  color: #409eff;
  margin: 0 0 18px;
}
.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>