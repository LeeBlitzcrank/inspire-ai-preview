<template>
  <div class="login-page">
    <!-- 品牌头条区（薄荷清新） -->
    <div class="brand-band">
      <div class="top-nav">
        <div class="left-logo" @click="$router.push('/')">
          <svg viewBox="0 0 24 24" width="22" height="22">
            <path fill="#ffffff" d="M16.4 12.6c-.1-2.4 2-3.6 2.1-3.7-.9-1.3-2.3-1.5-2.8-1.5-1.2-.1-2.3.7-2.9.7-.6 0-1.5-.7-2.5-.7-1.3 0-2.5.8-3.2 2-1.4 2.4-.4 5.9 1 7.8.7 1 1.5 2.1 2.6 2.1 1 0 1.4-.7 2.6-.7 1.2 0 1.6.7 2.6.7 1.1 0 1.8-1 2.5-2 1.1-1.7 1.6-3.3 1.6-3.4-.1 0-2.4-.9-2.5-3.3zM14.4 5.6c.5-.6.8-1.4.7-2.2-.7 0-1.5.5-2 1.1-.4.5-.8 1.3-.7 2.1.8.1 1.6-.4 2-1z"/>
          </svg>
        </div>
        <div class="right-icons">
          <div class="icon-item search-icon" @click="$router.push('/search')">
            <svg viewBox="0 0 24 24" width="22" height="22">
              <path fill="#ffffff" d="M10 2a8 8 0 1 0 5.3 14l4.4 4.3 1.4-1.4-4.3-4.4A8 8 0 0 0 10 2zm0 2a6 6 0 1 1 0 12 6 6 0 0 1 0-12z"/>
            </svg>
          </div>
          <div class="icon-item user-icon" @click="$router.push('/register')">
            <svg viewBox="0 0 24 24" width="22" height="22">
              <path fill="#ffffff" d="M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-4 0-8 2-8 6v2h16v-2c0-4-4-6-8-6z"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="masthead">
        <div class="kicker">INSPIRE DAILY</div>
        <h1 class="main-title">灵感集</h1>
        <p class="tagline">把每天路过脑海的闪光，<br>收藏成属于你的灵感宇宙。</p>
      </div>
    </div>

    <!-- 登录表单卡 -->
    <div class="form-wrap">
      <div class="form-card">
        <p class="greeting">欢迎回来 👋</p>
        <p class="subtitle">登录继续你的创作</p>

        <div class="input-group">
          <label>账号</label>
          <el-input v-model="form.account" placeholder="请输入账号" clearable></el-input>
        </div>

        <div class="input-group">
          <label>密码</label>
          <el-input v-model="form.password" placeholder="请输入密码" show-password></el-input>
        </div>

        <div class="tip-row">
          <el-checkbox v-model="remember">记住账号</el-checkbox>
          <span class="forget-pwd" @click="goForgot">忘记密码</span>
        </div>

        <el-button class="login-btn" :loading="loading" @click="handleLogin">
          立即登录
        </el-button>

        <div class="register-tip">
          暂无账号？
          <span class="register-text" @click="$router.push('/register')">前往注册</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth.js'
import { setRememberMe } from '@/utils/tokenStorage.js'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const goForgot = () => router.push('/forgot-password')

const loading = ref(false)
const remember = ref(false)
const form = ref({ account: '', password: '' })

const handleLogin = async () => {
  if (!form.value.account.trim()) return ElMessage.warning('请输入账号')
  if (!form.value.password.trim()) return ElMessage.warning('请输入密码')

  loading.value = true
  try {
    // 必须在发起登录前设置记住账号状态，login() 内部才能按约定持久化 AccessToken。
    setRememberMe(remember.value)

    const res = await login({
      username: form.value.account,
      password: form.value.password
    })

    const data = res?.data || {}
    if (res?.code === 200 && data.accessToken) {
      // 先写本地登录信息，再更新 Pinia 状态，确保首页首次挂载时能同步读取到用户 ID。
      localStorage.removeItem('adminToken')
      localStorage.removeItem('adminUser')
      localStorage.setItem('token', data.accessToken)
      localStorage.setItem('isLogin', '1')
      localStorage.setItem('userAccount', data.username || form.value.account)
      localStorage.setItem('userNickname', data.nickname || data.username || form.value.account)
      if (data.avatar) localStorage.setItem('userAvatar', data.avatar)

      const userId = data.userId || parseJwtUserId(data.accessToken)
      if (userId) localStorage.setItem('userId', String(userId))

      auth.setLoggedIn()
      ElMessage.success('登录成功')

      const redirect = localStorage.getItem('redirectPath')
      localStorage.removeItem('redirectPath')
      await router.replace(redirect || '/')
    } else {
      ElMessage.error(res?.msg || '登录失败')
    }
  } catch (e) {
    // 错误已由 request.js 拦截器处理
    console.error('[登录异常]', e)
  } finally {
    loading.value = false
  }
}

const parseJwtUserId = (token) => {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
    return JSON.parse(atob(padded))?.sub || null
  } catch (e) {
    return null
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #ffffff;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

/* ========== 顶部品牌头条区 ========== */
.brand-band {
  position: relative;
  background: linear-gradient(140deg,#a8dcd2 0%,#d9f0eb 50%,#ffffff 100%);
  overflow: hidden;
  padding: 16px 22px 48px;
  width: 100%;
  box-sizing: border-box;
}
/* 柔和圆形装饰 */
.brand-band:before{
  content:"";
  position:absolute;
  right:-52px;
  top:-62px;
  width:170px;
  height:170px;
  border-radius:50%;
  border:32px solid rgba(255,255,255,.06);
}
.brand-band:after{
  content:"";
  position:absolute;
  right:16px;
  bottom:-38px;
  width:110px;
  height:110px;
  border-radius:50%;
  background:rgba(15, 118, 110,.14);
}
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}
.left-logo {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,.28);
  border: 1px solid rgba(255,255,255,.48);
  cursor: pointer;
  transition: transform .2s;
}
.left-logo:hover{transform: translateY(-2px)}
.brand {
  font-size: 19px;
  font-weight: 700;
  color: #0f4e49;
  cursor: pointer;
}
.right-icons {
  display: flex;
  gap: 8px;
}
.icon-item {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,.28);
  border: 1px solid rgba(255,255,255,.48);
  cursor: pointer;
  transition: transform .2s;
}
.icon-item:hover{transform: translateY(-2px)}

.masthead {
  position: relative;
  z-index: 2;
  padding: 12px 6px 0;
}
.kicker {
  font-size: 12px;
  letter-spacing: 3px;
  color: #0f766e;
  font-weight: 600;
}
.main-title {
  margin: 6px 0 8px;
  font-size: 34px;
  font-weight: 800;
  color: #111827;
  letter-spacing: 0;
}
.tagline {
  margin: 0;
  font-size: 14px;
  color: #5f857f;
  line-height: 1.7;
}

/* ========== 登录表单区 ========== */
.form-wrap {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
  padding: 0 22px 60px;
  margin-top: -22px;
  position: relative;
  box-sizing: border-box;
  padding-left: 16px;
  padding-right: 16px;
}
.form-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 28px 24px;
  box-shadow: 0 16px 40px rgba(15, 118, 110,.12);
  width: 100%;
  box-sizing: border-box;
  margin-left: 0;
  margin-right: 0;
}
.greeting {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 4px;
}
.subtitle {
  margin: 0 0 22px;
  font-size: 14px;
  color: #6b7280;
}
.input-group {
  margin-bottom: 18px;
}
.input-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}
::v-deep .el-input__wrapper {
  border-radius: 14px;
  box-shadow: 0 0 0 1.5px #d1fae5 inset !important;
  background-color: #ffffff;
  transition: all .2s;
}
::v-deep .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1.5px #34d399 inset, 0 4px 14px rgba(16, 185, 129,.12) !important;
  background-color: #ffffff;
}
::v-deep .el-input__inner {
  font-size: 15px;
  height: 46px;
}

.tip-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 22px;
  font-size: 14px;
}
.forget-pwd {
  color: #0f766e;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  background: #0f766e !important;
  color: #ffffff !important;
  box-shadow: 0 10px 22px rgba(15, 118, 110,.28);
  transition: transform .2s, box-shadow .2s;
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 118, 110,.32);
}

.register-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 15px;
  color: #6b7280;
}
.register-text {
  color: #0f766e;
  cursor: pointer;
  font-weight: 600;
}

@media (max-width: 480px) {
  .main-title {
    font-size: 28px;
  }
  .tagline {
    font-size: 12px;
  }
  .form-card {
    padding: 22px 18px;
  }
  .input-group {
    margin-bottom: 14px;
  }
}
</style>
