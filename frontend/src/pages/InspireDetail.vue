<template>
  <div id="detail-root" class="detail-page">
    <!-- 顶部导航 -->
    <div id="detail-top-nav" class="top-nav">
      <div id="detail-back-btn" class="left-logo" @click="$router.back()">←</div>
      <div id="detail-title">灵感详情</div>
      <div class="placeholder"></div>
    </div>

    <div v-if="detailInfo.id" id="detail-main-card" class="main-card">
      <div class="img-box">
        <img :src="detailInfo.img" alt="" />
      </div>
      <h2 class="title">{{ detailInfo.title }}</h2>
      <p class="desc">{{ detailInfo.content }}</p>
      <div class="stat-row">
        <span>浏览 {{ detailInfo.viewCount }}</span>
        <span>热度 {{ detailInfo.heatScore }}</span>
      </div>
      <div class="action-row">
        <el-button :type="isLike ? 'primary' : ''" icon="thumb-up" @click="handleLike">
          {{ isLike ? '已点赞' : '点赞' }} {{ likeNum }}
        </el-button>
        <el-button :type="isCollect ? 'primary' : ''" icon="star" @click="handleCollect">
          {{ isCollect ? '已收藏' : '收藏' }}
        </el-button>
      </div>
    </div>
    <div v-else class="loading-tip">页面加载中...</div>

    <div v-if="detailInfo.id" id="map-block" class="map-wrap">
      <h3 class="map-title">附近相关商铺（{{ mapKeyword }}）</h3>
      <div id="baidu-map" class="map-container">
        <div v-if="mapLoading" class="map-loading">地图加载中，请稍候...</div>
        <div v-if="mapError" class="map-error">{{ mapErrorMsg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const route = useRoute()
const router = useRouter()

const mapLoading = ref(true)
const mapError = ref(false)
const mapErrorMsg = ref('')
window._BMapGL = null
let mapInstance = null
let mapScriptDom = null
let pageDestroyed = false // 页面销毁标记，阻断DOM操作

const allHotData = {
  美食: [
    {id:1,img:"https://picsum.photos/id/102/300/160",title:"家常鸡腿做法大全",content:"包含清炖、红烧、油炸、卤制多种做法，食材简单易获取，新手也能一次成功。",viewCount:1234,collectCount:233,heatScore:987,tag:"美食"},
    {id:2,img:"https://picsum.photos/id/103/300/160",title:"家庭火锅底料搭配技巧",content:"番茄、牛油、清汤三种锅底调配，蘸料搭配完整方案。",viewCount:889,collectCount:156,heatScore:860,tag:"美食"},
  ],
  运动: [
    {id:6,img:"https://picsum.photos/id/202/300/160",title:"科学跑步减脂计划",content:"不伤膝盖慢跑节奏，完整热身拉伸流程。",viewCount:668,collectCount:98,heatScore:820,tag:"运动"},
  ],
  电影: [
    {id:11,img:"https://picsum.photos/id/302/300/160",title:"高分悬疑电影推荐",content:"反转细腻、逻辑完整优质悬疑片合集。",viewCount:1123,collectCount:210,heatScore:940,tag:"电影"},
  ],
  穿搭: [
    {id:16,img:"https://picsum.photos/id/402/300/160",title:"清爽夏日穿搭灵感",content:"低饱和简约搭配，学生通勤通用。",viewCount:988,collectCount:166,heatScore:880,tag:"穿搭"},
  ],
  文案: [
    {id:21,img:"https://picsum.photos/id/502/300/160",title:"短视频万能文案模板",content:"美食、生活、好物通用高完播文案。",viewCount:2241,collectCount:532,heatScore:990,tag:"文案"},
  ]
}
const allList = Object.values(allHotData).flat()

const detailInfo = ref({
  id: '',
  img: '',
  title: '',
  content: '',
  viewCount: 0,
  heatScore: 0,
  tag: ''
})
const mapKeyword = ref('')
const isLike = ref(false)
const isCollect = ref(false)
const likeNum = ref(0)

const getMapSearchWord = (title, tag) => {
  if (tag === '美食' || title.includes('鸡腿') || title.includes('火锅')) return '炸鸡店,火锅店'
  if (tag === '电影') return '电影院'
  if (tag === '运动') return '健身房,运动场馆'
  if (tag === '穿搭') return '服装店'
  if (tag === '文案') return '文创店'
  return '商铺'
}

window.baiduMapInitCallback = () => {
  window._BMapGL = window.BMapGL
}

const loadBaiduMap = () => {
  return new Promise((resolve, reject) => {
    // 页面已销毁，直接中断
    if (pageDestroyed) return reject('页面已关闭，终止地图加载')
    if (window._BMapGL) return resolve(window._BMapGL)
    // 复用已创建的script，不重复生成
    if (mapScriptDom) {
      const timer = setInterval(() => {
        if (pageDestroyed) {
          clearInterval(timer)
          reject('页面销毁')
        }
        if (window._BMapGL) {
          clearInterval(timer)
          resolve(window._BMapGL)
        }
      }, 100)
      setTimeout(() => {
        clearInterval(timer)
        reject('地图加载超时')
      }, 2000)
      return
    }
    mapScriptDom = document.createElement('script')
    mapScriptDom.src = 'https://api.map.baidu.com/api?v=1.0&type=webgl&ak=XXXXXXXXXX&callback=baiduMapInitCallback'
    mapScriptDom.onload = () => {
      const timer = setInterval(() => {
        if (pageDestroyed) {
          clearInterval(timer)
          reject('页面销毁')
        }
        if (window._BMapGL) {
          clearInterval(timer)
          resolve(window._BMapGL)
        }
      }, 100)
      setTimeout(() => {
        clearInterval(timer)
        reject('地图脚本加载超时')
      }, 2000)
    }
    mapScriptDom.onerror = () => reject('地图资源加载失败')
    // 插入前校验body存在
    if (document && document.body) {
      document.body.insertBefore(mapScriptDom, document.body.firstChild)
    } else {
      reject('DOM容器不存在')
    }
  })
}

const initMap = async () => {
  mapLoading.value = true
  mapError.value = false
  mapErrorMsg.value = ''
  try {
    const BMapGL = await loadBaiduMap()
    if (pageDestroyed) return
    if (!BMapGL || !BMapGL.Map) throw new Error('地图组件缺失')
    const mapDom = document.getElementById('baidu-map')
    if (!mapDom) throw new Error('地图容器DOM不存在')

    mapInstance = new BMapGL.Map(mapDom)
    const centerPoint = new BMapGL.Point(112.93, 28.23)
    mapInstance.centerAndZoom(centerPoint, 14)
    mapInstance.enableScrollWheelZoom(true)

    const localSearch = new BMapGL.LocalSearch(mapInstance, {
      renderOptions: { map: mapInstance, panel: null }
    })
    localSearch.search(mapKeyword.value)
    mapLoading.value = false
  } catch (err) {
    if (pageDestroyed) return
    console.error('地图异常：', err)
    mapLoading.value = false
    mapError.value = true
    mapErrorMsg.value = err.message
    ElMessage.error('地图加载异常：' + err.message)
  }
}

const getLocalState = (id) => {
  const likeArr = JSON.parse(localStorage.getItem('likeList') || '[]')
  const collectArr = JSON.parse(localStorage.getItem('collectList') || '[]')
  isLike.value = likeArr.includes(id)
  isCollect.value = collectArr.includes(id)
  likeNum.value = Number(localStorage.getItem(`like_${id}`)) || 0
}

const handleLike = () => {
  const id = detailInfo.value.id
  const likeArr = JSON.parse(localStorage.getItem('likeList') || '[]')
  if (isLike.value) {
    likeArr.splice(likeArr.indexOf(id), 1)
    likeNum.value--
    ElMessage('取消点赞')
  } else {
    likeArr.push(id)
    likeNum.value++
    ElMessage.success('点赞成功')
  }
  localStorage.setItem('likeList', JSON.stringify(likeArr))
  localStorage.setItem(`like_${id}`, likeNum.value)
  isLike.value = !isLike.value
}

const handleCollect = () => {
  const id = detailInfo.value.id
  const collectArr = JSON.parse(localStorage.getItem('collectList') || '[]')
  if (isCollect.value) {
    collectArr.splice(collectArr.indexOf(id), 1)
    ElMessage('取消收藏')
  } else {
    collectArr.push(id)
    ElMessage.success('收藏成功')
  }
  localStorage.setItem('collectList', JSON.stringify(collectArr))
  isCollect.value = !isCollect.value
}

watch(() => route.params.id, async (id) => {
  // 切换路由先销毁旧地图
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  const targetItem = allList.find(item => item.id == id)
  if (!targetItem) {
    ElMessage.error('该灵感不存在')
    return router.back()
  }
  detailInfo.value = targetItem
  mapKeyword.value = getMapSearchWord(targetItem.title, targetItem.tag)
  getLocalState(targetItem.id)
  await initMap()
}, { immediate: true })

// 页面卸载全局销毁所有地图相关资源
onUnmounted(() => {
  pageDestroyed = true
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.detail-page {
  width: 94%;
  max-width: 620px;
  margin: 0 auto;
  padding: 16px 0 80px;
  background: #fbfcfe;
  min-height: 100vh;
}
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0 24px;
}
.left-logo {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
  cursor: pointer;
  font-size: 20px;
}
#detail-title {
  font-size: 18px;
  font-weight: 600;
}
.placeholder {
  width: 40px;
}
.main-card {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
  margin-bottom: 30px;
}
.img-box {
  width: 100%;
  height: 200px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 16px;
}
.img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.title {
  font-size: 20px;
  margin: 0 0 12px;
  color: #1d1d1f;
}
.desc {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
}
.stat-row {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #888;
  margin-bottom: 20px;
}
.action-row {
  display: flex;
  gap: 16px;
}
.map-wrap {
  background: #fff;
  border-radius: 20px;
  padding: 20px;
}
.map-title {
  font-size: 17px;
  margin: 0 0 16px;
}
.map-container {
  width: 100%;
  height: 320px;
  border-radius: 12px;
  border: 1px solid #eee;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.map-loading {
  color: #666;
}
.map-error {
  color: #f56c6c;
  text-align: center;
  padding: 0 20px;
}
.loading-tip {
  text-align: center;
  padding: 60px 0;
  color: #999;
  font-size: 15px;
}
</style>