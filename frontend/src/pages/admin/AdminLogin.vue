<template>
  <div class="admin-login">
    <div class="login-card">
      <h1>灵思集 管理后台</h1>
      <p class="sub">运营管理系统</p>
      <template v-if="!mfaRequired">
        <el-input v-model="username" placeholder="管理员账号" size="large" class="field" />
        <el-input v-model="password" type="password" placeholder="密码" size="large" class="field" show-password />
      </template>
      <template v-else>
        <div class="mfa-tip">
          <template v-if="mfaSetupRequired">
            首次登录需要绑定身份验证器
          </template>
          <template v-else>
            请输入身份验证器中的 6 位动态验证码
          </template>
        </div>
        <div v-if="mfaSetupRequired" class="mfa-setup">
          <img v-if="qrImage" :src="qrImage" alt="TOTP 二维码" class="qr-image">
          <div class="secret-row">
            <span>手动密钥</span>
            <code>{{ totpSecret }}</code>
          </div>
        </div>
        <el-input
          v-model="mfaCode"
          placeholder="请输入 6 位动态验证码"
          size="large"
          class="field"
          maxlength="6"
          @keyup.enter="doLogin"
        />
      </template>
      <el-button type="primary" size="large" :loading="loading" @click="doLogin" class="btn">
        {{ mfaRequired ? '验证并进入后台' : '登录' }}
      </el-button>
      <el-button v-if="mfaRequired" text @click="resetMfa">返回账号登录</el-button>
      <el-button text @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>
<script setup>
import {ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {adminLogin} from '@/api/inspire.js'
import {useRouter} from 'vue-router'
import QRCode from 'qrcode'

const router = useRouter()
const username = ref('')
const password = ref('')
const mfaCode = ref('')
const loading = ref(false)
const mfaRequired = ref(false)
const mfaSetupRequired = ref(false)
const totpSecret = ref('')
const qrImage = ref('')
const doLogin = async () => {
  if (!mfaRequired.value) {
    if (!username.value) return ElMessage.warning('请输入账号')
    if (!password.value) return ElMessage.warning('请输入密码')
  } else if (!mfaCode.value.trim()) {
    return ElMessage.warning('请输入动态验证码')
  }
  loading.value = true
  try {
    const res = await adminLogin({
      username: username.value,
      password: password.value,
      mfaCode: mfaRequired.value ? mfaCode.value.trim() : undefined
    })
    if (res.code === 200) {
      if (res.data?.token) {
        sessionStorage.setItem('adminToken', res.data.token)
        sessionStorage.setItem('adminUser', res.data.username)
        ElMessage.success('登录成功')
        router.push('/admin/dashboard')
      } else if (res.data?.mfaRequired) {
        mfaRequired.value = true
        mfaSetupRequired.value = Boolean(res.data.mfaSetupRequired)
        totpSecret.value = res.data.totpSecret || ''
        qrImage.value = res.data.otpauthUri
          ? await QRCode.toDataURL(res.data.otpauthUri, {width: 188, margin: 1})
          : ''
      }
    } else ElMessage.error(res.msg || '登录失败')
  } catch (e) { console.error(e) } finally { loading.value = false }
}
const resetMfa = () => {
  mfaRequired.value = false
  mfaSetupRequired.value = false
  mfaCode.value = ''
  totpSecret.value = ''
  qrImage.value = ''
}
</script>
<style scoped>
.admin-login { height:100vh; display:flex; align-items:center; justify-content:center; background:#f0f3f9; }
.login-card { background:#fff; border-radius:20px; padding:40px; width:380px; text-align:center; }
h1 { margin:0 0 4px; font-size:22px; }
.sub { color:#86868b; margin-bottom:28px; font-size:14px; }
.field { margin-bottom:16px; }
.btn { width:100%; height:44px; border-radius:12px; margin-bottom:12px; }
.mfa-tip { color:#606266; font-size:14px; line-height:1.6; margin-bottom:16px; }
.mfa-setup { display:flex; flex-direction:column; align-items:center; gap:12px; margin-bottom:16px; }
.qr-image { width:188px; height:188px; border-radius:12px; border:1px solid #e4e7ed; }
.secret-row { display:flex; flex-direction:column; gap:4px; width:100%; color:#909399; font-size:12px; }
.secret-row code { color:#303133; font-size:12px; word-break:break-all; user-select:all; }
</style>
