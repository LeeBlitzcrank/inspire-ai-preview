<template>
  <div class="create-page">
    <!-- 统一左右导航 -->
    <div class="top-nav">
      <div class="left-logo" @click="toggleHomeMenu">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="toggleSearchMenu">🔍</div>
        <div class="icon-item" @click="toggleUserMenu">👤</div>
      </div>
    </div>

    <div class="input-box">
      <h2 class="page-title">录入新灵感</h2>
      <p class="page-desc">填写创意内容，保存到灵感库</p>

      <div class="row">
        <label>灵感标题</label>
        <el-input v-model="form.title" placeholder="输入简短标题"></el-input>
      </div>
      <div class="row">
        <label>所属分类</label>
        <el-select v-model="form.category" placeholder="选择分类" style="width:100%">
          <el-option label="美食" value="美食" />
          <el-option label="运动" value="运动" />
          <el-option label="电影" value="电影" />
          <el-option label="穿搭" value="穿搭" />
          <el-option label="文案" value="文案" />
        </el-select>
      </div>
      <div class="row">
        <label>灵感详情</label>
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="详细描述你的创意"></el-input>
      </div>

      <el-button class="submit-btn" type="primary" :loading="loading" @click="submitInspire">保存灵感</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()

// 下拉菜单
const showHomeMenu = ref(false)
const showSearchMenu = ref(false)
const showUserMenu = ref(false)
const toggleHomeMenu = () => {
  showHomeMenu.value = !showHomeMenu.value
  showSearchMenu.value = false
  showUserMenu.value = false
}
const toggleSearchMenu = () => {
  showSearchMenu.value = !showSearchMenu.value
  showHomeMenu.value = false
  showUserMenu.value = false
}
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
  showHomeMenu.value = false
  showSearchMenu.value = false
}

const loading = ref(false)
const form = ref({ title: '', category: '', content: '' })

const submitInspire = () => {
  const { title, category, content } = form.value
  if (!title) return ElMessage.warning('请填写标题')
  if (!category) return ElMessage.warning('请选择分类')
  if (!content) return ElMessage.warning('请填写灵感详情')
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('灵感录入成功')
    router.push('/')
  }, 1000)
}
</script>

<style scoped>
.create-page {
  width: 94%;
  max-width: 620px;
  margin: 0 auto;
  padding: 16px 0 80px;
  background: #fbfcfe;
  min-height: 100vh;
}
/* 导航通用样式和注册页完全一致，复用 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0 16px;
  position: relative;
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
.slide-down-enter-from, .slide-down-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
.slide-down-enter-active, .slide-down-leave-active {
  transition: all 0.24s ease;
}
.home-dropdown {
  background: #fff;
  border-radius: 12px;
  padding: 8px 0;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
  margin-bottom: 16px;
}
.right-dropdown {
  position: absolute;
  top: 54px;
  background: #fff;
  border-radius: 12px;
  padding: 8px 0;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
  z-index: 99;
}
.right-drop-search {
  right: 54px;
  width: 110px;
}
.right-drop-user {
  right: 0;
  width: 120px;
}
.menu-item {
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
}
.menu-item:hover {
  background: #f4f7fd;
}
/* 录入表单 */
.input-box {
  background: #fff;
  border-radius: 20px;
  padding: 30px 24px;
  margin-top: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 500;
  text-align: center;
  margin: 0 0 6px;
  color: #1d1d1f;
}
.page-desc {
  text-align: center;
  font-size: 14px;
  color: #86868b;
  margin-bottom: 28px;
}
.row {
  margin-bottom: 18px;
}
.row label {
  display: block;
  font-size: 14px;
  color: #1d1d1f;
  margin-bottom: 8px;
}
.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 14px;
  font-size: 16px;
  background: #409eff;
  border: none;
  margin-top: 10px;
}
</style>