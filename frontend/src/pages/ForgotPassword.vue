<template>
  <div class="forgot-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
        <div class="icon-item" @click="$router.push('/login')">👤</div>
      </div>
    </div>
    <div class="form-box">
      <h2 class="title">找回密码</h2>
      <p class="desc">输入注册邮箱，我们将发送重置链接</p>
      <div class="input-group"><el-input v-model="email" placeholder="请输入注册邮箱"></el-input></div>
      <el-button class="btn" type="primary" :loading="loading" @click="handleSubmit">发送重置邮件</el-button>
      <div class="tip" v-if="sent">重置链接已发送至您的邮箱，请查收（如未收到请查看垃圾邮件）</div>
      <div class="tip">已有账号？<span class="link" @click="$router.push('/login')">前往登录</span></div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { forgotPassword } from '@/api/inspire'
const email = ref(''); const loading = ref(false); const sent = ref(false)
const handleSubmit = async () => {
  if (!email.value) return ElMessage.warning('请输入邮箱')
  loading.value = true
  try {
    const res = await forgotPassword({ email: email.value })
    if (res.code === 200) sent.value = true
    else ElMessage.error(res.msg || '发送失败')
  } catch (e) {} finally { loading.value = false }
}
</script>
<style scoped>
.forgot-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 0 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.form-box { background:#fff; border-radius:20px; padding:36px 24px; margin-top:30px; }
.title { font-size:24px; font-weight:500; text-align:center; margin:0 0 8px; color:#1d1d1f; }
.desc { text-align:center; font-size:14px; color:#86868b; margin-bottom:32px; }
.input-group { margin-bottom:16px; }
.btn { width:100%; height:46px; border-radius:14px; font-size:16px; background:#409eff; border:none; }
.tip { text-align:center; margin-top:20px; font-size:14px; color:#86868b; }
.link { color:#409eff; cursor:pointer; }
</style>
