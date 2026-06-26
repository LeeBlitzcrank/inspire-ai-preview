<template>
  <div class="personal-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/personal')">👤</div>
      </div>
    </div>
    <div class="user-header">
      <div class="avatar">👤</div>
      <div class="user-info">
        <h2>灵感爱好者</h2>
        <p>收藏、管理你的全部创意灵感</p>
      </div>
    </div>
    <div class="stat-wrap">
      <div class="stat-card" v-for="item in statList" :key="item.id">
        <div class="stat-num">{{ item.num }}</div>
        <div class="stat-line"></div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>
    <div class="my-title">我的收藏灵感</div>
    <div class="list-wrap">
      <InspireCard v-for="item in myCollectList" :key="item.id" :item="item" @collect="cancelCollect" />
    </div>
    <div class="logout-wrap">
      <el-button type="text" class="logout-btn" @click="handleLogout">退出登录</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()
const allData = [
  {id:1,img:"https://picsum.photos/id/102/300/160",title:"家常鸡腿做法大全",content:"包含清炖、红烧、油炸、卤制多种做法，食材简单易获取，新手也能一次成功。",viewCount:1234,collectCount:233,heatScore:987},
  {id:2,img:"https://picsum.photos/id/103/300/160",title:"家庭火锅底料搭配技巧",content:"番茄、牛油、清汤三种锅底调配，蘸料搭配完整方案。",viewCount:889,collectCount:156,heatScore:860},
  {id:6,img:"https://picsum.photos/id/202/300/160",title:"科学跑步减脂计划",content:"不伤膝盖慢跑节奏，完整热身拉伸流程。",viewCount:668,collectCount:98,heatScore:820},
  {id:11,img:"https://picsum.photos/id/302/300/160",title:"高分悬疑电影推荐",content:"反转细腻、逻辑完整优质悬疑片合集。",viewCount:1123,collectCount:210,heatScore:940},
  {id:16,img:"https://picsum.photos/id/402/300/160",title:"清爽夏日穿搭灵感",content:"低饱和简约搭配，学生通勤通用。",viewCount:988,collectCount:166,heatScore:880},
  {id:21,img:"https://picsum.photos/id/502/300/160",title:"短视频万能文案模板",content:"美食、生活、好物通用高完播文案。",viewCount:2241,collectCount:532,heatScore:990},
]
const statList = ref([
  { id:1, num:0, label:'收藏灵感' },
  { id:2, num:5, label:'灵感分类' },
  { id:3, num:0, label:'总浏览量' },
])
const myCollectList = ref([])
const getCollectList = () => localStorage.getItem('collectList') ? JSON.parse(localStorage.getItem('collectList')) : []
const loadCollectData = () => {
  const ids = getCollectList()
  myCollectList.value = allData.filter(item => ids.includes(item.id))
  statList.value[0].num = myCollectList.value.length
  statList.value[2].num = myCollectList.value.reduce((sum, item) => sum + item.viewCount, 0)
}
const cancelCollect = (targetId) => {
  let list = getCollectList().filter(id => id !== targetId)
  localStorage.setItem('collectList', JSON.stringify(list))
  ElMessage.success('已取消收藏')
  loadCollectData()
}
const handleLogout = () => {
  localStorage.removeItem('isLogin')
  localStorage.removeItem('userAccount')
  ElMessage.success('已退出登录')
  router.push('/login')
}
onMounted(() => loadCollectData())
</script>
<style scoped>
.personal-page {
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
  padding: 8px 0 16px;
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
.user-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 30px;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
.user-info h2 {
  margin: 0;
  font-size: 20px;
  color: #1d1d1f;
}
.user-info p {
  margin: 4px 0 0;
  font-size: 14px;
  color: #86868b;
}
.stat-wrap {
  display: grid;
  grid-template-columns: repeat(3,1fr);
  gap: 12px;
  margin-bottom: 36px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 10px;
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 500;
  color: #1d1d1f;
}
.stat-line {
  width: 30px;
  height: 2px;
  background: #409eff;
  margin: 10px auto;
}
.stat-label {
  font-size: 13px;
  color: #86868b;
}
.my-title {
  font-size: 18px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 16px;
}
.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.logout-wrap {
  margin-top: 40px;
  text-align: center;
}
.logout-btn {
  color: #f56c6c;
  font-size: 14px;
}
</style>