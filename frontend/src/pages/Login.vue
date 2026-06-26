<template>
  <div class="login-page">
    <div class="top-nav">
      <div class="left-logo" @click="toggleHomeMenu">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="toggleSearchMenu">🔍</div>
        <div class="icon-item" @click="toggleUserMenu">👤</div>
      </div>
    </div>

    <div class="login-box">
      <div class="login-logo">💡</div>
      <h2 class="login-title">灵感集 登录</h2>
      <p class="login-desc">登录查看、收藏全部创意灵感</p>

      <div class="input-group">
        <el-input v-model="form.account" placeholder="请输入账号" clearable></el-input>
      </div>
      <div class="input-group">
        <el-input v-model="form.password" placeholder="请输入密码" show-password></el-input>
      </div>

      <div class="tip-row">
        <el-checkbox v-model="remember">记住账号</el-checkbox>
        <span class="forget-pwd">忘记密码</span>
      </div>

      <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">立即登录</el-button>

      <div class="register-tip">
        暂无账号？<span class="register-text" @click="$router.push('/register')">前往注册</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const route = useRoute()
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
const remember = ref(false)
const form = ref({ account: '', password: '' })

const handleLogin = async () => {
  if (!form.value.account.trim()) {
    ElMessage.warning('请输入账号')
    return
  }
  if (!form.value.password.trim()) {
    ElMessage.warning('请输入密码')
    return
  }
  loading.value = true
  setTimeout(() => {
    loading.value = false
    localStorage.setItem('isLogin', '1')
    localStorage.setItem('userAccount', form.value.account)
    ElMessage.success('登录成功')
    router.push('/')
  }, 1200)
}
</script>

<style scoped>
.login-page {
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
  padding: 8px 0 30px;
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
.login-box {
  background: #fff;
  border-radius: 20px;
  padding: 36px 24px;
}
.login-logo {
  font-size: 48px;
  text-align: center;
  margin-bottom: 12px;
}
.login-title {
  font-size: 24px;
  font-weight: 500;
  color: #1d1d1f;
  text-align: center;
  margin: 0 0 8px;
}
.login-desc {
  font-size: 14px;
  color: #86868b;
  text-align: center;
  margin: 0 0 32px;
}
.input-group {
  margin-bottom: 16px;
}
.tip-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0 28px;
  font-size: 14px;
}
.forget-pwd {
  color: #409eff;
  cursor: pointer;
}
.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 14px;
  font-size: 16px;
  background: #409eff;
  border: none;
}
.register-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #86868b;
}
.register-text {
  color: #409eff;
  cursor: pointer;
}
</style>