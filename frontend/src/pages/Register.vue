<template>
  <div class="register-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/login')">👤</div>
      </div>
    </div>
    <div class="form-box">
      <h2 class="title">注册灵感账号</h2>
      <p class="desc">注册后可录入、收藏全部创意灵感</p>
      <div class="input-group"><el-input v-model="form.account" placeholder="请设置账号"></el-input></div>
      <div class="input-group"><el-input v-model="form.email" placeholder="请填写邮箱（用于找回密码）"></el-input></div>
      <div class="input-group"><el-input v-model="form.pwd" placeholder="请设置密码" show-password></el-input></div>
      <div class="input-group"><el-input v-model="form.rePwd" placeholder="请确认密码" show-password></el-input></div>
      <el-button class="reg-btn" type="primary" :loading="loading" @click="handleRegister">立即注册</el-button>
      <div class="tip">已有账号？<span class="link" @click="$router.push('/login')">前往登录</span></div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/inspire'
const router = useRouter()
const loading = ref(false)
const form = ref({ account: '', email: '', pwd: '', rePwd: '' })
const handleRegister = async () => {
  const { account, email, pwd, rePwd } = form.value
  if (!account) return ElMessage.warning('请输入账号')
  if (!email) return ElMessage.warning('请输入邮箱')
  if (!pwd) return ElMessage.warning('请输入密码')
  if (pwd !== rePwd) return ElMessage.warning('两次密码不一致')
  if (pwd.length < 6 || pwd.length > 16) return ElMessage.warning('密码长度6-16位')
  loading.value = true
  try {
    const res = await register({ username: account, password: pwd, confirmPassword: rePwd, email })
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('isLogin', '1')
      localStorage.setItem('userAccount', res.data.username)
      localStorage.setItem('userId', res.data.userId)
      ElMessage.success('注册成功，自动登录')
      router.push('/')
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (e) {} finally { loading.value = false }
}
</script>
<style scoped>
.register-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 0 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.form-box { background:#fff; border-radius:20px; padding:36px 24px; margin-top:30px; }
.title { font-size:24px; font-weight:500; text-align:center; margin:0 0 8px; color:#1d1d1f; }
.desc { text-align:center; font-size:14px; color:#86868b; margin-bottom:32px; }
.input-group { margin-bottom:16px; }
.reg-btn { width:100%; height:46px; border-radius:14px; font-size:16px; background:#409eff; border:none; }
.tip { text-align:center; margin-top:24px; font-size:14px; color:#86868b; }
.link { color:#409eff; cursor:pointer; }
</style>
