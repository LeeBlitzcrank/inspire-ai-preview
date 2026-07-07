<template>
  <div class="admin-login">
    <div class="login-card">
      <h1>灵思集 管理后台</h1>
      <p class="sub">运营管理系统</p>
      <el-input v-model="username" placeholder="管理员账号" size="large" class="field" />
      <el-input v-model="password" type="password" placeholder="密码" size="large" class="field" show-password />
      <el-button type="primary" size="large" :loading="loading" @click="doLogin" class="btn">登录</el-button>
      <el-button text @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminLogin } from '@/api/inspire.js'
import { useRouter } from 'vue-router'
const router = useRouter()
const username = ref(''); const password = ref(''); const loading = ref(false)
const doLogin = async () => {
  if (!username.value) return ElMessage.warning('请输入账号')
  if (!password.value) return ElMessage.warning('请输入密码')
  loading.value = true
  try {
    const res = await adminLogin({ username: username.value, password: password.value })
    if (res.code === 200) {
      localStorage.setItem('adminToken', res.data.token)
      localStorage.setItem('adminUser', res.data.username)
      ElMessage.success('登录成功')
      router.push('/admin/dashboard')
    } else ElMessage.error(res.msg || '登录失败')
  } catch (e) {} finally { loading.value = false }
}
</script>
<style scoped>
.admin-login { height:100vh; display:flex; align-items:center; justify-content:center; background:#f0f3f9; }
.login-card { background:#fff; border-radius:20px; padding:40px; width:380px; text-align:center; }
h1 { margin:0 0 4px; font-size:22px; }
.sub { color:#86868b; margin-bottom:28px; font-size:14px; }
.field { margin-bottom:16px; }
.btn { width:100%; height:44px; border-radius:12px; margin-bottom:12px; }
</style>
