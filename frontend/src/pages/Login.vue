<template>
  <div class="login-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="goRegister">👤</div>
      </div>
    </div>
    <div class="login-box">
      <div class="login-logo">💡</div>
      <h2 class="login-title">灵感集 登录</h2>
      <p class="login-desc">登录查看、收藏全部创意灵感</p>
      <div class="input-group"><el-input v-model="form.account" placeholder="请输入账号" clearable></el-input></div>
      <div class="input-group"><el-input v-model="form.password" placeholder="请输入密码" show-password></el-input></div>
      <div class="tip-row"><el-checkbox v-model="remember">记住账号</el-checkbox><span class="forget-pwd" @click="goForgot">忘记密码</span></div>
      <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">立即登录</el-button>
      <div class="register-tip">暂无账号？<span class="register-text" @click="$router.push('/register')">前往注册</span></div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/inspire.js'
const router = useRouter()
const goForgot = () => { router.push('/forgot-password') }
const loading = ref(false)
const remember = ref(false)
const form = ref({ account: '', password: '' })
const goRegister = () => { router.push('/register') }

const handleLogin = async () => {
  if (!form.value.account.trim()) return ElMessage.warning('请输入账号')
  if (!form.value.password.trim()) return ElMessage.warning('请输入密码')
  loading.value = true
  try {
    const res = await login({ username: form.value.account, password: form.value.password })
    if (res.code === 200) {
      // 清除可能残留的管理员 token，避免 adminToken 影响普通用户请求
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUser')
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('isLogin', '1')
      localStorage.setItem('userAccount', res.data.username)
      localStorage.setItem('userNickname', res.data.nickname)
      if (res.data.avatar) localStorage.setItem('userAvatar', res.data.avatar)
      const _payload = JSON.parse(atob(res.data.token.split('.')[1]))
      localStorage.setItem('userId', _payload.sub)
      ElMessage.success('登录成功')
      const redirect = localStorage.getItem('redirectPath')
      localStorage.removeItem('redirectPath')
      router.push(redirect || '/')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }
  } catch (e) {
    // 错误已由 request.js 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 30px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.login-box { background:#fff; border-radius:20px; padding:36px 24px; }
.login-logo { font-size:48px; text-align:center; margin-bottom:12px; }
.login-title { font-size:24px; font-weight:500; color:#1d1d1f; text-align:center; margin:0 0 8px; }
.login-desc { font-size:14px; color:#86868b; text-align:center; margin:0 0 32px; }
.input-group { margin-bottom:16px; }
.tip-row { display:flex; justify-content:space-between; align-items:center; margin:8px 0 28px; font-size:14px; }
.login-btn { width:100%; height:46px; border-radius:14px; font-size:16px; background:#409eff; border:none; }
.register-tip { text-align:center; margin-top:24px; font-size:14px; color:#86868b; }
.register-text { color:#409eff; cursor:pointer; }
</style>
