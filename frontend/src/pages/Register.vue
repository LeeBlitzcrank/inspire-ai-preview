<template>
  <div class="register-page">
    <div class="top-nav">
      <div class="left-logo" @click="toggleHomeMenu">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="toggleSearchMenu">🔍</div>
        <div class="icon-item" @click="toggleUserMenu">👤</div>
      </div>
    </div>

    <div class="form-box">
      <h2 class="title">注册灵感账号</h2>
      <p class="desc">注册后可录入、收藏全部创意灵感</p>

      <div class="input-group">
        <el-input v-model="form.account" placeholder="请设置账号"></el-input>
      </div>
      <div class="input-group">
        <el-input v-model="form.pwd" placeholder="请设置密码" show-password></el-input>
      </div>
      <div class="input-group">
        <el-input v-model="form.rePwd" placeholder="请确认密码" show-password></el-input>
      </div>

      <el-button class="reg-btn" type="primary" :loading="loading" @click="handleRegister">立即注册</el-button>
      <div class="tip">已有账号？<span class="link" @click="$router.push('/login')">前往登录</span></div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()

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
const form = ref({ account: '', pwd: '', rePwd: '' })

const handleRegister = () => {
  const { account, pwd, rePwd } = form.value
  if (!account) return ElMessage.warning('请输入账号')
  if (!pwd) return ElMessage.warning('请输入密码')
  if (pwd !== rePwd) return ElMessage.warning('两次密码不一致')
  loading.value = true
  setTimeout(() => {
    loading.value = false
    localStorage.setItem('isLogin', '1')
    localStorage.setItem('userAccount', account)
    ElMessage.success('注册成功，自动登录')
    router.push('/')
  }, 1000)
}
</script>

<style scoped>
.register-page {
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
.form-box {
  background: #fff;
  border-radius: 20px;
  padding: 36px 24px;
  margin-top: 30px;
}
.title {
  font-size: 24px;
  font-weight: 500;
  text-align: center;
  margin: 0 0 8px;
  color: #1d1d1f;
}
.desc {
  text-align: center;
  font-size: 14px;
  color: #86868b;
  margin-bottom: 32px;
}
.input-group {
  margin-bottom: 16px;
}
.reg-btn {
  width: 100%;
  height: 46px;
  border-radius: 14px;
  font-size: 16px;
  background: #409eff;
  border: none;
}
.tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #86868b;
}
.link {
  color: #409eff;
  cursor: pointer;
}
</style>