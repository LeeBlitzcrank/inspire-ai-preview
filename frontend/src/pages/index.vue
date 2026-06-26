<template>
  <div id="index-root" class="index-page">
    <!-- 顶部导航栏 -->
    <div id="index-top-nav" class="top-nav">
      <div id="nav-logo" class="left-logo" @click="$router.push('/')">🍎</div>
      <div id="nav-icon-group" class="right-icons">
        <div id="icon-search" class="icon-item" @click="$router.push('/search')">🔍</div>
        <div id="icon-user" class="icon-item" @click="goPersonal">👤</div>
      </div>
    </div>

    <!-- 一级分类标题 -->
    <p id="category-main-title" class="all-title">全部灵感分类</p>

    <!-- 一级分类网格容器 无activeCategory时显示 -->
    <transition name="slide-fade">
      <div id="category-grid-wrap" class="category-grid" v-if="!activeCategory">
        <div
            class="category-card"
            v-for="(item, idx) in categoryList"
            :key="item.id"
            @click="handleClickCategory(item)"
            :style="{'--idx': idx}"
        >
          <div class="cate-icon">{{ item.icon }}</div>
          <div class="cate-name">{{ item.name }}</div>
          <div class="cate-num">{{ item.count }}条灵感</div>
        </div>
      </div>
    </transition>

    <!-- 二级子标签外层容器 activeCategory存在且未点二级才显示 -->
    <transition name="slide-fade">
      <div
          id="sub-tag-wrap"
          v-if="activeCategory && !activeSubItem"
          class="sub-wrap"
          :key="activeCategory"
      >
        <div id="back-to-category" class="back-btn" @click="resetCategory">← 返回全部分类</div>
        <div id="sub-tag-list" class="sub-tag-list">
          <div
              class="sub-tag"
              v-for="item in currentSubList"
              :key="item.id"
              @click="selectSubItem(item)"
          >
            {{ item.name }}
          </div>
        </div>
        <div id="sub-empty-tip" v-if="currentSubList.length === 0" class="empty-sub">暂无子分类</div>
      </div>
    </transition>

    <!-- 二级分类对应灵感列表容器 点击二级后显示 -->
    <transition name="slide-fade">
      <div id="detail-inspire-wrap" v-if="activeSubItem" class="detail-wrap" key="activeSubItem">
        <div id="back-to-subtag" class="back-btn" @click="resetSubItem">← 返回{{ activeCategory }}</div>
        <div id="inspire-card-list" class="list-wrap">
          <InspireCard
              v-for="(item, idx) in targetDetailList"
              :key="item.id"
              :item="item"
              :style="{'--idx': idx}"
              @collect="handleCollect"
          />
        </div>
        <div id="detail-empty-tip" v-if="targetDetailList.length === 0" class="empty-sub">暂无该分类灵感</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()

const goPersonal = () => {
  if (!localStorage.getItem('isLogin')) {
    ElMessage.warning('请先登录账号')
    router.push('/login')
    return
  }
  router.push('/personal')
}

const categoryList = ref([
  { id:1, name:'美食', icon:'🍜', count:268 },
  { id:2, name:'运动', icon:'🏃', count:135 },
  { id:3, name:'电影', icon:'🎬', count:96 },
  { id:4, name:'穿搭', icon:'👗', count:182 },
  { id:5, name:'文案', icon:'✍️', count:321 }
])

const subData = {
  美食: [{id:101,name:'鸡腿'},{id:102,name:'火锅'},{id:103,name:'烧烤'}],
  运动: [{id:201,name:'跑步'},{id:202,name:'健身'},{id:203,name:'篮球'}],
  电影: [{id:301,name:'悬疑片'},{id:302,name:'纪录片'},{id:303,name:'喜剧片'}],
  穿搭: [{id:401,name:'夏日穿搭'},{id:402,name:'通勤穿搭'}],
  文案: [{id:501,name:'短视频文案'},{id:502,name:'朋友圈文案'}]
}

const subHotData = {
  鸡腿: [
    {id:1,img:"https://picsum.photos/id/102/300/160",title:"家常鸡腿做法大全",content:"包含清炖、红烧、油炸、卤制多种做法，食材简单易获取，新手也能一次成功。",viewCount:1234,collectCount:233,heatScore:987},
  ],
  火锅: [
    {id:2,img:"https://picsum.photos/id/103/300/160",title:"家庭火锅底料搭配技巧",content:"番茄、牛油、清汤三种锅底调配，蘸料搭配完整方案。",viewCount:889,collectCount:156,heatScore:860},
  ],
  烧烤: [
    {id:3,img:"https://picsum.photos/id/104/300/160",title:"户外烧烤腌制配方",content:"荤素通用腌制料，火候把控小技巧，摆摊家用都合适。",viewCount:756,collectCount:122,heatScore:720},
  ],
  跑步: [
    {id:6,img:"https://picsum.photos/id/202/300/160",title:"科学跑步减脂计划",content:"不伤膝盖慢跑节奏，完整热身拉伸流程。",viewCount:668,collectCount:98,heatScore:820},
  ],
  健身: [
    {id:7,img:"https://picsum.photos/id/203/300/160",title:"居家无器械健身教程",content:"全套徒手塑形动作，早晚15分钟即可。",viewCount:992,collectCount:188,heatScore:910},
  ],
  篮球: [
    {id:8,img:"https://picsum.photos/id/204/300/160",title:"新手篮球基础技巧",content:"运球、投篮零基础入门教学。",viewCount:556,collectCount:76,heatScore:680},
  ],
  悬疑片: [
    {id:11,img:"https://picsum.photos/id/302/300/160",title:"高分悬疑电影推荐",content:"反转细腻、逻辑完整优质悬疑片合集。",viewCount:1123,collectCount:210,heatScore:940},
  ],
  纪录片: [
    {id:12,img:"https://picsum.photos/id/303/300/160",title:"人文治愈纪录片合集",content:"拓宽眼界、舒缓情绪优质纪实影片。",viewCount:665,collectCount:132,heatScore:780},
  ],
  喜剧片: [
    {id:13,img:"https://picsum.photos/id/304/300/160",title:"轻松解压喜剧片单",content:"无压抑剧情，适合休闲放松观看。",viewCount:886,collectCount:167,heatScore:850},
  ],
  夏日穿搭: [
    {id:16,img:"https://picsum.photos/id/402/300/160",title:"清爽夏日穿搭灵感",content:"低饱和简约搭配，学生通勤通用。",viewCount:988,collectCount:166,heatScore:880},
  ],
  通勤穿搭: [
    {id:17,img:"https://picsum.photos/id/403/300/160",title:"低调职场穿搭方案",content:"简约高级，日常上班不出错搭配。",viewCount:774,collectCount:124,heatScore:760},
  ],
  短视频文案: [
    {id:21,img:"https://picsum.photos/id/502/300/160",title:"短视频万能文案模板",content:"美食、生活、好物通用高完播文案。",viewCount:2241,collectCount:532,heatScore:990},
  ],
  朋友圈文案: [
    {id:22,img:"https://picsum.photos/id/503/300/160",title:"小众高级朋友圈短句",content:"温柔简约，告别土味文案。",viewCount:1567,collectCount:389,heatScore:930},
  ]
}

const activeCategory = ref('')
const activeSubItem = ref('')
const currentSubList = ref([])
const targetDetailList = ref([])

const handleClickCategory = (item) => {
  activeCategory.value = item.name
  activeSubItem.value = ''
  targetDetailList.value = []
  currentSubList.value = subData[item.name] ?? []
}

const selectSubItem = (item) => {
  activeSubItem.value = item.name
  targetDetailList.value = subHotData[item.name] ?? []
}

const resetCategory = () => {
  activeCategory.value = ''
  currentSubList.value = []
  activeSubItem.value = ''
  targetDetailList.value = []
}

const resetSubItem = () => {
  activeSubItem.value = ''
  targetDetailList.value = []
}

const getCollectList = () => localStorage.getItem('collectList') ? JSON.parse(localStorage.getItem('collectList')) : []
const handleCollect = (targetId) => {
  const list = getCollectList()
  if(list.includes(targetId)) return ElMessage.info('该灵感已收藏')
  list.push(targetId)
  localStorage.setItem('collectList', JSON.stringify(list))
  ElMessage.success('收藏成功，前往个人中心查看')
}
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
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 24px;
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
.all-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 20px;
}
.category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}
.category-card {
  --idx: 0;
  padding: 32px 14px;
  background: #ffffff;
  border-radius: 20px;
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
.category-card:active {
  transform: scale(0.96);
}
.category-card:hover {
  border-color: #e1e6f0;
  box-shadow: 0 6px 18px rgba(120,140,180,0.06);
  transform: translateY(-2px);
}
.cate-icon {
  font-size: 40px;
  margin-bottom: 14px;
}
.cate-name {
  font-size: 17px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 6px;
}
.cate-num {
  font-size: 13px;
  color: #86868b;
}
.back-btn {
  font-size: 15px;
  color: #409eff;
  cursor: pointer;
  margin: 24px 0 20px;
}
.back-btn:hover {
  color: #1d1d1f;
}
/* 二级标签列表改为和一级网格完全相同双列布局 */
#sub-tag-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
  margin-bottom: 24px;
}
/* 取消横向滚动相关样式 */
#sub-tag-list::-webkit-scrollbar {
  display: none;
}
.sub-tag {
  padding: 32px 14px;
  background: #ffffff;
  border-radius: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.26s ease;
  border: 1px solid #f0f3f9;
  font-size: 17px;
  font-weight: 500;
  color: #1d1d1f;
}
.sub-tag:active {
  transform: scale(0.96);
}
.sub-tag:hover {
  border-color: #e1e6f0;
  box-shadow: 0 6px 18px rgba(120,140,180,0.06);
  transform: translateY(-2px);
}
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
.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detail-wrap {
  margin-top: 8px;
}
.empty-sub {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 20px 0;
}
</style>