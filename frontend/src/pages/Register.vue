<template>
  <div class="register-page">
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
          <div class="icon-item user-icon" @click="$router.push('/login')">
            <svg viewBox="0 0 24 24" width="22" height="22">
              <path fill="#ffffff" d="M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-4 0-8 2-8 6v2h16v-2c0-4-4-6-8-6z"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="masthead">
        <div class="kicker">INSPIRE DAILY</div>
        <h1 class="main-title">注册灵感账号</h1>
        <p class="tagline">注册后可录入、收藏全部创意灵感</p>
      </div>
    </div>

    <!-- 注册表单卡 -->
    <div class="form-wrap">
      <div class="form-card">
        <div class="profile-preview">
          <div class="preview-avatar">{{ previewAvatar }}</div>
          <div class="preview-info">
            <div class="preview-nickname">{{ previewNickname }}</div>
            <div class="preview-label">随机头像 · 注册后可修改</div>
          </div>
          <div class="preview-refresh" @click="refreshPreview">🔄</div>
        </div>

        <div class="input-group">
          <label>账号</label>
          <el-input v-model="form.account" placeholder="请设置账号" clearable></el-input>
        </div>

        <div class="input-group">
          <label>邮箱</label>
          <el-input v-model="form.email" placeholder="请填写邮箱（用于找回密码）" clearable></el-input>
        </div>

        <div class="input-group">
          <label>密码</label>
          <el-input v-model="form.pwd" placeholder="请设置密码" show-password></el-input>
        </div>

        <div class="input-group">
          <label>确认密码</label>
          <el-input v-model="form.rePwd" placeholder="请确认密码" show-password></el-input>
        </div>

        <el-button class="register-btn" :loading="loading" @click="handleRegister">
          立即注册
        </el-button>

        <div class="login-tip">
          已有账号？
          <span class="login-link" @click="$router.push('/login')">前往登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from '@/utils/uiFeedback.js'
import { register } from '@/api/auth.js'
import { randomNickname } from '@/utils/nickname.js'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const form = ref({
  account: '',
  email: '',
  pwd: '',
  rePwd: '',
})

const avatarOptions = ['🐰','🐿','🦊','🐼','🐱','🦌','🐨','🐻','🦝','🐹','🦉','🐧']
const randomAvatarEmoji = () =>
  avatarOptions[Math.floor(Math.random() * avatarOptions.length)]

// 初始化随机头像昵称
const previewAvatar = ref(randomAvatarEmoji())
const previewNickname = ref(randomNickname())

// 刷新随机预览
const refreshPreview = () => {
  previewAvatar.value = randomAvatarEmoji()
  previewNickname.value = randomNickname()
}

const handleRegister = async () => {
  if (!form.value.account.trim()) return ElMessage.warning('请输入账号')
  if (!form.value.email.trim()) return ElMessage.warning('请输入邮箱')
  if (!form.value.pwd.trim()) return ElMessage.warning('请输入密码')
  if (form.value.pwd !== form.value.rePwd) return ElMessage.warning('两次密码不一致')
  if (form.value.pwd.length < 6 || form.value.pwd.length > 16) {
    return ElMessage.warning('密码长度需为6-16位')
  }

  loading.value = true
  try {
    const res = await register({
      username: form.value.account,
      email: form.value.email,
      password: form.value.pwd,
      confirmPassword: form.value.rePwd,
      avatar: previewAvatar.value,
      nickname: previewNickname.value,
    })

    if (res.code === 200) {
      const data = res.data || {}
      sessionStorage.setItem('token', data.accessToken || '')
      sessionStorage.setItem('isLogin', '1')
      sessionStorage.setItem('userAccount', data.username || form.value.account)
      sessionStorage.setItem('userNickname', data.nickname || previewNickname.value)
      sessionStorage.setItem('userAvatar', data.avatar || previewAvatar.value)

      if (data.accessToken) {
        const payload = JSON.parse(atob(data.accessToken.split('.')[1]))
        sessionStorage.setItem('userId', payload.sub)
      }

      auth.setLoggedIn()
      ElMessage.success('注册成功')
      router.push('/')
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (e) {
    // 错误已拦截
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

/* ========== 表单区 ========== */
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

/* 头像预览 */
.profile-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fdfa;
  margin-bottom: 18px;
}
.preview-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #a8dcd2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 28px;
  font-weight: bold;
}
.preview-info {
  flex: 1;
}
.preview-nickname {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 2px;
}
.preview-label {
  font-size: 12px;
  color: #6b7280;
}
.preview-refresh {
  padding: 6px 8px;
  border-radius: 8px;
  background: #e6f4f0;
  cursor: pointer;
  transition: background .2s;
}
.preview-refresh:hover {
  background: #d1fae5;
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

.register-btn {
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
.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 118, 110,.32);
}

.login-tip {
  text-align: center;
  margin-top: 24px;
  font-size: 15px;
  color: #6b7280;
}
.login-link {
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
  .profile-preview {
    padding: 10px 8px;
  }
}
</style>
