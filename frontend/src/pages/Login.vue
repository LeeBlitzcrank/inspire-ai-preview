<!--
  文件：frontend/src/pages/Login.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
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

        <div class="login-mode-tabs">
          <button
            type="button"
            :class="{ active: loginMode === 'password' }"
            @click="switchLoginMode('password')"
          >账号密码</button>
          <button
            type="button"
            :class="{ active: loginMode === 'sms' }"
            @click="switchLoginMode('sms')"
          >手机号验证码</button>
        </div>

        <template v-if="loginMode === 'password'">
          <div class="input-group">
            <label>账号</label>
            <el-input v-model="form.account" placeholder="请输入账号" maxlength="20" clearable></el-input>
          </div>

          <div class="input-group">
            <label>密码</label>
            <el-input v-model="form.password" placeholder="请输入密码" maxlength="128" show-password></el-input>
          </div>
        </template>

        <template v-else>
          <div class="input-group">
            <label>手机号</label>
            <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" clearable></el-input>
          </div>
          <div class="input-group">
            <label>短信验证码</label>
            <div class="sms-code-row">
              <el-input
                v-model="form.smsCode"
                placeholder="6位验证码"
                maxlength="6"
                @keyup.enter="handleLogin"
              ></el-input>
              <button
                class="sms-send-button"
                type="button"
                :disabled="smsCountdown > 0 || smsSending"
                @click="handleSendSms"
              >{{ smsButtonText }}</button>
            </div>
          </div>
          <div class="sms-tip">未注册手机号验证成功后将自动创建账号</div>
        </template>

        <div v-if="loginMode === 'password' && captchaRequired" class="input-group">
          <label>验证码</label>
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
              placeholder="请输入验证码"
              maxlength="4"
              @keyup.enter="handleLogin"
            ></el-input>
            <button class="captcha-image" type="button" title="点击刷新验证码" @click="loadCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码">
            </button>
          </div>
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

    <el-dialog v-model="forcePasswordDialog" title="请先升级密码" width="90%" :close-on-click-modal="false"
               :close-on-press-escape="false" :show-close="false" append-to-body>
      <div class="force-password-copy">
        当前账号使用的是历史上的弱密码。修改成功后需要使用新密码重新登录。
      </div>
      <div class="input-group">
        <label>新密码</label>
        <el-input v-model="forcePwdForm.newPassword" maxlength="64" show-password
                  placeholder="8-64位，不能使用纯数字" />
      </div>
      <div class="input-group">
        <label>确认新密码</label>
        <el-input v-model="forcePwdForm.confirmPassword" maxlength="64" show-password
                  placeholder="再次输入新密码" />
      </div>
      <template #footer>
        <el-button type="primary" :loading="forcePwdLoading" @click="handleForcePassword">
          修改并重新登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from '@/utils/uiFeedback.js'
import {getLoginCaptcha, login, loginBySms, sendSmsCode} from '@/api/auth.js'
import {clearAllTokens, setRememberMe} from '@/utils/tokenStorage.js'
import {useAuthStore} from '@/stores/auth'
import {changePassword} from '@/api/inspire.js'
import {
  clearSmsCooldown,
  getSmsCooldown,
  saveSmsCooldown,
  smsCooldownFromError,
  validatePassword
} from '@/utils/validation.js'

const router = useRouter()
const auth = useAuthStore()

const goForgot = () => router.push('/forgot-password')

const loading = ref(false)
const remember = ref(false)
const captchaRequired = ref(false)
const captchaImage = ref('')
const loginMode = ref('password')
const form = ref({
  account: '',
  password: '',
  captchaId: '',
  captchaCode: '',
  phone: '',
  smsCode: ''
})
const smsSending = ref(false)
const smsCountdown = ref(0)
const forcePasswordDialog = ref(false)
const forcePwdLoading = ref(false)
const forcePwdForm = ref({ newPassword: '', confirmPassword: '' })
let smsTimer = null
let activeSmsPhone = ''

const smsButtonText = computed(() => {
  if (smsSending.value) return '发送中'
  if (smsCountdown.value > 0) return `${smsCountdown.value}s后重发`
  return '获取验证码'
})

const switchLoginMode = (mode) => {
  loginMode.value = mode
  if (mode === 'password' && captchaRequired.value && !captchaImage.value) loadCaptcha()
}

const stopSmsCountdown = () => {
  clearInterval(smsTimer)
  smsTimer = null
  smsCountdown.value = 0
  activeSmsPhone = ''
}

const startSmsCountdown = (seconds = 60, phone = form.value.phone, persist = true) => {
  const targetPhone = String(phone || '').trim()
  smsCountdown.value = Math.max(1, Number(seconds) || 60)
  activeSmsPhone = targetPhone
  if (persist && targetPhone) saveSmsCooldown('login', targetPhone, smsCountdown.value)
  clearInterval(smsTimer)
  smsTimer = setInterval(() => {
    smsCountdown.value -= 1
    if (smsCountdown.value <= 0) {
      clearSmsCooldown('login')
      stopSmsCountdown()
    }
  }, 1000)
}

const restoreSmsCooldown = (phone = form.value.phone) => {
  const state = getSmsCooldown('login', phone)
  if (!state) return false
  form.value.phone = state.phone
  loginMode.value = 'sms'
  startSmsCountdown(state.remainingSeconds, state.phone, false)
  return true
}

watch(() => form.value.phone, (phone) => {
  const normalized = String(phone || '').trim()
  if (smsCountdown.value > 0 && activeSmsPhone && normalized !== activeSmsPhone) {
    stopSmsCountdown()
  }
  if (loginMode.value === 'sms' && normalized) {
    restoreSmsCooldown(normalized)
  }
})

onMounted(() => {
  restoreSmsCooldown()
})

onBeforeUnmount(() => clearInterval(smsTimer))

const loadCaptcha = async () => {
  try {
    const res = await getLoginCaptcha()
    if (res?.code === 200) {
      form.value.captchaId = res.data?.captchaId || ''
      form.value.captchaCode = ''
      captchaImage.value = res.data?.image || ''
    }
  } catch (e) {
    captchaImage.value = ''
  }
}

const handleSendSms = async () => {
  const phone = form.value.phone.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    return ElMessage.warning('请输入正确的11位手机号')
  }
  smsSending.value = true
  try {
    const res = await sendSmsCode(phone, 'login')
    if (res?.code !== 200) {
      const seconds = smsCooldownFromError(res)
      if (seconds > 0) {
        startSmsCountdown(seconds, phone)
        return
      }
      return ElMessage.error(res?.msg || '验证码发送失败')
    }
    const devCode = res.data?.devCode
    if (devCode) {
      form.value.smsCode = devCode
      ElMessage.success(`开发验证码 ${devCode} 已自动填入`)
    } else {
      ElMessage.success('验证码已发送')
    }
    startSmsCountdown(res.data?.cooldownSeconds, phone)
  } catch (e) {
    const seconds = smsCooldownFromError(e)
    if (seconds > 0) startSmsCountdown(seconds, phone)
  } finally {
    smsSending.value = false
  }
}

const completeLogin = async (res, fallbackAccount) => {
  const data = res?.data || {}
  if (res?.code !== 200 || !data.accessToken) {
    ElMessage.error(res?.msg || '登录失败')
    return false
  }
  sessionStorage.removeItem('adminToken')
  sessionStorage.removeItem('adminUser')
  sessionStorage.setItem('token', data.accessToken)
  sessionStorage.setItem('isLogin', '1')
  sessionStorage.setItem('userAccount', data.username || fallbackAccount || '')
  sessionStorage.setItem('userNickname', data.nickname || '灵感用户')
  if (data.avatar) sessionStorage.setItem('userAvatar', data.avatar)

  const userId = data.userId || parseJwtUserId(data.accessToken)
  if (userId) sessionStorage.setItem('userId', String(userId))

  auth.setLoggedIn()
  if (data.passwordUpgradeRequired) {
    forcePasswordDialog.value = true
    ElMessage.warning('当前账号必须先升级密码')
    return false
  }
  ElMessage.success('登录成功')
  const redirect = sessionStorage.getItem('redirectPath')
  sessionStorage.removeItem('redirectPath')
  await router.replace(redirect || '/')
  return true
}

const handleForcePassword = async () => {
  const {newPassword, confirmPassword} = forcePwdForm.value
  if (newPassword !== confirmPassword) return ElMessage.warning('两次密码不一致')
  const error = validatePassword(newPassword, {
    username: form.value.account,
    email: ''
  })
  if (error) return ElMessage.warning(error)
  forcePwdLoading.value = true
  try {
    const res = await changePassword({
      oldPassword: form.value.password,
      newPassword
    })
    if (res.code === 200) {
      clearAllTokens()
      forcePasswordDialog.value = false
      forcePwdForm.value = {newPassword: '', confirmPassword: ''}
      form.value.password = ''
      ElMessage.success('密码已升级，请使用新密码重新登录')
    } else {
      ElMessage.error(res.msg || '密码修改失败')
    }
  } catch (e) {
    // 请求层已提示
  } finally {
    forcePwdLoading.value = false
  }
}

const handleLogin = async () => {
  if (loginMode.value === 'sms') {
    if (!/^1[3-9]\d{9}$/.test(form.value.phone.trim())) {
      return ElMessage.warning('请输入正确的11位手机号')
    }
    if (!/^\d{6}$/.test(form.value.smsCode.trim())) {
      return ElMessage.warning('请输入6位短信验证码')
    }
  } else {
    if (!form.value.account.trim()) return ElMessage.warning('请输入账号')
    if (!form.value.password.trim()) return ElMessage.warning('请输入密码')
    if (captchaRequired.value && !form.value.captchaCode.trim()) return ElMessage.warning('请输入验证码')
  }

  loading.value = true
  try {
    // 必须在发起登录前设置记住账号状态，login() 内部才能按约定持久化 AccessToken。
    setRememberMe(remember.value)

    const res = loginMode.value === 'sms'
      ? await loginBySms(form.value.phone.trim(), form.value.smsCode.trim())
      : await login({
          username: form.value.account,
          password: form.value.password,
          captchaId: captchaRequired.value ? form.value.captchaId : undefined,
          captchaCode: captchaRequired.value ? form.value.captchaCode : undefined
        })
    await completeLogin(res, loginMode.value === 'sms' ? form.value.phone.trim() : form.value.account)
  } catch (e) {
    // 错误已由 request.js 拦截器处理
    console.error('[登录异常]', e)
    const msg = e?.response?.data?.msg || ''
    if (loginMode.value === 'password' && !msg.includes('锁定')) {
      captchaRequired.value = true
      await loadCaptcha()
    }
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
.login-mode-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 4px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: #f2f8f6;
}
.login-mode-tabs button {
  height: 38px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: #6b7f7b;
  font-size: 14px;
  cursor: pointer;
}
.login-mode-tabs button.active {
  background: #ffffff;
  color: #0f766e;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(15, 118, 110, .1);
}
.input-group {
  margin-bottom: 18px;
}
.sms-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 108px;
  gap: 10px;
}
.sms-send-button {
  height: 46px;
  border: 1px solid #9fd8cd;
  border-radius: 12px;
  background: #eaf8f5;
  color: #0f766e;
  font-size: 13px;
  cursor: pointer;
}
.sms-send-button:disabled {
  cursor: default;
  opacity: .55;
}
.sms-tip {
  margin: -8px 0 14px;
  color: #7b8e8a;
  font-size: 12px;
}
.force-password-copy {
  margin-bottom: 18px;
  padding: 12px;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a5b16;
  font-size: 13px;
  line-height: 1.6;
}
.input-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}
.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 10px;
  align-items: center;
}
.captcha-image {
  width: 132px;
  height: 46px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 12px;
  background: #f4faf8;
  cursor: pointer;
}
.captcha-image img {
  width: 100%;
  height: 100%;
  display: block;
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
