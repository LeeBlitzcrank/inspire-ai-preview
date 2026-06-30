<template>
  <div class="err-page"><div class="err-box">
    <div class="err-code">500</div>
    <div class="err-text">服务异常</div>
    <p class="err-desc">服务器暂时无法处理你的请求，请稍后重试</p>
    <el-button type="primary" @click="$router.push('/')">回到首页</el-button>
    <el-button @click="$router.go(0)">重新加载</el-button>
  </div></div>
</template>
<style scoped>
.err-page { height:100vh; display:flex; align-items:center; justify-content:center; background:#fbfcfe; }
.err-box { text-align:center; }
.err-code { font-size:80px; font-weight:700; color:#f56c6c; line-height:1; margin-bottom:16px; }
.err-text { font-size:24px; color:#1d1d1f; margin-bottom:8px; }
.err-desc { color:#86868b; margin-bottom:32px; }
</style>
